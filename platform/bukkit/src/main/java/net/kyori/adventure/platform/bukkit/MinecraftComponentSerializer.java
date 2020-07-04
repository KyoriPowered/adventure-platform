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

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import net.kyori.adventure.platform.impl.Knobs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static net.kyori.adventure.platform.bukkit.Crafty.LOOKUP;
import static net.kyori.adventure.platform.bukkit.Crafty.nmsClass;

/**
 * An implementation that converts between Adventure
 * components and native server Components.
 *
 * <p>Due to Bukkit's package namespacing,
 * this interface's return type does not reflect the actual value type.
 * Components are {@code net.minecraft.server.<version>.IChatBaseComponent} instances.</p>
 *
 * <p>Downsampling will be performed as necessary for the running server version</p>
 *
 * <p>If not {@link #supported()}, an {@link IllegalStateException} will be thrown on any serialize or deserialize operations</p>
 */
public class MinecraftComponentSerializer implements ComponentSerializer<Component, Component, Object> {
  public static final MinecraftComponentSerializer INSTANCE = new MinecraftComponentSerializer();

  /* package */ static final @Nullable Class<?> CLASS_CHAT_COMPONENT = Crafty.findNmsClass("IChatBaseComponent");
  private static final Gson MC_TEXT_GSON;
  private static final MethodHandle TEXT_SERIALIZER_DESERIALIZE;
  private static final MethodHandle TEXT_SERIALIZER_SERIALIZE;

  static {
    Gson gson = null;
    MethodHandle textSerializerDeserialize = null;
    MethodHandle textSerializerSerialize = null;

    try {
      if(CLASS_CHAT_COMPONENT != null) {
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
          final Method[] declaredMethods = chatSerializerClass.getDeclaredMethods();
          final Method deserialize = Arrays.stream(declaredMethods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getReturnType().equals(CLASS_CHAT_COMPONENT))
            .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(String.class))
            .min(Comparator.comparing(Method::getName)) // prefer the #a method
            .orElse(null);
          final Method serialize = Arrays.stream(declaredMethods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getReturnType().equals(String.class))
            .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(CLASS_CHAT_COMPONENT))
            .findFirst()
            .orElse(null);
          if(deserialize != null) {
            textSerializerDeserialize = LOOKUP.unreflect(deserialize);
          }
          if(serialize != null) {
            textSerializerSerialize = LOOKUP.unreflect(serialize);
          }
        }
      }
    } catch(IllegalAccessException | IllegalArgumentException ex) {
      Knobs.logError("finding chat serializer", ex);
    }
    MC_TEXT_GSON = gson;
    TEXT_SERIALIZER_DESERIALIZE = textSerializerDeserialize;
    TEXT_SERIALIZER_SERIALIZE = textSerializerSerialize;
  }

  public static boolean supported() {
    return (MC_TEXT_GSON != null || (TEXT_SERIALIZER_DESERIALIZE != null && TEXT_SERIALIZER_SERIALIZE != null)) && CLASS_CHAT_COMPONENT != null;
  }

  @Override
  public @NonNull Component deserialize(final @NonNull Object input) {
    if(!supported()) {
      throw new IllegalStateException("Components cannot be converted properly");
    }
    if(MC_TEXT_GSON != null) {
      final JsonElement element = MC_TEXT_GSON.toJsonTree(input);
      return BukkitPlatform.GSON_SERIALIZER.serializer().fromJson(element, Component.class);
    } else { // when we don't share a Gson instance
      try {
        return GsonComponentSerializer.gson().deserialize((String) TEXT_SERIALIZER_SERIALIZE.invoke(input));
      } catch(final Throwable throwable) {
        Knobs.logError("converting MC component to Adventure component", throwable);
        throw new RuntimeException(throwable);
      }
    }
  }

  @Override
  public @NonNull Object serialize(final @NonNull Component component) {
    if(!supported()) {
      throw new IllegalStateException("Not supported");
    }
    if(MC_TEXT_GSON != null) {
      final JsonElement json = BukkitPlatform.GSON_SERIALIZER.serializer().toJsonTree(component);
      try {
        return MC_TEXT_GSON.fromJson(json, CLASS_CHAT_COMPONENT);
      } catch(final Throwable error) {
        Knobs.logError("converting adventure Component to MC Component", error);
        throw new RuntimeException(error); // unrecoverable
      }
    } else {
      try {
        return TEXT_SERIALIZER_DESERIALIZE.invoke(BukkitPlatform.GSON_SERIALIZER.serialize(component));
      } catch(final Throwable error) {
        Knobs.logError("converting adventure Component to MC Component (via 1.7 String serialization)", error);
        throw new RuntimeException(error); // unrecoverable
      }
    }
  }
}
