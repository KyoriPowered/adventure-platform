package net.kyori.adventure.platform.bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.platform.facet.Knob;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.platform.facet.Knob.logError;

final class MappedBuilder {
  private final Class<?> builtClass;
  private final Class<?> builderClass;
  private final MethodHandle builderFactory;
  private final MethodHandle buildMethod;
  private final Map<String, MethodHandle> builderMethods;

  static @Nullable MappedBuilder mappedBuilder(final @Nullable Class<?> built, final @Nullable Class<?> builder, final String builderMethodName) {
    if (built == null || builder == null) {
      return null;
    }

    final MethodHandle builderMethod = MinecraftReflection.findStaticMethod(built, builderMethodName, builder);
    final MethodHandle buildMethod = MinecraftReflection.findMethod(builder, "build", built);

    if (builderMethod == null || buildMethod == null) return null;

    return new MappedBuilder(built, builder, builderMethod, buildMethod, discoverBuilderMethods(builder));
  }

  private static Map<String, MethodHandle> discoverBuilderMethods(final Class<?> builderClass) {
    final Map<String, MethodHandle> discoveredMethods = new HashMap<>();

    for (final Method method : builderClass.getMethods()) {
      final String name = method.getName();
      final int mod = method.getModifiers();

      if (!Modifier.isPublic(mod) || Modifier.isStatic(mod)) continue;
      if (
        !builderClass.isAssignableFrom(method.getReturnType())
        && !method.getReturnType().isAssignableFrom(builderClass)
      ) continue;
      if (name.equals("build")) continue;

      method.setAccessible(true);

      try {
        discoveredMethods.put(name, MinecraftReflection.lookup().unreflect(method));
      } catch (final IllegalAccessException ex) {
        logError(ex, "Could not unreflect builder method {}", method);
      }
    }

    return Collections.unmodifiableMap(discoveredMethods);
  }

  public MappedBuilder(final Class<?> built, final Class<?> builder, final MethodHandle builderFactory, final MethodHandle build, final Map<String, MethodHandle> builderMethods) {
    this.builtClass = built;
    this.builderClass = builder;
    this.builderFactory = builderFactory;
    this.buildMethod = build;
    this.builderMethods = builderMethods;
  }

  public Instance begin() {
    try {
      final Object builder = this.builderFactory.invoke();
      return new Instance(builder);
    } catch (final Throwable ex) {
      logError(ex, "Failed to create builder instance for {}", this.builderClass);
      return new Instance(null);
    }
  }

  final class Instance {
    private final Object builder;

    Instance(final Object builder) {
      this.builder = builder;
    }

    Instance set(final String key, final Object value) {
      if (this.builder != null) {
        final MethodHandle builderMethod = MappedBuilder.this.builderMethods.get(key);
        if (builderMethod == null) {
          Knob.logMessage("No builder method found in {} for {}", MappedBuilder.this.builderClass, key);
        } else {
          try {
            builderMethod.invoke(this.builder, value);
          } catch (final Throwable ex) {
            logError(ex, "Failed to invoke builder method {}", builderMethod);
          }
        }
      }

      return this;
    }

    @Nullable Object build() {
      if (this.builder == null) return null;

      try {
        return MappedBuilder.this.buildMethod.invoke(this.builder);
      } catch (final Throwable ex) {
        logError(ex, "Failed to create an instance of {}", MappedBuilder.this.builtClass);
        return null;
      }
    }
  }
}
