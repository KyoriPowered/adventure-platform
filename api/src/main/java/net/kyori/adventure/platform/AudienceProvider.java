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
import java.util.function.Function;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.flattener.ComponentFlattener;
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
   * Return a component flattener that can use game data to resolve extra information about components.
   *
   * <p>This can be used for displaying components, or with serializers including the plain and legacy serializers.</p>
   *
   * @return the flattener
   * @since 4.0.0
   */
  @NotNull ComponentFlattener flattener();

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
     * @see #componentRenderer(Function, ComponentRenderer)
     * @since 4.0.0
     */
    @NotNull B componentRenderer(final @NotNull ComponentRenderer<Pointered> componentRenderer);

    /**
     * Set the partition function for the provider.
     *
     * <p>The output of the function must have {@link Object#equals(Object)} and {@link Object#hashCode()}
     * methods overridden to ensure efficient operation.</p>
     *
     * <p>The output of the partition function must also be something suitable for use as a map key and
     * as such, for long-term storage. This excludes objects that may hold live game state
     * like {@code Entity} or {@code Level}.</p>
     *
     * <p>The configured {@link #componentRenderer(ComponentRenderer) component renderer} must produce
     * the same result for two {@link Pointered} instances where this partition function provides the
     * same output. If this condition is violated, caching issues are likely to occur, producing
     * incorrect output for at least one of the inputs.</p>
     *
     * <p>A local {@code record} is a good way to produce a compound output value for this function.</p>
     *
     * @param partitionFunction the partition function to apply
     * @return this builder
     * @see #componentRenderer(Function, ComponentRenderer)
     * @since 4.0.0
     */
    @NotNull B partition(final @NotNull Function<Pointered, ?> partitionFunction);

    /**
     * Sets the component renderer and partition function for the provider.
     *
     * <p>This variant validates that the component renderer only depends on information included in the partition.</p>
     *
     * @param componentRenderer a component renderer
     * @return this builder
     * @since 4.0.0
     */
    default <T> @NotNull B componentRenderer(final @NotNull Function<Pointered, T> partition, final @NotNull ComponentRenderer<T> componentRenderer) {
      return this.partition(partition)
        .componentRenderer(componentRenderer.mapContext(partition));
    }

    /**
     * Builds the provider.
     *
     * @return the built provider
     * @since 4.0.0
     */
    @NotNull P build();
  }
}
