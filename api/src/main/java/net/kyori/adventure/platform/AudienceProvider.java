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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;
import java.util.UUID;

/**
 * A provider for creating {@link Audience}s.
 *
 * @since 4.0.0
 */
public interface AudienceProvider extends AutoCloseable {
  /**
   * Gets an audience for all online players, including the server's console.
   *
   * <p>The audience is dynamically updated as players join and leave.</p>
   *
   * @return the players' and console audience
   * @since 4.0.0
   */
  @NonNull Audience all();

  /**
   * Gets an audience for the server's console.
   *
   * @return the console audience
   * @since 4.0.0
   */
  @NonNull Audience console();

  /**
   * Gets an audience for all online players.
   *
   * <p>The audience is dynamically updated as players join and leave.</p>
   *
   * @return the players' audience
   * @since 4.0.0
   */
  @NonNull Audience players();

  /**
   * Gets an audience for an individual player.
   *
   * <p>If the player is not online, messages are silently dropped.</p>
   *
   * @param playerId a player uuid
   * @return a player audience
   * @since 4.0.0
   */
  @NonNull Audience player(final @NonNull UUID playerId);

  /**
   * Gets or creates an audience containing all viewers with the provided permission.
   *
   * <p>The audience is dynamically updated as permissions change.</p>
   *
   * @param permission the permission to filter sending to
   * @return a permissible audience
   * @since 4.0.0
   */
  default @NonNull Audience permission(final @NonNull Key permission) {
    return this.permission(permission.namespace() + '.' + permission.value());
  }

  /**
   * Gets or creates an audience containing all viewers with the provided permission.
   *
   * <p>The audience is dynamically updated as permissions change.</p>
   *
   * @param permission the permission to filter sending to
   * @return a permissible audience
   * @since 4.0.0
   */
  @NonNull Audience permission(final @NonNull String permission);

  /**
   * Gets an audience for online players in a world, including the server's console.
   *
   * <p>The audience is dynamically updated as players join and leave.</p>
   *
   * <p>World identifiers were introduced in Minecraft 1.16. On older game instances,
   * worlds will be assigned the {@link Key} {@code minecraft:<world name>}</p>
   *
   * @param world identifier for a world
   * @return the world's audience
   * @since 4.0.0
   */
  @NonNull Audience world(final @NonNull Key world);

  /**
   * Gets an audience for online players on a server, including the server's console.
   *
   * <p>If the platform is not a proxy, the audience defaults to everyone.</p>
   *
   * @param serverName a server name
   * @return a server's audience
   * @since 4.0.0
   */
  @NonNull Audience server(final @NonNull String serverName);

  /**
   * Gets the active locale-based renderer for operations on provided audiences.
   *
   * @return Active renderer
   * @since 4.0.0
   */
  ComponentRenderer<Locale> localeRenderer();

  /**
   * Sets the active locale-based renderer for operations on provided audiences.
   *
   * @param renderer Active renderer
   * @since 4.0.0
   */
  void localeRenderer(final @NonNull ComponentRenderer<Locale> renderer);

  /**
   * Closes the provider and forces audiences to be empty.
   *
   * @since 4.0.0
   */
  @Override
  void close();
}
