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
package net.kyori.text.adapter.bukkit;

import com.google.gson.JsonDeserializer;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

final class CraftBukkitAdapter implements Adapter {
  static final Binding BINDING = load();

  private static Binding load() {
    try {
      if(!Shiny.isCompatibleServer()) {
        throw new UnsupportedOperationException("Incompatible server version");
      }
      final Class<?> craftPlayerClass = Shiny.craftClass("entity.CraftPlayer");
      final Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
      final Class<?> entityPlayerClass = getHandleMethod.getReturnType();
      final Field playerConnectionField = entityPlayerClass.getField("playerConnection");
      final Class<?> playerConnectionClass = playerConnectionField.getType();
      final Class<?> packetClass = Shiny.vanillaClass("Packet");
      final Method sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);
      final Class<?> baseComponentClass = Shiny.vanillaClass("IChatBaseComponent");
      final Class<?> chatPacketClass = Shiny.vanillaClass("PacketPlayOutChat");
      final Constructor<?> chatPacketConstructor = chatPacketClass.getConstructor(baseComponentClass);
      final Class<?> titlePacketClass = Shiny.maybeVanillaClass("PacketPlayOutTitle");
      final Class<? extends Enum> titlePacketClassAction;
      final Constructor<?> titlePacketConstructor;
      if(titlePacketClass != null) {
        titlePacketClassAction = (Class<? extends Enum>) Shiny.vanillaClass("PacketPlayOutTitle$EnumTitleAction");
        titlePacketConstructor = titlePacketClass.getConstructor(titlePacketClassAction, baseComponentClass);
      } else {
        titlePacketClassAction = null;
        titlePacketConstructor = null;
      }
      final Class<?> chatSerializerClass = Arrays.stream(baseComponentClass.getClasses())
        .filter(JsonDeserializer.class::isAssignableFrom)
        .findAny()
        // fallback to the 1.7 class?
        .orElseGet(() -> {
          try {
            return Shiny.vanillaClass("ChatSerializer");
          } catch(final ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        });
      final Method deserializeMethod = Arrays.stream(chatSerializerClass.getMethods())
        .filter(m -> Modifier.isStatic(m.getModifiers()))
        .filter(m -> m.getReturnType().equals(baseComponentClass))
        .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(String.class))
        .min(Comparator.comparing(Method::getName)) // prefer the #a method
        .orElseThrow(() -> new RuntimeException("Unable to find deserialize method"));
      final Method serializeMethod = Arrays.stream(chatSerializerClass.getMethods())
        .filter(m -> Modifier.isStatic(m.getModifiers()))
        .filter(m -> m.getReturnType().equals(String.class))
        .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(baseComponentClass))
        .min(Comparator.comparing(Method::getName)) // prefer the #a method
        .orElseThrow(() -> new RuntimeException("Unable to find serialize method"));
      return new AliveBinding(getHandleMethod, playerConnectionField, sendPacketMethod, chatPacketConstructor, titlePacketClassAction, titlePacketConstructor, deserializeMethod, serializeMethod);
    } catch(final Throwable e) {
      return new DeadBinding();
    }
  }

  @Override
  public void sendComponent(final List<? extends CommandSender> viewers, final Component component, final boolean actionBar) {
    if(!BINDING.valid()) {
      return;
    }
    Object packet = null;
    for(final Iterator<? extends CommandSender> iterator = viewers.iterator(); iterator.hasNext(); ) {
      final CommandSender sender = iterator.next();
      if(sender instanceof Player) {
        try {
          final Player player = (Player) sender;
          if(packet == null) {
            packet = BINDING.createPacket(component, actionBar);
          }
          BINDING.sendPacket(packet, player);
          iterator.remove();
        } catch(final Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  static abstract class Binding {
    abstract boolean valid();

    abstract Object createPacket(final Component component, final boolean actionBar);

    abstract void sendPacket(final Object packet, final Player player);

    abstract Component asKyori(final Object object);

    abstract Object asVanilla(final Component component);
  }

  private static final class DeadBinding extends Binding {
    @Override
    boolean valid() {
      return false;
    }

    @Override
    Object createPacket(final Component component, final boolean actionBar) {
      throw new UnsupportedOperationException();
    }

    @Override
    void sendPacket(final Object packet, final Player player) {
      throw new UnsupportedOperationException();
    }

    @Override
    Component asKyori(final Object object) {
      throw new UnsupportedOperationException();
    }

    @Override
    Object asVanilla(final Component component) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class AliveBinding extends Binding {
    private final Method getHandleMethod;
    private final Field playerConnectionField;
    private final Method sendPacketMethod;
    private final Constructor<?> chatPacketConstructor;
    private final Class<? extends Enum> titlePacketClassAction;
    private final Constructor<?> titlePacketConstructor;
    private final boolean canMakeTitle;
    private final Method deserializeMethod;
    private final Method serializeMethod;

    AliveBinding(final Method getHandleMethod, final Field playerConnectionField, final Method sendPacketMethod, final Constructor<?> chatPacketConstructor, final Class<? extends Enum> titlePacketClassAction, final Constructor<?> titlePacketConstructor, final Method deserializeMethod, final Method serializeMethod) {
      this.getHandleMethod = getHandleMethod;
      this.playerConnectionField = playerConnectionField;
      this.sendPacketMethod = sendPacketMethod;
      this.chatPacketConstructor = chatPacketConstructor;
      this.titlePacketClassAction = titlePacketClassAction;
      this.titlePacketConstructor = titlePacketConstructor;
      this.canMakeTitle = this.titlePacketClassAction != null && this.titlePacketConstructor != null;
      this.deserializeMethod = deserializeMethod;
      this.serializeMethod = serializeMethod;
    }

    @Override
    boolean valid() {
      return true;
    }

    @Override
    Object createPacket(final Component component, final boolean actionBar) {
      final String json = Adapter.asJson(component);
      try {
        if(actionBar && this.canMakeTitle) {
          Enum constant;
          try {
            constant = Enum.valueOf(this.titlePacketClassAction, "ACTIONBAR");
          } catch(final IllegalArgumentException e) {
            constant = this.titlePacketClassAction.getEnumConstants()[2];
          }
          return this.titlePacketConstructor.newInstance(constant, this.deserializeMethod.invoke(null, json));
        } else {
          return this.chatPacketConstructor.newInstance(this.deserializeMethod.invoke(null, json));
        }
      } catch(final Exception e) {
        throw new UnsupportedOperationException("An exception was encountered while creating a packet for a component", e);
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

    @Override
    Component asKyori(final Object object) {
      try {
        return GsonComponentSerializer.INSTANCE.deserialize((String) this.serializeMethod.invoke(null, object));
      } catch(final Throwable t) {
        throw new UnsupportedOperationException("An exception was encountered while converting from vanilla Component", t);
      }
    }

    @Override
    Object asVanilla(final Component component) {
      try {
        return this.deserializeMethod.invoke(null, Adapter.asJson(component));
      } catch(final Throwable t) {
        throw new UnsupportedOperationException("An exception was encountered while converting to vanilla Component", t);
      }
    }
  }
}
