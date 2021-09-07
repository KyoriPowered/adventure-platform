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
package net.kyori.adventure.platform.bukkit;

import java.util.function.Predicate;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.sound.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * An audience provider for {@link org.bukkit.Bukkit}.
 *
 * @see AudienceProvider
 * @since 4.0.0
 */
public interface BukkitAudiences extends AudienceProvider {
  /**
   * Creates an audience provider for a plugin.
   *
   * <p>There will only be one provider for each plugin.</p>
   *
   * @param plugin a plugin
   * @return an audience provider
   * @since 4.0.0
   */
  static @NotNull BukkitAudiences create(final @NotNull Plugin plugin) {
    return BukkitAudiencesImpl.instanceFor(plugin);
  }

  /**
   * Creates an audience provider builder for a plugin.
   *
   * <p>There will only be one provider for each plugin.</p>
   *
   * @param plugin a plugin
   * @return an audience provider
   * @since 4.0.0
   */
  static @NotNull Builder builder(final @NotNull Plugin plugin) {
    return BukkitAudiencesImpl.builder(plugin);
  }

  /**
   * Represent an entity as an emitter of sound.
   *
   * @param entity the entity to represent
   * @return an emitter
   * @since 4.0.0
   */
  static Sound.@NotNull Emitter asEmitter(final @NotNull Entity entity) {
    return new BukkitEmitter(entity);
  }

  /**
   * Gets an audience for a command sender.
   *
   * @param sender a command sender
   * @return an audience
   * @since 4.0.0
   */
  @NotNull Audience sender(final @NotNull CommandSender sender);

  /**
   * Gets an audience for a player.
   *
   * @param player a player
   * @return an audience
   * @since 4.0.0
   */
  @NotNull Audience player(final @NotNull Player player);

  /**
   * Creates an audience based on a filter.
   *
   * @param filter a filter
   * @return an audience
   * @since 4.0.0
   */
  @NotNull Audience filter(final @NotNull Predicate<CommandSender> filter);

  /**
   * A builder for {@link BukkitAudiences}.
   *
   * @since 4.0.0
   */
  interface Builder extends AudienceProvider.Builder<BukkitAudiences, Builder> {
  }
}

