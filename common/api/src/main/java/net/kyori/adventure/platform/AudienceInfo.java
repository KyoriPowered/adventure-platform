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
package net.kyori.adventure.platform;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * Contextual information about an {@link Audience}.
 */
public interface AudienceInfo {
    /**
     * Gets the locale.
     *
     * @return a locale or {@code null} if unknown
     */
    @Nullable Locale getLocale();

    /**
     * Gets the uuid, if a player.
     *
     * @return a player uuid or {@code null} if not a player
     */
    @Nullable UUID getId();

    /**
     * Gets the world identifier.
     *
     * @return a world id or {@code null} if unknown
     */
    @Nullable Key getWorld();

    /**
     * Gets the server name.
     *
     * @return a server name or {@code null} if unknown
     */
    @Nullable String getServer();

    /**
     * Checks whether there is permission.
     *
     * @param permission a permission node
     * @return if the audience has permission
     */
    boolean hasPermission(final @NonNull String permission);

    /**
     * Gets if a console.
     *
     * @return if the audience is a console
     */
    boolean isConsole();

    /**
     * Gets if a player.
     *
     * @return if the audience is a player
     */
    boolean isPlayer();
}
