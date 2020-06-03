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
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.PlatformAudience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayerAudience implements PlatformAudience<ProxiedPlayer> {
  private final ProxiedPlayer player;

  public PlayerAudience(final ProxiedPlayer player) {
    this.player = player;
  }

  @Override
  public ProxiedPlayer viewer() {
    return this.player;
  }

  @Override
  public void sendMessage(final @NonNull Component message) {
    this.player.sendMessage(ChatMessageType.SYSTEM, TextAdapter.toBungeeCord(message));
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
    this.player.sendMessage(ChatMessageType.ACTION_BAR, TextAdapter.toBungeeCord(message));
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
  public void stopSound(final @NonNull SoundStop stop) {
    // TODO: Out of scope?
    // 17w46a (pre of 1.13) (proto 345): pkt 0x4A
    // starting 17w45a (pre of 1.13) (proto 343): pkt 0x49
    // created: 1.9.3-pre2 (proto 110): custom data MC|StopSound
    // format: string category | string name, either can be empty string meaning wildcard
  }

  @Override
  public void showTitle(@NonNull final Title title) {
    final net.md_5.bungee.api.Title bungee = ProxyServer.getInstance().createTitle();
    if (!TextComponent.empty().equals(title.title())) {
      bungee.title(TextAdapter.toBungeeCord(title.title()));
    }
    if (!TextComponent.empty().equals(title.subtitle())) {
      bungee.subTitle(TextAdapter.toBungeeCord(title.subtitle()));
    }

    bungee.fadeIn(ticks(title.fadeInTime()))
      .fadeOut(ticks(title.fadeOutTime()))
      .stay(ticks(title.stayTime()));

    this.player.sendTitle(bungee);
  }

  private int ticks(Duration duration) {
    return (int) duration.getSeconds() * 20;
  }

  @Override
  public void clearTitle() {
    this.player.sendTitle(ProxyServer.getInstance().createTitle().clear());
  }

  @Override
  public void resetTitle() {
    this.player.sendTitle(ProxyServer.getInstance().createTitle().reset());
  }
}
