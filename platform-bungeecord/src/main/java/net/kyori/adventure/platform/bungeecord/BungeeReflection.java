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
package net.kyori.adventure.platform.bungeecord;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.jetbrains.annotations.Nullable;

/**
 * Reflection utilities for accessing legacy BungeeCord methods.
 */
final class BungeeReflection {
  private BungeeReflection() {
  }

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  /**
   * Checks if the specified class has a method with the given name and parameter types.
   *
   * @param holderClass The class to check for the method. Can be {@code null}, in which case the method returns {@code false}.
   * @param methodName  The name of the method to check for.
   * @param parameters  The parameter types of the method. The method returns {@code false} if any parameter type is {@code null}.
   * @return {@code true} if the method exists in the class; {@code false} otherwise.
   */
  public static boolean hasMethod(final @Nullable Class<?> holderClass, final String methodName, final Class<?>... parameters) {
    if (holderClass == null) return false;
    for (final Class<?> parameter : parameters) {
      if (parameter == null) return false;
    }
    try {
      holderClass.getMethod(methodName, parameters);
      return true;
    } catch (final NoSuchMethodException ignored) {
    }
    return false;
  }

  /**
   * Finds and returns a {@link MethodHandle} for the specified method of the given class. This allows for dynamic method invocation.
   *
   * @param holderClass The class containing the method.
   * @param methodName  The name of the method to find.
   * @param returnType  The return type of the method.
   * @param parameters  The parameter types of the method.
   * @return A {@link MethodHandle} for the specified method, or {@code null} if the method cannot be found or if any parameter is {@code null}.
   */
  public static MethodHandle findMethod(final @Nullable Class<?> holderClass, final String methodName, final Class<?> returnType, final Class<?>... parameters) {
    if (holderClass == null || returnType == null) return null;
    for (final Class<?> parameter : parameters) {
      if (parameter == null) return null;
    }
    try {
      return LOOKUP.findVirtual(holderClass, methodName, MethodType.methodType(returnType, parameters));
    } catch (NoSuchMethodException | IllegalAccessException ignored) {
    }
    return null;
  }

}
