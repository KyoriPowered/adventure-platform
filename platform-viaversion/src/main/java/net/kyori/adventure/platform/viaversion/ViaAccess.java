package net.kyori.adventure.platform.viaversion;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;
import net.kyori.adventure.platform.impl.Handler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ViaAccess {
  /**
   * The JVM is super needy about class initialization, so we can't directly instantiate ViaVersion handler classes for some reason?
   * So we have to do this weird hack to catch any errors resolving the class.
   */
  private static final String PAGKAGE_NAME = ViaAccess.class.getPackage().getName();

  private ViaAccess() {}

  @SuppressWarnings("unchecked")
  public static <V, T extends Handler<V>> @Nullable T via(final @NonNull String handlerName, final @NonNull ViaAPIProvider<? super V> provider, final @NonNull Class<?> expectedParent)  {
    try {
      final Class<?> clazz = Class.forName(PAGKAGE_NAME + ".ViaVersionHandlers$" + handlerName);
      if(expectedParent.isAssignableFrom(clazz)) {
        return (T) clazz.asSubclass(expectedParent).getConstructor(ViaAPIProvider.class).newInstance(provider);
      }
    } catch(InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | NoClassDefFoundError e) {
      e.printStackTrace();
      // expected, viaversion is not present
    }
    return null;
  }

  // this is ugly, i feel you //
  @SuppressWarnings("unchecked")
  public static <V> Handler.PlaySound<V> sound(final @NonNull ViaAPIProvider<? super V> provider, final Function<V, ViaVersionHandlers.PlaySound.Pos> locationProvider) {
    try {
      final Class<?> clazz = Class.forName(PAGKAGE_NAME + ".ViaVersionHandlers$PlaySound");
      if(Handler.PlaySound.class.isAssignableFrom(clazz)) {
        return (Handler.PlaySound<V>) clazz.getConstructor(ViaAPIProvider.class, Function.class).newInstance(provider, locationProvider);
      }
    } catch(InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | NoClassDefFoundError e) {
      e.printStackTrace();
      // expected, viaversion is not present
    }
    return null;
  }

}
