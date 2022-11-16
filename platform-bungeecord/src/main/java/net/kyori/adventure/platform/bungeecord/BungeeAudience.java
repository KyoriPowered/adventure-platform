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
package net.kyori.adventure.platform.bungeecord;

import com.viaversion.viaversion.api.connection.UserConnection;
import java.util.Collection;
import java.util.function.Function;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetAudience;
import net.kyori.adventure.platform.viaversion.ViaFacet;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

final class BungeeAudience extends FacetAudience<CommandSender> {
  private static final Function<ProxiedPlayer, UserConnection> VIA = new BungeeFacet.ViaHook();
  private static final Collection<Facet.Chat<? extends CommandSender, ?>> CHAT = Facet.of(
    () -> new ViaFacet.Chat<>(ProxiedPlayer.class, VIA),
    BungeeFacet.ChatPlayerSenderId::new,
    BungeeFacet.ChatPlayer::new,
    BungeeFacet.ChatConsole::new);
  private static final Collection<Facet.ActionBar<ProxiedPlayer, ?>> ACTION_BAR = Facet.of(
    () -> new ViaFacet.ActionBar<>(ProxiedPlayer.class, VIA),
    BungeeFacet.ActionBar::new);
  private static final Collection<Facet.Title<ProxiedPlayer, ?, ?, ?>> TITLE = Facet.of(
    () -> new ViaFacet.Title<>(ProxiedPlayer.class, VIA),
    BungeeFacet.Title::new);
  private static final Collection<Facet.BossBar.Builder<ProxiedPlayer, ? extends Facet.BossBar<ProxiedPlayer>>> BOSS_BAR = Facet.of(
    () -> new ViaFacet.BossBar.Builder<>(ProxiedPlayer.class, VIA),
    BungeeFacet.BossBar.Builder::new);
  private static final Collection<Facet.TabList<ProxiedPlayer, ?>> TAB_LIST = Facet.of(
      () -> new ViaFacet.TabList<>(ProxiedPlayer.class, VIA),
    BungeeFacet.TabList::new
  );
  private static final Collection<Facet.Pointers<? extends CommandSender>> POINTERS = Facet.of(
    BungeeFacet.CommandSenderPointers::new,
    BungeeFacet.PlayerPointers::new
  );

  BungeeAudience(final @NotNull BungeeAudiencesImpl provider, final @NotNull Collection<? extends CommandSender> viewers) {
    super(provider, viewers, CHAT, ACTION_BAR, TITLE, null, null, null, BOSS_BAR, TAB_LIST, POINTERS);
  }
}
