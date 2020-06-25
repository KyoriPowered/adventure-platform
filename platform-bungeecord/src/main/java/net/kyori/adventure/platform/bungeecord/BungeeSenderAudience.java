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
package net.kyori.adventure.platform.bungeecord;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.platform.audience.AdventureAudience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.Connection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

public class BungeeSenderAudience implements AdventureAudience {

  private final CommandSender sender;

  public BungeeSenderAudience(final @NonNull CommandSender sender) {
    this.sender = requireNonNull(sender, "command sender");
  }

  @Override
  public @Nullable Locale locale() {
    return null;
  }

  @Override
  public boolean hasPermission(@NonNull String permission) {
    return this.sender.hasPermission(requireNonNull(permission, "permission"));
  }

  @Override
  public boolean console() {
    return !(this.sender instanceof Connection);
  }

  @Override
  public void sendMessage(final @NonNull Component message) {
    this.sender.sendMessage(BungeeComponentSerializer.INSTANCE.serialize(requireNonNull(message, "message")));
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    // No-op
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    // No-op
  }

  @Override
  public void sendActionBar(final @NonNull Component message) {
    // No-op
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    // No-op
  }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
    // No-op
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    // No-op
  }

  @Override
  public void openBook(final @NonNull Book book) {

  }

  @Override
  public void showTitle(final @NonNull Title title) {
    // No-op
  }

  @Override
  public void clearTitle() {
    // No-op
  }

  @Override
  public void resetTitle() {
    // No-op
  }
}
