/*
 * This file is part of text-adapters, licensed under the MIT License.
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
import net.kyori.text.serializer.ComponentSerializers;
import org.bukkit.Bukkit;
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
  private static final ReflectionBindings REFLECTION_BINDINGS;
  static {
    ReflectionBindings reflectionBindings;
    try {
      // check we're dealing with a "CraftServer" and that the server isn't non-versioned.
      final Class<?> server = Bukkit.getServer().getClass();
      if(!server.getSimpleName().equals("CraftServer") ||
        server.getName().equals("org.bukkit.craftbukkit.CraftServer") ||
        !server.getPackage().getName().startsWith("org.bukkit.craftbukkit.")) {
        throw new UnsupportedOperationException("Incompatible server version");
      }
      final String serverVersion = server.getPackage().getName().substring("org.bukkit.craftbukkit.".length());
      final Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + serverVersion + ".entity.CraftPlayer");
      final Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
      final Class<?> entityPlayerClass = getHandleMethod.getReturnType();
      final Field playerConnectionField = entityPlayerClass.getField("playerConnection");
      final Class<?> playerConnectionClass = playerConnectionField.getType();
      final Class<?> packetClass = Class.forName("net.minecraft.server." + serverVersion + ".Packet");
      final Method sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);
      final Class<?> baseComponentClass = Class.forName("net.minecraft.server." + serverVersion + ".IChatBaseComponent");
      final Class<?> chatPacketClass = Class.forName("net.minecraft.server." + serverVersion + ".PacketPlayOutChat");
      final Constructor<?> chatPacketConstructor = chatPacketClass.getConstructor(baseComponentClass);
      final Class<?> chatSerializerClass = Arrays.stream(baseComponentClass.getClasses())
        .filter(JsonDeserializer.class::isAssignableFrom)
        .findAny()
        // fallback to the 1.7 class?
        .orElseGet(() -> {
          try {
            return Class.forName("net.minecraft.server." + serverVersion + ".ChatSerializer");
          } catch(final ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        });
      final Method serializeMethod = Arrays.stream(chatSerializerClass.getMethods())
        .filter(m -> Modifier.isStatic(m.getModifiers()))
        .filter(m -> m.getReturnType().equals(baseComponentClass))
        .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(String.class))
        .min(Comparator.comparing(Method::getName)) // prefer the #a method
        .orElseThrow(() -> new RuntimeException("Unable to find serialize method"));
      reflectionBindings = new ReflectionBindings(getHandleMethod, playerConnectionField, sendPacketMethod, chatPacketConstructor, serializeMethod);
    } catch(final Exception e) {
      reflectionBindings = new ReflectionBindings(e);
    }
    REFLECTION_BINDINGS = reflectionBindings;
  }

  @Override
  public void sendComponent(final List<? extends CommandSender> senders, final Component component) {
    if(REFLECTION_BINDINGS.error != null) {
      return;
    }
    Object packet = null;
    for(final Iterator<? extends CommandSender> iterator = senders.iterator(); iterator.hasNext(); ) {
      final CommandSender sender = iterator.next();
      if(sender instanceof Player) {
        try {
          final Player player = (Player) sender;
          if(packet == null) {
            packet = REFLECTION_BINDINGS.createPacket(component);
          }
          REFLECTION_BINDINGS.sendPacket(packet, player);
          iterator.remove();
        } catch(final Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private static final class ReflectionBindings {
    private final Exception error;
    private final Method getHandleMethod;
    private final Field playerConnectionField;
    private final Method sendPacketMethod;
    private final Constructor<?> chatPacketConstructor;
    private final Method serializeMethod;

    ReflectionBindings(final Method getHandleMethod, final Field playerConnectionField, final Method sendPacketMethod, final Constructor<?> chatPacketConstructor, final Method serializeMethod) {
      this.error = null;
      this.getHandleMethod = getHandleMethod;
      this.playerConnectionField = playerConnectionField;
      this.sendPacketMethod = sendPacketMethod;
      this.chatPacketConstructor = chatPacketConstructor;
      this.serializeMethod = serializeMethod;
    }

    ReflectionBindings(final Exception error) {
      this.error = error;
      this.getHandleMethod = null;
      this.playerConnectionField = null;
      this.sendPacketMethod = null;
      this.chatPacketConstructor = null;
      this.serializeMethod = null;
    }

    Object createPacket(final Component component) {
      final String json = ComponentSerializers.JSON.serialize(component);
      try {
        return this.chatPacketConstructor.newInstance(this.serializeMethod.invoke(null, json));
      } catch(final Exception e) {
        throw new UnsupportedOperationException("Unable to send component messages. Error occurred during packet creation.", e);
      }
    }

    void sendPacket(final Object packet, final Player player) {
      try {
        final Object connection = this.playerConnectionField.get(this.getHandleMethod.invoke(player));
        this.sendPacketMethod.invoke(connection, packet);
      } catch(final Exception e) {
        throw new UnsupportedOperationException("Unable to send component messages. Error occurred during packet sending.", e);
      }
    }
  }
}
