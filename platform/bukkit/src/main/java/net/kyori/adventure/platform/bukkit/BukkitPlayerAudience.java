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
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class BukkitPlayerAudience extends BukkitSenderAudience<Player> {

    private Locale locale;
    private String localeRaw;

    BukkitPlayerAudience(final @NonNull Player sender,
                         final @Nullable HandlerCollection<? super Player, ? extends Handler.Chat<? super Player, ?>> chat,
                         final @Nullable HandlerCollection<? super Player, ? extends Handler.ActionBar<? super Player, ?>> actionBar,
                         final @Nullable HandlerCollection<? super Player, ? extends Handler.Titles<? super Player>> title,
                         final @Nullable HandlerCollection<? super Player, ? extends Handler.BossBars<? super Player>> bossBar,
                         final @Nullable HandlerCollection<? super Player, ? extends Handler.PlaySound<? super Player>> sound,
                         final @Nullable HandlerCollection<? super Player, ? extends Handler.Books<? super Player>> books) {
        super(sender, chat, actionBar, title, bossBar, sound, books);
    }

    @Override
    public @NonNull UUID getId() {
        return this.viewer.getUniqueId();
    }

    @Override
    public @Nullable Key getWorld() {
        return Key.of(Key.MINECRAFT_NAMESPACE, this.viewer.getWorld().getName()); // TODO: handle Keys
    }

    @Nullable
    @Override
    public Locale getLocale() {
        final String newLocaleRaw = this.viewer.getLocale();
        if (!Objects.equals(localeRaw, newLocaleRaw)) {
            locale = toLocale(localeRaw = newLocaleRaw);
        }
        return locale;
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    /**
     * Take a locale string provided from a minecraft client and attempt to parse it as a locale.
     * These are not strictly compliant with the iso standard, so we try to make things a bit more normalized.
     *
     * @param locale a locale string, in the format provided by the Minecraft client
     * @return a locale object matching the provided locale string
     */
     /* package */ static @Nullable Locale toLocale(final @Nullable String locale) {
        if(locale == null) return null;
        final String[] parts = locale.split("_", 3);
        switch(parts.length) {
            case 1: return parts[0].isEmpty() ? null : new Locale(parts[0]);
            case 2: return new Locale(parts[0], parts[1]);
            case 3: return new Locale(parts[0], parts[1], parts[2]);
            default: return null;
        }
     }
}
