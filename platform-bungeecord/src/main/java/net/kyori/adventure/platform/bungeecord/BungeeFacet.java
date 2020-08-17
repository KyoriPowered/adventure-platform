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

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetBase;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import static net.kyori.adventure.platform.facet.Knob.logUnsupported;
import static net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.legacy;
import static net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.get;

class BungeeFacet<V extends CommandSender> extends FacetBase<V> {
  protected BungeeFacet(final @Nullable Class<? extends V> viewerClass) {
    super(viewerClass);
  }

  static class ChatConsole extends BungeeFacet<CommandSender> implements Facet.Chat<CommandSender, BaseComponent[]> {
    protected ChatConsole() {
      super(CommandSender.class);
    }

    @Override
    public boolean isApplicable(final @NonNull CommandSender viewer) {
      return super.isApplicable(viewer) && !(viewer instanceof Connection); // Console only accepts legacy formatting
    }

    @Override
    public BaseComponent @NonNull[] createMessage(final @NonNull CommandSender viewer, final @NonNull Component message) {
      return legacy().serialize(message);
    }

    @Override
    public void sendMessage(final @NonNull CommandSender viewer, final BaseComponent @NonNull[] message, final @NonNull MessageType type) {
      viewer.sendMessage(message);
    }
  }

  static class Message extends BungeeFacet<ProxiedPlayer> implements Facet.Message<ProxiedPlayer, BaseComponent[]> {
    protected Message() {
      super(ProxiedPlayer.class);
    }

    @Override
    public BaseComponent @NonNull[] createMessage(final @NonNull ProxiedPlayer viewer, final @NonNull Component message) {
      if(viewer.getPendingConnection().getVersion() >= PROTOCOL_HEX_COLOR) {
        return get().serialize(message);
      } else {
        return legacy().serialize(message);
      }
    }
  }

  static class ChatPlayer extends Message implements Facet.Chat<ProxiedPlayer, BaseComponent[]> {
    public @Nullable ChatMessageType createType(final @NonNull MessageType type) {
      if(type == MessageType.CHAT) {
        return ChatMessageType.CHAT;
      } else if(type == MessageType.SYSTEM) {
        return ChatMessageType.SYSTEM;
      }
      logUnsupported(this, type);
      return null;
    }

    @Override
    public void sendMessage(final @NonNull ProxiedPlayer viewer, final BaseComponent @NonNull[] message, final @NonNull MessageType type) {
      final ChatMessageType chat = this.createType(type);
      if(chat != null) {
        viewer.sendMessage(chat, message);
      }
    }
  }

  static class ActionBar extends Message implements Facet.ActionBar<ProxiedPlayer, BaseComponent[]> {
    @Override
    public void sendMessage(final @NonNull ProxiedPlayer viewer, final BaseComponent @NonNull[] message) {
      viewer.sendMessage(ChatMessageType.ACTION_BAR, message);
    }
  }

  static class Title extends Message implements Facet.Title<ProxiedPlayer, BaseComponent[], net.md_5.bungee.api.Title> {
    private static final net.md_5.bungee.api.Title CLEAR = ProxyServer.getInstance().createTitle().clear();
    private static final net.md_5.bungee.api.Title RESET = ProxyServer.getInstance().createTitle().reset();

    @Override
    public net.md_5.bungee.api.@NonNull Title createTitle(final BaseComponent @Nullable[] title, final BaseComponent @Nullable[] subTitle, final int inTicks, final int stayTicks, final int outTicks) {
      final net.md_5.bungee.api.Title builder = ProxyServer.getInstance().createTitle();

      if(title != null) builder.title(title);
      if(subTitle != null) builder.subTitle(subTitle);
      if(inTicks > -1) builder.fadeIn(inTicks);
      if(stayTicks > -1) builder.stay(stayTicks);
      if(outTicks > -1) builder.fadeOut(outTicks);

      return builder;
    }

    @Override
    public void showTitle(final @NonNull ProxiedPlayer viewer, final net.md_5.bungee.api.@NonNull Title title) {
      viewer.sendTitle(title);
    }

    @Override
    public void clearTitle(final @NonNull ProxiedPlayer viewer) {
      viewer.sendTitle(CLEAR);
    }

    @Override
    public void resetTitle(final @NonNull ProxiedPlayer viewer) {
      viewer.sendTitle(RESET);
    }
  }

