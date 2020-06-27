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
      // expected, viaversion is not present
    }
    return null;
  }

}
