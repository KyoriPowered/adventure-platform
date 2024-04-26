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
import com.google.gson.JsonElement;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.invoke.MethodHandles.insertArguments;
import static net.kyori.adventure.platform.bukkit.BukkitComponentSerializer.gson;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findCraftClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findMcClassName;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findNmsClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findNmsClassName;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findStaticMethod;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.lookup;

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
 * @since 4.0.0
 */
@ApiStatus.Experimental // due to direct server implementation references
public final class MinecraftComponentSerializer implements ComponentSerializer<Component, Component, Object> {
  private static final MinecraftComponentSerializer INSTANCE = new MinecraftComponentSerializer();

  /**
   * Gets whether this serializer is supported.
   *
   * @return if the serializer is supported.
   * @since 4.0.0
   */
  public static boolean isSupported() {
    return SUPPORTED;
  }

  /**
   * Gets the component serializer.
   *
   * @return a component serializer
   * @since 4.0.0
   */
  public static @NotNull MinecraftComponentSerializer get() {
    return INSTANCE;
  }

  private static final @Nullable Class<?> CLASS_JSON_DESERIALIZER = findClass("com.goo".concat("gle.gson.JsonDeserializer")); // Hide from relocation checkers
  private static final @Nullable Class<?> CLASS_CHAT_COMPONENT = findClass(
    findNmsClassName("IChatBaseComponent"),
    findMcClassName("network.chat.IChatBaseComponent"),
    findMcClassName("network.chat.Component")
  );
  private static final @Nullable Class<?> CLASS_CRAFT_REGISTRY = findCraftClass("CraftRegistry");
  private static final @Nullable Class<?> CLASS_REGISTRY_ACCESS = findClass(
    findMcClassName("core.IRegistryCustom"),
    findMcClassName("core.RegistryAccess")
  );
  private static final @Nullable MethodHandle GET_REGISTRY = findStaticMethod(CLASS_CRAFT_REGISTRY, "getMinecraftRegistry", CLASS_REGISTRY_ACCESS);
  private static final AtomicReference<RuntimeException> INITIALIZATION_ERROR = new AtomicReference<>(new UnsupportedOperationException());

  private static final Object MC_TEXT_GSON;
  private static final MethodHandle TEXT_SERIALIZER_DESERIALIZE;
  private static final MethodHandle TEXT_SERIALIZER_SERIALIZE;
  private static final MethodHandle TEXT_SERIALIZER_DESERIALIZE_TREE;
  private static final MethodHandle TEXT_SERIALIZER_SERIALIZE_TREE;

