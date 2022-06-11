/*
 * This file is part of adventure-platform, licensed under the MIT License.
 *
 * Copyright (c) 2018-2020 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.kyori.adventure.platform.bukkit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetBase;
import net.kyori.adventure.platform.facet.FacetComponentFlattener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodType.methodType;
import static net.kyori.adventure.platform.bukkit.BukkitComponentSerializer.gson;
import static net.kyori.adventure.platform.bukkit.BukkitComponentSerializer.legacy;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findConstructor;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findCraftClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findEnum;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findField;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findMcClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findMcClassName;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findMethod;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findNmsClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findNmsClassName;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findSetterOf;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findStaticMethod;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.lookup;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.needClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.needField;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.searchMethod;
import static net.kyori.adventure.platform.facet.Knob.isEnabled;
import static net.kyori.adventure.platform.facet.Knob.logError;

class CraftBukkitFacet<V extends CommandSender> extends FacetBase<V> {
  protected CraftBukkitFacet(final @Nullable Class<? extends V> viewerClass) {
    super(viewerClass);
  }

  @Override
  public boolean isSupported() {
    return super.isSupported() && SUPPORTED;
  }

  private static final Class<?> CLASS_NMS_ENTITY = findClass(
    findNmsClassName("Entity"),
    findMcClassName("world.entity.Entity")
  );
  private static final Class<?> CLASS_CRAFT_ENTITY = findCraftClass("entity.CraftEntity");
  private static final MethodHandle CRAFT_ENTITY_GET_HANDLE = findMethod(CLASS_CRAFT_ENTITY, "getHandle", CLASS_NMS_ENTITY);
  static final @Nullable Class<? extends Player> CLASS_CRAFT_PLAYER = findCraftClass("entity.CraftPlayer", Player.class);
  private static final @Nullable MethodHandle CRAFT_PLAYER_GET_HANDLE;
  private static final @Nullable MethodHandle ENTITY_PLAYER_GET_CONNECTION;
  private static final @Nullable MethodHandle PLAYER_CONNECTION_SEND_PACKET;

  static {
    final Class<?> craftPlayerClass = findCraftClass("entity.CraftPlayer");
    final Class<?> packetClass = findClass(
      findNmsClassName("Packet"),
      findMcClassName("network.protocol.Packet")
    );

    MethodHandle craftPlayerGetHandle = null;
    MethodHandle entityPlayerGetConnection = null;
    MethodHandle playerConnectionSendPacket = null;
    if (craftPlayerClass != null && packetClass != null) {
      try {
        final Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
        final Class<?> entityPlayerClass = getHandleMethod.getReturnType();
        craftPlayerGetHandle = lookup().unreflect(getHandleMethod);
        final Field playerConnectionField = findField(entityPlayerClass, "playerConnection", "connection");
        Class<?> playerConnectionClass = null;
        if (playerConnectionField != null) { // fields are named
          entityPlayerGetConnection = lookup().unreflectGetter(playerConnectionField);
          playerConnectionClass = playerConnectionField.getType();
        } else { // fields are obf, let's discover the field by type
          final Class<?> serverGamePacketListenerImpl = findClass(
            findNmsClassName("PlayerConnection"),
            findMcClassName("server.network.PlayerConnection"),
            findMcClassName("server.network.ServerGamePacketListenerImpl")
          );
          for (final Field field : entityPlayerClass.getDeclaredFields()) {
            final int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) && !Modifier.isFinal(modifiers)) {
              if (serverGamePacketListenerImpl == null || field.getType().equals(serverGamePacketListenerImpl)) {
                entityPlayerGetConnection = lookup().unreflectGetter(field);
                playerConnectionClass = field.getType();
              }
            }
          }
        }
        playerConnectionSendPacket = searchMethod(playerConnectionClass, Modifier.PUBLIC, new String[]{"sendPacket", "send"}, void.class, packetClass);
      } catch (final Throwable error) {
        logError(error, "Failed to initialize CraftBukkit sendPacket");
      }
    }

    CRAFT_PLAYER_GET_HANDLE = craftPlayerGetHandle;
    ENTITY_PLAYER_GET_CONNECTION = entityPlayerGetConnection;
    PLAYER_CONNECTION_SEND_PACKET = playerConnectionSendPacket;
  }

  private static final boolean SUPPORTED = isEnabled("craftbukkit", true)
    && MinecraftComponentSerializer.isSupported()
    && CRAFT_PLAYER_GET_HANDLE != null && ENTITY_PLAYER_GET_CONNECTION != null && PLAYER_CONNECTION_SEND_PACKET != null;

  static class PacketFacet<V extends CommandSender> extends CraftBukkitFacet<V> implements Facet.Message<V, Object> {
    @SuppressWarnings("unchecked")
    protected PacketFacet() {
      super((Class<V>) CLASS_CRAFT_PLAYER);
    }

    public void sendPacket(final @NotNull Player player, final @Nullable Object packet) {
      if (packet == null) return;

      try {
        PLAYER_CONNECTION_SEND_PACKET.invoke(ENTITY_PLAYER_GET_CONNECTION.invoke(CRAFT_PLAYER_GET_HANDLE.invoke(player)), packet);
      } catch (final Throwable error) {
        logError(error, "Failed to invoke CraftBukkit sendPacket: %s", packet);
      }
    }

    public void sendMessage(final @NotNull V player, final @Nullable Object packet) {
      this.sendPacket((Player) player, packet);
    }

    @Nullable
    @Override
    public Object createMessage(final @NotNull V viewer, final @NotNull Component message) {
      try {
        return MinecraftComponentSerializer.get().serialize(message);
      } catch (final Throwable error) {
        logError(error, "Failed to serialize net.minecraft.server IChatBaseComponent: %s", message);
        return null;
      }
    }
  }

  private static final @Nullable Class<?> CLASS_CHAT_COMPONENT = findClass(
    findNmsClassName("IChatBaseComponent"),
    findMcClassName("network.chat.IChatBaseComponent"),
    findMcClassName("network.chat.Component")
  );
  private static final @Nullable Class<?> CLASS_MESSAGE_TYPE = findClass(
    findNmsClassName("ChatMessageType"),
    findMcClassName("network.chat.ChatMessageType"),
    findMcClassName("network.chat.ChatType")
  );
  private static final @Nullable Object MESSAGE_TYPE_CHAT;
  private static final @Nullable Object MESSAGE_TYPE_SYSTEM;
  private static final @Nullable Object MESSAGE_TYPE_ACTIONBAR;

  static {
    if (CLASS_MESSAGE_TYPE != null && !CLASS_MESSAGE_TYPE.isEnum()) {
      MESSAGE_TYPE_CHAT = 0;
      MESSAGE_TYPE_SYSTEM = 1;
      MESSAGE_TYPE_ACTIONBAR = 2;
    } else {
      MESSAGE_TYPE_CHAT = findEnum(CLASS_MESSAGE_TYPE, "CHAT", 0);
      MESSAGE_TYPE_SYSTEM = findEnum(CLASS_MESSAGE_TYPE, "SYSTEM", 1);
      MESSAGE_TYPE_ACTIONBAR = findEnum(CLASS_MESSAGE_TYPE, "GAME_INFO", 2);
    }
  }

  private static final @Nullable MethodHandle LEGACY_CHAT_PACKET_CONSTRUCTOR; // (IChatBaseComponent, byte)
  private static final @Nullable MethodHandle CHAT_PACKET_CONSTRUCTOR; // (ChatMessageType, IChatBaseComponent, UUID) -> PacketPlayOutChat

  static {
    MethodHandle legacyChatPacketConstructor = null;
    MethodHandle chatPacketConstructor = null;

    try {
      if (CLASS_CHAT_COMPONENT != null) {
        final Class<?> chatPacketClass = needClass(
          findNmsClassName("PacketPlayOutChat"),
          findMcClassName("network.protocol.game.PacketPlayOutChat"),
          findMcClassName("network.protocol.game.ClientboundChatPacket"),
          findMcClassName("network.protocol.game.ClientboundSystemChatPacket")
        );
        // ClientboundSystemChatPacket constructor changed for 1.19
        chatPacketConstructor = findConstructor(chatPacketClass, CLASS_CHAT_COMPONENT, int.class);
        if (chatPacketConstructor == null) {
          // ClientboundChatPacket constructor changed for 1.16
          chatPacketConstructor = findConstructor(chatPacketClass, CLASS_CHAT_COMPONENT);
        }
        if (chatPacketConstructor == null) {
          if (CLASS_MESSAGE_TYPE != null) {
            chatPacketConstructor = findConstructor(chatPacketClass, CLASS_CHAT_COMPONENT, CLASS_MESSAGE_TYPE, UUID.class);
          }
        } else {
          if (MESSAGE_TYPE_CHAT == Integer.valueOf(0)) {
            // for 1.19, create a function that drops the last UUID argument while keeping the integer message type argument
            chatPacketConstructor = dropArguments(chatPacketConstructor, 2, UUID.class);
          } else {
            // Create a function that ignores the message type and sender id arguments to call the underlying one-argument constructor
            chatPacketConstructor = dropArguments(chatPacketConstructor, 1, CLASS_MESSAGE_TYPE == null ? Object.class : CLASS_MESSAGE_TYPE, UUID.class);
          }
        }
        legacyChatPacketConstructor = findConstructor(chatPacketClass, CLASS_CHAT_COMPONENT, byte.class);
        if (legacyChatPacketConstructor == null) { // 1.7 paper protocol hack?
          legacyChatPacketConstructor = findConstructor(chatPacketClass, CLASS_CHAT_COMPONENT, int.class);
        }
      }
    } catch (final Throwable error) {
      logError(error, "Failed to initialize ClientboundChatPacket constructor");
    }

    CHAT_PACKET_CONSTRUCTOR = chatPacketConstructor;
    LEGACY_CHAT_PACKET_CONSTRUCTOR = legacyChatPacketConstructor;
  }

  static class Chat extends PacketFacet<CommandSender> implements Facet.Chat<CommandSender, Object> {
    @Override
    public boolean isSupported() {
      return super.isSupported() && CHAT_PACKET_CONSTRUCTOR != null;
    }

    @Override
    public void sendMessage(final @NotNull CommandSender viewer, final @NotNull Identity source, final @NotNull Object message, final @NotNull MessageType type) {
      final Object messageType = type == MessageType.CHAT ? MESSAGE_TYPE_CHAT : MESSAGE_TYPE_SYSTEM;
      try {
        this.sendMessage(viewer, CHAT_PACKET_CONSTRUCTOR.invoke(message, messageType, source.uuid()));
      } catch (final Throwable error) {
        logError(error, "Failed to invoke PacketPlayOutChat constructor: %s %s", message, messageType);
      }
    }
  }

  private static final @Nullable Class<?> CLASS_TITLE_PACKET = findClass(
    findNmsClassName("PacketPlayOutTitle"),
    findMcClassName("network.protocol.game.PacketPlayOutTitle")
  );
  private static final @Nullable Class<?> CLASS_TITLE_ACTION = findClass(
    findNmsClassName("PacketPlayOutTitle$EnumTitleAction"), // welcome to spigot, where we can't name classes? i guess?
    findMcClassName("network.protocol.game.PacketPlayOutTitle$EnumTitleAction")
  );
  private static final MethodHandle CONSTRUCTOR_TITLE_MESSAGE = findConstructor(CLASS_TITLE_PACKET, CLASS_TITLE_ACTION, CLASS_CHAT_COMPONENT); // (EnumTitleAction, IChatBaseComponent)
  private static final @Nullable MethodHandle CONSTRUCTOR_TITLE_TIMES = findConstructor(CLASS_TITLE_PACKET, int.class, int.class, int.class);
  private static final @Nullable Object TITLE_ACTION_TITLE = findEnum(CLASS_TITLE_ACTION, "TITLE", 0);
  private static final @Nullable Object TITLE_ACTION_SUBTITLE = findEnum(CLASS_TITLE_ACTION, "SUBTITLE", 1);
  private static final @Nullable Object TITLE_ACTION_ACTIONBAR = findEnum(CLASS_TITLE_ACTION, "ACTIONBAR");
  private static final @Nullable Object TITLE_ACTION_CLEAR = findEnum(CLASS_TITLE_ACTION, "CLEAR");
  private static final @Nullable Object TITLE_ACTION_RESET = findEnum(CLASS_TITLE_ACTION, "RESET");

  static class ActionBar_1_17 extends PacketFacet<Player> implements Facet.ActionBar<Player, Object> {
    private static final @Nullable Class<?> CLASS_SET_ACTION_BAR_TEXT_PACKET = findMcClass("network.protocol.game.ClientboundSetActionBarTextPacket");
    private static final @Nullable MethodHandle CONSTRUCTOR_ACTION_BAR = findConstructor(CLASS_SET_ACTION_BAR_TEXT_PACKET, CLASS_CHAT_COMPONENT);

    @Override
    public boolean isSupported() {
      return super.isSupported() && CONSTRUCTOR_ACTION_BAR != null;
    }

    @Nullable
    @Override
    public Object createMessage(final @NotNull Player viewer, final @NotNull Component message) {
      try {
        return CONSTRUCTOR_ACTION_BAR.invoke(super.createMessage(viewer, message));
      } catch (final Throwable error) {
        logError(error, "Failed to invoke PacketPlayOutTitle constructor: %s", message);
        return null;
      }
    }
  }

  static class ActionBar extends PacketFacet<Player> implements Facet.ActionBar<Player, Object> {
    @Override
    public boolean isSupported() {
      return super.isSupported() && TITLE_ACTION_ACTIONBAR != null;
    }

    @Nullable
    @Override
    public Object createMessage(final @NotNull Player viewer, final @NotNull Component message) {
      try {
        return CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_ACTIONBAR, super.createMessage(viewer, message));
      } catch (final Throwable error) {
        logError(error, "Failed to invoke PacketPlayOutTitle constructor: %s", message);
        return null;
      }
    }
  }

  static class ActionBarLegacy extends PacketFacet<Player> implements Facet.ActionBar<Player, Object> {
    @Override
    public boolean isSupported() {
      return super.isSupported() && LEGACY_CHAT_PACKET_CONSTRUCTOR != null;
    }

    @Nullable
    @Override
    public Object createMessage(final @NotNull Player viewer, final @NotNull Component message) {
      // Due to a Minecraft client bug, Action bars through the chat packet don't properly support formatting
      final TextComponent legacyMessage = Component.text(legacy().serialize(message));
      try {
        return LEGACY_CHAT_PACKET_CONSTRUCTOR.invoke(super.createMessage(viewer, legacyMessage), (byte) 2);
      } catch (final Throwable error) {
        logError(error, "Failed to invoke PacketPlayOutChat constructor: %s", legacyMessage);
        return null;
      }
    }
  }

  static class EntitySound extends PacketFacet<Player> implements Facet.EntitySound<Player, Object> {
    private static final Class<?> CLASS_CLIENTBOUND_ENTITY_SOUND = findClass(
      findNmsClassName("PacketPlayOutEntitySound"),
      findMcClassName("network.protocol.game.PacketPlayOutEntitySound"),
      findMcClassName("network.protocol.game.ClientboundEntitySoundPacket")
    );
    private static final Class<?> CLASS_CLIENTBOUND_CUSTOM_SOUND = findClass(
      findNmsClassName("PacketPlayOutCustomSoundEffect"),
      findMcClassName("network.protocol.game.ClientboundCustomSoundPacket"),
      findMcClassName("network.protocol.game.PacketPlayOutCustomSoundEffect")
    );
    private static final Class<?> CLASS_REGISTRY = findClass(
      findNmsClassName("IRegistry"),
      findMcClassName("core.IRegistry"),
      findMcClassName("core.Registry")
    );
    private static final Class<?> CLASS_RESOURCE_LOCATION = findClass(
      findNmsClassName("MinecraftKey"),
      findMcClassName("resources.MinecraftKey"),
      findMcClassName("resources.ResourceLocation")
    );
    private static final Class<?> CLASS_WRITABLE_REGISTRY = findClass(
      findNmsClassName("IRegistryWritable"),
      findMcClassName("core.IRegistryWritable"),
      findMcClassName("core.WritableRegistry")
    );
    private static final Class<?> CLASS_SOUND_EFFECT = findClass(
      findNmsClassName("SoundEffect"),
      findMcClassName("sounds.SoundEffect"),
      findMcClassName("sounds.SoundEvent")
    );
    private static final Class<?> CLASS_SOUND_SOURCE = findClass(
      findNmsClassName("SoundCategory"),
      findMcClassName("sounds.SoundCategory"),
      findMcClassName("sounds.SoundSource")
    );
    private static final Class<?> CLASS_VEC3 = findClass(
      findNmsClassName("Vec3D"),
      findMcClassName("world.phys.Vec3D"),
      findMcClassName("world.phys.Vec3")
    );

    private static final MethodHandle NEW_CLIENTBOUND_ENTITY_SOUND = findConstructor(CLASS_CLIENTBOUND_ENTITY_SOUND, CLASS_SOUND_EFFECT, CLASS_SOUND_SOURCE, CLASS_NMS_ENTITY, float.class, float.class);
    private static final MethodHandle NEW_CLIENTBOUND_CUSTOM_SOUND = findConstructor(CLASS_CLIENTBOUND_CUSTOM_SOUND, CLASS_RESOURCE_LOCATION, CLASS_SOUND_SOURCE, CLASS_VEC3, float.class, float.class);
    private static final MethodHandle NEW_VEC3 = findConstructor(CLASS_VEC3, double.class, double.class, double.class);
    private static final MethodHandle NEW_RESOURCE_LOCATION = findConstructor(CLASS_RESOURCE_LOCATION, String.class, String.class);
    private static final MethodHandle REGISTRY_GET_OPTIONAL = searchMethod(CLASS_REGISTRY, Modifier.PUBLIC, "getOptional", Optional.class, CLASS_RESOURCE_LOCATION);
    private static final MethodHandle SOUND_SOURCE_GET_NAME;
    private static final Object REGISTRY_SOUND_EVENT;

    static {
      Object registrySoundEvent = null;
      MethodHandle soundSourceGetName = null;
      if (CLASS_SOUND_SOURCE != null) {
        for (final Method method : CLASS_SOUND_SOURCE.getDeclaredMethods()) {
          if (
            method.getReturnType().equals(String.class)
              && method.getParameterCount() == 0
              && !"name".equals(method.getName())
              && Modifier.isPublic(method.getModifiers())
          ) {
            try {
              soundSourceGetName = lookup().unreflect(method);
            } catch (final IllegalAccessException ex) {
              // ignored, getName is public
            }
            break;
          }
        }
      }
      if (CLASS_REGISTRY != null) {
        // we don't have always field names, so:
        // first: try to find SOUND_EVENT field
        try {
          final Field soundEventField = findField(CLASS_REGISTRY, "SOUND_EVENT");
          if (soundEventField != null) {
            registrySoundEvent = soundEventField.get(null);
          } else {
            // then, if not found:
            // iterate through fields, find the `protected static final` field
            // it's IRegistryWritable, the registry root
            // then we'll getOptional(NEW_RESOURCE_LOCATION.invoke("minecraft", "sound_event"))
            Object rootRegistry = null;
            for (final Field field : CLASS_REGISTRY.getDeclaredFields()) {
              final int mask = Modifier.PROTECTED | Modifier.STATIC | Modifier.FINAL;
              if ((field.getModifiers() & mask) == mask
                && field.getType().equals(CLASS_WRITABLE_REGISTRY)) {
                // we've found the root registry
                field.setAccessible(true);
                rootRegistry = field.get(null);
                break;
              }
            }

            if (rootRegistry != null) {
              registrySoundEvent = ((Optional<?>) REGISTRY_GET_OPTIONAL.invoke(rootRegistry, NEW_RESOURCE_LOCATION.invoke("minecraft", "sound_event"))).orElse(null);
            }
          }
        } catch (final Throwable thr) {
          logError(thr, "Failed to initialize EntitySound CraftBukkit facet");
        }
      }
      REGISTRY_SOUND_EVENT = registrySoundEvent;
      SOUND_SOURCE_GET_NAME = soundSourceGetName;
    }

    private static final Map<String, Object> MC_SOUND_SOURCE_BY_NAME = new ConcurrentHashMap<>();

    @Override
    public boolean isSupported() {
      return super.isSupported() && NEW_CLIENTBOUND_ENTITY_SOUND != null && NEW_RESOURCE_LOCATION != null && REGISTRY_SOUND_EVENT != null && REGISTRY_GET_OPTIONAL != null && CRAFT_ENTITY_GET_HANDLE != null && SOUND_SOURCE_GET_NAME != null;
    }

    @Override
    public Object createForSelf(final Player viewer, final net.kyori.adventure.sound.@NotNull Sound sound) {
      return this.createForEntity(sound, viewer);
    }

    @Override
    public Object createForEmitter(final net.kyori.adventure.sound.@NotNull Sound sound, final net.kyori.adventure.sound.Sound.@NotNull Emitter emitter) {
      final Entity entity;
      if (emitter instanceof BukkitEmitter) {
        entity = ((BukkitEmitter) emitter).entity;
      } else if (emitter instanceof Entity) { // how? but just in case
        entity = (Entity) emitter;
      } else {
        return null;
      }
      return this.createForEntity(sound, entity);
    }

    private Object createForEntity(final net.kyori.adventure.sound.Sound sound, final Entity entity) {
      try {
        final Object nmsEntity = this.toNativeEntity(entity);
        if (nmsEntity == null) return null;

        final Object soundCategory = this.toVanilla(sound.source());
        if (soundCategory == null) return null;
        final Object nameRl = NEW_RESOURCE_LOCATION.invoke(sound.name().namespace(), sound.name().value());
        final java.util.Optional<?> event = (Optional<?>) REGISTRY_GET_OPTIONAL.invoke(REGISTRY_SOUND_EVENT, nameRl);
        if (event.isPresent()) {
          return NEW_CLIENTBOUND_ENTITY_SOUND.invoke(event.get(), soundCategory, nmsEntity, sound.volume(), sound.pitch());
        } else if (NEW_CLIENTBOUND_CUSTOM_SOUND != null && NEW_VEC3 != null) {
          final Location loc = entity.getLocation();
          return NEW_CLIENTBOUND_CUSTOM_SOUND.invoke(nameRl, soundCategory, NEW_VEC3.invoke(loc.getX(), loc.getY(), loc.getZ()), sound.volume(), sound.pitch());
        }
      } catch (final Throwable error) {
        logError(error, "Failed to send sound tracking an entity");
      }
      return null;
    }

    private Object toNativeEntity(final Entity entity) throws Throwable {
      if (!CLASS_CRAFT_ENTITY.isInstance(entity)) return null;

      return CRAFT_ENTITY_GET_HANDLE.invoke(entity);
    }

    private Object toVanilla(final net.kyori.adventure.sound.Sound.Source source) throws Throwable {
      if (MC_SOUND_SOURCE_BY_NAME.isEmpty()) {
        for (final Object enumConstant : CLASS_SOUND_SOURCE.getEnumConstants()) {
          MC_SOUND_SOURCE_BY_NAME.put((String) SOUND_SOURCE_GET_NAME.invoke(enumConstant), enumConstant);
        }
      }

      return MC_SOUND_SOURCE_BY_NAME.get(net.kyori.adventure.sound.Sound.Source.NAMES.key(source));
    }

    @Override
    public void playSound(final @NotNull Player viewer, final Object message) {
      this.sendPacket(viewer, message);
    }
  }

  static class Title_1_17 extends PacketFacet<Player> implements Facet.Title<Player, Object, List<Object>, List<?>> {

    private static final Class<?> PACKET_SET_TITLE = findMcClass("network.protocol.game.ClientboundSetTitleTextPacket");
    private static final Class<?> PACKET_SET_SUBTITLE = findMcClass("network.protocol.game.ClientboundSetSubtitleTextPacket");
    private static final Class<?> PACKET_SET_TITLE_ANIMATION = findMcClass("network.protocol.game.ClientboundSetTitlesAnimationPacket");
    private static final Class<?> PACKET_CLEAR_TITLES = findMcClass("network.protocol.game.ClientboundClearTitlesPacket");

    private static final MethodHandle CONSTRUCTOR_SET_TITLE = findConstructor(PACKET_SET_TITLE, CLASS_CHAT_COMPONENT);
    private static final MethodHandle CONSTRUCTOR_SET_SUBTITLE = findConstructor(PACKET_SET_SUBTITLE, CLASS_CHAT_COMPONENT);
    private static final MethodHandle CONSTRUCTOR_SET_TITLE_ANIMATION = findConstructor(PACKET_SET_TITLE_ANIMATION, int.class, int.class, int.class);
    private static final MethodHandle CONSTRUCTOR_CLEAR_TITLES = findConstructor(PACKET_CLEAR_TITLES, boolean.class);

    @Override
    public boolean isSupported() {
      return super.isSupported() && CONSTRUCTOR_SET_TITLE != null && CONSTRUCTOR_SET_SUBTITLE != null && CONSTRUCTOR_SET_TITLE_ANIMATION != null && CONSTRUCTOR_CLEAR_TITLES != null;
    }

    @Override
    public @NotNull List<Object> createTitleCollection() {
      return new ArrayList<>();
    }

    @Override
    public void contributeTitle(final @NotNull List<Object> coll, final @NotNull Object title) {
      try {
        coll.add(CONSTRUCTOR_SET_TITLE.invoke(title));
      } catch (final Throwable error) {
        logError(error, "Failed to invoke title packet constructor");
      }
    }

    @Override
    public void contributeSubtitle(final @NotNull List<Object> coll, final @NotNull Object subtitle) {
      try {
        coll.add(CONSTRUCTOR_SET_SUBTITLE.invoke(subtitle));
      } catch (final Throwable error) {
        logError(error, "Failed to invoke subtitle packet constructor");
      }
    }

    @Override
    public void contributeTimes(final @NotNull List<Object> coll, final int inTicks, final int stayTicks, final int outTicks) {
      try {
        coll.add(CONSTRUCTOR_SET_TITLE_ANIMATION.invoke(inTicks, stayTicks, outTicks));
      } catch (final Throwable error) {
        logError(error, "Failed to invoke title animations packet constructor");
      }
    }

    @Override
    public @Nullable List<?> completeTitle(final @NotNull List<Object> coll) {
      return coll;
    }

    @Override
    public void showTitle(final @NotNull Player viewer, final @NotNull List<?> packets) {
      for (final Object packet : packets) {
        this.sendMessage(viewer, packet);
      }
    }

    @Override
    public void clearTitle(final @NotNull Player viewer) {
      try {
        if (CONSTRUCTOR_CLEAR_TITLES != null) {
          this.sendPacket(viewer, CONSTRUCTOR_CLEAR_TITLES.invoke(false));
        } else {
          viewer.sendTitle("", "", -1, -1, -1);
        }
      } catch (final Throwable error) {
        logError(error, "Failed to clear title");
      }
    }

    @Override
    public void resetTitle(final @NotNull Player viewer) {
      try {
        if (CONSTRUCTOR_CLEAR_TITLES != null) {
          this.sendPacket(viewer, CONSTRUCTOR_CLEAR_TITLES.invoke(true));
        } else {
          viewer.resetTitle();
        }
      } catch (final Throwable error) {
        logError(error, "Failed to clear title");
      }
    }
  }

  static class Title extends PacketFacet<Player> implements Facet.Title<Player, Object, List<Object>, List<?>> {
    @Override
    public boolean isSupported() {
      return super.isSupported() && CONSTRUCTOR_TITLE_MESSAGE != null && CONSTRUCTOR_TITLE_TIMES != null;
    }

    @Override
    public @NotNull List<Object> createTitleCollection() {
      return new ArrayList<>();
    }

    @Override
    public void contributeTitle(final @NotNull List<Object> coll, final @NotNull Object title) {
      try {
        coll.add(CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_TITLE, title));
      } catch (final Throwable error) {
        logError(error, "Failed to invoke title packet constructor");
      }
    }

    @Override
    public void contributeSubtitle(final @NotNull List<Object> coll, final @NotNull Object subtitle) {
      try {
        coll.add(CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_SUBTITLE, subtitle));
      } catch (final Throwable error) {
        logError(error, "Failed to invoke subtitle packet constructor");
      }
    }

    @Override
    public void contributeTimes(final @NotNull List<Object> coll, final int inTicks, final int stayTicks, final int outTicks) {
      try {
        coll.add(CONSTRUCTOR_TITLE_TIMES.invoke(inTicks, stayTicks, outTicks));
      } catch (final Throwable error) {
        logError(error, "Failed to invoke title animations packet constructor");
      }
    }

    @Override
    public @Nullable List<?> completeTitle(final @NotNull List<Object> coll) {
      return coll;
    }

    @Override
    public void showTitle(final @NotNull Player viewer, final @NotNull List<?> packets) {
      for (final Object packet : packets) {
        this.sendMessage(viewer, packet);
      }
    }

    @Override
    public void clearTitle(final @NotNull Player viewer) {
      try {
        if (TITLE_ACTION_CLEAR != null) {
          this.sendPacket(viewer, CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_CLEAR, null));
        } else {
          viewer.sendTitle("", "", -1, -1, -1);
        }
      } catch (final Throwable error) {
        logError(error, "Failed to clear title");
      }
    }

    @Override
    public void resetTitle(final @NotNull Player viewer) {
      try {
        if (TITLE_ACTION_RESET != null) {
          this.sendPacket(viewer, CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_RESET, null));
        } else {
          viewer.resetTitle();
        }
      } catch (final Throwable error) {
        logError(error, "Failed to clear title");
      }
    }
  }

  protected static abstract class AbstractBook extends PacketFacet<Player> implements Facet.Book<Player, Object, ItemStack> {
    protected static final int HAND_MAIN = 0;
    private static final Material BOOK_TYPE = (Material) findEnum(Material.class, "WRITTEN_BOOK");
    private static final ItemStack BOOK_STACK = BOOK_TYPE == null ? null : new ItemStack(BOOK_TYPE);

    protected abstract void sendOpenPacket(final @NotNull Player viewer) throws Throwable;

    @Override
    public boolean isSupported() {
      return super.isSupported()
        && NBT_IO_DESERIALIZE != null && MC_ITEMSTACK_SET_TAG != null && CRAFT_ITEMSTACK_CRAFT_MIRROR != null && CRAFT_ITEMSTACK_NMS_COPY != null
        && BOOK_STACK != null;
    }

    @NotNull
    @Override
    public String createMessage(final @NotNull Player viewer, final @NotNull Component message) {
      return gson().serialize(message);
    }

    @NotNull
    @Override
    public ItemStack createBook(final @NotNull String title, final @NotNull String author, final @NotNull Iterable<Object> pages) {
      return this.applyTag(BOOK_STACK, tagFor(title, author, pages));
    }

    @Deprecated
    @Override
    public void openBook(final @NotNull Player viewer, final @NotNull ItemStack book) {
      final PlayerInventory inventory = viewer.getInventory();
      final ItemStack current = inventory.getItemInHand();
      try {
        inventory.setItemInHand(book);
        this.sendOpenPacket(viewer);
      } catch (final Throwable error) {
        logError(error, "Failed to send openBook packet: %s", book);
      } finally {
        inventory.setItemInHand(current);
      }
    }

    private static final String BOOK_TITLE = "title";
    private static final String BOOK_AUTHOR = "author";
    private static final String BOOK_PAGES = "pages";
    private static final String BOOK_RESOLVED = "resolved"; // set resolved to save on a parse as MC Components for parseable texts

    private static CompoundBinaryTag tagFor(final @NotNull String title, final @NotNull String author, final @NotNull Iterable<Object> pages) {
      final ListBinaryTag.Builder<StringBinaryTag> builder = ListBinaryTag.builder(BinaryTagTypes.STRING);
      for (final Object page : pages) {
        builder.add(StringBinaryTag.of((String) page));
      }
      return CompoundBinaryTag.builder()
        .putString(BOOK_TITLE, title)
        .putString(BOOK_AUTHOR, author)
        .put(BOOK_PAGES, builder.build())
        .putByte(BOOK_RESOLVED, (byte) 1)
        .build();
    }

    private static final Class<?> CLASS_NBT_TAG_COMPOUND = findClass(
      findNmsClassName("NBTTagCompound"),
      findMcClassName("nbt.CompoundTag"),
      findMcClassName("nbt.NBTTagCompound")
    );
    private static final Class<?> CLASS_NBT_IO = findClass(
      findNmsClassName("NBTCompressedStreamTools"),
      findMcClassName("nbt.NbtIo"),
      findMcClassName("nbt.NBTCompressedStreamTools")
    );
    private static final MethodHandle NBT_IO_DESERIALIZE;

    static {
      MethodHandle nbtIoDeserialize = null;

      if (CLASS_NBT_IO != null) { // obf obf obf
        // public static NBTCompressedStreamTools.___(DataInputStream)NBTTagCompound
        for (final Method method : CLASS_NBT_IO.getDeclaredMethods()) {
          if (Modifier.isStatic(method.getModifiers())
            && method.getReturnType().equals(CLASS_NBT_TAG_COMPOUND)
            && method.getParameterCount() == 1) {
            final Class<?> firstParam = method.getParameterTypes()[0];
            if (firstParam.equals(DataInputStream.class) || firstParam.equals(DataInput.class)) {
              try {
                nbtIoDeserialize = lookup().unreflect(method);
              } catch (final IllegalAccessException ignore) {
              }
              break;
            }
          }
        }
      }

      NBT_IO_DESERIALIZE = nbtIoDeserialize;
    }

    private static final class TrustedByteArrayOutputStream extends ByteArrayOutputStream {
      public InputStream toInputStream() {
        return new ByteArrayInputStream(this.buf, 0, this.count);
      }
    }

    private @NotNull Object createTag(final @NotNull CompoundBinaryTag tag) throws IOException {
      final TrustedByteArrayOutputStream output = new TrustedByteArrayOutputStream();
      BinaryTagIO.writer().write(tag, output);

      try (final DataInputStream dis = new DataInputStream(output.toInputStream())) {
        return NBT_IO_DESERIALIZE.invoke(dis);
      } catch (final Throwable err) {
        throw new IOException(err);
      }
    }

    private static final Class<?> CLASS_CRAFT_ITEMSTACK = findCraftClass("inventory.CraftItemStack");
    private static final Class<?> CLASS_MC_ITEMSTACK = findClass(
      findNmsClassName("ItemStack"),
      findMcClassName("world.item.ItemStack")
    );

    private static final MethodHandle MC_ITEMSTACK_SET_TAG = searchMethod(CLASS_MC_ITEMSTACK, Modifier.PUBLIC, "setTag", void.class, CLASS_NBT_TAG_COMPOUND);

    private static final MethodHandle CRAFT_ITEMSTACK_NMS_COPY = findStaticMethod(CLASS_CRAFT_ITEMSTACK, "asNMSCopy", CLASS_MC_ITEMSTACK, ItemStack.class);
    private static final MethodHandle CRAFT_ITEMSTACK_CRAFT_MIRROR = findStaticMethod(CLASS_CRAFT_ITEMSTACK, "asCraftMirror", CLASS_CRAFT_ITEMSTACK, CLASS_MC_ITEMSTACK);

    private ItemStack applyTag(final @NotNull ItemStack input, final CompoundBinaryTag binTag) {
      if (CRAFT_ITEMSTACK_NMS_COPY == null || MC_ITEMSTACK_SET_TAG == null || CRAFT_ITEMSTACK_CRAFT_MIRROR == null) {
        return input;
      }
      try {
        final Object stack = CRAFT_ITEMSTACK_NMS_COPY.invoke(input);
        final Object tag = this.createTag(binTag);

        MC_ITEMSTACK_SET_TAG.invoke(stack, tag);
        return (ItemStack) CRAFT_ITEMSTACK_CRAFT_MIRROR.invoke(stack);
      } catch (final Throwable error) {
        logError(error, "Failed to apply NBT tag to ItemStack: %s %s", input, binTag);
        return input;
      }
    }
  }

  static final class BookPost1_13 extends AbstractBook {
    private static final Class<?> CLASS_ENUM_HAND = findClass(
      findNmsClassName("EnumHand"),
      findMcClassName("world.EnumHand"),
      findMcClassName("world.InteractionHand")
    );
    private static final Object HAND_MAIN = findEnum(CLASS_ENUM_HAND, "MAIN_HAND", 0);
    private static final Class<?> PACKET_OPEN_BOOK = findClass(
      findNmsClassName("PacketPlayOutOpenBook"),
      findMcClassName("network.protocol.game.PacketPlayOutOpenBook"),
      findMcClassName("network.protocol.game.ClientboundOpenBookPacket")
    );
    private static final MethodHandle NEW_PACKET_OPEN_BOOK = findConstructor(PACKET_OPEN_BOOK, CLASS_ENUM_HAND);

    @Override
    public boolean isSupported() {
      return super.isSupported() && HAND_MAIN != null && NEW_PACKET_OPEN_BOOK != null;
    }

    @Override
    protected void sendOpenPacket(final @NotNull Player viewer) throws Throwable {
      this.sendMessage(viewer, NEW_PACKET_OPEN_BOOK.invoke(HAND_MAIN));
    }
  }

  static final class Book1_13 extends AbstractBook {
    private static final Class<?> CLASS_BYTE_BUF = findClass("io.netty.buffer.ByteBuf");
    private static final Class<?> CLASS_PACKET_CUSTOM_PAYLOAD = findNmsClass("PacketPlayOutCustomPayload");
    private static final Class<?> CLASS_FRIENDLY_BYTE_BUF = findNmsClass("PacketDataSerializer");
    private static final Class<?> CLASS_RESOURCE_LOCATION = findNmsClass("MinecraftKey");
    private static final Object PACKET_TYPE_BOOK_OPEN;

    private static final MethodHandle NEW_PACKET_CUSTOM_PAYLOAD = findConstructor(CLASS_PACKET_CUSTOM_PAYLOAD, CLASS_RESOURCE_LOCATION, CLASS_FRIENDLY_BYTE_BUF); // (channelId: String, payload: PacketByteBuf)
    private static final MethodHandle NEW_FRIENDLY_BYTE_BUF = findConstructor(CLASS_FRIENDLY_BYTE_BUF, CLASS_BYTE_BUF); // (wrapped: ByteBuf)

    static {
      Object packetType = null;
      if (CLASS_RESOURCE_LOCATION != null) {
        try {
          packetType = CLASS_RESOURCE_LOCATION.getConstructor(String.class).newInstance("minecraft:book_open");
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
          // ignore, we will be unsupported
        }
      }
      PACKET_TYPE_BOOK_OPEN = packetType;
    }

    @Override
    public boolean isSupported() {
      return super.isSupported() && CLASS_BYTE_BUF != null && NEW_PACKET_CUSTOM_PAYLOAD != null && PACKET_TYPE_BOOK_OPEN != null;
    }

    @Override
    protected void sendOpenPacket(final @NotNull Player viewer) throws Throwable {
      final ByteBuf data = Unpooled.buffer();
      data.writeByte(HAND_MAIN);
      final Object packetByteBuf = NEW_FRIENDLY_BYTE_BUF.invoke(data);
      this.sendMessage(viewer, NEW_PACKET_CUSTOM_PAYLOAD.invoke(PACKET_TYPE_BOOK_OPEN, packetByteBuf));
    }

  }

  static final class BookPre1_13 extends AbstractBook {
    private static final String PACKET_TYPE_BOOK_OPEN = "MC|BOpen"; // Before 1.13 the open book packet is a packet250
    private static final Class<?> CLASS_BYTE_BUF = findClass("io.netty.buffer.ByteBuf");
    private static final Class<?> CLASS_PACKET_CUSTOM_PAYLOAD = findNmsClass("PacketPlayOutCustomPayload");
    private static final Class<?> CLASS_PACKET_DATA_SERIALIZER = findNmsClass("PacketDataSerializer");

    private static final MethodHandle NEW_PACKET_CUSTOM_PAYLOAD = findConstructor(CLASS_PACKET_CUSTOM_PAYLOAD, String.class, CLASS_PACKET_DATA_SERIALIZER); // (channelId: String, payload: PacketByteBuf)
    private static final MethodHandle NEW_PACKET_BYTE_BUF = findConstructor(CLASS_PACKET_DATA_SERIALIZER, CLASS_BYTE_BUF); // (wrapped: ByteBuf)

    @Override
    public boolean isSupported() {
      return super.isSupported() && CLASS_BYTE_BUF != null && CLASS_PACKET_CUSTOM_PAYLOAD != null && NEW_PACKET_CUSTOM_PAYLOAD != null;
    }

    @Override
    protected void sendOpenPacket(final @NotNull Player viewer) throws Throwable {
      final ByteBuf data = Unpooled.buffer();
      data.writeByte(HAND_MAIN);
      final Object packetByteBuf = NEW_PACKET_BYTE_BUF.invoke(data);
      this.sendMessage(viewer, NEW_PACKET_CUSTOM_PAYLOAD.invoke(PACKET_TYPE_BOOK_OPEN, packetByteBuf));
    }
  }

  static final class BossBar extends BukkitFacet.BossBar {
    private static final Class<?> CLASS_CRAFT_BOSS_BAR = findCraftClass("boss.CraftBossBar");
    private static final Class<?> CLASS_BOSS_BAR_ACTION = findClass(
      findNmsClassName("PacketPlayOutBoss$Action"),
      findMcClassName("network.protocol.game.PacketPlayOutBoss$Action"),
      findMcClassName("network.protocol.game.ClientboundBossEventPacket$Operation")
    );
    private static final Object BOSS_BAR_ACTION_TITLE = findEnum(CLASS_BOSS_BAR_ACTION, "UPDATE_NAME", 3);
    private static final MethodHandle CRAFT_BOSS_BAR_HANDLE;
    private static final MethodHandle NMS_BOSS_BATTLE_SET_NAME;
    private static final MethodHandle NMS_BOSS_BATTLE_SEND_UPDATE;

    static {
      MethodHandle craftBossBarHandle = null;
      MethodHandle nmsBossBattleSetName = null;
      MethodHandle nmsBossBattleSendUpdate = null;

      if (CLASS_CRAFT_BOSS_BAR != null && CLASS_CHAT_COMPONENT != null && BOSS_BAR_ACTION_TITLE != null) {
        try {
          final Field craftBossBarHandleField = needField(CLASS_CRAFT_BOSS_BAR, "handle");
          craftBossBarHandle = lookup().unreflectGetter(craftBossBarHandleField);
          final Class<?> nmsBossBattleType = craftBossBarHandleField.getType();
          for (final Field field : nmsBossBattleType.getFields()) {
            if (field.getType().equals(CLASS_CHAT_COMPONENT)) {
              nmsBossBattleSetName = lookup().unreflectSetter(field);
              break;
            }
          }
          nmsBossBattleSendUpdate = lookup().findVirtual(nmsBossBattleType, "sendUpdate", methodType(void.class, CLASS_BOSS_BAR_ACTION));
        } catch (final Throwable error) {
          logError(error, "Failed to initialize CraftBossBar constructor");
        }
      }

      CRAFT_BOSS_BAR_HANDLE = craftBossBarHandle;
      NMS_BOSS_BATTLE_SET_NAME = nmsBossBattleSetName;
      NMS_BOSS_BATTLE_SEND_UPDATE = nmsBossBattleSendUpdate;
    }

    public static class Builder extends CraftBukkitFacet<Player> implements Facet.BossBar.Builder<Player, CraftBukkitFacet.BossBar> {
      protected Builder() {
        super(Player.class);
      }

      @Override
      public boolean isSupported() {
        return super.isSupported()
          && CLASS_CRAFT_BOSS_BAR != null && CRAFT_BOSS_BAR_HANDLE != null && NMS_BOSS_BATTLE_SET_NAME != null && NMS_BOSS_BATTLE_SEND_UPDATE != null;
      }

      @Override
      public CraftBukkitFacet.@NotNull BossBar createBossBar(final @NotNull Collection<Player> viewers) {
        return new CraftBukkitFacet.BossBar(viewers);
      }
    }

    private BossBar(final @NotNull Collection<Player> viewers) {
      super(viewers);
    }

    @Override
    public void bossBarNameChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final @NotNull Component oldName, final @NotNull Component newName) {
      try {
        final Object handle = CRAFT_BOSS_BAR_HANDLE.invoke(this.bar);
        final Object text = MinecraftComponentSerializer.get().serialize(newName);
        // Boss bar was introduced MC 1.9, but the name setter method didn't exist until later versions, so for max compatibility we'll do field set and update separately
        NMS_BOSS_BATTLE_SET_NAME.invoke(handle, text);
        NMS_BOSS_BATTLE_SEND_UPDATE.invoke(handle, BOSS_BAR_ACTION_TITLE);
      } catch (final Throwable error) {
        logError(error, "Failed to set CraftBossBar name: %s %s", this.bar, newName);
        super.bossBarNameChanged(bar, oldName, newName); // Fallback to the Bukkit method
      }
    }
  }

  static class FakeEntity<E extends Entity> extends PacketFacet<Player> implements Facet.FakeEntity<Player, Location>, Listener {
    private static final Class<? extends World> CLASS_CRAFT_WORLD = findCraftClass("CraftWorld", World.class);
    private static final Class<?> CLASS_NMS_LIVING_ENTITY = findNmsClass("EntityLiving");
    private static final Class<?> CLASS_DATA_WATCHER = findNmsClass("DataWatcher");

    private static final MethodHandle CRAFT_WORLD_CREATE_ENTITY = findMethod(CLASS_CRAFT_WORLD, "createEntity", CLASS_NMS_ENTITY, Location.class, Class.class);
    private static final MethodHandle NMS_ENTITY_GET_BUKKIT_ENTITY = findMethod(CLASS_NMS_ENTITY, "getBukkitEntity", CLASS_CRAFT_ENTITY);
    private static final MethodHandle NMS_ENTITY_GET_DATA_WATCHER = findMethod(CLASS_NMS_ENTITY, "getDataWatcher", CLASS_DATA_WATCHER);
    private static final MethodHandle NMS_ENTITY_SET_LOCATION = findMethod(CLASS_NMS_ENTITY, "setLocation", void.class, double.class, double.class, double.class, float.class, float.class); // (x, y, z, pitch, yaw) -> void
    private static final MethodHandle NMS_ENTITY_SET_INVISIBLE = findMethod(CLASS_NMS_ENTITY, "setInvisible", void.class, boolean.class);
    private static final MethodHandle DATA_WATCHER_WATCH = findMethod(CLASS_DATA_WATCHER, "watch", void.class, int.class, Object.class);

    private static final Class<?> CLASS_SPAWN_LIVING_PACKET = findNmsClass("PacketPlayOutSpawnEntityLiving");
    private static final MethodHandle NEW_SPAWN_LIVING_PACKET = findConstructor(CLASS_SPAWN_LIVING_PACKET, CLASS_NMS_LIVING_ENTITY); // (entityToSpawn: LivingEntity)
    private static final Class<?> CLASS_ENTITY_DESTROY_PACKET = findNmsClass("PacketPlayOutEntityDestroy");
    private static final MethodHandle NEW_ENTITY_DESTROY_PACKET = findConstructor(CLASS_ENTITY_DESTROY_PACKET, int[].class); // (ids: int[])
    private static final Class<?> CLASS_ENTITY_METADATA_PACKET = findNmsClass("PacketPlayOutEntityMetadata");
    private static final MethodHandle NEW_ENTITY_METADATA_PACKET = findConstructor(CLASS_ENTITY_METADATA_PACKET, int.class, CLASS_DATA_WATCHER, boolean.class); // (entityId: int, DataWatcher, updateAll: boolean)
    private static final Class<?> CLASS_ENTITY_TELEPORT_PACKET = findNmsClass("PacketPlayOutEntityTeleport");
    private static final MethodHandle NEW_ENTITY_TELEPORT_PACKET = findConstructor(CLASS_ENTITY_TELEPORT_PACKET, CLASS_NMS_ENTITY);

    private static final Class<?> CLASS_ENTITY_WITHER = findNmsClass("EntityWither");
    private static final Class<?> CLASS_WORLD = findNmsClass("World");
    private static final Class<?> CLASS_WORLD_SERVER = findNmsClass("WorldServer");
    private static final MethodHandle CRAFT_WORLD_GET_HANDLE = findMethod(CLASS_CRAFT_WORLD, "getHandle", CLASS_WORLD_SERVER);
    private static final MethodHandle NEW_ENTITY_WITHER = findConstructor(CLASS_ENTITY_WITHER, CLASS_WORLD);

    private static final boolean SUPPORTED = (CRAFT_WORLD_CREATE_ENTITY != null || (NEW_ENTITY_WITHER != null && CRAFT_WORLD_GET_HANDLE != null))
      && CRAFT_ENTITY_GET_HANDLE != null && NMS_ENTITY_GET_BUKKIT_ENTITY != null && NMS_ENTITY_GET_DATA_WATCHER != null;

    private final E entity;
    private final Object entityHandle;
    protected final Set<Player> viewers;

    protected FakeEntity(final @NotNull Class<E> entityClass, final @NotNull Location location) {
      this(BukkitAudience.PLUGIN.get(), entityClass, location);
    }

    @SuppressWarnings("unchecked")
    protected FakeEntity(final @NotNull Plugin plugin, final @NotNull Class<E> entityClass, final @NotNull Location location) {
      E entity = null;
      Object handle = null;

      if (SUPPORTED) {
        try {
          if (CRAFT_WORLD_CREATE_ENTITY != null) {
            final Object nmsEntity = CRAFT_WORLD_CREATE_ENTITY.invoke(location.getWorld(), location, entityClass);
            entity = (E) NMS_ENTITY_GET_BUKKIT_ENTITY.invoke(nmsEntity);
          } else if (Wither.class.isAssignableFrom(entityClass) && NEW_ENTITY_WITHER != null) { // 1.7.10 compact
            final Object nmsEntity = NEW_ENTITY_WITHER.invoke(CRAFT_WORLD_GET_HANDLE.invoke(location.getWorld()));
            entity = (E) NMS_ENTITY_GET_BUKKIT_ENTITY.invoke(nmsEntity);
          }
          if (CLASS_CRAFT_ENTITY.isInstance(entity)) {
            handle = CRAFT_ENTITY_GET_HANDLE.invoke(entity);
          }
        } catch (final Throwable error) {
          logError(error, "Failed to create fake entity: %s", entityClass.getSimpleName());
        }
      }

      this.entity = entity;
      this.entityHandle = handle;
      this.viewers = new HashSet<>();

      if (this.isSupported()) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
      }
    }

    @Override
    public boolean isSupported() {
      return super.isSupported() && this.entity != null && this.entityHandle != null;
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onPlayerMove(final PlayerMoveEvent event) {
      final Player viewer = event.getPlayer();
      if (this.viewers.contains(viewer)) {
        this.teleport(viewer, this.createPosition(viewer));
      }
    }

    public @Nullable Object createSpawnPacket() {
      // Later versions of MC add a createSpawnPacket()Packet method on Entity -- for broader support that could be used.
      // For 1.8 and 1.7 at least, we are stuck with this.
      if (this.entity instanceof LivingEntity) {
        try {
          return NEW_SPAWN_LIVING_PACKET.invoke(this.entityHandle);
        } catch (final Throwable error) {
          logError(error, "Failed to create spawn packet: %s", this.entity);
        }
      }
      return null;
    }

    public @Nullable Object createDespawnPacket() {
      try {
        return NEW_ENTITY_DESTROY_PACKET.invoke(this.entity.getEntityId());
      } catch (final Throwable error) {
        logError(error, "Failed to create despawn packet: %s", this.entity);
        return null;
      }
    }

    public @Nullable Object createMetadataPacket() {
      try {
        final Object dataWatcher = NMS_ENTITY_GET_DATA_WATCHER.invoke(this.entityHandle);
        return NEW_ENTITY_METADATA_PACKET.invoke(this.entity.getEntityId(), dataWatcher, false);
      } catch (final Throwable error) {
        logError(error, "Failed to create update metadata packet: %s", this.entity);
        return null;
      }
    }

    public @Nullable Object createLocationPacket() {
      try {
        return NEW_ENTITY_TELEPORT_PACKET.invoke(this.entityHandle);
      } catch (final Throwable error) {
        logError(error, "Failed to create teleport packet: %s", this.entity);
        return null;
      }
    }

    public void broadcastPacket(final @Nullable Object packet) {
      for (final Player viewer : this.viewers) {
        this.sendPacket(viewer, packet);
      }
    }

    @NotNull
    @Override
    public Location createPosition(final @NotNull Player viewer) {
      return viewer.getLocation();
    }

    @NotNull
    @Override
    public Location createPosition(final double x, final double y, final double z) {
      return new Location(null, x, y, z);
    }

    @Override
    public void teleport(final @NotNull Player viewer, final @Nullable Location position) {
      if (position == null) {
        this.viewers.remove(viewer);
        this.sendPacket(viewer, this.createDespawnPacket());
        return;
      }

      if (!this.viewers.contains(viewer)) {
        this.sendPacket(viewer, this.createSpawnPacket());
        this.viewers.add(viewer);
      }

      try {
        NMS_ENTITY_SET_LOCATION.invoke(this.entityHandle, position.getX(), position.getY(), position.getZ(), position.getPitch(), position.getYaw());
      } catch (final Throwable error) {
        logError(error, "Failed to set entity location: %s %s", this.entity, position);
      }
      this.sendPacket(viewer, this.createLocationPacket());
    }

    @Override
    public void metadata(final int position, final @NotNull Object data) {
      // DataWatchers were refactored at some point and use TrackedData as their key, not ints -- but this works for 1.8
      if (DATA_WATCHER_WATCH != null) {
        try {
          final Object dataWatcher = NMS_ENTITY_GET_DATA_WATCHER.invoke(this.entityHandle);
          DATA_WATCHER_WATCH.invoke(dataWatcher, position, data);
        } catch (final Throwable error) {
          logError(error, "Failed to set entity metadata: %s %s=%s", this.entity, position, data);
        }
        this.broadcastPacket(this.createMetadataPacket());
      }
    }

    @Override
    public void invisible(final boolean invisible) {
      if (NMS_ENTITY_SET_INVISIBLE != null) {
        try {
          NMS_ENTITY_SET_INVISIBLE.invoke(this.entityHandle, invisible);
        } catch (final Throwable error) {
          logError(error, "Failed to change entity visibility: %s", this.entity);
        }
      }
    }

    @Deprecated
    @Override
    public void health(final float health) {
      if (this.entity instanceof Damageable) {
        final Damageable entity = (Damageable) this.entity;
        entity.setHealth(health * (entity.getMaxHealth() - 0.1f) + 0.1f);
        this.broadcastPacket(this.createMetadataPacket());
      }
    }

    @Override
    public void name(final @NotNull Component name) {
      this.entity.setCustomName(legacy().serialize(name));
      this.broadcastPacket(this.createMetadataPacket());
    }

    @Override
    public void close() {
      HandlerList.unregisterAll(this);
      for (final Player viewer : new LinkedList<>(this.viewers)) {
        this.teleport(viewer, null);
      }
    }
  }

  static final class BossBarWither extends FakeEntity<Wither> implements Facet.BossBarEntity<Player, Location> {
    public static class Builder extends CraftBukkitFacet<Player> implements Facet.BossBar.Builder<Player, BossBarWither> {
      protected Builder() {
        super(Player.class);
      }

      @NotNull
      @Override
      public BossBarWither createBossBar(final @NotNull Collection<Player> viewers) {
        return new BossBarWither(viewers);
      }
    }

    private volatile boolean initialized = false;

    private BossBarWither(final @NotNull Collection<Player> viewers) {
      super(Wither.class, viewers.iterator().next().getWorld().getSpawnLocation());
      this.invisible(true);
      this.metadata(INVULNERABLE_KEY, INVULNERABLE_TICKS);
    }

    @Override
    public void bossBarInitialized(final net.kyori.adventure.bossbar.@NotNull BossBar bar) {
      Facet.BossBarEntity.super.bossBarInitialized(bar);
      this.initialized = true;
    }

    @Override
    public @NotNull Location createPosition(final @NotNull Player viewer) {
      final Location position = super.createPosition(viewer);
      position.setPitch(position.getPitch() - OFFSET_PITCH);
      position.setYaw(position.getYaw() + OFFSET_YAW);
      position.add(position.getDirection().multiply(OFFSET_MAGNITUDE));
      return position;
    }

    @Override
    public boolean isEmpty() {
      return !this.initialized || this.viewers.isEmpty();
    }
  }

  static class TabList extends PacketFacet<Player> implements Facet.TabList<Player, Object> {
    private static final Class<?> CLIENTBOUND_TAB_LIST_PACKET = findClass(
      findNmsClassName("PacketPlayOutPlayerListHeaderFooter"),
      findMcClassName("network.protocol.game.PacketPlayOutPlayerListHeaderFooter"),
      findMcClassName("network.protocol.game.ClientboundTabListPacket")
    );
    private static final @Nullable MethodHandle CLIENTBOUND_TAB_LIST_PACKET_CTOR_PRE_1_17 = findConstructor(CLIENTBOUND_TAB_LIST_PACKET);
    protected static final @Nullable MethodHandle CLIENTBOUND_TAB_LIST_PACKET_CTOR = findConstructor(CLIENTBOUND_TAB_LIST_PACKET, CLASS_CHAT_COMPONENT, CLASS_CHAT_COMPONENT);
    // Fields added by spigot -- names stable
    private static final @Nullable Field CRAFT_PLAYER_TAB_LIST_HEADER = findField(CLASS_CRAFT_PLAYER, "playerListHeader");
    private static final @Nullable Field CRAFT_PLAYER_TAB_LIST_FOOTER = findField(CLASS_CRAFT_PLAYER, "playerListFooter");

    protected static final @Nullable MethodHandle CLIENTBOUND_TAB_LIST_PACKET_SET_HEADER = first(
      findSetterOf(findField(CLIENTBOUND_TAB_LIST_PACKET, PaperFacet.NATIVE_COMPONENT_CLASS, "adventure$header")),
      findSetterOf(findField(CLIENTBOUND_TAB_LIST_PACKET, CLASS_CHAT_COMPONENT, "header", "a"))
    );
    protected static final @Nullable MethodHandle CLIENTBOUND_TAB_LIST_PACKET_SET_FOOTER = first(
      findSetterOf(findField(CLIENTBOUND_TAB_LIST_PACKET, PaperFacet.NATIVE_COMPONENT_CLASS, "adventure$footer")),
      findSetterOf(findField(CLIENTBOUND_TAB_LIST_PACKET, CLASS_CHAT_COMPONENT, "footer", "b"))
    );

    private static MethodHandle first(final MethodHandle... handles) {
      for (int i = 0; i < handles.length; i++) {
        final MethodHandle handle = handles[i];
        if (handle != null) {
          return handle;
        }
      }
      return null;
    }

    @Override
    public boolean isSupported() {
      return (CLIENTBOUND_TAB_LIST_PACKET_CTOR != null || CLIENTBOUND_TAB_LIST_PACKET_CTOR_PRE_1_17 != null) && CLIENTBOUND_TAB_LIST_PACKET_SET_HEADER != null && CLIENTBOUND_TAB_LIST_PACKET_SET_FOOTER != null && super.isSupported();
    }

    protected Object create117Packet(final Player viewer, final @Nullable Object header, final @Nullable Object footer) throws Throwable {
      return CLIENTBOUND_TAB_LIST_PACKET_CTOR.invoke(
        header == null ? this.createMessage(viewer, Component.empty()) : header,
        footer == null ? this.createMessage(viewer, Component.empty()) : footer
      );
    }

    @Override
    public void send(final Player viewer, @Nullable Object header, @Nullable Object footer) {
      try {
        if (CRAFT_PLAYER_TAB_LIST_HEADER != null && CRAFT_PLAYER_TAB_LIST_FOOTER != null) {
          if (header == null) {
            header = CRAFT_PLAYER_TAB_LIST_HEADER.get(viewer);
          } else {
            CRAFT_PLAYER_TAB_LIST_HEADER.set(viewer, header);
          }

          if (footer == null) {
            footer = CRAFT_PLAYER_TAB_LIST_FOOTER.get(viewer);
          } else {
            CRAFT_PLAYER_TAB_LIST_FOOTER.set(viewer, footer);
          }
        }

        final Object packet;
        if (CLIENTBOUND_TAB_LIST_PACKET_CTOR != null) {
          packet = this.create117Packet(viewer, header, footer);
        } else {
          packet = CLIENTBOUND_TAB_LIST_PACKET_CTOR_PRE_1_17.invoke();
          CLIENTBOUND_TAB_LIST_PACKET_SET_HEADER.invoke(packet, header == null ? this.createMessage(viewer, Component.empty()) : header);
          CLIENTBOUND_TAB_LIST_PACKET_SET_FOOTER.invoke(packet, footer == null ? this.createMessage(viewer, Component.empty()) : footer);
        }

        this.sendPacket(viewer, packet);
      } catch (final Throwable thr) {
        logError(thr, "Failed to send tab list header and footer to %s", viewer);
      }
    }
  }

  static final class Translator extends FacetBase<Server> implements FacetComponentFlattener.Translator<Server> {
    private static final Class<?> CLASS_LANGUAGE = MinecraftReflection.findClass(
      findNmsClassName("LocaleLanguage"),
      findMcClassName("locale.LocaleLanguage"),
      findMcClassName("locale.Language")
    );
    private static final MethodHandle LANGUAGE_GET_INSTANCE;
    private static final MethodHandle LANGUAGE_GET_OR_DEFAULT;

    static {
      if (CLASS_LANGUAGE == null) {
        LANGUAGE_GET_INSTANCE = null;
        LANGUAGE_GET_OR_DEFAULT = null;
      } else {
        LANGUAGE_GET_INSTANCE = Arrays.stream(CLASS_LANGUAGE.getDeclaredMethods())
          .filter(m -> Modifier.isStatic(m.getModifiers()) && !Modifier.isPrivate(m.getModifiers())
            && m.getReturnType().equals(CLASS_LANGUAGE)
            && m.getParameterCount() == 0)
          .findFirst()
          .map(Translator::unreflectUnchecked)
          .orElse(null);

        LANGUAGE_GET_OR_DEFAULT = Arrays.stream(CLASS_LANGUAGE.getDeclaredMethods())
          .filter(m -> !Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers())
            && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class && m.getReturnType().equals(String.class))
          .findFirst()
          .map(Translator::unreflectUnchecked)
          .orElse(null);
      }
    }

    private static MethodHandle unreflectUnchecked(final Method m) {
      try {
        m.setAccessible(true);
        return MinecraftReflection.lookup().unreflect(m);
      } catch (final IllegalAccessException ex) {
        return null;
      }
    }

    Translator() {
      super(Server.class);
    }

    @Override
    public boolean isSupported() {
      return super.isSupported() && LANGUAGE_GET_INSTANCE != null && LANGUAGE_GET_OR_DEFAULT != null;
    }

    @Override
    public @NotNull String valueOrDefault(final @NotNull Server game, final @NotNull String key) {
      try {
        return (String) LANGUAGE_GET_OR_DEFAULT.invoke(LANGUAGE_GET_INSTANCE.invoke(), key);
      } catch (final Throwable ex) {
        logError(ex, "Failed to transate key '%s'", key);
        return key;
      }
    }
  }

}
