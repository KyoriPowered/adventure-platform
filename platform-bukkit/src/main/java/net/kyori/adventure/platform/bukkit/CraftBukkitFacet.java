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
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetBase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.craftbukkit.MinecraftComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodType.methodType;
import static net.kyori.adventure.platform.facet.Knob.isEnabled;
import static net.kyori.adventure.platform.facet.Knob.logError;
import static net.kyori.adventure.text.serializer.craftbukkit.BukkitComponentSerializer.gson;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findClass;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findConstructor;
import static net.kyori.adventure.text.serializer.craftbukkit.BukkitComponentSerializer.legacy;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findCraftClass;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findEnum;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findMethod;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findNmsClass;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findStaticMethod;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.lookup;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.needField;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.needNmsClass;

class CraftBukkitFacet<V extends CommandSender> extends FacetBase<V> {
  protected CraftBukkitFacet(final @Nullable Class<? extends V> viewerClass) {
    super(viewerClass);
  }

  @Override
  public boolean isSupported() {
    return super.isSupported() && SUPPORTED;
  }

  private static final @Nullable Class<? extends Player> CLASS_CRAFT_PLAYER = findCraftClass("entity.CraftPlayer", Player.class);
  private static final @Nullable MethodHandle CRAFT_PLAYER_GET_HANDLE;
  private static final @Nullable MethodHandle ENTITY_PLAYER_GET_CONNECTION;
  private static final @Nullable MethodHandle PLAYER_CONNECTION_SEND_PACKET;

  static {
    final Class<?> craftPlayerClass = findCraftClass("entity.CraftPlayer");
    final Class<?> packetClass = findNmsClass("Packet");

    MethodHandle craftPlayerGetHandle = null;
    MethodHandle entityPlayerGetConnection = null;
    MethodHandle playerConnectionSendPacket = null;
    if(craftPlayerClass != null && packetClass != null) {
      try {
        final Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
        final Class<?> entityPlayerClass = getHandleMethod.getReturnType();
        craftPlayerGetHandle = lookup().unreflect(getHandleMethod);
        final Field playerConnectionField = entityPlayerClass.getField("playerConnection");
        entityPlayerGetConnection = lookup().unreflectGetter(playerConnectionField);
        final Class<?> playerConnectionClass = playerConnectionField.getType();
        playerConnectionSendPacket = lookup().findVirtual(playerConnectionClass, "sendPacket", methodType(void.class, packetClass));
      } catch(final Throwable error) {
        logError(error, "Failed to initialize CraftBukkit sendPacket");
      }
    }

    CRAFT_PLAYER_GET_HANDLE = craftPlayerGetHandle;
    ENTITY_PLAYER_GET_CONNECTION = entityPlayerGetConnection;
    PLAYER_CONNECTION_SEND_PACKET = playerConnectionSendPacket;
  }

  private static final boolean SUPPORTED = isEnabled("craftbukkit")
    && MinecraftComponentSerializer.isSupported()
    && CRAFT_PLAYER_GET_HANDLE != null && ENTITY_PLAYER_GET_CONNECTION != null && PLAYER_CONNECTION_SEND_PACKET != null;

  static class PacketFacet<V extends CommandSender> extends CraftBukkitFacet<V> implements Facet.Message<V, Object> {
    @SuppressWarnings("unchecked")
    protected PacketFacet() {
      super((Class<V>) CLASS_CRAFT_PLAYER);
    }

    public void sendPacket(final @NonNull Player player, final @Nullable Object packet) {
      if(packet == null) return;

      try {
        PLAYER_CONNECTION_SEND_PACKET.invoke(ENTITY_PLAYER_GET_CONNECTION.invoke(CRAFT_PLAYER_GET_HANDLE.invoke(player)), packet);
      } catch(final Throwable error) {
        logError(error, "Failed to invoke CraftBukkit sendPacket: %s", packet);
      }
    }

    public void sendMessage(final @NonNull V player, final @Nullable Object packet) {
      this.sendPacket((Player) player, packet);
    }

