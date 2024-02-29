package net.kyori.adventure.platform.bungeecord;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jetbrains.annotations.Nullable;

/**
 * Reflection utilities for accessing legacy BungeeCord methods
 */
public class BungeeReflection {

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  private BungeeReflection() {
  }

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
    for (Class<?> parameter : parameters) {
      if (parameter == null) return false;
    }
    try {
      holderClass.getMethod(methodName, parameters);
      return true;
    } catch (NoSuchMethodException ignored) {
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
    for (Class<?> parameter : parameters) {
      if (parameter == null) return null;
    }
    try {
      return LOOKUP.findVirtual(holderClass, methodName, MethodType.methodType(returnType, parameters));
    } catch (NoSuchMethodException | IllegalAccessException ignored) {
    }
    return null;
  }

}
