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

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetBase;
import net.kyori.adventure.platform.facet.FacetPointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.platform.facet.Knob.logUnsupported;
import static net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.get;
import static net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.legacy;

class BungeeFacet<V extends CommandSender> extends FacetBase<V> {
  static final BaseComponent[] EMPTY_COMPONENT_ARRAY = new BaseComponent[0];

  protected BungeeFacet(final @Nullable Class<? extends V> viewerClass) {
    super(viewerClass);
  }

  static class ChatConsole extends BungeeFacet<CommandSender> implements Facet.Chat<CommandSender, BaseComponent[]> {
    protected ChatConsole() {
      super(CommandSender.class);
    }

    @Override
    public boolean isApplicable(final @NotNull CommandSender viewer) {
      return super.isApplicable(viewer) && !(viewer instanceof Connection); // Console only accepts legacy formatting
    }

    @Override
    public BaseComponent @NotNull[] createMessage(final @NotNull CommandSender viewer, final @NotNull Component message) {
      return legacy().serialize(message);
    }

    @Override
    public void sendMessage(final @NotNull CommandSender viewer, final @NotNull Identity source, final BaseComponent @NotNull [] message, final @NotNull MessageType type) {
      viewer.sendMessage(message);
    }
  }

  static class Message extends BungeeFacet<ProxiedPlayer> implements Facet.Message<ProxiedPlayer, BaseComponent[]> {
    protected Message() {
      super(ProxiedPlayer.class);
    }

    @Override
    public BaseComponent @NotNull[] createMessage(final @NotNull ProxiedPlayer viewer, final @NotNull Component message) {
      if (viewer.getPendingConnection().getVersion() >= PROTOCOL_HEX_COLOR) {
        return get().serialize(message);
      } else {
        return legacy().serialize(message);
      }
    }
  }

  static class ChatPlayer extends Message implements Facet.Chat<ProxiedPlayer, BaseComponent[]> {
    public @Nullable ChatMessageType createType(final @NotNull MessageType type) {
      if (type == MessageType.CHAT) {
        return ChatMessageType.CHAT;
      } else if (type == MessageType.SYSTEM) {
        return ChatMessageType.SYSTEM;
      }
      logUnsupported(this, type);
      return null;
    }

    @Override
    public void sendMessage(final @NotNull ProxiedPlayer viewer, final @NotNull Identity source, final BaseComponent @NotNull [] message, final @NotNull MessageType type) {
      final ChatMessageType chat = this.createType(type);
      if (chat != null) {
        viewer.sendMessage(chat, message);
      }
    }
  }

  static class ActionBar extends Message implements Facet.ActionBar<ProxiedPlayer, BaseComponent[]> {
    @Override
    public void sendMessage(final @NotNull ProxiedPlayer viewer, final BaseComponent @NotNull[] message) {
      viewer.sendMessage(ChatMessageType.ACTION_BAR, message);
    }
  }

  static class Title extends Message implements Facet.Title<ProxiedPlayer, BaseComponent[], net.md_5.bungee.api.Title, net.md_5.bungee.api.Title> {
    private static final net.md_5.bungee.api.Title CLEAR = ProxyServer.getInstance().createTitle().clear();
    private static final net.md_5.bungee.api.Title RESET = ProxyServer.getInstance().createTitle().reset();

    @Override
    public net.md_5.bungee.api.@NotNull Title createTitleCollection() {
      return ProxyServer.getInstance().createTitle();
    }

    @Override
    public void contributeTitle(final net.md_5.bungee.api.@NotNull Title coll, final BaseComponent@NotNull[] title) {
      coll.title(title);
    }

    @Override
    public void contributeSubtitle(final net.md_5.bungee.api.@NotNull Title coll, final BaseComponent@NotNull[] subtitle) {
      coll.subTitle(subtitle);
    }

    @Override
    public void contributeTimes(final net.md_5.bungee.api.@NotNull Title coll, final int inTicks, final int stayTicks, final int outTicks) {
      if (inTicks > -1) coll.fadeIn(inTicks);
      if (stayTicks > -1) coll.stay(stayTicks);
      if (outTicks > -1) coll.fadeOut(outTicks);
    }

    @Nullable
    @Override
    public net.md_5.bungee.api.Title completeTitle(final net.md_5.bungee.api.@NotNull Title coll) {
      return coll;
    }

    @Override
    public void showTitle(final @NotNull ProxiedPlayer viewer, final net.md_5.bungee.api.@NotNull Title title) {
      viewer.sendTitle(title);
    }

    @Override
    public void clearTitle(final @NotNull ProxiedPlayer viewer) {
      viewer.sendTitle(CLEAR);
    }

    @Override
    public void resetTitle(final @NotNull ProxiedPlayer viewer) {
      viewer.sendTitle(RESET);
    }
  }

  static class BossBar extends Message implements BossBarPacket<ProxiedPlayer> {
    private final Set<ProxiedPlayer> viewers;
    private final net.md_5.bungee.protocol.packet.BossBar bar;
    private volatile boolean initialized = false;

    protected BossBar(final @NotNull Collection<ProxiedPlayer> viewers) {
      super();
      this.viewers = new CopyOnWriteArraySet<>(viewers);
      this.bar = new net.md_5.bungee.protocol.packet.BossBar(UUID.randomUUID(), ACTION_ADD);
    }