  static {
    Object gson = null;
    MethodHandle textSerializerDeserialize = null;
    MethodHandle textSerializerSerialize = null;
    MethodHandle textSerializerDeserializeTree = null;
    MethodHandle textSerializerSerializeTree = null;

    try {
      if (CLASS_CHAT_COMPONENT != null) {
        final Object registryAccess = GET_REGISTRY != null ? GET_REGISTRY.invoke() : null;
        // Chat serializer //
        final Class<?> chatSerializerClass = Arrays.stream(CLASS_CHAT_COMPONENT.getClasses())
          .filter(c -> {
            if (CLASS_JSON_DESERIALIZER != null) {
              return CLASS_JSON_DESERIALIZER.isAssignableFrom(c);
            } else {
              for (final Class<?> itf : c.getInterfaces()) {
                if (itf.getSimpleName().equals("JsonDeserializer")) {
                  return true;
                }
              }
              return false;
            }
          })
          .findAny()
          .orElse(findNmsClass("ChatSerializer")); // 1.7.10 compat
        if (chatSerializerClass != null) {
          final Field gsonField = Arrays.stream(chatSerializerClass.getDeclaredFields())
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getType().equals(Gson.class))
            .findFirst()
            .orElse(null);
          if (gsonField != null) {
            gsonField.setAccessible(true);
            gson = gsonField.get(null);
          }
        }
        final List<Class<?>> candidates = new ArrayList<>();
        if (chatSerializerClass != null) {
          candidates.add(chatSerializerClass);
        }
        candidates.addAll(Arrays.asList(CLASS_CHAT_COMPONENT.getClasses()));
        for (final Class<?> serializerClass : candidates) {
          final Method[] declaredMethods = serializerClass.getDeclaredMethods();
          final Method deserialize = Arrays.stream(declaredMethods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> CLASS_CHAT_COMPONENT.isAssignableFrom(m.getReturnType()))
            .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(String.class))
            .min(Comparator.comparing(Method::getName)) // prefer the #a method
            .orElse(null);
          final Method serialize = Arrays.stream(declaredMethods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getReturnType().equals(String.class))
            .filter(m -> m.getParameterCount() == 1 && CLASS_CHAT_COMPONENT.isAssignableFrom(m.getParameterTypes()[0]))
            .findFirst()
            .orElse(null);
          final Method deserializeTree = Arrays.stream(declaredMethods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> CLASS_CHAT_COMPONENT.isAssignableFrom(m.getReturnType()))
            .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(JsonElement.class))
            .findFirst()
            .orElse(null);
          final Method serializeTree = Arrays.stream(declaredMethods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getReturnType().equals(JsonElement.class))
            .filter(m -> m.getParameterCount() == 1 && CLASS_CHAT_COMPONENT.isAssignableFrom(m.getParameterTypes()[0]))
            .findFirst()
            .orElse(null);
          final Method deserializeTreeWithRegistryAccess = Arrays.stream(declaredMethods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> CLASS_CHAT_COMPONENT.isAssignableFrom(m.getReturnType()))
            .filter(m -> m.getParameterCount() == 2)
            .filter(m -> m.getParameterTypes()[0].equals(JsonElement.class))
            .filter(m -> m.getParameterTypes()[1].isInstance(registryAccess))
            .findFirst()
            .orElse(null);
          final Method serializeTreeWithRegistryAccess = Arrays.stream(declaredMethods)
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getReturnType().equals(JsonElement.class))
            .filter(m -> m.getParameterCount() == 2)
            .filter(m -> CLASS_CHAT_COMPONENT.isAssignableFrom(m.getParameterTypes()[0]))
            .filter(m -> m.getParameterTypes()[1].isInstance(registryAccess))
            .findFirst()
            .orElse(null);
          if (deserialize != null) {
            textSerializerDeserialize = lookup().unreflect(deserialize);
          }
          if (serialize != null) {
            textSerializerSerialize = lookup().unreflect(serialize);
          }
          if (deserializeTree != null) {
            textSerializerDeserializeTree = lookup().unreflect(deserializeTree);
          } else if (deserializeTreeWithRegistryAccess != null) {
            deserializeTreeWithRegistryAccess.setAccessible(true);
            textSerializerDeserializeTree = insertArguments(lookup().unreflect(deserializeTreeWithRegistryAccess), 1, registryAccess);
          }
          if (serializeTree != null) {
            textSerializerSerializeTree = lookup().unreflect(serializeTree);
          } else if (serializeTreeWithRegistryAccess != null) {
            serializeTreeWithRegistryAccess.setAccessible(true);
            textSerializerSerializeTree = insertArguments(lookup().unreflect(serializeTreeWithRegistryAccess), 1, registryAccess);
          }
        }
      }
    } catch (final Throwable error) {
      INITIALIZATION_ERROR.set(new UnsupportedOperationException("Error occurred during initialization", error));
    }

    MC_TEXT_GSON = gson;
    TEXT_SERIALIZER_DESERIALIZE = textSerializerDeserialize;
    TEXT_SERIALIZER_SERIALIZE = textSerializerSerialize;
    TEXT_SERIALIZER_DESERIALIZE_TREE = textSerializerDeserializeTree;
    TEXT_SERIALIZER_SERIALIZE_TREE = textSerializerSerializeTree;
  }

  private static final boolean SUPPORTED = MC_TEXT_GSON != null || (TEXT_SERIALIZER_DESERIALIZE != null && TEXT_SERIALIZER_SERIALIZE != null) || (TEXT_SERIALIZER_DESERIALIZE_TREE != null && TEXT_SERIALIZER_SERIALIZE_TREE != null);

  @Override
  public @NotNull Component deserialize(final @NotNull Object input) {
    if (!SUPPORTED) throw INITIALIZATION_ERROR.get();

    try {
      final JsonElement element;
      if (TEXT_SERIALIZER_SERIALIZE_TREE != null) {
        element = (JsonElement) TEXT_SERIALIZER_SERIALIZE_TREE.invoke(input);
      } else if (MC_TEXT_GSON != null) {
        element = ((Gson) MC_TEXT_GSON).toJsonTree(input);
      } else {
        return gson().deserialize((String) TEXT_SERIALIZER_SERIALIZE.invoke(input));
      }
      return gson().serializer().fromJson(element, Component.class);
    } catch (final Throwable error) {
      throw new UnsupportedOperationException(error);
    }
  }

  @Override
  public @NotNull Object serialize(final @NotNull Component component) {
    if (!SUPPORTED) throw INITIALIZATION_ERROR.get();

    if (TEXT_SERIALIZER_DESERIALIZE_TREE != null || MC_TEXT_GSON != null) {
      final JsonElement json = gson().serializer().toJsonTree(component);
      try {
        if (TEXT_SERIALIZER_DESERIALIZE_TREE != null) {
          return TEXT_SERIALIZER_DESERIALIZE_TREE.invoke(json);
        }
        return ((Gson) MC_TEXT_GSON).fromJson(json, CLASS_CHAT_COMPONENT);
      } catch (final Throwable error) {
        throw new UnsupportedOperationException(error);
      }
    } else {
      try {
        return TEXT_SERIALIZER_DESERIALIZE.invoke(gson().serialize(component));
      } catch (final Throwable error) {
        throw new UnsupportedOperationException(error);
      }
    }
  }
}