  static class BossBar extends Message implements BossBarPacket<ProxiedPlayer> {
    private final Set<ProxiedPlayer> viewers;
    private final net.md_5.bungee.protocol.packet.BossBar bar;
    private volatile boolean initialized = false;

    protected BossBar(final @NonNull Collection<ProxiedPlayer> viewers) {
      super();
      this.viewers = new CopyOnWriteArraySet<>(viewers);
      this.bar = new net.md_5.bungee.protocol.packet.BossBar(UUID.randomUUID(), ACTION_ADD);
    }

    static class Builder extends BungeeFacet<ProxiedPlayer> implements Facet.BossBar.Builder<ProxiedPlayer, net.kyori.adventure.platform.bungeecord.BungeeFacet.BossBar> {
      protected Builder() {
        super(ProxiedPlayer.class);
      }

      @Override
      public boolean isApplicable(final @NonNull ProxiedPlayer viewer) {
        return super.isApplicable(viewer) && viewer.getPendingConnection().getVersion() >= PROTOCOL_BOSS_BAR;
      }

      @Override
      public net.kyori.adventure.platform.bungeecord.BungeeFacet.@NonNull BossBar createBossBar(final @NonNull Collection<ProxiedPlayer> viewers) {
        return new net.kyori.adventure.platform.bungeecord.BungeeFacet.BossBar(viewers);
      }
    }

    @Override
    public void bossBarInitialized(final net.kyori.adventure.bossbar.@NonNull BossBar bar) {
      BossBarPacket.super.bossBarInitialized(bar);
      this.initialized = true;
      this.broadcastPacket(ACTION_ADD);
    }

    @Override
    public void bossBarNameChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final @NonNull Component oldName, final @NonNull Component newName) {
      if(!this.viewers.isEmpty()) {
        this.bar.setTitle(ComponentSerializer.toString(this.createMessage(this.viewers.iterator().next(), newName)));
        this.broadcastPacket(ACTION_TITLE);
      }
    }

    @Override
    public void bossBarPercentChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final float oldPercent, final float newPercent) {
      this.bar.setHealth(newPercent);
      this.broadcastPacket(ACTION_HEALTH);
    }

    @Override
    public void bossBarColorChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NonNull Color oldColor, final net.kyori.adventure.bossbar.BossBar.@NonNull Color newColor) {
      this.bar.setColor(this.createColor(newColor));
      this.broadcastPacket(ACTION_STYLE);
    }

    @Override
    public void bossBarOverlayChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NonNull Overlay oldOverlay, final net.kyori.adventure.bossbar.BossBar.@NonNull Overlay newOverlay) {
      this.bar.setDivision(this.createOverlay(newOverlay));
      this.broadcastPacket(ACTION_STYLE);
    }

    @Override
    public void bossBarFlagsChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final @NonNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsAdded, final @NonNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsRemoved) {
      this.bar.setFlags(this.createFlag(this.bar.getFlags(), flagsAdded, flagsRemoved));
      this.broadcastPacket(ACTION_FLAG);
    }

    @Override
    public void addViewer(final @NonNull ProxiedPlayer viewer) {
      this.viewers.add(viewer);
      this.sendPacket(ACTION_ADD, viewer);
    }

    @Override
    public void removeViewer(final @NonNull ProxiedPlayer viewer) {
      this.viewers.remove(viewer);
      this.sendPacket(ACTION_REMOVE, viewer);
    }

    @Override
    public boolean isEmpty() {
      return !this.initialized || this.viewers.isEmpty();
    }

    @Override
    public void close() {
      this.broadcastPacket(ACTION_REMOVE);
      this.viewers.clear();
    }

    private void broadcastPacket(final int action) {
      if(this.isEmpty()) return;

      synchronized(this.bar) {
        this.bar.setAction(action);
        for(final ProxiedPlayer viewer : this.viewers) {
          viewer.unsafe().sendPacket(this.bar);
        }
      }
    }

    private void sendPacket(final int action, final ProxiedPlayer... viewers) {
      synchronized(this.bar) {
        final int lastAction = this.bar.getAction();
        this.bar.setAction(action);
        for(final ProxiedPlayer viewer : viewers) {
          viewer.unsafe().sendPacket(this.bar);
        }
        this.bar.setAction(lastAction);
      }
    }
  }
}