    static class Builder extends BungeeFacet<ProxiedPlayer> implements Facet.BossBar.Builder<ProxiedPlayer, net.kyori.adventure.platform.bungeecord.BungeeFacet.BossBar> {
      protected Builder() {
        super(ProxiedPlayer.class);
      }

      @Override
      public boolean isApplicable(final @NotNull ProxiedPlayer viewer) {
        return super.isApplicable(viewer) && viewer.getPendingConnection().getVersion() >= PROTOCOL_BOSS_BAR;
      }

      @Override
      public net.kyori.adventure.platform.bungeecord.BungeeFacet.@NotNull BossBar createBossBar(final @NotNull Collection<ProxiedPlayer> viewers) {
        return new net.kyori.adventure.platform.bungeecord.BungeeFacet.BossBar(viewers);
      }
    }

    @Override
    public void bossBarInitialized(final net.kyori.adventure.bossbar.@NotNull BossBar bar) {
      BossBarPacket.super.bossBarInitialized(bar);
      this.initialized = true;
      this.broadcastPacket(ACTION_ADD);
    }

    @Override
    public void bossBarNameChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final @NotNull Component oldName, final @NotNull Component newName) {
      if (!this.viewers.isEmpty()) {
        this.bar.setTitle(ComponentSerializer.toString(this.createMessage(this.viewers.iterator().next(), newName)));
        this.broadcastPacket(ACTION_TITLE);
      }
    }

    @Override
    public void bossBarProgressChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final float oldPercent, final float newPercent) {
      this.bar.setHealth(newPercent);
      this.broadcastPacket(ACTION_HEALTH);
    }

    @Override
    public void bossBarColorChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NotNull Color oldColor, final net.kyori.adventure.bossbar.BossBar.@NotNull Color newColor) {
      this.bar.setColor(this.createColor(newColor));
      this.broadcastPacket(ACTION_STYLE);
    }

    @Override
    public void bossBarOverlayChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NotNull Overlay oldOverlay, final net.kyori.adventure.bossbar.BossBar.@NotNull Overlay newOverlay) {
      this.bar.setDivision(this.createOverlay(newOverlay));
      this.broadcastPacket(ACTION_STYLE);
    }

    @Override
    public void bossBarFlagsChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsAdded, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsRemoved) {
      this.bar.setFlags(this.createFlag(this.bar.getFlags(), flagsAdded, flagsRemoved));
      this.broadcastPacket(ACTION_FLAG);
    }

    @Override
    public void addViewer(final @NotNull ProxiedPlayer viewer) {
      this.viewers.add(viewer);
      this.sendPacket(ACTION_ADD, viewer);
    }

    @Override
    public void removeViewer(final @NotNull ProxiedPlayer viewer) {
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
      if (this.isEmpty()) return;

      synchronized(this.bar) {
        this.bar.setAction(action);
        for (final ProxiedPlayer viewer : this.viewers) {
          viewer.unsafe().sendPacket(this.bar);
        }
      }
    }

    private void sendPacket(final int action, final ProxiedPlayer... viewers) {
      synchronized(this.bar) {
        final int lastAction = this.bar.getAction();
        this.bar.setAction(action);
        for (final ProxiedPlayer viewer : viewers) {
          viewer.unsafe().sendPacket(this.bar);
        }
        this.bar.setAction(lastAction);
      }
    }
  }

  static final class TabList extends Message implements Facet.TabList<ProxiedPlayer, BaseComponent[]> {

    @Override
    public void send(final ProxiedPlayer viewer, final BaseComponent@Nullable[] header, final BaseComponent@Nullable[] footer) {
      viewer.setTabHeader(
        header == null ? EMPTY_COMPONENT_ARRAY : header,
        footer == null ? EMPTY_COMPONENT_ARRAY : footer);
    }
  }

  static final class CommandSenderPointers extends BungeeFacet<CommandSender> implements Facet.Pointers<CommandSender> {
    CommandSenderPointers() {
      super(CommandSender.class);
    }

    @Override
    public void contributePointers(final CommandSender viewer, final net.kyori.adventure.pointer.Pointers.Builder builder) {
      builder.withDynamic(Identity.NAME, viewer::getName);
      // todo: bungee doesn't expose any sort of TriState/isPermissionSet value :((((
      builder.withStatic(PermissionChecker.POINTER, perm -> viewer.hasPermission(perm) ? TriState.TRUE : TriState.FALSE);
      if (!(viewer instanceof ProxiedPlayer)) {
        builder.withStatic(FacetPointers.TYPE, viewer == ProxyServer.getInstance().getConsole() ? FacetPointers.Type.CONSOLE : FacetPointers.Type.OTHER);
      }
    }
  }

  static final class PlayerPointers extends BungeeFacet<ProxiedPlayer> implements Facet.Pointers<ProxiedPlayer> {
    PlayerPointers() {
      super(ProxiedPlayer.class);
    }

    @Override
    public void contributePointers(final ProxiedPlayer viewer, final net.kyori.adventure.pointer.Pointers.Builder builder) {
      builder.withDynamic(Identity.UUID, viewer::getUniqueId);
      builder.withDynamic(Identity.LOCALE, viewer::getLocale);
      builder.withDynamic(FacetPointers.SERVER, () -> viewer.getServer().getInfo().getName());
      builder.withStatic(FacetPointers.TYPE, FacetPointers.Type.PLAYER);
    }
  }
}
