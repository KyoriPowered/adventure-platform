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
package net.kyori.adventure.platform.spongeapi;

import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.AudienceIdentity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.source.ProxySource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.channel.MessageReceiver;

import static java.util.Objects.requireNonNull;

final class SpongeIdentity implements AudienceIdentity {
  private final MessageReceiver receiver;
  private final Player player;

  SpongeIdentity(@NonNull MessageReceiver receiver) {
    if(receiver instanceof ProxySource) {
      receiver = ((ProxySource) receiver).getOriginalSource();
    }
    this.receiver = requireNonNull(receiver, "message receiver");
    this.player = this.player() ? (Player) receiver : null;
  }

  @Override
  public @NonNull UUID uuid() {
    if(this.player()) {
      return this.player.getUniqueId();
    }
    return Identity.nil().uuid();
  }

  @Override
  public @NonNull Locale locale() {
    if(this.player()) {
      return this.player.getLocale();
    }
    return Locale.US;
  }

  @Override
  public @Nullable String world() {
    if(this.player()) {
      return this.player.getWorld().getName();
    }
    return null;
  }

  @Override
  public @Nullable String server() {
    return null; // Sponge is not a proxy
  }

  @Override
  public boolean player() {
    return this.receiver instanceof Player;
  }

  @Override
  public boolean console() {
    return this.receiver instanceof ConsoleSource;
  }

  @Override
  public boolean permission(final @NonNull String key) {
    if(this.receiver instanceof Subject) {
      return ((Subject) this.receiver).hasPermission(key);
    }
    return false;
  }
}
