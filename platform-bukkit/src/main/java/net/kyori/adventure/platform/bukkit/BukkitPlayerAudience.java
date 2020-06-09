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

import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.platform.audience.PlayerAudience;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class BukkitPlayerAudience extends BukkitSenderAudience<Player> implements PlayerAudience {

    private final UUID id;

    BukkitPlayerAudience(final @NonNull Player sender, final @Nullable HandlerCollection<? super Player, ? extends Handler.Chat<? super Player, ?>> chat, final @Nullable HandlerCollection<? super Player, ? extends Handler.ActionBar<? super Player, ?>> actionBar, final @Nullable HandlerCollection<? super Player, ? extends Handler.Title<? super Player>> title, final @Nullable HandlerCollection<? super Player, ? extends Handler.BossBar<? super Player>> bossBar, final @Nullable HandlerCollection<? super Player, ? extends Handler.PlaySound<? super Player>> sound) {
        super(sender, toLocale(sender.getLocale()), chat, actionBar, title, bossBar, sound);
        this.id = sender.getUniqueId();
    }

    @Override
    public @NonNull UUID getId() {
        return this.id;
    }

    @Override
    public @Nullable UUID getWorldId() {
        return this.viewer.getWorld().getUID();
    }

    @Override
    public @Nullable String getServerName() {
        return this.viewer.getServer().getName();
    }

    /**
     * Take a locale string provided from a minecraft client and attempt to parse it as a locale.
     * These are not strictly compliant with the iso standard, so we try to make things a bit more normalized.
     *
     * @param mcLocale The locale string, in the format provided by the Minecraft client
     * @return A Locale object matching the provided locale string
     */
    private static @NonNull Locale toLocale(final @Nullable String mcLocale) {
        if(mcLocale == null) return Locale.getDefault();

        final String[] parts = mcLocale.split("_", 3);
        switch(parts.length) {
            case 0: return Locale.getDefault();
            case 1: return new Locale(parts[0]);
            case 2: return new Locale(parts[0], parts[1]);
            case 3: return new Locale(parts[0], parts[1], parts[2]);
            default: throw new IllegalArgumentException("Provided locale '" + mcLocale + "' was not in a valid format!");
        }
    }
}
