/*
 * This file is part of text-extras, licensed under the MIT License.
 *
 * Copyright (c) 2018 KyoriPowered
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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.NonNull;

import static com.google.common.collect.Iterables.transform;
import static java.util.Objects.requireNonNull;

final class PermissibleAudience implements MultiAudience {
  private final PluginManager pluginManager;
  private final String permission;

  public PermissibleAudience(final @NonNull Server server, final @NonNull String permission) {
    this.pluginManager = requireNonNull(server, "server").getPluginManager();
    this.permission = requireNonNull(permission, "permission");
  }

  @Override
  public @NonNull Iterable<Audience> audiences() {
    return transform(this.pluginManager.getPermissionSubscriptions(this.permission), this::audience);
  }

  private Audience audience(Permissible permissible) {
    if (permissible.hasPermission(this.permission) && permissible instanceof CommandSender) {
      return BukkitPlatform.audience((CommandSender) permissible);
    }
    return Audience.empty();
  }
}
