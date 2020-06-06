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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import net.kyori.adventure.platform.AdventurePlatform;
import net.kyori.adventure.platform.PlatformAudience;
import net.kyori.adventure.platform.ProviderSupport;
import net.kyori.adventure.platform.impl.HandledAudience;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

public final class BukkitPlatform implements AdventurePlatform {

  static HandlerCollection<? super CommandSender, ? extends Handler.Chat<? super CommandSender, ?>> CHAT = new HandlerCollection<>(new SpigotHandlers.Chat(),
    new CraftBukkitHandlers.Chat(), new BukkitHandlers.Chat());
  static HandlerCollection<Player, Handler.ActionBar<Player, ?>> ACTION_BAR = new HandlerCollection<>(new SpigotHandlers.ActionBar(),
    new CraftBukkitHandlers.ActionBarModern(), new CraftBukkitHandlers.ActionBar1_8thru1_11());
  static HandlerCollection<Player, Handler.Title<Player>> TITLE = new HandlerCollection<>(new PaperHandlers.Title(), new CraftBukkitHandlers.Title());
  static HandlerCollection<Player, Handler.BossBar<Player>> BOSS_BAR = new HandlerCollection<>(new BukkitHandlers.BossBar());
  static HandlerCollection<Player, Handler.PlaySound<Player>> PLAY_SOUND = new HandlerCollection<>(new BukkitHandlers.PlaySound_WithCategory(),
    new BukkitHandlers.PlaySound_NoCategory());

  public BukkitPlatform() {}

  private final PlatformAudience<? extends CommandSender> console = audience(Bukkit.getConsoleSender());

  // TODO: ugly but it's here to test with until proper solution
  public static PlatformAudience<Player> audience(final Player player) {
    return new HandledAudience<>(player, CHAT, ACTION_BAR, TITLE, BOSS_BAR, PLAY_SOUND);
  }

  public static PlatformAudience<? extends CommandSender> audience(final CommandSender sender) {
    if(sender instanceof Player) {
      return audience((Player) sender);
    } else {
      return new HandledAudience<>(sender, CHAT, null, null, null, null);
    }
  }

  @Override
  public @NonNull String name() {
    return "CraftBukkit";
  }

  @Override
  public @NonNull ProviderSupport supportLevel() {
    return ProviderSupport.FULL;
  }

  @Override
  public @NonNull PlatformAudience<? extends CommandSender> console() {
    return console;
  }

  @Override
  public @NonNull MultiAudience audience(final @NonNull Iterable<Audience> audiences) {
    return MultiAudience.of(audiences);
  }

  @Override
  public @NonNull MultiAudience permission(final @NonNull String permission) {
    return new WithPermissionAudience(Bukkit.getServer(), requireNonNull(permission, "permission"));
  }

  @Override
  public @NonNull MultiAudience online() {
    return new OnlinePlayersAudience(Bukkit.getServer());
  }

}
