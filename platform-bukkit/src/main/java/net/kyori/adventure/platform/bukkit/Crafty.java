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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Excluder;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.reflection.qual.ForName;

import static java.util.Objects.requireNonNull;

/**
 * Get your snacks... and your hacks
 */
final class Crafty {

  private Crafty() {
  }

  // Determining our environment //
  static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final String PREFIX_NMS = "net.minecraft.server";
  private static final String PREFIX_CRAFTBUKKIT = "org.bukkit.craftbukkit";
  private static final String CRAFT_SERVER = "CraftServer";
  private static final @Nullable String VERSION;

  static {
    Class<?> serverClass = Bukkit.getServer().getClass();
    if(!serverClass.getSimpleName().equals(CRAFT_SERVER)) {
      VERSION = null;
    } else if(serverClass.getName().equals(PREFIX_CRAFTBUKKIT + "." + CRAFT_SERVER)) {
      VERSION = ".";
    } else {
      String name = serverClass.getName();
      name = name.substring(PREFIX_CRAFTBUKKIT.length());
      name = name.substring(0, name.length() - CRAFT_SERVER.length());
      VERSION = name;
    }
  }

  static boolean hasClass(final @NonNull String clazz) {
    try {
      Class.forName(clazz);
      return true;
    } catch(ClassNotFoundException e) {
      return false;
    }
  }

  static @Nullable Class<?> findClass(final @NonNull String clazz) {
    try {
      return Class.forName(clazz);
    } catch(ClassNotFoundException ex) {
      return null;
    }
  }

  static boolean hasMethod(final @NonNull Class<?> klass, final @NonNull String methodName, final @Nullable Class<?> @NonNull ... parameters) {
    for(Class<?> param : parameters) {
      if(param == null) {
        return false;
      }
    }

    try {
      klass.getMethod(methodName, parameters);
      return true;
    } catch(NoSuchMethodException e) {
      return false;
    }
  }

  static Field field(final Class<?> klass, final String name) throws NoSuchFieldException {
    final Field field = klass.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  @SuppressWarnings("unchecked")
  static @Nullable Object enumValue(@Nullable Class<?> klass, String name, int ordinal) {
    if(klass == null) {
      return null;
    }
    if(!klass.isAssignableFrom(Enum.class)) {
      return null;
    }

    try {
      return Enum.valueOf(klass.asSubclass(Enum.class), name);
    } catch(IllegalArgumentException ex) {
      final Object[] constants = klass.getEnumConstants();
      if(constants.length > ordinal) {
        return ordinal;
      }
    }
    return null;
  }

  public static boolean hasCraftBukkit() {
    return VERSION != null;
  }

  /**
   * Get the versioned class name from a class name without the o.b.c prefix.
   *
   * @param name The name of the class without the "org.bukkit.craftbukkit" prefix
   * @return The versioned class name, or {@code null} if not CraftBukkit.
   */
  static @Nullable String craftClassName(String name) {
    if(VERSION == null) {
      return null;
    }

    return PREFIX_CRAFTBUKKIT + VERSION + name;
  }

  /**
   * Get the versioned class name from a class name without the o.b.c prefix.
   *
   * @param name The name of the class without the "net.minecraft.server" prefix
   * @return The versioned class name, or {@code null} if not CraftBukkit.
   */
  public static @Nullable String nmsClassName(String name) {
    if(VERSION == null) {
      return null;
    }

    return PREFIX_NMS + VERSION + name;
  }

  @ForName
  static @Nullable Class<?> findCraftClass(@NonNull String name) {
    final @Nullable String className = craftClassName(name);
    if(className == null) {
      return null;
    }

    return findClass(className);
  }

  @ForName
  static @Nullable Class<?> findNmsClass(@NonNull String name) {
    final @Nullable String className = nmsClassName(name);
    if(className == null) {
      return null;
    }

    return findClass(className);
  }

  @ForName
  static Class<?> craftClass(String name) {
    return requireNonNull(findCraftClass(name), "Could not find CraftBukkit class " + name);
  }

  @ForName
  static Class<?> nmsClass(String name) {
    return requireNonNull(findNmsClass(name), "Could not find net.minecraft.server class " + name);
  }


  // Gson //

  @SuppressWarnings("unchecked")
  public static boolean injectGson(Gson existing, Consumer<GsonBuilder> accepter) {
    try {
      final Field factoriesField = field(Gson.class, "factories");
      final Field builderFactoriesField = field(GsonBuilder.class, "factories");
      final Field builderHierarchyFactoriesField = field(GsonBuilder.class, "hierarchyFactories");

      final GsonBuilder builder = new GsonBuilder();
      accepter.accept(builder);

      final List<TypeAdapterFactory> existingFactories = (List<TypeAdapterFactory>) factoriesField.get(existing);
      final List<TypeAdapterFactory> newFactories = new ArrayList<>();
      newFactories.addAll((List<TypeAdapterFactory>) builderFactoriesField.get(builder));
      Collections.reverse(newFactories);
      newFactories.addAll((List<TypeAdapterFactory>) builderHierarchyFactoriesField.get(builder));

      final List<TypeAdapterFactory> modifiedFactories = new ArrayList<>(existingFactories);

      // the excluder must precede all adapters that handle user-defined types
      final int index = findExcluderIndex(modifiedFactories);

      for(final TypeAdapterFactory newFactory : Lists.reverse(newFactories)) {
        modifiedFactories.add(index, newFactory);
      }

      /*Class<?> treeTypeAdapterClass; // TODO: Is this necessary?
      try {
        // newer gson releases
        treeTypeAdapterClass = Class.forName("com.google.gson.internal.bind.TreeTypeAdapter");
      } catch(final ClassNotFoundException e) {
        // old gson releases
        treeTypeAdapterClass = Class.forName("com.google.gson.TreeTypeAdapter");
      }

      final Method newFactoryWithMatchRawTypeMethod = treeTypeAdapterClass.getMethod("newFactoryWithMatchRawType", TypeToken.class, Object.class);
      final TypeAdapterFactory adapterComponentFactory = (TypeAdapterFactory) newFactoryWithMatchRawTypeMethod.invoke(null, TypeToken.get(AdapterComponent.class), new SpigotAdapter.Serializer());
      modifiedFactories.add(index, adapterComponentFactory);*/

      factoriesField.set(existing, modifiedFactories);
      return true;
    } catch(NoSuchFieldException | IllegalAccessException ex) {
      return false;
    }
  }

  private static int findExcluderIndex(final List<TypeAdapterFactory> factories) {
    for(int i = 0, size = factories.size(); i < size; i++) {
      final TypeAdapterFactory factory = factories.get(i);
      if(factory instanceof Excluder) {
        return i + 1;
      }
    }
    return 0;
  }
}
