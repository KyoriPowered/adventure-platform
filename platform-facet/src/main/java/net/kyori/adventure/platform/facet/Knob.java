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
package net.kyori.adventure.platform.facet;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Facet utilities and logging pipeline.
 */
public final class Knob {
  private Knob() {
  }

  private static final String NAMESPACE = "net.kyo".concat("ri.adventure"); // Concat is used to trick package relocations
  private static final boolean DEBUG = isEnabled("debug", false);
  private static final Set<Object> UNSUPPORTED = new CopyOnWriteArraySet<>();

  public static volatile Consumer<String> OUT = System.out::println;
  public static volatile BiConsumer<String, Throwable> ERR = (message, err) -> {
    System.err.println(message);
    if(err != null) {
      err.printStackTrace(System.err);
    }
  };

  /**
   * Gets whether a facet should be enabled.
   *
   * <p>Use the JVM flag, {@code -Dnet.kyori.adventure.<key>=true}, to enable the facet.</p>
   *
   * @param key a key
   * @param defaultValue the default value
   * @return if the feature is enabled
   */
  public static boolean isEnabled(final @NonNull String key, final boolean defaultValue) {
    return System.getProperty(NAMESPACE + "." + key, Boolean.toString(defaultValue)).equalsIgnoreCase("true");
  }

  /**
   * Logs an error.
   *
   * @param error an error
   * @param format a string format
   * @param arguments an array of arguments
   */
  public static void logError(final @Nullable Throwable error, final @NonNull String format, final @NonNull Object... arguments) {
    if(DEBUG) {
      ERR.accept(String.format(format, arguments), error);
    }
  }

  /**
   * Logs a message.
   *
   * @param format a string format
   * @param arguments an array of arguments
   */
  public static void logMessage(final @NonNull String format, final @NonNull Object... arguments) {
    if(DEBUG) {
      OUT.accept(String.format(format, arguments));
    }
  }

  /**
   * Logs an unsupported value.
   *
   * @param facet a facet
   * @param value a value
   */
  public static void logUnsupported(final @NonNull Object facet, final @NonNull Object value) {
    if(DEBUG && UNSUPPORTED.add(value)) {
      OUT.accept(String.format("Unsupported value '%s' for facet: %s", value, facet));
    }
  }
}
