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

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetAudience;
import net.kyori.adventure.platform.viaversion.ViaFacet;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import us.myles.ViaVersion.api.data.UserConnection;

import java.util.Collection;
import java.util.Locale;
import java.util.function.Function;

final class BukkitAudience extends FacetAudience<CommandSender> {
  static final ThreadLocal<Plugin> PLUGIN = new ThreadLocal<>();
  private static final Function<Player, UserConnection> VIA = new BukkitFacet.ViaHook();
  private static final Collection<Facet.Chat<? extends CommandSender, ?>> CHAT = Facet.of(
    () -> new ViaFacet.Chat<>(Player.class, VIA),
    () -> new SpigotFacet.ChatWithType(),
    () -> new SpigotFacet.Chat(),
    () -> new CraftBukkitFacet.Chat(),
    () -> new BukkitFacet.Chat());
  private static final Collection<Facet.ActionBar<Player, ?>> ACTION_BAR = Facet.of(
    () -> new ViaFacet.ActionBarTitle<>(Player.class, VIA),
    () -> new ViaFacet.ActionBar<>(Player.class, VIA),
    () -> new SpigotFacet.ActionBar(),
    () -> new CraftBukkitFacet.ActionBar(),
    () -> new CraftBukkitFacet.ActionBarLegacy());
  private static final Collection<Facet.Title<Player, ?, ?>> TITLE = Facet.of(
    () -> new ViaFacet.Title<>(Player.class, VIA),
    () -> new PaperFacet.Title(),
    () -> new CraftBukkitFacet.Title());
  private static final Collection<Facet.Sound<Player, Vector>> SOUND = Facet.of(
    () -> new BukkitFacet.SoundWithCategory(),
    () -> new BukkitFacet.Sound());
  private static final Collection<Facet.Book<Player, ?, ?>> BOOK = Facet.of(
    () -> new SpigotFacet.Book(),
    () -> new CraftBukkitFacet.Book(),
    () -> new CraftBukkitFacet.BookLegacy());
  private static final Collection<Facet.BossBar.Builder<Player, ?>> BOSS_BAR = Facet.of(
    () -> new ViaFacet.BossBar.Builder<>(Player.class, VIA),
    () -> new ViaFacet.BossBar.Builder1_9_To_1_15<>(Player.class, VIA),
    () -> new CraftBukkitFacet.BossBar.Builder(),
    () -> new BukkitFacet.BossBarBuilder(),
    () -> new CraftBukkitFacet.BossBarWither.Builder());

  private final @NonNull Plugin plugin;

  BukkitAudience(final @NonNull Plugin plugin, final @NonNull Collection<CommandSender> viewers, final @Nullable Locale locale) {
    super(viewers, locale, CHAT, ACTION_BAR, TITLE, SOUND, BOOK, BOSS_BAR);
    this.plugin = plugin;
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    // Some boss bar listeners need access to a Plugin to register events.
    // So keep track of the last plugin before each boss bar invocation.
    PLUGIN.set(this.plugin);

    super.showBossBar(bar);
  }
}
