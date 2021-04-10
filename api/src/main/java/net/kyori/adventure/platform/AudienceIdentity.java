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
package net.kyori.adventure.platform;

import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identity for an {@link Audience}.
 *
 * @since 4.5.0
 */
public interface AudienceIdentity extends Identity {
  /**
   * Gets the UUID.
   *
   * @return a uuid
   * @since 4.5.0
   */
  @Override
  @NotNull
  UUID uuid();

  /**
   * Gets the locale.
   *
   * @return a locale
   * @since 4.5.0
   */
  @NotNull
  Locale locale();

  /**
   * Gets the world name.
   *
   * @return a world name, or null if not in world
   * @since 4.5.0
   */
  @Nullable
  String world();

  /**
   * Gets the server name.
   *
   * @return a server name, or null if not on a proxy
   * @since 4.5.0
   */
  @Nullable
  String server();

  /**
   * Tests for a player.
   *
   * @return if a player
   * @since 4.5.0
   */
  boolean player();

  /**
   * Tests for a console.
   *
   * @return if a console
   * @since 4.5.0
   */
  boolean console();

  /**
   * Tests for a permission.
   *
   * @param key a permission key
   * @return if it has permission
   * @since 4.5.0
   */
  boolean permission(final @NotNull String key);
}