    @Nullable
    @Override
    public Object createMessage(final @NonNull V viewer, final @NonNull Component message) {
      try {
        return MinecraftComponentSerializer.get().serialize(message);
      } catch(final Throwable error) {
        logError(error, "Failed to serialize net.minecraft.server IChatBaseComponent: %s", message);
        return null;
      }
    }
  }

  private static final @Nullable Class<?> CLASS_CHAT_COMPONENT = findNmsClass("IChatBaseComponent");
  private static final @Nullable Class<?> CLASS_MESSAGE_TYPE = findNmsClass("ChatMessageType");
  private static final @Nullable Object MESSAGE_TYPE_CHAT = findEnum(CLASS_MESSAGE_TYPE, "CHAT", 0);
  private static final @Nullable Object MESSAGE_TYPE_SYSTEM = findEnum(CLASS_MESSAGE_TYPE, "SYSTEM", 1);
  private static final @Nullable Object MESSAGE_TYPE_ACTIONBAR = findEnum(CLASS_MESSAGE_TYPE, "GAME_INFO", 2);

  private static final @Nullable MethodHandle LEGACY_CHAT_PACKET_CONSTRUCTOR; // (IChatBaseComponent, byte)
  private static final @Nullable MethodHandle CHAT_PACKET_CONSTRUCTOR; // (ChatMessageType, IChatBaseComponent, UUID) -> PacketPlayOutChat

  static {
    MethodHandle legacyChatPacketConstructor = null;
    MethodHandle chatPacketConstructor = null;

    try {
      if(CLASS_CHAT_COMPONENT != null) {
        final Class<?> chatPacketClass = needNmsClass("PacketPlayOutChat");
        // PacketPlayOutChat constructor changed for 1.16
        chatPacketConstructor = findConstructor(chatPacketClass, CLASS_CHAT_COMPONENT);
        if(chatPacketConstructor == null) {
          if(CLASS_MESSAGE_TYPE != null) {
            chatPacketConstructor = findConstructor(chatPacketClass,CLASS_CHAT_COMPONENT, CLASS_MESSAGE_TYPE, UUID.class);
          }
        } else {
          // Create a function that ignores the message type and sender id arguments to call the underlying one-argument constructor
          chatPacketConstructor = dropArguments(chatPacketConstructor, 1, CLASS_MESSAGE_TYPE == null ? Object.class : CLASS_MESSAGE_TYPE, UUID.class);
        }
        legacyChatPacketConstructor = findConstructor(chatPacketClass, CLASS_CHAT_COMPONENT, byte.class);
        if(legacyChatPacketConstructor == null) { // 1.7 paper protocol hack?
          legacyChatPacketConstructor = findConstructor(chatPacketClass, CLASS_CHAT_COMPONENT, int.class);
        }
      }
    } catch(final Throwable error) {
      logError(error, "Failed to initialize PacketPlayOutChat constructor");
    }

    CHAT_PACKET_CONSTRUCTOR = chatPacketConstructor;
    LEGACY_CHAT_PACKET_CONSTRUCTOR = legacyChatPacketConstructor;
  }

  static class Chat extends PacketFacet<CommandSender> implements Facet.Chat<CommandSender, Object> {
    private static final UUID NULL_UUID = new UUID(0, 0);

    @Override
    public boolean isSupported() {
      return super.isSupported() && CHAT_PACKET_CONSTRUCTOR != null;
    }

    @Override
    public void sendMessage(final @NonNull CommandSender viewer, final @NonNull Object message, final @NonNull MessageType type) {
      final Object messageType = type == MessageType.CHAT ? MESSAGE_TYPE_CHAT : MESSAGE_TYPE_SYSTEM;
      try {
        this.sendMessage(viewer, CHAT_PACKET_CONSTRUCTOR.invoke(message, messageType, NULL_UUID));
      } catch(final Throwable error) {
        logError(error, "Failed to invoke PacketPlayOutChat constructor: %s %s", message, messageType);
      }
    }
  }

