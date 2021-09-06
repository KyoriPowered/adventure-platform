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

import java.util.UUID;
import java.util.function.ToIntFunction;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import org.jetbrains.annotations.NotNull;

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
  @NotNull Audience all();

  /**
   * Gets an audience for the server's console.
   *
   * @return the console audience
   * @since 4.0.0
   */
  @NotNull Audience console();

  /**
   * Gets an audience for all online players.
   *
   * <p>The audience is dynamically updated as players join and leave.</p>
   *
   * @return the players' audience
   * @since 4.0.0
   */
  @NotNull Audience players();

  /**
   * Gets an audience for an individual player.
   *
   * <p>If the player is not online, messages are silently dropped.</p>
   *
   * @param playerId a player uuid
   * @return a player audience
   * @since 4.0.0
   */
  @NotNull Audience player(final @NotNull UUID playerId);

  /**
   * Gets or creates an audience containing all viewers with the provided permission.
   *
   * <p>The audience is dynamically updated as permissions change.</p>
   *
   * @param permission the permission to filter sending to
   * @return a permissible audience
   * @since 4.0.0
   */
  default @NotNull Audience permission(final @NotNull Key permission) {
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
  @NotNull Audience permission(final @NotNull String permission);

  /**
   * Gets an audience for online players in a world, including the server's console.
   *
   * <p>The audience is dynamically updated as players join and leave.</p>
   *
   * <p>World identifiers were introduced in Minecraft 1.16. On older game instances, worlds will be
   * assigned the {@link Key} {@code minecraft:<world name>}</p>
   *
   * @param world identifier for a world
   * @return the world's audience
   * @since 4.0.0
   */
  @NotNull Audience world(final @NotNull Key world);

  /**
   * Gets an audience for online players on a server, including the server's console.
   *
   * <p>If the platform is not a proxy, the audience defaults to everyone.</p>
   *
   * @param serverName a server name
   * @return a server's audience
   * @since 4.0.0
   */
  @NotNull Audience server(final @NotNull String serverName);

  /**
   * Closes the provider and forces audiences to be empty.
   *
   * @since 4.0.0
   */
  @Override
  void close();

  /**
   * A builder for {@link AudienceProvider}.
   *
   * @since 4.0.0
   */
  interface Builder<P extends AudienceProvider, B extends Builder<P, B>> {
    /**
     * Sets the component renderer for the provider.
     *
     * @param componentRenderer a component renderer
     * @return this builder
     * @since 4.0.0
     */
    @NotNull B componentRenderer(final @NotNull ComponentRenderer<Pointered> componentRenderer);

    /**
     * Sets the partition function for the provider.
     *
     * <p>Determines how to group audiences together, for optimization purposes. This will depend on
     * the logic of {@link #componentRenderer(ComponentRenderer)}. For example, if the renderer only
     * checks the audience's locale, then the partition function should return the hashCode of the
     * locale.</p>
     *
     * <p>When in doubt, do not set this since the default partition will always work.</p>
     *
     * @param partitionFunction a partition function
     * @return this builder
     * @since 4.0.0
     */
    @NotNull B partitionBy(final @NotNull ToIntFunction<Pointered> partitionFunction);

    /**
     * Builds the provider.
     *
     * @return the built provider
     * @since 4.0.0
     */
    @NotNull
    P build();
  }
}
