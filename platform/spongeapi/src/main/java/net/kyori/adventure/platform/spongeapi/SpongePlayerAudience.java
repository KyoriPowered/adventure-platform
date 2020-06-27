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

import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.audience.AdventurePlayerAudience;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.Player;

/* package */ final class SpongePlayerAudience extends SpongeSenderAudience<Player> implements AdventurePlayerAudience {
  public SpongePlayerAudience(final @NonNull Player viewer, 
                              final @Nullable HandlerCollection<? super Player, ? extends Handler.Chat<? super Player, ?>> chat, 
                              final @Nullable HandlerCollection<? super Player, ? extends Handler.ActionBar<? super Player, ?>> actionBar, 
                              final @Nullable HandlerCollection<? super Player, ? extends Handler.Titles<? super Player>> title, 
                              final @Nullable HandlerCollection<? super Player, ? extends Handler.BossBars<? super Player>> bossBar, 
                              final @Nullable HandlerCollection<? super Player, ? extends Handler.PlaySound<? super Player>> sound,
                              final @Nullable HandlerCollection<? super Player, ? extends Handler.Books<? super Player>> books) {
    super(viewer, chat, actionBar, title, bossBar, sound, books);
  }

  @Override
  public @NonNull UUID id() {
    return this.viewer.getUniqueId();
  }

  @Override
  public @NonNull Key world() {
    return Key.of(Key.MINECRAFT_NAMESPACE, this.viewer.getWorld().getName());
  }

  @Override
  public @Nullable String serverName() {
    return null;
  }
}
