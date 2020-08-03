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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

import com.google.common.annotations.Beta;
import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.reflection.qual.ForName;

import static java.util.Objects.requireNonNull;

/**
 * Reflection utilities for accessing {@code net.minecraft.server}.
 *
 * <p>This is not an official API and can break at any time. You've been warned.</p>
 */
@Beta // Causes users to see "UnstableApiUsage"
public final class MinecraftReflection {
  private MinecraftReflection() {
  }

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final String PREFIX_NMS = "net.minecraft.server";
  private static final String PREFIX_CRAFTBUKKIT = "org.bukkit.craftbukkit";
  private static final String CRAFT_SERVER = "CraftServer";
  private static final @Nullable String VERSION;

  static {
    final Class<?> serverClass = Bukkit.getServer().getClass(); // TODO: use reflection here too?
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

  /**
   * Gets a class by its name.
   *
   * @param className a class name
   * @return a class or {@code null} if not found
   */
  @ForName
  public static @Nullable Class<?> findClass(final @NonNull String className) {
    try {
      return Class.forName(className);
    } catch(final ClassNotFoundException e) {
      return null;
    }
  }

  /**
   * Gets whether a class is loaded.
   *
   * @param className a class name
   * @return if the class is loaded
   */
  public static boolean hasClass(final @NonNull String className) {
    return findClass(className) != null;
  }

  /**
   * Gets a handle for a class method.
   *
   * @param holderClass a class
   * @param methodName a method name
   * @param returnClass a method return class
   * @param parameterClasses an array of method parameter classes
   * @return a method handle or {@code null} if not found
   */
  public static @Nullable MethodHandle findMethod(final @Nullable Class<?> holderClass, final String methodName, final @Nullable Class<?> returnClass, final Class<?>... parameterClasses) {
    if(holderClass == null || returnClass == null) return null;
    for(final Class<?> parameterClass : parameterClasses) {
      if(parameterClass == null) return null;
    }

    try {
      return LOOKUP.findVirtual(holderClass, methodName, MethodType.methodType(returnClass, parameterClasses));
    } catch(final NoSuchMethodException | IllegalAccessException e) {
      return null;
    }
  }

  /**
   * Gets a handle for a class method.
   *
   * @param holderClass a class
   * @param methodName a method name
   * @param returnClass a method return class
   * @param parameterClasses an array of method parameter classes
   * @return a method handle or {@code null} if not found
   */
  public static @Nullable MethodHandle findStaticMethod(final @Nullable Class<?> holderClass, final String methodName, final @Nullable Class<?> returnClass, final Class<?>... parameterClasses) {
    if(holderClass == null || returnClass == null) return null;
    for(final Class<?> parameterClass : parameterClasses) {
      if(parameterClass == null) return null;
    }

    try {
      return LOOKUP.findStatic(holderClass, methodName, MethodType.methodType(returnClass, parameterClasses));
    } catch(final NoSuchMethodException | IllegalAccessException e) {
      return null;
    }
  }

  /**
   * Gets whether a class has a method.
   *
   * @param holderClass a class
   * @param methodName a method name
   * @param parameterClasses an array of method parameter classes
   * @return if the method exists
   */
  public static boolean hasMethod(final @Nullable Class<?> holderClass, final String methodName, final Class<?>... parameterClasses) {
    if(holderClass == null) return false;
    for(final Class<?> parameterClass : parameterClasses) {
      if(parameterClass == null) return false;
    }

    try {
      holderClass.getMethod(methodName, parameterClasses);
      return true;
    } catch(final NoSuchMethodException e) {
      return false;
    }
  }

  /**
   * Gets a handle for a class constructor.
   *
   * @param holderClass a class
   * @param parameterClasses an array of method parameter classes
   * @return a method handle or {@code null} if not found
   */
  public static @Nullable MethodHandle findConstructor(final @Nullable Class<?> holderClass, final @Nullable Class<?>... parameterClasses) {
    if(holderClass == null) return null;
    for(final Class<?> parameterClass : parameterClasses) {
      if(parameterClass == null) return null;
    }

    try {
      return LOOKUP.findConstructor(holderClass, MethodType.methodType(void.class, parameterClasses));
    } catch(final NoSuchMethodException | IllegalAccessException e) {
      return null;
    }
  }

  /**
   * Gets a class field and makes it accessible.
   *
   * @param holderClass a class
   * @param fieldName a field name
   * @return an accessible field
   * @throws NoSuchFieldException when thrown by {@link Class#getDeclaredField(String)}
   */
  public static @NonNull Field needField(final @NonNull Class<?> holderClass, final @NonNull String fieldName) throws NoSuchFieldException {
    final Field field = holderClass.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field;
  }

  /**
   * Gets an enum value.
   *
   * @param enumClass an enum class
   * @param enumName an enum name
   * @return an enum value or {@code null} if not found
   */
  public static @Nullable Object findEnum(final @Nullable Class<?> enumClass, final @NonNull String enumName) {
    return findEnum(enumClass, enumName, Integer.MAX_VALUE);
  }

  /**
   * Gets an enum value.
   *
   * @param enumClass an enum class
   * @param enumName an enum name
   * @param enumFallbackOrdinal an enum ordinal, when the name is not found
   * @return an enum value or {@code null} if not found
   */
  @SuppressWarnings("unchecked")
  public static @Nullable Object findEnum(final @Nullable Class<?> enumClass, final @NonNull String enumName, final int enumFallbackOrdinal) {
    if(enumClass == null || !Enum.class.isAssignableFrom(enumClass)) {
      return null;
    }

    try {
      return Enum.valueOf(enumClass.asSubclass(Enum.class), enumName);
    } catch(final IllegalArgumentException e) {
      final Object[] constants = enumClass.getEnumConstants();
      if(constants.length > enumFallbackOrdinal) {
        return constants[enumFallbackOrdinal];
      }
    }

    return null;
  }

  /**
   * Gets whether CraftBukkit is available.
   *
   * @return if CraftBukkit is available
   */
  public static boolean isCraftBukkit() {
    return VERSION != null;
  }

  /**
   * Gets a {@code org.bukkit.craftbukkit} class name.
   *
   * @param className a class name, without the {@code org.bukkit.craftbukkit} prefix
   * @return a class name or {@code null} if not found
   */
  public static @Nullable String findCraftClassName(final @NonNull String className) {
    return isCraftBukkit() ? PREFIX_CRAFTBUKKIT + VERSION + className : null;
  }

  /**
   * Gets a {@code org.bukkit.craftbukkit} class.
   *
   * @param className a class name, without the {@code org.bukkit.craftbukkit} prefix
   * @return a class or {@code null} if not found
   */
  @ForName
  public static @Nullable Class<?> findCraftClass(final @NonNull String className) {
    final String craftClassName = findCraftClassName(className);
    if(craftClassName == null) {
      return null;
    }

    return findClass(craftClassName);
  }

  /**
   * Gets a {@code org.bukkit.craftbukkit} class.
   *
   * @param className a class name, without the {@code org.bukkit.craftbukkit} prefix
   * @param superClass a super class
   * @return a class or {@code null} if not found
   * @param <T> a super type
   */
  @ForName
  public static <T> @Nullable Class<? extends T> findCraftClass(final @NonNull String className, final @NonNull Class<T> superClass) {
    final Class<?> craftClass = findCraftClass(className);
    if(craftClass == null || !requireNonNull(superClass, "superClass").isAssignableFrom(craftClass)) {
      return null;
    }
    return craftClass.asSubclass(superClass);
  }

  /**
   * Gets a {@code org.bukkit.craftbukkit} class.
   *
   * @param className a class name, without the {@code org.bukkit.craftbukkit} prefix
   * @return a class
   * @throws NullPointerException if the class was not found
   */
  @ForName
  public static @NonNull Class<?> needCraftClass(final @NonNull String className) {
    return requireNonNull(findCraftClass(className), "Could not find org.bukkit.craftbukkit class " + className);
  }

  /**
   * Gets a {@code net.minecraft.server} class name.
   *
   * @param className a class name, without the {@code net.minecraft.server} prefix
   * @return a class name or {@code null} if not found
   */
  public static @Nullable String findNmsClassName(final @NonNull String className) {
    return isCraftBukkit() ? PREFIX_NMS + VERSION + className : null;
  }

  /**
   * Get a {@code net.minecraft.server} class.
   *
   * @param className a class name, without the {@code net.minecraft.server} prefix
   * @return a class name or {@code null} if not found
   */
  @ForName
  public static @Nullable Class<?> findNmsClass(final @NonNull String className) {
    final String nmsClassName = findNmsClassName(className);
    if(nmsClassName == null) {
      return null;
    }

    return findClass(nmsClassName);
  }

  /**
   * Gets a {@code net.minecraft.server} class.
   *
   * @param className a class name, without the {@code org.bukkit.craftbukkit} prefix
   * @return a class
   * @throws NullPointerException if the class was not found
   */
  @ForName
  public static @NonNull Class<?> needNmsClass(final @NonNull String className) {
    return requireNonNull(findNmsClass(className), "Could not find net.minecraft.server class " + className);
  }

  /**
   * Gets the singleton method handle lookup.
   *
   * @return the method handle lookup
   */
  public static MethodHandles.@NonNull Lookup lookup() {
    return LOOKUP;
  }
}
