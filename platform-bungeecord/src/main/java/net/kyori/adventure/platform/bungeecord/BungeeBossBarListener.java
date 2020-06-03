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

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.checkerframework.checker.nullness.qual.NonNull;

final class BungeeBossBarListener implements net.kyori.adventure.bossbar.BossBar.Listener {
  private static final int ACTION_CREATE = 0;
  private static final int ACTION_REMOVE = 1;
  private static final int ACTION_PERCENT = 2;
  private static final int ACTION_TITLE = 3;
  private static final int ACTION_STYLE = 4;
  private static final int ACTION_FLAGS = 5;

  private static final byte FLAG_DARKEN_SCREEN = 1;
  private static final byte FLAG_BOSS_MUSIC = 1 << 1;
  private static final byte FLAG_CREATE_WORLD_FOG = 1 << 2;

  static final BungeeBossBarListener INSTANCE = new BungeeBossBarListener();

  private final Map<BossBar, Instance> bars = new IdentityHashMap<>();

  BungeeBossBarListener() {
  }

  @Override
  public void bossBarNameChanged(@NonNull final BossBar bar, @NonNull final Component oldName, @NonNull final Component newName) {
    bungee(bar).sendToSubscribers(bar, ACTION_TITLE, (adv, pkt) -> pkt.setTitle(GsonComponentSerializer.INSTANCE.serialize(adv.name())));
  }

  @Override
  public void bossBarPercentChanged(@NonNull final BossBar bar, final float oldPercent, final float newPercent) {
    bungee(bar).sendToSubscribers(bar, ACTION_PERCENT, (adv, pkt) -> pkt.setHealth(adv.percent()));
  }

  @Override
  public void bossBarColorChanged(@NonNull final BossBar bar, final BossBar.@NonNull Color oldColor, final BossBar.@NonNull Color newColor) {
    bungee(bar).sendToSubscribers(bar, ACTION_STYLE, (adv, pkt) -> {
      pkt.setColor(bungee(adv.color()));
      pkt.setDivision(bungee(adv.overlay()));
    });
  }

  @Override
  public void bossBarOverlayChanged(@NonNull final BossBar bar, final BossBar.@NonNull Overlay oldOverlay, final BossBar.@NonNull Overlay newOverlay) {
    bungee(bar).sendToSubscribers(bar, ACTION_STYLE, (adv, pkt) -> {
      pkt.setColor(bungee(adv.color()));
      pkt.setDivision(bungee(adv.overlay()));
    });
  }

  @Override
  public void bossBarFlagsChanged(@NonNull final BossBar bar, @NonNull final Set<BossBar.Flag> oldFlags, @NonNull final Set<BossBar.Flag> newFlags) {
    bungee(bar).sendToSubscribers(bar, ACTION_FLAGS, (adv, pkt) -> pkt.setFlags(bitmaskFlags(adv.flags())));
  }

  static byte bitmaskFlags(Set<BossBar.Flag> flags) {
    byte mask = 0;
    for(BossBar.Flag flag : flags) {
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

  private @NonNull Instance bungee(@NonNull BossBar bar) {
    final Instance ret = this.bars.get(bar);
    if(ret == null) {
      throw new IllegalArgumentException("Unknown boss bar instance " + bar);
    }
    return ret;
  }

  private @NonNull Instance bungeeCreating(final @NonNull BossBar bar) {
    return this.bars.computeIfAbsent(bar, key -> {
      key.addListener(this);
      return new Instance();
    });
  }

  private int bungee(net.kyori.adventure.bossbar.BossBar.@NonNull Color color) {
    return color.ordinal();
  }

  private int bungee(net.kyori.adventure.bossbar.BossBar.@NonNull Overlay overlay) {
    return overlay.ordinal();
  }

  public void subscribe(BossBar bar, ProxiedPlayer player) {
    if(canSeeBossBars(player)) {
      final Instance bungee = bungeeCreating(bar);
      if(bungee.subscribers.add(player)) {
        player.unsafe().sendPacket(bungee.newCreatePacket(bar));
      }
    }
  }

  public void unsubscribe(BossBar bar, ProxiedPlayer player) {
    this.bars.computeIfPresent(bar, (key, instance) -> {
      if(instance.subscribers.remove(player)) {
        player.unsafe().sendPacket(instance.newPacket(ACTION_REMOVE));
        if(instance.isEmpty()) {
          return null;
        }
      }
      return instance;
    });
  }

  public void subscribeAll(net.kyori.adventure.bossbar.BossBar bar, Iterable<ProxiedPlayer> players) {
    final Iterator<ProxiedPlayer> it = players.iterator();
    if(!it.hasNext()) {
      return;
    }
    final Instance bungee = bungeeCreating(bar);
    final net.md_5.bungee.protocol.packet.BossBar packet = bungee.newCreatePacket(bar);
    while(it.hasNext()) {
      final ProxiedPlayer ply = it.next();
      if(canSeeBossBars(ply) && bungee.subscribers.add(ply)) {
        ply.unsafe().sendPacket(packet);
      }
    }
  }

  public void unsubscribeAll(BossBar bar, Iterable<ProxiedPlayer> players) {
    final Iterator<ProxiedPlayer> it = players.iterator();
    if(!it.hasNext()) {
      return;
    }
    this.bars.computeIfPresent(bar, (key, instance) -> {
      while(it.hasNext()) {
        final ProxiedPlayer player = it.next();
        if(instance.subscribers.remove(player)) {
          player.unsafe().sendPacket(instance.newPacket(ACTION_REMOVE));
        }
      }
      if(instance.isEmpty()) {
        return null;
      }
      return instance;
    });
  }

  static class Instance {
    private final UUID id = UUID.randomUUID();
    final Set<ProxiedPlayer> subscribers = ConcurrentHashMap.newKeySet();

    net.md_5.bungee.protocol.packet.BossBar newCreatePacket(net.kyori.adventure.bossbar.BossBar bar) {
      final net.md_5.bungee.protocol.packet.BossBar packet = newPacket(ACTION_CREATE);
      packet.setTitle(GsonComponentSerializer.INSTANCE.serialize(bar.name()));
      packet.setHealth(bar.percent());
      packet.setColor(bar.color().ordinal()); // TODO: see above
      packet.setDivision(bar.overlay().ordinal());
      packet.setFlags(bitmaskFlags(bar.flags()));
      return packet;
    }

    net.md_5.bungee.protocol.packet.BossBar newPacket(final int action) {
      return new net.md_5.bungee.protocol.packet.BossBar(this.id, action);
    }

    void sendToSubscribers(final BossBar bar, final int action, final BiConsumer<BossBar, net.md_5.bungee.protocol.packet.BossBar> packetModifier) {
      final net.md_5.bungee.protocol.packet.BossBar packet = newPacket(action);
      packetModifier.accept(bar, packet);
      sendToSubscribers(packet);
    }

    void sendToSubscribers(final net.md_5.bungee.protocol.packet.BossBar packet) {
      for(ProxiedPlayer player : this.subscribers) {
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
  private static boolean canSeeBossBars(@NonNull ProxiedPlayer player) {
    return player.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_9;
  }
}
