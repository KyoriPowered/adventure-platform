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

import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.AudienceIdentity;
import net.kyori.adventure.translation.Translator;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

final class BukkitIdentity implements AudienceIdentity {
  private final CommandSender sender;
  private final Player player;
  private Locale locale;

  BukkitIdentity(@NotNull CommandSender sender) {
    if(sender instanceof ProxiedCommandSender) {
      sender = ((ProxiedCommandSender) sender).getCallee();
    }
    this.sender = requireNonNull(sender, "command sender");
    this.player = this.player() ? (Player) sender : null;
    if(this.player != null) {
      try {
        final Object locale = this.player.getLocale();
        // Some Bukkit forks have Locale built-in
        if(locale instanceof Locale) {
          this.locale = (Locale) locale;
        } else {
          this.locale = Translator.parseLocale(locale.toString());
        }
      } catch(final NoSuchMethodError e) {
        // Old Bukkit versions do not expose Locale
      }
      if(this.locale == null) {
        this.locale = Locale.US;
      }
    }
  }

  @Override
  public @NotNull UUID uuid() {
    if(this.player()) {
      return this.player.getUniqueId();
    }
    return Identity.nil().uuid();
  }

  @Override
  public @NotNull Locale locale() {
    return this.locale;
  }

  @Override
  public @Nullable String world() {
    if(this.player()) {
      final World world = this.player.getWorld();
      // Null worlds can exist in very strange situations
      if(world != null) {
        return world.getName();
      }
    }
    return null;
  }

  @Override
  public @Nullable String server() {
    return null; // Bukkit is not a proxy
  }

  @Override
  public boolean player() {
    return this.sender instanceof Player;
  }

  @Override
  public boolean console() {
    return this.sender instanceof ConsoleCommandSender;
  }

  @Override
  public boolean permission(final @NotNull String key) {
    return this.sender.hasPermission(key);
  }

  @Override
  public int hashCode() {
    return this.sender.hashCode();
  }
}
