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
package net.kyori.adventure.platform.impl;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;

public final class Knobs {

  private Knobs() {
  }

  private static final String PROPERTY_PREFIX = "net.kyo".concat("ri.adventure."); // prevent from being changed by shade relocation
  private static final @NonNull Pattern COMMA_SPLIT = Pattern.compile(",");

  /**
   * Get a boolean property in the Adventure namespace.
   * `
   * @param key property key
   * @param def default value
   * @return property value
   */
  private static boolean bool(final @NonNull String key, final boolean def) {
    final String property = System.getProperty(PROPERTY_PREFIX + key);
    if(property == null || property.isEmpty()) return def;
    return Boolean.parseBoolean(property);
  }

  /**
   * Get a property that is a set, from a comma-separated string.
   *
   * @param key property key, will be appended to adventure namespace
   * @return property value, or empty set if unset.
   */
  private static Set<String> set(final @NonNull String key) {
    final String prop = System.getProperty(PROPERTY_PREFIX + key);
    if(prop == null || prop.isEmpty()) {
      return Collections.emptySet();
    }

    return Collections.unmodifiableSet(COMMA_SPLIT.splitAsStream(prop)
      .map(String::toLowerCase)
      .collect(Collectors.toSet()));
  }

  /**
   * A set of names of providers that should be disabled. Default: empty list
   */
  private static final Set<String> DISABLED_PROVIDERS = set("disabled");

  /**
   * Set to true to print out errors that occur when trying to enable providers.
   * Errors that are printed would generally be expected
   */
  private static final boolean PRINT_ERRORS = bool("printErrors", false);

  /**
   * Log the handlers chosen for any single audience created
   */
  private static final boolean PRINT_CHOSEN_HANDLER = bool("printChosenHandler", false);

  /**
   * @param handlerId the id of the handler to check
   * @return if the handler has been explicitly blocked
   */
  public static boolean enabled(final @NonNull String handlerId) {
    return !DISABLED_PROVIDERS.contains(handlerId.toLowerCase());
  }

  /**
   * Log an error that occurred while performing an Adventure operation.
   *
   * No more specific information
   *
   * @param ex the exception
   */
  public static void logError(final @Nullable Throwable ex) {
    logError("performing an operation", ex);
  }

  public static void logError(final @NonNull String description, final @Nullable Throwable ex) {
    if(PRINT_ERRORS) {
      logger.error(ex, "Adventure detected an error when {0}: {1}.", description, ex == null ? "no exception" : ex.getMessage());
    }
  }

  public static <V> void logChosenHandler(final @NonNull V viewer, final @Nullable Handler<V> handler) {
    if(PRINT_CHOSEN_HANDLER) {
      if(handler == null) {
        logger.info("No handler found in this collection for viewer {0}", viewer);
      } else {
        logger.info("Chose handler {0} for viewer {1}", handler, viewer);
      }
    }
  }

  private static LogHandler logger = new DefaultLogHandler();

  /**
   * Change the logger from the default handler that uses standard
   * in/out streams to one that is more suited to the platform.
   *
   * @param handler handler to use.
   */
  public static void logger(final @NonNull LogHandler handler) {
    logger = requireNonNull(handler, "handler");
  }

  /**
   * Simple logger interface for internal use.
   *
   * All messages should be interpreted as {@link MessageFormat}-style pattern strings
   */
  public interface LogHandler {
    void info(final @NonNull String message, final Object@NonNull... params);

    void error(final @Nullable Throwable exc, final @NonNull String message, final Object@NonNull... params);
  }

  /* package */ static class DefaultLogHandler implements LogHandler {
    @Override
    public void info(final @NonNull String message, final Object@NonNull... params) {
      System.out.println(MessageFormat.format(message, params));
    }

    @Override
    public void error(final @Nullable Throwable exc, final @NonNull String message, final Object@NonNull... params) {
      System.err.println(MessageFormat.format(message, params));
      if(exc != null) {
        exc.printStackTrace();
      }
    }
  }

}
