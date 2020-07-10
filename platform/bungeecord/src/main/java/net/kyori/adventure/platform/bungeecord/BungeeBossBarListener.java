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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.common.AbstractBossBarListener;
import net.kyori.adventure.platform.common.Handler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;

import static net.kyori.adventure.platform.common.Handler.BossBars.color;
import static net.kyori.adventure.platform.common.Handler.BossBars.overlay;

/* package */ final class BungeeBossBarListener extends AbstractBossBarListener<ProxiedPlayer, BungeeBossBarListener.Instance> {

  /* package */ BungeeBossBarListener() {
  }

  @Override
  public void bossBarNameChanged(final @NonNull BossBar bar, final @NonNull Component oldName, final @NonNull Component newName) {
    this.handle(bar, newName, (val, inst) -> inst.sendToSubscribers(ACTION_NAME, pkt -> pkt.setTitle(GsonComponentSerializer.gson().serialize(val)))); // TODO: based on viewer
  }

  @Override
  public void bossBarPercentChanged(final @NonNull BossBar bar, final float oldPercent, final float newPercent) {
    this.handle(bar, newPercent, (val, inst) -> inst.sendToSubscribers(ACTION_PERCENT, pkt -> pkt.setHealth(val)));
  }

  @Override
  public void bossBarColorChanged(final @NonNull BossBar bar, final BossBar.@NonNull Color oldColor, final BossBar.@NonNull Color newColor) {
    this.handle(bar, newColor, (val, inst) -> inst.sendToSubscribers(ACTION_STYLE, pkt -> {
      pkt.setColor(color(val));
      pkt.setDivision(overlay(inst.adventure.overlay()));
    }));
  }

  @Override
  public void bossBarOverlayChanged(final @NonNull BossBar bar, final BossBar.@NonNull Overlay oldOverlay, final BossBar.@NonNull Overlay newOverlay) {
    this.handle(bar, newOverlay, (val, inst) -> inst.sendToSubscribers(ACTION_STYLE, pkt -> {
      pkt.setColor(color(inst.adventure.color()));
      pkt.setDivision(overlay(val));
    }));
  }

  @Override
  public void bossBarFlagsChanged(final @NonNull BossBar bar, final @NonNull Set<BossBar.Flag> oldFlags, final @NonNull Set<BossBar.Flag> newFlags) {
    this.handle(bar, newFlags, (val, inst) -> inst.sendToSubscribers(ACTION_STYLE, pkt -> pkt.setFlags(BossBars.bitmaskFlags(val))));
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @NonNull
  @Override
  protected Instance newInstance(final @NonNull BossBar adventure) {
    return new Instance(adventure);
  }

  @Override
  protected void show(final @NonNull ProxiedPlayer viewer, final @NonNull Instance bar) {
    if(canSeeBossBars(viewer)) {
      if(bar.subscribers.add(viewer)) {
        viewer.unsafe().sendPacket(bar.newCreatePacket());
      }
    }
  }

  @Override
  protected boolean hide(final @NonNull ProxiedPlayer viewer, final @NonNull Instance bar) {
    if(bar.subscribers.remove(viewer)) {
      viewer.unsafe().sendPacket(bar.newPacket(ACTION_REMOVE));
      return true;
    }
    return false;
  }

  @Override
  protected boolean isEmpty(final @NonNull Instance bar) {
    return bar.subscribers.isEmpty();
  }

  @Override
  protected void hideFromAll(final @NonNull Instance bar) {
    bar.sendToSubscribers(bar.newPacket(ACTION_REMOVE));
    bar.subscribers.clear();
  }

  /* package */ static class Instance {
    private final UUID id = UUID.randomUUID();
    private final BossBar adventure;
    final Set<ProxiedPlayer> subscribers = ConcurrentHashMap.newKeySet();

    /* package */ Instance(final BossBar adventure) {
      this.adventure = adventure;
    }

    /* package */ net.md_5.bungee.protocol.packet.@NonNull BossBar newCreatePacket() {
      final net.md_5.bungee.protocol.packet.BossBar packet = this.newPacket(Handler.BossBars.ACTION_ADD);
      packet.setTitle(GsonComponentSerializer.gson().serialize(this.adventure.name())); // TODO: Based on viewer protocol
      packet.setHealth(this.adventure.percent());
      packet.setColor(color(this.adventure.color()));
      packet.setDivision(overlay(this.adventure.overlay()));
      packet.setFlags(BossBars.bitmaskFlags(this.adventure.flags()));
      return packet;
    }

    /* package */ net.md_5.bungee.protocol.packet.@NonNull BossBar newPacket(final int action) {
      return new net.md_5.bungee.protocol.packet.BossBar(this.id, action);
    }

    /* package */ void sendToSubscribers(final int action, final @NonNull Consumer<net.md_5.bungee.protocol.packet.BossBar> packetModifier) {
      final net.md_5.bungee.protocol.packet.BossBar packet = this.newPacket(action);
      packetModifier.accept(packet);
      this.sendToSubscribers(packet);
    }

    /* package */ void sendToSubscribers(final net.md_5.bungee.protocol.packet.@NonNull BossBar packet) {
      for(final ProxiedPlayer player : this.subscribers) {
        player.unsafe().sendPacket(packet);
      }
    }

    public boolean isEmpty() {
      return this.subscribers.isEmpty();
    }
  }

  /**
   * Check if a player is connecting with at least Minecraft 1.9, the version when boss bars were added
   *
   * @param player The player to check
   * @return if the player has a client with boss bar support
   */
  private static boolean canSeeBossBars(final @NonNull ProxiedPlayer player) {
    return player.getPendingConnection().getVersion() >= BungeePlatform.PROTCOOL_1_9;
  }
}
