/*
 * This file is part of text-extras, licensed under the MIT License.
 *
 * Copyright (c) 2018 KyoriPowered
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

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.platform.impl.AbstractBossBarListener;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.Knobs;
import net.kyori.adventure.platform.impl.TypedHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodType.methodType;
import static net.kyori.adventure.platform.bukkit.BukkitHandlers.legacy;
import static net.kyori.adventure.platform.bukkit.Crafty.LOOKUP;
import static net.kyori.adventure.platform.bukkit.Crafty.nmsClass;
import static net.kyori.adventure.platform.bukkit.Crafty.findConstructor;

public class CraftBukkitHandlers {
  
  private static final boolean ENABLED = Knobs.enabled("craftbukkit");
  
  private static final @Nullable Class<? extends Player> CLASS_CRAFT_PLAYER = Crafty.findCraftClass("entity.CraftPlayer", Player.class);

  // Packets //
  private static final @Nullable MethodHandle CRAFT_PLAYER_GET_HANDLE;
  private static final @Nullable MethodHandle ENTITY_PLAYER_GET_CONNECTION;
  private static final @Nullable MethodHandle PLAYER_CONNECTION_SEND_PACKET;


  static {
    final @Nullable Class<?> craftPlayerClass = Crafty.findCraftClass("entity.CraftPlayer");
    final @Nullable Class<?> packetClass = Crafty.findNmsClass("Packet");
    @Nullable MethodHandle craftPlayerGetHandle = null;
    @Nullable MethodHandle entityPlayerGetConnection = null;
    @Nullable MethodHandle playerConnectionSendPacket = null;
    if(craftPlayerClass != null && packetClass != null) {
      try {
        final Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
        final Class<?> entityPlayerClass = getHandleMethod.getReturnType();
        craftPlayerGetHandle = Crafty.LOOKUP.unreflect(getHandleMethod);
        final Field playerConnectionField = entityPlayerClass.getField("playerConnection");
        entityPlayerGetConnection = Crafty.LOOKUP.unreflectGetter(playerConnectionField);
        final Class<?> playerConnectionClass = playerConnectionField.getType();
        playerConnectionSendPacket = Crafty.LOOKUP.findVirtual(playerConnectionClass, "sendPacket", methodType(void.class, packetClass));
      } catch(NoSuchMethodException | IllegalAccessException | NoSuchFieldException ex) {
        Knobs.logError("finding packet send methods", ex);
      }
    }
    CRAFT_PLAYER_GET_HANDLE = craftPlayerGetHandle;
    ENTITY_PLAYER_GET_CONNECTION = entityPlayerGetConnection;
    PLAYER_CONNECTION_SEND_PACKET = playerConnectionSendPacket;
  }

  /* package */ static void sendPacket(final @NonNull Player player, final @Nullable Object packet) {
    if(packet == null) {
      return;
    }

    try {
      PLAYER_CONNECTION_SEND_PACKET.invoke(ENTITY_PLAYER_GET_CONNECTION.invoke(CRAFT_PLAYER_GET_HANDLE.invoke(player)), packet);
    } catch(Throwable throwable) {
      Knobs.logError("sending packet to user", throwable);
    }
  }

  /* package */ static class PacketSendingHandler<V extends CommandSender> extends TypedHandler<V> {

    @SuppressWarnings("unchecked")
    protected PacketSendingHandler() {
      super((Class<V>) CLASS_CRAFT_PLAYER);
    }

    @Override
    public boolean isAvailable() {
      return ENABLED && super.isAvailable() && CRAFT_PLAYER_GET_HANDLE != null && ENTITY_PLAYER_GET_CONNECTION != null && PLAYER_CONNECTION_SEND_PACKET != null
        && (MC_TEXT_GSON != null || TEXT_SERIALIZER_DESERIALIZE != null);
    }

    public void send(final @NonNull V player, final @Nullable Object packet) {
      sendPacket((Player) player, packet);
    }
  }

  // Components //
  private static final @Nullable Class<?> CLASS_CHAT_COMPONENT = Crafty.findNmsClass("IChatBaseComponent");
  private static final @Nullable Class<?> CLASS_MESSAGE_TYPE = Crafty.findNmsClass("ChatMessageType");
  private static final @Nullable Object MESSAGE_TYPE_CHAT = Crafty.enumValue(CLASS_MESSAGE_TYPE, "CHAT", 0);
  private static final @Nullable Object MESSAGE_TYPE_SYSTEM = Crafty.enumValue(CLASS_MESSAGE_TYPE, "SYSTEM", 1);
  private static final @Nullable Object MESSAGE_TYPE_ACTIONBAR = Crafty.enumValue(CLASS_MESSAGE_TYPE, "GAME_INFO", 2);
  private static final Gson MC_TEXT_GSON;
  private static final MethodHandle TEXT_SERIALIZER_DESERIALIZE;

  private static final @Nullable MethodHandle LEGACY_CHAT_PACKET_CONSTRUCTOR; // (IChatBaseComponent, byte)
  private static final @Nullable MethodHandle CHAT_PACKET_CONSTRUCTOR; // (ChatMessageType, IChatBaseComponent, UUID) -> PacketPlayOutChat

  static {
    MethodHandle legacyChatPacketConstructor = null;
    MethodHandle chatPacketConstructor = null;
    Gson gson = null;
    MethodHandle textSerializerDeserialize = null;

    try {
      if(CLASS_CHAT_COMPONENT != null) {
        // Chat packet //
        final Class<?> chatPacketClass = Crafty.nmsClass("PacketPlayOutChat");
        // PacketPlayOutChat constructor changed for 1.16
        chatPacketConstructor = Crafty.findConstructor(chatPacketClass, CLASS_CHAT_COMPONENT);
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

        // Chat serializer //
        final Class<?> chatSerializerClass = Arrays.stream(CLASS_CHAT_COMPONENT.getClasses())
          .filter(JsonDeserializer.class::isAssignableFrom)
          .findAny()
          // fallback to the 1.7 class?
          .orElseGet(() -> {
            return nmsClass("ChatSerializer");
          });
        final Field gsonField = Arrays.stream(chatSerializerClass.getDeclaredFields())
          .filter(m -> Modifier.isStatic(m.getModifiers()))
          .filter(m -> m.getType().equals(Gson.class))
          .findFirst()
          .orElse(null);
        if(gsonField != null) {
          gsonField.setAccessible(true);
          gson = (Gson) gsonField.get(null);
        } else {
          final Method deserialize = Arrays.stream(chatSerializerClass.getDeclaredMethods())
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getReturnType().equals(CLASS_CHAT_COMPONENT))
            .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(String.class))
            .min(Comparator.comparing(Method::getName)) // prefer the #a method
            .orElse(null);
          if(deserialize != null) {
            textSerializerDeserialize = LOOKUP.unreflect(deserialize);
          }
        }
      }
    } catch(IllegalAccessException | IllegalArgumentException ex) {
      Knobs.logError("finding chat serializer", ex);
    }
    CHAT_PACKET_CONSTRUCTOR = chatPacketConstructor;
    MC_TEXT_GSON = gson;
    TEXT_SERIALIZER_DESERIALIZE = textSerializerDeserialize;
    LEGACY_CHAT_PACKET_CONSTRUCTOR = legacyChatPacketConstructor;
  }

  private static Object mcTextFromComponent(final @NonNull Component message) {
    if((MC_TEXT_GSON == null && TEXT_SERIALIZER_DESERIALIZE == null) || CLASS_CHAT_COMPONENT == null) {
      throw new IllegalStateException("Not supported");
    }
    if(MC_TEXT_GSON != null) {
      final JsonElement json = BukkitPlatform.GSON_SERIALIZER.serializeToTree(message);
      try {
        return MC_TEXT_GSON.fromJson(json, CLASS_CHAT_COMPONENT);
      } catch(Throwable error) {
        Knobs.logError("converting adventure Component to MC Component", error);
        return null;
      }
    } else {
      try {
        return TEXT_SERIALIZER_DESERIALIZE.invoke(BukkitPlatform.GSON_SERIALIZER.serialize(message));
      } catch(Throwable error) {
        Knobs.logError("converting adventure Component to MC Component (via 1.7 String serialization)", error);
        return null;
      }
    }
  }

  /* package */ static class Chat extends PacketSendingHandler<CommandSender> implements Handler.Chat<CommandSender, Object> {
    
    @Override
    public boolean isAvailable() {
      return super.isAvailable() && CHAT_PACKET_CONSTRUCTOR != null;
    }

    @Override
    public Object initState(final @NonNull Component message) {
      final Object nmsMessage = mcTextFromComponent(message);
      if(nmsMessage == null) {
        return null;
      }

      try {
        return CHAT_PACKET_CONSTRUCTOR.invoke(nmsMessage, MESSAGE_TYPE_SYSTEM, NIL_UUID);
      } catch(Throwable throwable) {
        Knobs.logError("constructing MC chat packet", throwable);
        return null;
      }
    }
  }


  // Titles //
  private static final @Nullable Class<?> CLASS_TITLE_PACKET = Crafty.findNmsClass("PacketPlayOutTitle");
  private static final @Nullable Class<?> CLASS_TITLE_ACTION = Crafty.findNmsClass("PacketPlayOutTitle$EnumTitleAction"); // welcome to spigot, where we can't name classes? i guess?
  private static final MethodHandle CONSTRUCTOR_TITLE_MESSAGE = Crafty.findConstructor(CLASS_TITLE_PACKET, CLASS_TITLE_ACTION, CLASS_CHAT_COMPONENT); // (EnumTitleAction, IChatBaseComponent)
  private static final @Nullable MethodHandle CONSTRUCTOR_TITLE_TIMES = Crafty.findConstructor(CLASS_TITLE_PACKET, int.class, int.class, int.class);
  private static final @Nullable Object TITLE_ACTION_TITLE = Crafty.enumValue(CLASS_TITLE_ACTION, "TITLE", 0);
  private static final @Nullable Object TITLE_ACTION_SUBTITLE = Crafty.enumValue(CLASS_TITLE_ACTION, "SUBTITLE", 1);
  private static final @Nullable Object TITLE_ACTION_ACTIONBAR = Crafty.enumValue(CLASS_TITLE_ACTION, "ACTIONBAR");

  /* package */static class ActionBarModern extends PacketSendingHandler<Player> implements Handler.ActionBar<Player, Object> {

    @Override
    public boolean isAvailable() {
      return super.isAvailable() && TITLE_ACTION_ACTIONBAR != null;
    }

    @Override
    public Object initState(final @NonNull Component message) {
      try {
        return CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_ACTIONBAR, mcTextFromComponent(message));
      } catch(Throwable throwable) {
        Knobs.logError("constructing MC action bar packet", throwable);
        return null;
      }
    }
  }

  /* package */ static class ActionBar1_8thru1_11 extends PacketSendingHandler<Player> implements Handler.ActionBar<Player, Object> {

    @Override
    public boolean isAvailable() {
      return super.isAvailable() && LEGACY_CHAT_PACKET_CONSTRUCTOR != null;
    }

    @Override
    public Object initState(final @NonNull Component message) {
      // Action bar through the chat packet doesn't properly support formatting
      final TextComponent legacyMessage = TextComponent.of(LegacyComponentSerializer.legacy().serialize(message));
      try {
        return LEGACY_CHAT_PACKET_CONSTRUCTOR.invoke(mcTextFromComponent(legacyMessage), Chat.TYPE_ACTIONBAR);
      } catch(Throwable throwable) {
        Knobs.logError("constructing legacy MC action bar packet", throwable);
        return null;
      }
    }
  }

  /* package */ static class Titles extends PacketSendingHandler<Player> implements Handler.Titles<Player> {

    @Override
    public boolean isAvailable() {
      return super.isAvailable() && CONSTRUCTOR_TITLE_MESSAGE != null && CONSTRUCTOR_TITLE_TIMES != null;
    }

    @Override
    public void send(final @NonNull Player viewer, final @NonNull Title title) {
      final Object nmsTitleText = mcTextFromComponent(title.title());
      final Object nmsSubtitleText = mcTextFromComponent(title.subtitle());
      try {
        final Object titlePacket = CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_TITLE, nmsTitleText);
        final Object subtitlePacket = CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_SUBTITLE, nmsSubtitleText);
        Object timesPacket = null;

        final int fadeIn = Titles.ticks(title.fadeInTime());
        final int stay = Titles.ticks(title.stayTime());
        final int fadeOut = Titles.ticks(title.fadeOutTime());

        if(fadeIn != -1 || stay != -1 || fadeOut != -1) {
          timesPacket = CONSTRUCTOR_TITLE_TIMES.invoke(fadeIn, stay, fadeOut);
        }

        send(viewer, subtitlePacket);
        if(timesPacket != null) {
          send(viewer, timesPacket);
        }
        send(viewer, titlePacket);
      } catch(Throwable throwable) {
        Knobs.logError("constructing legacy MC title packet", throwable);
      }
    }

    @Override
    public void clear(final @NonNull Player viewer) {
      viewer.sendTitle("", "", -1, -1, -1);
    }

    @Override
    public void reset(final @NonNull Player viewer) {
      viewer.resetTitle();
    }
  }

  /* package */ static class BossBarNameSetter implements BukkitBossBarListener.NameSetter {
    private static final Class<?> CLASS_CRAFT_BOSS_BAR = Crafty.findCraftClass("boss.CraftBossBar");
    private static final Class<?> CLASS_BOSS_BAR_ACTION = Crafty.findNmsClass("PacketPlayOutBoss$Action");
    private static final Object BOSS_BAR_ACTION_TITLE = Crafty.enumValue(CLASS_BOSS_BAR_ACTION, "UPDATE_NAME", 3);
    private static final MethodHandle CRAFT_BOSS_BAR_HANDLE;
    private static final MethodHandle NMS_BOSS_BATTLE_SET_NAME;
    private static final MethodHandle NMS_BOSS_BATTLE_SEND_UPDATE;

    static {
      MethodHandle craftBossBarHandle = null;
      MethodHandle nmsBossBattleSetName = null;
      MethodHandle nmsBossBattleSendUpdate = null;
      if(CLASS_CRAFT_BOSS_BAR != null && CLASS_CHAT_COMPONENT != null && BOSS_BAR_ACTION_TITLE != null) {
        try {
          final Field craftBossBarHandleField = Crafty.field(CLASS_CRAFT_BOSS_BAR, "handle");
          craftBossBarHandle = Crafty.LOOKUP.unreflectGetter(craftBossBarHandleField);
          final Class<?> nmsBossBattleType = craftBossBarHandleField.getType();
          nmsBossBattleSetName = Crafty.LOOKUP.findSetter(nmsBossBattleType, "title", CLASS_CHAT_COMPONENT);
          nmsBossBattleSendUpdate = Crafty.LOOKUP.findVirtual(nmsBossBattleType, "sendUpdate", methodType(void.class, CLASS_BOSS_BAR_ACTION));
        } catch(NoSuchFieldException | IllegalAccessException | NoSuchMethodException ex) {
          Knobs.logError("finding boss bar name operations", ex);
        }
      }
      CRAFT_BOSS_BAR_HANDLE = craftBossBarHandle;
      NMS_BOSS_BATTLE_SET_NAME = nmsBossBattleSetName;
      NMS_BOSS_BATTLE_SEND_UPDATE = nmsBossBattleSendUpdate;
    }

    @Override
    public boolean isAvailable() {
      return ENABLED && (MC_TEXT_GSON != null || TEXT_SERIALIZER_DESERIALIZE != null)
        && CLASS_CRAFT_BOSS_BAR != null && CRAFT_BOSS_BAR_HANDLE != null && NMS_BOSS_BATTLE_SET_NAME != null && NMS_BOSS_BATTLE_SEND_UPDATE != null;
    }

    @Override
    public void setName(final org.bukkit.boss.@NonNull BossBar bar, final @NonNull Component name) {
      try {
        final Object nmsBar = CRAFT_BOSS_BAR_HANDLE.invoke(bar);
        final Object mcText = mcTextFromComponent(name);
        // Boss bar was introduced MC 1.9, but the name setter method didn't exist until later versions, so for max compatibility we'll do field set and update separately
        NMS_BOSS_BATTLE_SET_NAME.invoke(nmsBar, mcText);
        NMS_BOSS_BATTLE_SEND_UPDATE.invoke(nmsBar, BOSS_BAR_ACTION_TITLE);
      } catch(final Error err) {
        throw err;
      } catch(final Throwable ex) {
        Knobs.logError("sending boss bar name change", ex);
      }
    }
  }

  /* package */ static class BossBars_1_8 extends AbstractBossBarListener<Player, PhantomEntity<Wither>> {
    private static final int WITHER_DATA_INVULN_TICKS = 20;
    private static final double WITHER_DISTANCE = 40;
    private static final double WITHER_OFFSET_PITCH = 30 /* degrees */;

    private final PhantomEntityTracker tracker;

    BossBars_1_8(final PhantomEntityTracker tracker) {
      this.tracker = tracker;
    }

    @Override
    public boolean isAvailable() {
      return ENABLED && PhantomEntity.Impl.SUPPORTED && Crafty.hasClass("org.bukkit.entity.Wither");
    }

    @Override
    public void bossBarNameChanged(final @NonNull BossBar bar, final @NonNull Component oldName, final @NonNull Component newName) {
      handle(bar, newName, (val, entity) -> {
        if(entity.entity() != null) {
          entity.entity().setCustomName(legacy(val));
          entity.sendUpdate();
        }
      });
    }

    private static double health(float percent, double maxHealth) {
      return percent * (maxHealth - 0.1f) + 0.1f; // don't go to zero health -- if we do the death animation is shown
    }

    @Override
    @SuppressWarnings("deprecation")
    public void bossBarPercentChanged(final @NonNull BossBar bar, final float oldPercent, final float newPercent) {
      handle(bar, newPercent, (val, entity) -> {
        if(entity.entity() != null) {
          entity.entity().setHealth(health(val, entity.entity().getMaxHealth()));
          entity.sendUpdate();
        }
      });
    }

    @Override
    @SuppressWarnings("deprecation")
    protected @NonNull PhantomEntity<Wither> newInstance(final @NonNull BossBar adventure) {
      final PhantomEntity<Wither> tracker = this.tracker.create(Wither.class)
        .relative(WITHER_DISTANCE, WITHER_OFFSET_PITCH, 0)
        .invisible(true)
        .data(WITHER_DATA_INVULN_TICKS, 890); // hide the shimmering armor when below 50% health
      final @Nullable Wither entity = tracker.entity();
      if(entity != null) {
        entity.setCustomName(legacy(adventure.name()));
        entity.setHealth(health(adventure.percent(), entity.getMaxHealth()));
      }
      return tracker;
    }

    @Override
    protected void show(final @NonNull Player viewer, final @NonNull PhantomEntity<Wither> bar) {
      bar.add(viewer);
    }

    @Override
    protected boolean hide(final @NonNull Player viewer, final @NonNull PhantomEntity<Wither> bar) {
      return bar.remove(viewer);
    }

    @Override
    protected boolean isEmpty(final @NonNull PhantomEntity<Wither> bar) {
      return !bar.watching();
    }

    @Override
    protected void hideFromAll(final @NonNull PhantomEntity<Wither> bar) {
      bar.removeAll();
    }
  }

  /* package */ static class Books extends PacketSendingHandler<Player> implements Handler.Books<Player> {
    private static final Class<?> CLASS_ENUM_HAND = Crafty.findNmsClass("EnumHand");
    private static final Object HAND_MAIN = Crafty.enumValue(CLASS_ENUM_HAND, "MAIN_HAND", 0);
    private static final Class<?> PACKET_OPEN_BOOK = Crafty.findNmsClass("PacketPlayOutOpenWrittenBook");
    private static final MethodHandle NEW_PACKET_OPEN_BOOK = Crafty.findConstructor(PACKET_OPEN_BOOK, CLASS_ENUM_HAND);

    @Override
    public boolean isAvailable() {
      return super.isAvailable() && NEW_PACKET_OPEN_BOOK != null;
    }

    @Override
    public void openBook(final @NonNull Player viewer, final @NonNull Book book) {
      // Build NBT (w/ adventure)
      // set to nms ItemStack
      // send inventory update item
      // send open book packet
      // restore main hand item
      // todo: actually implement
      try {
        send(viewer, NEW_PACKET_OPEN_BOOK.invoke(HAND_MAIN));
      } catch(Throwable throwable) {
        Knobs.logError("sending book to " + viewer, throwable);
      }
    }
  }

  // before 1.13 the open book packet is a packet250
  /* package */ static class Books_Pre1_13 extends PacketSendingHandler<Player> implements Handler.Books<Player> {
    private static final int HAND_MAIN = 0;
    private static final String PACKET_TYPE_BOOK_OPEN = "MC|BOpen";
    private static final Class<?> CLASS_BYTE_BUF = Crafty.findClass("io.netty.buffer.ByteBuf");
    private static final Class<?> CLASS_PACKET_CUSTOM_PAYLOAD = Crafty.findNmsClass("PacketPlayOutCustomPayload");
    private static final Class<?> CLASS_PACKET_DATA_SERIALIZER = Crafty.findNmsClass("PacketDataSerializer");

    private static final MethodHandle NEW_PACKET_CUSTOM_PAYLOAD = Crafty.findConstructor(CLASS_PACKET_CUSTOM_PAYLOAD, String.class, CLASS_PACKET_DATA_SERIALIZER); // (channelId: String, payload: PacketByteBuf)
    private static final MethodHandle NEW_PACKET_BYTE_BUF = Crafty.findConstructor(CLASS_PACKET_DATA_SERIALIZER, CLASS_BYTE_BUF); // (wrapped: ByteBuf)

    @Override
    public void openBook(final @NonNull Player viewer, final @NonNull Book book) {
      // TODO: construct stack
      final ByteBuf data = Unpooled.buffer();
      data.writeByte(HAND_MAIN);
      try {
        final Object packetByteBuf = NEW_PACKET_BYTE_BUF.invoke(data);
        send(viewer, NEW_PACKET_CUSTOM_PAYLOAD.invoke(PACKET_TYPE_BOOK_OPEN, packetByteBuf));
      } catch(Throwable throwable) {
        Knobs.logError("sending legacy open book packet to " + viewer, throwable);
      }
    }
  }
}
