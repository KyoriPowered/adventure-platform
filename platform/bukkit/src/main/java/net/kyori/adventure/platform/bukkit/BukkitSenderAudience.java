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

import net.kyori.adventure.platform.impl.HandledAudience;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;

class BukkitSenderAudience<S extends CommandSender> extends HandledAudience<S> {

    BukkitSenderAudience(final @NonNull S sender,
                         final @Nullable HandlerCollection<? super S, ? extends Handler.Chat<? super S, ?>> chat,
                         final @Nullable HandlerCollection<? super S, ? extends Handler.ActionBar<? super S, ?>> actionBar,
                         final @Nullable HandlerCollection<? super S, ? extends Handler.Titles<? super S>> title,
                         final @Nullable HandlerCollection<? super S, ? extends Handler.BossBars<? super S>> bossBar,
                         final @Nullable HandlerCollection<? super S, ? extends Handler.PlaySound<? super S>> sound,
                         final @Nullable HandlerCollection<? super S, ? extends Handler.Books<? super  S>> books) {
        super(requireNonNull(sender, "command sender"), chat, actionBar, title, bossBar, sound, books);
    }

    @Override
    public boolean hasPermission(final @NonNull String permission) {
        return this.viewer.hasPermission(permission);
    }

    @Override
    public boolean isConsole() {
        return this.viewer instanceof ConsoleCommandSender;
    }
}
