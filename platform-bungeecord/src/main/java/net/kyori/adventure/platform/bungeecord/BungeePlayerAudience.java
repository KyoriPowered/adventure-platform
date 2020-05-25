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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BungeePlayerAudience implements Audience {
  private final ProxiedPlayer player;

  public BungeePlayerAudience(final ProxiedPlayer player) {
    this.player = player;
  }

  @Override
  public void message(final @NonNull Component message) {
    this.player.sendMessage(ChatMessageType.SYSTEM, TextAdapter.toBungeeCord(message));
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    if(!(bar instanceof BungeeBossBar)) {
      throw new IllegalArgumentException("Only Bungee-created boss bars are supported");
    }
    ((BungeeBossBar) bar).subscribe(this.player);
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    if(!(bar instanceof BungeeBossBar)) {
      throw new IllegalArgumentException("Only Bungee-created boss bars are supported");
    }
    ((BungeeBossBar) bar).unsubscribe(this.player);
  }

  @Override
  public void showActionBar(final @NonNull Component message) {
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
}