  private static final @Nullable Class<?> CLASS_TITLE_PACKET = findNmsClass("PacketPlayOutTitle");
  private static final @Nullable Class<?> CLASS_TITLE_ACTION = findNmsClass("PacketPlayOutTitle$EnumTitleAction"); // welcome to spigot, where we can't name classes? i guess?
  private static final MethodHandle CONSTRUCTOR_TITLE_MESSAGE = findConstructor(CLASS_TITLE_PACKET, CLASS_TITLE_ACTION, CLASS_CHAT_COMPONENT); // (EnumTitleAction, IChatBaseComponent)
  private static final @Nullable MethodHandle CONSTRUCTOR_TITLE_TIMES = findConstructor(CLASS_TITLE_PACKET, int.class, int.class, int.class);
  private static final @Nullable Object TITLE_ACTION_TITLE = findEnum(CLASS_TITLE_ACTION, "TITLE", 0);
  private static final @Nullable Object TITLE_ACTION_SUBTITLE = findEnum(CLASS_TITLE_ACTION, "SUBTITLE", 1);
  private static final @Nullable Object TITLE_ACTION_ACTIONBAR = findEnum(CLASS_TITLE_ACTION, "ACTIONBAR");

  static class ActionBar extends PacketFacet<Player> implements Facet.ActionBar<Player, Object> {
    @Override
    public boolean isSupported() {
      return super.isSupported() && TITLE_ACTION_ACTIONBAR != null;
    }

    @Nullable
    @Override
    public Object createMessage(final @NonNull Player viewer, final @NonNull Component message) {
      try {
        return CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_ACTIONBAR, super.createMessage(viewer, message));
      } catch(final Throwable error) {
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
    public Object createMessage(final @NonNull Player viewer, final @NonNull Component message) {
      // Due to a Minecraft client bug, Action bars through the chat packet don't properly support formatting
      final TextComponent legacyMessage = TextComponent.of(legacy().serialize(message));
      try {
        return LEGACY_CHAT_PACKET_CONSTRUCTOR.invoke(super.createMessage(viewer, legacyMessage), (byte) 2);
      } catch(final Throwable error) {
        logError(error, "Failed to invoke PacketPlayOutChat constructor: %s", legacyMessage);
        return null;
      }
    }
  }

  static class Title extends PacketFacet<Player> implements Facet.Title<Player, Object, List<?>> {
    @Override
    public boolean isSupported() {
      return super.isSupported() && CONSTRUCTOR_TITLE_MESSAGE != null && CONSTRUCTOR_TITLE_TIMES != null;
    }

    @NonNull
    @Override
    public List<?> createTitle(final @Nullable Object title, final @Nullable Object subTitle, final int inTicks, final int stayTicks, final int outTicks) {
      final List<Object> packets = new LinkedList<>();
      try {
        if(subTitle != null) {
          packets.add(CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_SUBTITLE, subTitle));
        }
        if(inTicks != -1 || stayTicks != -1 || outTicks != -1) {
          packets.add(CONSTRUCTOR_TITLE_TIMES.invoke(inTicks, stayTicks, outTicks));
        }
        if(title != null) {
          packets.add(CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_TITLE, title));
        }
      } catch(final Throwable error) {
        logError(error, "Failed to invoke PacketPlayOutTitle constructor");
      }
      return packets;
    }

    @Override
    public void showTitle(final @NonNull Player viewer, final @NonNull List<?> packets) {
      for(final Object packet : packets) {
        this.sendMessage(viewer, packet);
      }
    }

    @Override
    public void clearTitle(final @NonNull Player viewer) {
      viewer.sendTitle("", "", -1, -1, -1);
    }

