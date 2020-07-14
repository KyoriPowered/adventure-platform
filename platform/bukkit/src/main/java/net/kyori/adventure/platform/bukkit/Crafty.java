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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.function.Consumer;
import javax.annotation.Nonnegative;
import net.kyori.adventure.platform.common.Knobs;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.reflection.qual.ForName;

import static java.util.Objects.requireNonNull;

/**
 * Get your snacks... and your hacks
 */
/* package */ final class Crafty {

  private Crafty() {
  }

  // Determining our environment //
  static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final String PREFIX_NMS = "net.minecraft.server";
  private static final String PREFIX_CRAFTBUKKIT = "org.bukkit.craftbukkit";
  private static final String CRAFT_SERVER = "CraftServer";
  private static final @Nullable String VERSION;

  static {
    final Class<?> serverClass = Bukkit.getServer().getClass();
    if(!serverClass.getSimpleName().equals(CRAFT_SERVER)) {
      Knobs.logError("finding CraftServer", null);
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

  /* package */ static boolean hasClass(final @NonNull String clazz) {
    try {
      Class.forName(clazz);
      return true;
    } catch(final ClassNotFoundException ex) {
      Knobs.logError("finding class", ex);
      return false;
    }
  }

  /* package */ static @Nullable Class<?> findClass(final @NonNull String clazz) {
    try {
      return Class.forName(clazz);
    } catch(final ClassNotFoundException ex) {
      Knobs.logError("finding class", ex);
      return null;
    }
  }

  /* package */ static boolean hasMethod(final @Nullable Class<?> klass, final @NonNull String methodName, final @Nullable Class<?> @NonNull ... parameters) {
    if(klass == null) {
      return false;
    }
    
    for(final Class<?> param : parameters) {
      if(param == null) {
        return false;
      }
    }

    try {
      klass.getMethod(methodName, parameters);
      return true;
    } catch(final NoSuchMethodException e) {
      Knobs.logError("finding method", e);
      return false;
    }
  }

  /* package */ static @Nullable MethodHandle findConstructor(final @Nullable Class<?> target, final @Nullable Class<?> @NonNull... pTypes) {
    if(target == null) {
      return null;
    }
    for(final Class<?> clazz : pTypes) {
      if(clazz == null) return null;
    }

    try {
      return LOOKUP.findConstructor(target, MethodType.methodType(void.class, pTypes));
    } catch(final NoSuchMethodException | IllegalAccessException ex) {
      Knobs.logError("finding constructor", ex);
      return null;
    }
  }

  /* package */ static @Nullable MethodHandle findMethod(final @Nullable Class<?> holder, final String methodName, final Class<?> rType, final Class<?>... pTypes) {
    if(holder == null) return null;
    if(rType == null) return null;
    for(final Class<?> clazz : pTypes) {
      if(clazz == null) return null;
    }

    try {
      return LOOKUP.findVirtual(holder, methodName, MethodType.methodType(rType, pTypes));
    } catch(final NoSuchMethodException | IllegalAccessException ex) {
      Knobs.logError("finding method", ex);
      return null;
    }
  }

  /* package */ static @Nullable MethodHandle findStatic(final @Nullable Class<?> holder, final String methodName, final Class<?> rType, final Class<?>... pTypes) {
    if(holder == null) return null;
    if(rType == null) return null;
    for(final Class<?> clazz : pTypes) {
      if(clazz == null) return null;
    }

    try {
      return LOOKUP.findStatic(holder, methodName, MethodType.methodType(rType, pTypes));
    } catch(final NoSuchMethodException | IllegalAccessException ex) {
      Knobs.logError("finding method", ex);
      return null;
    }
  }

  /**
   * Get a field from {@code klass} and make it accessible .
   * 
   * @param klass containing class
   * @param name field name
   * @return the field
   * @throws NoSuchFieldException when thrown by {@link Class#getDeclaredField(String)}
   */
  /* package */ static Field field(final @NonNull Class<?> klass, final @NonNull String name) throws NoSuchFieldException {
    final Field field = klass.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  /* package */ static @Nullable Object enumValue(final @Nullable Class<?> klass, final String name) {
    return enumValue(klass, name, Integer.MAX_VALUE);
  }

  @SuppressWarnings("unchecked")
  /* package */ static @Nullable Object enumValue(final @Nullable Class<?> klass, final @NonNull String name, final int ordinal) {
    if(klass == null) {
      return null;
    }
    if(!Enum.class.isAssignableFrom(klass)) {
      return null;
    }

    try {
      return Enum.valueOf(klass.asSubclass(Enum.class), name);
    } catch(final IllegalArgumentException ex) {
      final Object[] constants = klass.getEnumConstants();
      if(constants.length > ordinal) {
        return constants[ordinal];
      }
    }
    Knobs.logError("finding enum value for " + klass + ": " + name, null);
    return null;
  }

  /* package */ static boolean hasCraftBukkit() {
    return VERSION != null;
  }

  /**
   * Get the versioned class name from a class name without the o.b.c prefix.
   *
   * @param name The name of the class without the "org.bukkit.craftbukkit" prefix
   * @return The versioned class name, or {@code null} if not CraftBukkit.
   */
  /* package */ static @Nullable String craftClassName(final @NonNull String name) {
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
  /* package */ static @Nullable String nmsClassName(final @NonNull String name) {
    if(VERSION == null) {
      return null;
    }

    return PREFIX_NMS + VERSION + name;
  }

  @ForName
  /* package */ static @Nullable Class<?> findCraftClass(final @NonNull String name) {
    final /* @Nullable */ String className = craftClassName(name);
    if(className == null) {
      return null;
    }

    return findClass(className);
  }
  
  @ForName
  /* package */ static <T> @Nullable Class<? extends T> findCraftClass(final @NonNull String name, final @Nonnegative Class<T> parentType) {
    final /* @Nullable */ Class<?> clazz = findCraftClass(name);
    if(clazz == null || !requireNonNull(parentType, "parentType").isAssignableFrom(clazz)) {
      return null;
    }
    return clazz.asSubclass(parentType);
  }

  @ForName
  /* package */ static @Nullable Class<?> findNmsClass(final @NonNull String name) {
    final /* @Nullable */ String className = nmsClassName(name);
    if(className == null) {
      return null;
    }

    return findClass(className);
  }

  @ForName
  /* package */ static Class<?> craftClass(final @NonNull String name) {
    return requireNonNull(findCraftClass(name), "Could not find CraftBukkit class " + name);
  }

  @ForName
  /* package */ static Class<?> nmsClass(final @NonNull String name) {
    return requireNonNull(findNmsClass(name), "Could not find net.minecraft.server class " + name);
  }

  // Events //
  private static final Listener EVENT_LISTENER = new Listener() {};

  /* package */ static <T extends Event> void registerEvent(final @NonNull Plugin owner, final Class<T> type, final Consumer<T> handler) {
    registerEvent(owner, type, EventPriority.NORMAL, true, handler);
  }

  @SuppressWarnings("unchecked")
  /* package */ static <T extends Event> void registerEvent(final @NonNull Plugin owner, final Class<T> type, final EventPriority priority, final boolean ignoreCancelled, final Consumer<T> handler) {
    requireNonNull(handler, "handler");
    Bukkit.getServer().getPluginManager().registerEvent(type, EVENT_LISTENER, priority, (listener, event) -> handler.accept((T) event), owner, ignoreCancelled);
  }
}
