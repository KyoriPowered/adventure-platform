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

import java.util.Collection;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayersWithPermissionAudience implements MultiAudience {
  private final Collection<? extends Player> viewers;
  private final Permission permission;

  // All online players with the permission
  public PlayersWithPermissionAudience(final @NonNull Server server, final @NonNull Permission permission) {
    this(server.getOnlinePlayers(), permission);
  }

  public PlayersWithPermissionAudience(final @NonNull Collection<? extends Player> viewers, final @NonNull Permission permission) {
    this.viewers = viewers;
    this.permission = permission;
  }

  @Override
  public @NonNull Iterable<Audience> audiences() {
    return this.viewers.stream()
      .filter(viewer -> viewer.hasPermission(this.permission))
      .map(PlayerAudience::new)
      .collect(Collectors.toList());
  }
}