    @Override
    public void resetTitle(final @NonNull Player viewer) {
      viewer.resetTitle();
    }
  }

  protected static abstract class AbstractBook extends PacketFacet<Player> implements Facet.Book<Player, Object, ItemStack> {
    private static final Material BOOK_TYPE = (Material) findEnum(Material.class, "WRITTEN_BOOK");
    private static final ItemStack BOOK_STACK = BOOK_TYPE == null ? null : new ItemStack(BOOK_TYPE);

    protected abstract void sendOpenPacket(final @NonNull Player viewer) throws Throwable;

    @Override
    public boolean isSupported() {
      return super.isSupported()
        && NBT_IO_DESERIALIZE != null && MC_ITEMSTACK_SET_TAG != null && CRAFT_ITEMSTACK_CRAFT_MIRROR != null && CRAFT_ITEMSTACK_NMS_COPY != null
        && BOOK_STACK != null;
    }

    @NonNull
    @Override
    public String createMessage(final @NonNull Player viewer, final @NonNull Component message) {
      return gson().serialize(message);
    }

    @NonNull
    @Override
    public ItemStack createBook(final @NonNull Object title, final @NonNull Object author, final @NonNull Iterable<Object> pages) {
      return this.applyTag(BOOK_STACK, tagFor(title, author, pages));
    }

    @Deprecated
    @Override
    public void openBook(final @NonNull Player viewer, final @NonNull ItemStack book) {
      final PlayerInventory inventory = viewer.getInventory();
      final ItemStack current = inventory.getItemInHand();
      try {
        inventory.setItemInHand(book);
        this.sendOpenPacket(viewer);
      } catch(final Throwable error) {
        logError(error, "Failed to send openBook packet: %s", book);
      } finally {
        inventory.setItemInHand(current);
      }
    }

    private static final String BOOK_TITLE = "title";
    private static final String BOOK_AUTHOR = "author";
    private static final String BOOK_PAGES = "pages";
    private static final String BOOK_RESOLVED = "resolved"; // set resolved to save on a parse as MC Components for parseable texts

    private static CompoundBinaryTag tagFor(final @NonNull Object title, final @NonNull Object author, final @NonNull Iterable<Object> pages) {
      final ListBinaryTag.Builder<StringBinaryTag> builder = ListBinaryTag.builder(BinaryTagTypes.STRING);
      for(final Object page : pages) {
        builder.add(StringBinaryTag.of((String) page));
      }
      return CompoundBinaryTag.builder()
        .putString(BOOK_TITLE, (String) title)
        .putString(BOOK_AUTHOR, (String) author)
        .put(BOOK_PAGES, builder.build())
        .putByte(BOOK_RESOLVED, (byte) 1)
        .build();
    }

    private static final Class<?> CLASS_NBT_TAG_COMPOUND = findNmsClass("NBTTagCompound");
    private static final Class<?> CLASS_NBT_IO = findNmsClass("NBTCompressedStreamTools");
    private static final MethodHandle NBT_IO_DESERIALIZE;

    static {
      MethodHandle nbtIoDeserialize = null;

      if(CLASS_NBT_IO != null) { // obf obf obf
        // public static NBTCompressedStreamTools.___(DataInputStream)NBTTagCompound
        for(final Method method : CLASS_NBT_IO.getDeclaredMethods()) {
          if(Modifier.isStatic(method.getModifiers())
            && method.getReturnType().equals(CLASS_NBT_TAG_COMPOUND)
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0].equals(DataInputStream.class)) {
            try {
              nbtIoDeserialize = lookup().unreflect(method);
            } catch(final IllegalAccessException ignore) {
            }
            break;
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

    private @NonNull Object createTag(final @NonNull CompoundBinaryTag tag) throws IOException {
      final TrustedByteArrayOutputStream output = new TrustedByteArrayOutputStream();
      BinaryTagIO.writeOutputStream(tag, output);

      try(final DataInputStream dis = new DataInputStream(output.toInputStream())) {
        return NBT_IO_DESERIALIZE.invoke(dis);
      } catch(final Throwable err) {
        throw new IOException(err);
      }
    }

    private static final Class<?> CLASS_CRAFT_ITEMSTACK = findCraftClass("inventory.CraftItemStack");
    private static final Class<?> CLASS_MC_ITEMSTACK = findNmsClass("ItemStack");

    private static final MethodHandle MC_ITEMSTACK_SET_TAG = findMethod(CLASS_MC_ITEMSTACK, "setTag", void.class, CLASS_NBT_TAG_COMPOUND);
    private static final MethodHandle MC_ITEMSTACK_GET_TAG = findMethod(CLASS_MC_ITEMSTACK, "getTag", CLASS_NBT_TAG_COMPOUND);

    private static final MethodHandle CRAFT_ITEMSTACK_NMS_COPY = findStaticMethod(CLASS_CRAFT_ITEMSTACK, "asNMSCopy", CLASS_MC_ITEMSTACK, ItemStack.class);
    private static final MethodHandle CRAFT_ITEMSTACK_CRAFT_MIRROR = findStaticMethod(CLASS_CRAFT_ITEMSTACK, "asCraftMirror", CLASS_CRAFT_ITEMSTACK, CLASS_MC_ITEMSTACK);

    private ItemStack applyTag(final @NonNull ItemStack input, final CompoundBinaryTag binTag) {
      if(CRAFT_ITEMSTACK_NMS_COPY == null || MC_ITEMSTACK_SET_TAG == null || CRAFT_ITEMSTACK_CRAFT_MIRROR == null) {
        return input;
      }
      try {
        final Object stack = CRAFT_ITEMSTACK_NMS_COPY.invoke(input);
        final Object tag = this.createTag(binTag);

        MC_ITEMSTACK_SET_TAG.invoke(stack, tag);
        return (ItemStack) CRAFT_ITEMSTACK_CRAFT_MIRROR.invoke(stack);
      } catch(final Throwable error) {
        logError(error, "Failed to apply NBT tag to ItemStack: %s %s", input, binTag);
        return input;
      }
    }
  }

  static final class Book extends AbstractBook {
    private static final Class<?> CLASS_ENUM_HAND = findNmsClass("EnumHand");
    private static final Object HAND_MAIN = findEnum(CLASS_ENUM_HAND, "MAIN_HAND", 0);
    private static final Class<?> PACKET_OPEN_BOOK = findNmsClass("PacketPlayOutOpenBook");
    private static final MethodHandle NEW_PACKET_OPEN_BOOK = findConstructor(PACKET_OPEN_BOOK, CLASS_ENUM_HAND);

    @Override
    public boolean isSupported() {
      return super.isSupported() && HAND_MAIN != null && NEW_PACKET_OPEN_BOOK != null;
    }

    @Override
    protected void sendOpenPacket(final @NonNull Player viewer) throws Throwable {
      this.sendMessage(viewer, NEW_PACKET_OPEN_BOOK.invoke(HAND_MAIN));
    }
  }

  static final class BookLegacy extends AbstractBook {
    private static final int HAND_MAIN = 0;
    private static final String PACKET_TYPE_BOOK_OPEN = "MC|BOpen"; // Before 1.13 the open book packet is a packet250
    private static final Class<?> CLASS_BYTE_BUF = findClass("io.netty.buffer.ByteBuf");
    private static final Class<?> CLASS_PACKET_CUSTOM_PAYLOAD = findNmsClass("PacketPlayOutCustomPayload");
    private static final Class<?> CLASS_PACKET_DATA_SERIALIZER = findNmsClass("PacketDataSerializer");

    private static final MethodHandle NEW_PACKET_CUSTOM_PAYLOAD = findConstructor(CLASS_PACKET_CUSTOM_PAYLOAD, String.class, CLASS_PACKET_DATA_SERIALIZER); // (channelId: String, payload: PacketByteBuf)
    private static final MethodHandle NEW_PACKET_BYTE_BUF = findConstructor(CLASS_PACKET_DATA_SERIALIZER, CLASS_BYTE_BUF); // (wrapped: ByteBuf)

    @Override
    public boolean isSupported() {
      return super.isSupported() && CLASS_BYTE_BUF != null && CLASS_PACKET_CUSTOM_PAYLOAD != null;
    }

    @Override
    protected void sendOpenPacket(final @NonNull Player viewer) throws Throwable {
      final ByteBuf data = Unpooled.buffer();
      data.writeByte(HAND_MAIN);
      final Object packetByteBuf = NEW_PACKET_BYTE_BUF.invoke(data);
      this.sendMessage(viewer, NEW_PACKET_CUSTOM_PAYLOAD.invoke(PACKET_TYPE_BOOK_OPEN, packetByteBuf));
    }
  }

  static final class BossBar extends BukkitFacet.BossBar {
    private static final Class<?> CLASS_CRAFT_BOSS_BAR = findCraftClass("boss.CraftBossBar");
    private static final Class<?> CLASS_BOSS_BAR_ACTION = findNmsClass("PacketPlayOutBoss$Action");
    private static final Object BOSS_BAR_ACTION_TITLE = findEnum(CLASS_BOSS_BAR_ACTION, "UPDATE_NAME", 3);
    private static final MethodHandle CRAFT_BOSS_BAR_HANDLE;
    private static final MethodHandle NMS_BOSS_BATTLE_SET_NAME;
    private static final MethodHandle NMS_BOSS_BATTLE_SEND_UPDATE;

    static {
      MethodHandle craftBossBarHandle = null;
      MethodHandle nmsBossBattleSetName = null;
      MethodHandle nmsBossBattleSendUpdate = null;

      if(CLASS_CRAFT_BOSS_BAR != null && CLASS_CHAT_COMPONENT != null && BOSS_BAR_ACTION_TITLE != null) {
        try {
          final Field craftBossBarHandleField = needField(CLASS_CRAFT_BOSS_BAR, "handle");
          craftBossBarHandle = lookup().unreflectGetter(craftBossBarHandleField);
          final Class<?> nmsBossBattleType = craftBossBarHandleField.getType();
          nmsBossBattleSetName = lookup().findSetter(nmsBossBattleType, "title", CLASS_CHAT_COMPONENT);
          nmsBossBattleSendUpdate = lookup().findVirtual(nmsBossBattleType, "sendUpdate", methodType(void.class, CLASS_BOSS_BAR_ACTION));
        } catch(final Throwable error) {
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
      public CraftBukkitFacet.@NonNull BossBar createBossBar(final @NonNull Collection<Player> viewers) {
        return new CraftBukkitFacet.BossBar(viewers);
      }
    }

    private BossBar(final @NonNull Collection<Player> viewers) {
      super(viewers);
    }

    @Override
    public void bossBarNameChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final @NonNull Component oldName, final @NonNull Component newName) {
      try {
        final Object handle = CRAFT_BOSS_BAR_HANDLE.invoke(this.bar);
        final Object text = MinecraftComponentSerializer.get().serialize(newName);
        // Boss bar was introduced MC 1.9, but the name setter method didn't exist until later versions, so for max compatibility we'll do field set and update separately
        NMS_BOSS_BATTLE_SET_NAME.invoke(handle, text);
        NMS_BOSS_BATTLE_SEND_UPDATE.invoke(handle, BOSS_BAR_ACTION_TITLE);
      } catch(final Throwable error) {
        logError(error, "Failed to set CraftBossBar name: %s %s", this.bar, newName);
        super.bossBarNameChanged(bar, oldName, newName); // Fallback to the Bukkit method
      }
    }
  }

  static class FakeEntity<E extends Entity> extends PacketFacet<Player> implements Facet.FakeEntity<Player, Location>, Listener {
    private static final Class<? extends World> CLASS_CRAFT_WORLD = findCraftClass("CraftWorld", World.class);
    private static final Class<?> CLASS_NMS_ENTITY = findNmsClass("Entity");
    private static final Class<?> CLASS_NMS_LIVING_ENTITY = findNmsClass("EntityLiving");
    private static final Class<?> CLASS_CRAFT_ENTITY = findCraftClass("entity.CraftEntity");
    private static final Class<?> CLASS_DATA_WATCHER = findNmsClass("DataWatcher");

    private static final MethodHandle CRAFT_WORLD_CREATE_ENTITY = findMethod(CLASS_CRAFT_WORLD, "createEntity", CLASS_NMS_ENTITY, Location.class, Class.class);
    private static final MethodHandle CRAFT_ENTITY_GET_HANDLE = findMethod(CLASS_CRAFT_ENTITY, "getHandle", CLASS_NMS_ENTITY);
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

    protected FakeEntity(final @NonNull Class<E> entityClass, final @NonNull Location location) {
      this(BukkitAudience.PLUGIN.get(), entityClass, location);
    }

    @SuppressWarnings("unchecked")
    protected FakeEntity(final @NonNull Plugin plugin, final @NonNull Class<E> entityClass, final @NonNull Location location) {
      E entity = null;
      Object handle = null;

      if(SUPPORTED) {
        try {
          if(CRAFT_WORLD_CREATE_ENTITY != null) {
            final Object nmsEntity = CRAFT_WORLD_CREATE_ENTITY.invoke(location.getWorld(), location, entityClass);
            entity = (E) NMS_ENTITY_GET_BUKKIT_ENTITY.invoke(nmsEntity);
          } else if(Wither.class.isAssignableFrom(entityClass) && NEW_ENTITY_WITHER != null) { // 1.7.10 compact
            final Object nmsEntity = NEW_ENTITY_WITHER.invoke(CRAFT_WORLD_GET_HANDLE.invoke(location.getWorld()));
            entity = (E) NMS_ENTITY_GET_BUKKIT_ENTITY.invoke(nmsEntity);
          }
          if(CLASS_CRAFT_ENTITY.isInstance(entity)) {
            handle = CRAFT_ENTITY_GET_HANDLE.invoke(entity);
          }
        } catch(final Throwable error) {
          logError(error, "Failed to create fake entity: %s", entityClass.getSimpleName());
        }
      }

      this.entity = entity;
      this.entityHandle = handle;
      this.viewers = new HashSet<>();

      if(this.isSupported()) {
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
      if(this.viewers.contains(viewer)) {
        this.teleport(viewer, this.createPosition(viewer));
      }
    }

    public @Nullable Object createSpawnPacket() {
      // Later versions of MC add a createSpawnPacket()Packet method on Entity -- for broader support that could be used.
      // For 1.8 and 1.7 at least, we are stuck with this.
      if(this.entity instanceof LivingEntity) {
        try {
          return NEW_SPAWN_LIVING_PACKET.invoke(this.entityHandle);
        } catch(final Throwable error) {
          logError(error,"Failed to create spawn packet: %s", this.entity);
        }
      }
      return null;
    }

    public @Nullable Object createDespawnPacket() {
      try {
        return NEW_ENTITY_DESTROY_PACKET.invoke(this.entity.getEntityId());
      } catch(final Throwable error) {
        logError(error,"Failed to create despawn packet: %s", this.entity);
        return null;
      }
    }

    public @Nullable Object createMetadataPacket() {
      try {
        final Object dataWatcher = NMS_ENTITY_GET_DATA_WATCHER.invoke(this.entityHandle);
        return NEW_ENTITY_METADATA_PACKET.invoke(this.entity.getEntityId(), dataWatcher, false);
      } catch(final Throwable error) {
        logError(error,"Failed to create update metadata packet: %s", this.entity);
        return null;
      }
    }

    public @Nullable Object createLocationPacket() {
      try {
        return NEW_ENTITY_TELEPORT_PACKET.invoke(this.entityHandle);
      } catch(final Throwable error) {
        logError(error,"Failed to create teleport packet: %s", this.entity);
        return null;
      }
    }

    public void broadcastPacket(final @Nullable Object packet) {
      for(final Player viewer : this.viewers) {
        this.sendPacket(viewer, packet);
      }
    }

    @NonNull
    @Override
    public Location createPosition(final @NonNull Player viewer) {
      return viewer.getLocation();
    }

    @NonNull
    @Override
    public Location createPosition(final double x, final double y, final double z) {
      return new Location(null, x, y, z);
    }

    @Override
    public void teleport(final @NonNull Player viewer, final @Nullable Location position) {
      if(position == null) {
        this.viewers.remove(viewer);
        this.sendPacket(viewer, this.createDespawnPacket());
        return;
      }

      if(!this.viewers.contains(viewer)) {
        this.sendPacket(viewer, this.createSpawnPacket());
        this.viewers.add(viewer);
      }

      try {
        NMS_ENTITY_SET_LOCATION.invoke(this.entityHandle, position.getX(), position.getY(), position.getZ(), position.getPitch(), position.getYaw());
      } catch(final Throwable error) {
        logError(error,"Failed to set entity location: %s %s", this.entity, position);
      }
      this.sendPacket(viewer, this.createLocationPacket());
    }

    @Override
    public void metadata(final int position, final @NonNull Object data) {
      // DataWatchers were refactored at some point and use TrackedData as their key, not ints -- but this works for 1.8
      if(DATA_WATCHER_WATCH != null) {
        try {
          final Object dataWatcher = NMS_ENTITY_GET_DATA_WATCHER.invoke(this.entityHandle);
          DATA_WATCHER_WATCH.invoke(dataWatcher, position, data);
        } catch(final Throwable error) {
          logError(error,"Failed to set entity metadata: %s %s=%s", this.entity, position, data);
        }
        this.broadcastPacket(this.createMetadataPacket());
      }
    }

    @Override
    public void invisible(final boolean invisible) {
      if(NMS_ENTITY_SET_INVISIBLE != null) {
        try {
          NMS_ENTITY_SET_INVISIBLE.invoke(this.entityHandle, invisible);
        } catch(final Throwable error) {
          logError(error,"Failed to change entity visibility: %s", this.entity);
        }
      }
    }

    @Deprecated
    @Override
    public void health(final float health) {
      if(this.entity instanceof Damageable) {
        final Damageable entity = (Damageable) this.entity;
        entity.setHealth(health * (entity.getMaxHealth() - 0.1f) + 0.1f);
        this.broadcastPacket(this.createMetadataPacket());
      }
    }

    @Override
    public void name(final @NonNull Component name) {
      this.entity.setCustomName(legacy().serialize(name));
      this.broadcastPacket(this.createMetadataPacket());
    }

    @Override
    public void close() {
      HandlerList.unregisterAll(this);
      for(final Player viewer : new LinkedList<>(this.viewers)) {
        this.teleport(viewer, null);
      }
    }
  }

  static final class BossBarWither extends FakeEntity<Wither> implements Facet.BossBarEntity<Player, Location> {
    public static class Builder extends CraftBukkitFacet<Player> implements Facet.BossBar.Builder<Player, BossBarWither> {
      protected Builder() {
        super(Player.class);
      }

      @NonNull
      @Override
      public BossBarWither createBossBar(final @NonNull Collection<Player> viewers) {
        return new BossBarWither(viewers);
      }
    }

    private volatile boolean initialized = false;

    private BossBarWither(final @NonNull Collection<Player> viewers) {
      super(Wither.class, viewers.iterator().next().getWorld().getSpawnLocation());
      this.invisible(true);
      this.metadata(INVULNERABLE_KEY, INVULNERABLE_TICKS);
    }

    @Override
    public void bossBarInitialized(final net.kyori.adventure.bossbar.@NonNull BossBar bar) {
      Facet.BossBarEntity.super.bossBarInitialized(bar);
      this.initialized = true;
    }

    @Override
    public @NonNull Location createPosition(final @NonNull Player viewer) {
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
}
