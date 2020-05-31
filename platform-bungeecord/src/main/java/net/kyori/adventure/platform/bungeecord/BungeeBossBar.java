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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.BossBar;
import org.checkerframework.checker.nullness.qual.NonNull;

final class BungeeBossBar implements net.kyori.adventure.bossbar.BossBar.Listener {
  private static final int ACTION_CREATE = 0;
  private static final int ACTION_REMOVE = 1;
  private static final int ACTION_PERCENT = 2;
  private static final int ACTION_TITLE = 3;
  private static final int ACTION_STYLE = 4;
  private static final int ACTION_FLAGS = 5;

  private static final byte FLAG_DARKEN_SCREEN = 1;
  private static final byte FLAG_BOSS_MUSIC = 1 << 1;
  private static final byte FLAG_CREATE_WORLD_FOG = 1 << 2;

  private final UUID id = UUID.randomUUID();
  private final Set<ProxiedPlayer> subscribers = Collections.newSetFromMap(new WeakHashMap<>());

  BungeeBossBar() { }

  @Override
  public void bossBarChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final @NonNull Change type) {
    final BossBar packet;
    switch(type) {

      case NAME:
        packet = new BossBar(this.id, ACTION_TITLE);
        packet.setTitle(GsonComponentSerializer.INSTANCE.serialize(bar.name()));
        break;
      case PERCENT:
        packet = new BossBar(this.id, ACTION_PERCENT);
        packet.setHealth(bar.percent());
        break;
      case COLOR: // fall-through
      case OVERLAY:
        packet = new BossBar(this.id, ACTION_STYLE);
        packet.setColor(bar.color().ordinal()); // TODO: don't depend on enum ordering
        packet.setDivision(bar.overlay().ordinal());
        break;
      case FLAGS:
        packet = new BossBar(this.id, ACTION_FLAGS);
        packet.setFlags(this.bitmaskFlags(bar));
        break;
      default:
        throw new IllegalArgumentException("Unknown change type " + type);
    }

    for (ProxiedPlayer player : subscribers) {
      player.unsafe().sendPacket(packet);
    }
  }

  private byte bitmaskFlags(net.kyori.adventure.bossbar.BossBar bar) {
    byte mask = 0;
    for (net.kyori.adventure.bossbar.BossBar.Flag flag : bar.flags()) {
      switch(flag) {
        case DARKEN_SCREEN:
          mask |= FLAG_DARKEN_SCREEN;
          break;
        case PLAY_BOSS_MUSIC:
          mask |= FLAG_BOSS_MUSIC;
          break;
        case CREATE_WORLD_FOG:
          mask |= FLAG_CREATE_WORLD_FOG;
          break;
      }
    }
    return mask;
  }

  private BossBar newCreatePacket(net.kyori.adventure.bossbar.BossBar bar) {
    final BossBar packet = new BossBar(this.id, ACTION_CREATE);
    packet.setTitle(GsonComponentSerializer.INSTANCE.serialize(bar.name()));
    packet.setHealth(bar.percent());
    packet.setColor(bar.color().ordinal()); // TODO: see above
    packet.setDivision(bar.overlay().ordinal());
    packet.setFlags(bitmaskFlags(bar));
    return packet;
  }

  public void subscribe(net.kyori.adventure.bossbar.BossBar bar, ProxiedPlayer player) {
    if (canSeeBossBars(player) && subscribers.add(player)) {
      player.unsafe().sendPacket(newCreatePacket(bar));
    }
  }

  public void unsubscribe(ProxiedPlayer player) {
    if (subscribers.remove(player)) {
      player.unsafe().sendPacket(new BossBar(this.id, ACTION_REMOVE));
    }
  }

  public void subscribeAll(net.kyori.adventure.bossbar.BossBar bar, Iterable<ProxiedPlayer> players) {
    final Iterator<ProxiedPlayer> it = players.iterator();
    if (!it.hasNext()) {
      return;
    }
    final BossBar packet = newCreatePacket(bar);
    while (it.hasNext()) {
      final ProxiedPlayer ply = it.next();
      if (canSeeBossBars(ply) && subscribers.add(ply)) {
        ply.unsafe().sendPacket(packet);
      }
    }
  }

  public void unsubscribeAll(Iterable<ProxiedPlayer> players) {
    final Iterator<ProxiedPlayer> it = players.iterator();
    if (!it.hasNext()) {
      return;
    }
    final BossBar packet = new BossBar(this.id, ACTION_REMOVE);
    while (it.hasNext()) {
      final ProxiedPlayer ply = it.next();
      if (subscribers.remove(ply)) {
        ply.unsafe().sendPacket(packet);
      }
    }

  }

  /**
   * Check if a player is connecting with at least Minecraft 1.9, the version when boss bars were added
   *
   * @param player The player to check
   * @return if the player has a client with boss bar support
   */
  private static boolean canSeeBossBars(ProxiedPlayer player) {
    return player.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_9;
  }
}
