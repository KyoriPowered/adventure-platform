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
package net.kyori.adventure.text.serializer.craftbukkit;

import com.google.common.annotations.Beta;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static net.kyori.adventure.text.serializer.craftbukkit.BukkitComponentSerializer.gson;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findClass;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findNmsClass;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.lookup;

/**
 * A component serializer for {@code net.minecraft.server.<version>.IChatBaseComponent}.
 *
 * <p>Due to Bukkit version namespaces, the return type does not reflect the actual type.</p>
 *
 * <p>Color downsampling will be performed as necessary for the running server version.</p>
 *
 * <p>If not {@link #isSupported()}, an {@link UnsupportedOperationException} will be thrown on any serialize or deserialize operations.</p>
 *
 * @see #get()
 */
@Beta // Causes users to see "UnstableApiUsage"
public final class MinecraftComponentSerializer implements ComponentSerializer<Component, Component, Object> {
  private static final MinecraftComponentSerializer INSTANCE = new MinecraftComponentSerializer();

  /**
   * Gets whether this serializer is supported.
   *
   * @return if the serializer is supported.
   */
  public static boolean isSupported() {
    return SUPPORTED;
  }

  /**
   * Gets the component serializer.
   *
   * @return a component serializer
   */
  public static @NonNull MinecraftComponentSerializer get() {
    return INSTANCE;
  }

  private static final @Nullable Class<?> CLASS_JSON_DESERIALIZER = findClass("com.goo".concat("gle.gson.JsonDeserializer")); // Hide from relocation checkers
  private static final @Nullable Class<?> CLASS_CHAT_COMPONENT = findNmsClass("IChatBaseComponent");
  private static final AtomicReference<RuntimeException> INITIALIZATION_ERROR = new AtomicReference<>(new UnsupportedOperationException());

  private static final Object MC_TEXT_GSON;
  private static final MethodHandle TEXT_SERIALIZER_DESERIALIZE;
  private static final MethodHandle TEXT_SERIALIZER_SERIALIZE;

  static {
    Object gson = null;
    MethodHandle textSerializerDeserialize = null;
    MethodHandle textSerializerSerialize = null;

    try {
      if(CLASS_CHAT_COMPONENT != null) {
        // Chat serializer //
        final Class<?> chatSerializerClass = Arrays.stream(CLASS_CHAT_COMPONENT.getClasses())
          .filter(c -> CLASS_JSON_DESERIALIZER != null && CLASS_JSON_DESERIALIZER.isAssignableFrom(c))
          .findAny()
          .orElse(null);
        if(chatSerializerClass != null) {
          final Field gsonField = Arrays.stream(chatSerializerClass.getDeclaredFields())
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getType().equals(Gson.class))
            .findFirst()
            .orElse(null);
          if(gsonField != null) {
            gsonField.setAccessible(true);
            gson = gsonField.get(null);
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
              textSerializerDeserialize = lookup().unreflect(deserialize);
            }
            if(serialize != null) {
              textSerializerSerialize = lookup().unreflect(serialize);
            }
          }
        }
      }
    } catch(final Throwable error) {
      INITIALIZATION_ERROR.set(new UnsupportedOperationException(error));
    }

    MC_TEXT_GSON = gson;
    TEXT_SERIALIZER_DESERIALIZE = textSerializerDeserialize;
    TEXT_SERIALIZER_SERIALIZE = textSerializerSerialize;
  }

  private static final boolean SUPPORTED = MC_TEXT_GSON != null || (TEXT_SERIALIZER_DESERIALIZE != null && TEXT_SERIALIZER_SERIALIZE != null);

  @Override
  public @NonNull Component deserialize(final @NonNull Object input) {
    if(!SUPPORTED) throw INITIALIZATION_ERROR.get();

    try {
      if(MC_TEXT_GSON != null) {
        final JsonElement element = ((Gson) MC_TEXT_GSON).toJsonTree(input);
        return gson().serializer().fromJson(element, Component.class);
      }
      return GsonComponentSerializer.gson().deserialize((String) TEXT_SERIALIZER_SERIALIZE.invoke(input));
    } catch(final Throwable error) {
      throw new UnsupportedOperationException(error);
    }
  }

  @Override
  public @NonNull Object serialize(final @NonNull Component component) {
    if(!SUPPORTED) throw INITIALIZATION_ERROR.get();

    if(MC_TEXT_GSON != null) {
      final JsonElement json = gson().serializer().toJsonTree(component);
      try {
        return ((Gson) MC_TEXT_GSON).fromJson(json, CLASS_CHAT_COMPONENT);
      } catch(final Throwable error) {
        throw new UnsupportedOperationException(error);
      }
    } else {
      try {
        return TEXT_SERIALIZER_DESERIALIZE.invoke(gson().serialize(component));
      } catch(final Throwable error) {
        throw new UnsupportedOperationException(error);
      }
    }
  }
}
