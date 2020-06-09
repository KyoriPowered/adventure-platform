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

import net.kyori.adventure.audience.SenderAudience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

class BukkitSenderAudience implements SenderAudience {

    private final CommandSender sender;
    private final Locale locale;
    private final boolean console;

    BukkitSenderAudience(final @NonNull CommandSender sender, final @Nullable Locale locale) {
        this.sender = requireNonNull(sender, "command sender");
        this.locale = locale;
        this.console = sender instanceof ConsoleCommandSender;
    }

    @Override
    public @Nullable Locale getLocale() {
        return locale;
    }

    @Override
    public boolean hasPermission(final @NonNull String permission) {
        return sender.hasPermission(requireNonNull(permission, "permission"));
    }

    @Override
    public boolean isConsole() {
        return console;
    }

    @Override
    public void sendMessage(@NonNull Component message) {
        // TODO
    }

    @Override
    public void sendActionBar(@NonNull Component message) {
        // No-op
    }

    @Override
    public void showTitle(@NonNull Title title) {
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

    @Override
    public void showBossBar(@NonNull BossBar bar) {
        // No-op
    }

    @Override
    public void hideBossBar(@NonNull BossBar bar) {
        // No-op
    }

    @Override
    public void playSound(@NonNull Sound sound) {
        // No-op
    }

    @Override
    public void playSound(@NonNull Sound sound, double x, double y, double z) {
        // No-op
    }

    @Override
    public void stopSound(@NonNull SoundStop stop) {
        // No-op
    }
}
