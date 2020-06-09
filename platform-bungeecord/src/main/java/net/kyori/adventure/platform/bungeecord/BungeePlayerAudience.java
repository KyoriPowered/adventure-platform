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

import java.time.Duration;
import java.util.UUID;

import net.kyori.adventure.platform.audience.PlayerAudience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.BungeeComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.platform.impl.Handler.Title.ticks;

public class BungeePlayerAudience extends BungeeSenderAudience implements PlayerAudience {

  private final ProxyServer proxy;
  private final UUID playerId;
  private final ProxiedPlayer player;

  public BungeePlayerAudience(final @NonNull ProxyServer proxy, final @NonNull ProxiedPlayer player) {
    super(player, requireNonNull(player, "player").getLocale());
    this.proxy = requireNonNull(proxy, "proxy");
    this.playerId = player.getUniqueId();
    this.player = player;
  }

  @Override
  public @NonNull UUID getId() {
    return playerId;
  }

  @Override
  public @Nullable UUID getWorldId() {
    return null; // Bungee does not know about a player's world
  }

  @Override
  public @Nullable String getServerName() {
    return player.isConnected() ? player.getServer().getInfo().getName() : null;
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    BungeeBossBarListener.INSTANCE.subscribe(bar, this.player);
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    BungeeBossBarListener.INSTANCE.unsubscribe(bar, this.player);
  }

  @Override
  public void sendActionBar(final @NonNull Component message) {
    this.player.sendMessage(ChatMessageType.ACTION_BAR, BungeeComponentSerializer.INSTANCE.serialize(message));
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    // TODO: Out of scope?
    // Packet Named Sound Effect, 0x19
    // 19w34a (pre of 1.15) (proto 345): 0x19 -> 0x1A
    // 19w14b (pre of 1.15) (proto 345): 0x1A -> 0x19
    // 17w46a (pre of 1.13) (proto 345): 0x19 -> 0x1A
    // 17w45a (pre of 1.13) (proto 343): Uses identifiers
    // 16w20a (pre of 1.11) (proto 201): Pitch is float, used to be ubyte
    // 16w02a (pre of 1.9): Added category as varint enum
    // 15w43a (pre of 1.9): ID 0x23 -> 0x19
  }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
    // TODO: See above
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    // TODO: Out of scope?
    // 17w46a (pre of 1.13) (proto 345): pkt 0x4A
    // starting 17w45a (pre of 1.13) (proto 343): pkt 0x49
    // created: 1.9.3-pre2 (proto 110): custom data MC|StopSound
    // format: string category | string name, either can be empty string meaning wildcard
  }

  @Override
  public void showTitle(final @NonNull Title title) {
    final net.md_5.bungee.api.Title bungee = proxy.createTitle();
    if (!TextComponent.empty().equals(title.title())) {
      bungee.title(BungeeComponentSerializer.INSTANCE.serialize(title.title()));
    }
    if (!TextComponent.empty().equals(title.subtitle())) {
      bungee.subTitle(BungeeComponentSerializer.INSTANCE.serialize(title.subtitle()));
    }

    bungee.fadeIn(ticks(title.fadeInTime()))
      .fadeOut(ticks(title.fadeOutTime()))
      .stay(ticks(title.stayTime()));

    this.player.sendTitle(bungee);
  }

  @Override
  public void clearTitle() {
    this.player.sendTitle(proxy.createTitle().clear());
  }

  @Override
  public void resetTitle() {
    this.player.sendTitle(proxy.createTitle().reset());
  }
}
