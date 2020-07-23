/*
 * This file is part of text-extras, licensed under the MIT License.
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
package net.kyori.text.adapter.bukkit;

import com.google.gson.JsonDeserializer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class CraftBukkitAdapter implements Adapter {
  private static final Binding REFLECTION_BINDINGS = load();
  private static final boolean ALIVE = REFLECTION_BINDINGS.valid();

  private static Binding load() {
    try {
      final Class<?> server = Bukkit.getServer().getClass();
      if(!isCompatibleServer(server)) {
        throw new UnsupportedOperationException("Incompatible server version");
      }
      final String serverVersion = maybeVersion(server.getPackage().getName().substring("org.bukkit.craftbukkit".length()));
      final Class<?> craftPlayerClass = craftBukkitClass(serverVersion, "entity.CraftPlayer");
      final Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
      final Class<?> entityPlayerClass = getHandleMethod.getReturnType();
      final Field playerConnectionField = entityPlayerClass.getField("playerConnection");
      final Class<?> playerConnectionClass = playerConnectionField.getType();
      final Class<?> packetClass = minecraftClass(serverVersion, "Packet");
      final Method sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);
      final Class<?> baseComponentClass = minecraftClass(serverVersion, "IChatBaseComponent");
      final Class<?> chatMessageTypeClass = optionalMinecraftClass(serverVersion, "ChatMessageType");
      final Object chatMessageTypeSystem = enumValue(chatMessageTypeClass, "SYSTEM", 1);
      final Class<?> chatPacketClass = minecraftClass(serverVersion, "PacketPlayOutChat");

      boolean longConstructor = false;
      Constructor<?> chatPacketConstructor;
      try {
        chatPacketConstructor = chatPacketClass.getConstructor(baseComponentClass);
      } catch(NoSuchMethodException ex) { // 1.16 chat packet change
        if(chatMessageTypeClass == null) {
          throw ex;
        }
        longConstructor = true;
        chatPacketConstructor = chatPacketClass.getConstructor(baseComponentClass, chatMessageTypeClass, UUID.class);
      }
      final Constructor<?> legacyChatPacketConstructor = optionalConstructor(chatPacketClass, baseComponentClass, byte.class);
      final Class<?> titlePacketClass = optionalMinecraftClass(serverVersion, "PacketPlayOutTitle");
      final Object titleActionActionBar;
      final Constructor<?> titlePacketConstructor;
      if(titlePacketClass != null) {
        Class<?> titlePacketClassAction = minecraftClass(serverVersion, "PacketPlayOutTitle$EnumTitleAction");
        titleActionActionBar = enumValue(titlePacketClassAction, "ACTIONBAR", Integer.MAX_VALUE); // added after 1.8 in middle of enum, we don't want to look up by ordinal (or else we get TIMES)
        titlePacketConstructor = titlePacketClass.getConstructor(titlePacketClassAction, baseComponentClass);
      } else {
        titleActionActionBar = null;
        titlePacketConstructor = null;
      }
      final Class<?> chatSerializerClass = Arrays.stream(baseComponentClass.getClasses())
        .filter(JsonDeserializer.class::isAssignableFrom)
        .findAny()
        // fallback to the 1.7 class?
        .orElseGet(() -> {
          try {
            return minecraftClass(serverVersion, "ChatSerializer");
          } catch(final ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        });
      final Method serializeMethod = Arrays.stream(chatSerializerClass.getMethods())
        .filter(m -> Modifier.isStatic(m.getModifiers()))
        .filter(m -> baseComponentClass.isAssignableFrom(m.getReturnType()))
        .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(String.class))
        .min(Comparator.comparing(Method::getName)) // prefer the #a method
        .orElseThrow(() -> new RuntimeException("Unable to find serialize method"));
      return new AliveBinding(getHandleMethod, playerConnectionField, sendPacketMethod, chatMessageTypeSystem, legacyChatPacketConstructor, chatPacketConstructor, titleActionActionBar, titlePacketConstructor, serializeMethod, longConstructor);
    } catch(final Throwable e) {
      return new DeadBinding();
    }
  }

  private static boolean isCompatibleServer(final Class<?> serverClass) {
    return serverClass.getPackage().getName().startsWith("org.bukkit.craftbukkit")
      && serverClass.getSimpleName().equals("CraftServer");
  }

  private static Class<?> craftBukkitClass(final String version, final String name) throws ClassNotFoundException {
    return Class.forName("org.bukkit.craftbukkit." + version + name);
  }

  private static Class<?> minecraftClass(final String version, final String name) throws ClassNotFoundException {
    return Class.forName("net.minecraft.server." + version + name);
  }

  private static String maybeVersion(final String version) {
    if(version.isEmpty()) {
      return "";
    } else if(version.charAt(0) == '.') {
      return version.substring(1) + '.';
    }
    throw new IllegalArgumentException("Unknown version " + version);
  }

  private static Class<?> optionalMinecraftClass(final String version, final String name) {
    try {
      return minecraftClass(version, name);
    } catch(final ClassNotFoundException e) {
      return null;
    }
  }

  @Override
  public void sendMessage(final List<? extends CommandSender> viewers, final Component component) {
    if(!ALIVE) {
      return;
    }
    send(viewers, component, REFLECTION_BINDINGS::createMessagePacket);
  }

  @Override
  public void sendActionBar(final List<? extends CommandSender> viewers, final Component component) {
    if(!ALIVE) {
      return;
    }
    send(viewers, component, REFLECTION_BINDINGS::createActionBarPacket);
  }

  private static void send(final List<? extends CommandSender> viewers, final Component component, final Function<Component, Object> function) {
    Object packet = null;
    for(final Iterator<? extends CommandSender> iterator = viewers.iterator(); iterator.hasNext(); ) {
      final CommandSender sender = iterator.next();
      if(sender instanceof Player) {
        try {
          final Player player = (Player) sender;
          if(packet == null) {
            packet = function.apply(component);
          }
          REFLECTION_BINDINGS.sendPacket(packet, player);
          iterator.remove();
        } catch(final Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private static abstract class Binding {
    abstract boolean valid();

    abstract Object createMessagePacket(final Component component);

    abstract Object createActionBarPacket(final Component component);

    abstract void sendPacket(final Object packet, final Player player);
  }

  private static final class DeadBinding extends Binding {
    @Override
    boolean valid() {
      return false;
    }

    @Override
    Object createMessagePacket(final Component component) {
      throw new UnsupportedOperationException();
    }

    @Override
    Object createActionBarPacket(final Component component) {
      throw new UnsupportedOperationException();
    }

    @Override
    void sendPacket(final Object packet, final Player player) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class AliveBinding extends Binding {
    private static final UUID NIL_UUID = new UUID(0, 0);
    private static final byte LEGACY_CHAT_MESSAGE_TYPE_ACTION_BAR = 2;

    private final Method getHandleMethod;
    private final Field playerConnectionField;
    private final Method sendPacketMethod;
    private final Object chatMessageTypeSystem;
    private final Constructor<?> legacyChatPacketConstructor;
    private final Constructor<?> chatPacketConstructor;
    private final Object titlePacketActionActionBar;
    private final Constructor<?> titlePacketConstructor;
    private final boolean canMakeTitle;
    private final Method serializeMethod;
    private final boolean longChatPacketConstructor;

    AliveBinding(final Method getHandleMethod, final Field playerConnectionField, final Method sendPacketMethod, final Object chatMessageTypeSystem, final Constructor<?> legacyChatPacketConstructor, final Constructor<?> chatPacketConstructor, final Object titlePacketActionActionBar, final Constructor<?> titlePacketConstructor, final Method serializeMethod, final boolean longChatPacketConstructor) {
      this.getHandleMethod = getHandleMethod;
      this.playerConnectionField = playerConnectionField;
      this.sendPacketMethod = sendPacketMethod;
      this.chatMessageTypeSystem = chatMessageTypeSystem;
      this.legacyChatPacketConstructor = legacyChatPacketConstructor;
      this.chatPacketConstructor = chatPacketConstructor;
      this.titlePacketConstructor = titlePacketConstructor;
      this.titlePacketActionActionBar = titlePacketActionActionBar;
      this.canMakeTitle = this.titlePacketConstructor != null && this.titlePacketActionActionBar != null;
      this.serializeMethod = serializeMethod;
      this.longChatPacketConstructor = longChatPacketConstructor;
    }

    @Override
    boolean valid() {
      return true;
    }

    @Override
    Object createMessagePacket(final Component component) {
      final String json = GsonComponentSerializer.INSTANCE.serialize(component);
      try {
        final Object mc = this.serializeMethod.invoke(null, json);
        if(longChatPacketConstructor) {
          return this.chatPacketConstructor.newInstance(mc, this.chatMessageTypeSystem, NIL_UUID);
        } else {
          return this.chatPacketConstructor.newInstance(mc);
        }
      } catch(final Exception e) {
        throw new UnsupportedOperationException("An exception was encountered while creating a packet for a component", e);
      }
    }

    @Override
    Object createActionBarPacket(final Component component) {
      if(this.canMakeTitle) { // fuly supported (1.11+)
        try {
          final String json = GsonComponentSerializer.INSTANCE.serialize(component);
          return this.titlePacketConstructor.newInstance(this.titlePacketActionActionBar, this.serializeMethod.invoke(null, json));
        } catch(final Exception e) {
          throw new UnsupportedOperationException("An exception was encountered while creating a packet for a component", e);
        }
      } else if(this.legacyChatPacketConstructor != null) { // 1.8-1.10.2
        final String legacy = LegacyComponentSerializer.legacy().serialize(component);
        final String json = GsonComponentSerializer.INSTANCE.serialize(TextComponent.of(legacy));

        try {
          return this.legacyChatPacketConstructor.newInstance(this.serializeMethod.invoke(null, json), LEGACY_CHAT_MESSAGE_TYPE_ACTION_BAR);
        } catch(final Exception e) {
          throw new UnsupportedOperationException("An exception was encountered while creating a packet for a component", e);
        }
      } else { // oh well
        return this.createMessagePacket(component);
      }
    }

    @Override
    void sendPacket(final Object packet, final Player player) {
      try {
        final Object connection = this.playerConnectionField.get(this.getHandleMethod.invoke(player));
        this.sendPacketMethod.invoke(connection, packet);
      } catch(final Exception e) {
        throw new UnsupportedOperationException("An exception was encountered while sending a packet for a component", e);
      }
    }
  }

  @SuppressWarnings("unchecked") // intellij lies, it's needed
  private static @Nullable Object enumValue(final @Nullable Class<?> klass, String name, int ordinal) {
    if(klass == null) {
      return null;
    }
    if(!Enum.class.isAssignableFrom(klass)) {
      return null;
    }

    try {
      return Enum.valueOf(klass.asSubclass(Enum.class), name);
    } catch(IllegalArgumentException ex) {
      final Object[] constants = klass.getEnumConstants();
      if(constants.length > ordinal) {
        return constants[ordinal];
      }
    }
    return null;
  }

  private static <T> @Nullable Constructor<T> optionalConstructor(@Nullable Class<T> klass, @NonNull Class<?> @NonNull... args) {
    if (klass == null) {
      return null;
    }

    try {
      return klass.getConstructor(args);
    } catch(NoSuchMethodException e) {
      return null;
    }
  }
}
