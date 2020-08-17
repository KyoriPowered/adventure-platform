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
package net.kyori.adventure.platform.viaversion;

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetBase;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.protocol.ClientboundPacketType;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.kyori.adventure.platform.facet.Knob.logError;
import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;
import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.colorDownsamplingGson;

public class ViaFacet<V> extends FacetBase<V> implements Facet.Message<V, String> {
  private static final String PACKAGE = "us.myles.ViaVersion";
  private static final boolean SUPPORTED;

  static {
    boolean supported = false;
    try {
      Class.forName(PACKAGE + ".api.protocol.ProtocolRegistry");
      supported = true;
    } catch(final Throwable error) {
      // Silently fail, ViaVersion is not loaded
    }
    SUPPORTED = supported;
  }

  private final Function<V, UserConnection> connectionFunction;
  private final int minProtocol;

  public ViaFacet(final @NonNull Class<? extends V> viewerClass, final @NonNull Function<V, UserConnection> connectionFunction, final int minProtocol) {
    super(viewerClass);
    this.connectionFunction = connectionFunction;
    this.minProtocol = minProtocol;
  }

  @Override
  public boolean isSupported() {
    return super.isSupported()
      && SUPPORTED
      && this.connectionFunction != null
      && this.minProtocol >= 0;
  }

  @Override
  public boolean isApplicable(final @NonNull V viewer) {
    return super.isApplicable(viewer)
      && this.minProtocol > ProtocolRegistry.SERVER_PROTOCOL
      && this.findProtocol(viewer) >= this.minProtocol;
  }

  public @Nullable UserConnection findConnection(final @NonNull V viewer) {
    return this.connectionFunction.apply(viewer);
  }

  public int findProtocol(final @NonNull V viewer) {
    final UserConnection connection = this.findConnection(viewer);
    if(connection != null) {
      final ProtocolInfo info = connection.getProtocolInfo();
      if(info != null) {
        return info.getProtocolVersion();
      }
    }
    return -1;
  }

  @NonNull
  @Override
  public String createMessage(final @NonNull V viewer, final @NonNull Component message) {
    final int protocol = this.findProtocol(viewer);
    if(protocol >= PROTOCOL_HEX_COLOR) {
      return gson().serialize(message);
    } else {
      return colorDownsamplingGson().serialize(message);
    }
  }

  public static class ProtocolBased<V> extends ViaFacet<V> {
    private final Class<? extends Protocol<?, ?, ?, ?>> protocolClass;
    private final Class<? extends ClientboundPacketType> packetClass;
    private final int packetId;

    @SuppressWarnings("unchecked")
    protected ProtocolBased(final @NonNull String fromProtocol, final @NonNull String toProtocol, final int minProtocol, final @NonNull String packetName, final @NonNull Class<? extends V> viewerClass, final @NonNull Function<V, UserConnection> connectionFunction) {
      super(viewerClass, connectionFunction, minProtocol);

      final String protocolClassName = MessageFormat.format("{0}.protocols.protocol{1}to{2}.Protocol{1}To{2}", PACKAGE, fromProtocol, toProtocol);
      final String packetClassName = MessageFormat.format("{0}.protocols.protocol{1}to{2}.ClientboundPackets{1}", PACKAGE, fromProtocol, toProtocol);

      Class<? extends Protocol<?, ?, ?, ?>> protocolClass = null;
      Class<? extends ClientboundPacketType> packetClass = null;
      int packetId = -1;
      try {
        protocolClass = (Class<? extends Protocol<?, ?, ?, ?>>) Class.forName(protocolClassName);
        packetClass = (Class<? extends ClientboundPacketType>) Class.forName(packetClassName);
        for(final ClientboundPacketType type : packetClass.getEnumConstants()) {
          if(type.name().equals(packetName)) {
            packetId = type.ordinal();
            break;
          }
        }
      } catch(final Throwable error) {
        // No-op, ViaVersion is not loaded
      }

      this.protocolClass = protocolClass;
      this.packetClass = packetClass;
      this.packetId = packetId;
    }

    @Override
    public boolean isSupported() {
      return super.isSupported()
        && this.protocolClass != null
        && this.packetClass != null
        && this.packetId >= 0;
    }

    public PacketWrapper createPacket(final @NonNull V viewer) {
      return new PacketWrapper(this.packetId, null, this.findConnection(viewer));
    }

    public void sendPacket(final @NonNull PacketWrapper packet) {
      if(packet.user() == null) return;
      try {
        packet.send(this.protocolClass);
      } catch(final Throwable error) {
        logError(error, "Failed to send ViaVersion packet: %s %s", packet.user(), packet);
      }
    }
  }

  public static class Chat<V> extends ProtocolBased<V> implements ChatPacket<V, String> {
    public Chat(final @NonNull Class<? extends V> viewerClass, final @NonNull Function<V, UserConnection> connectionFunction) {
      super("1_16", "1_15_2", PROTOCOL_HEX_COLOR, "CHAT_MESSAGE", viewerClass, connectionFunction);
    }

    @Override
    public void sendMessage(final @NonNull V viewer, final @NonNull String message, final @NonNull MessageType type) {
      final PacketWrapper packet = this.createPacket(viewer);
      packet.write(Type.STRING, message);
      packet.write(Type.BYTE, this.createMessageType(type));
      packet.write(Type.UUID, SENDER_NULL);
      this.sendPacket(packet);
    }
  }

  public static class ActionBar<V> extends Chat<V> implements Facet.ActionBar<V, String> {
    public ActionBar(final @NonNull Class<? extends V> viewerClass, final @NonNull Function<V, UserConnection> connectionFunction) {
      super(viewerClass, connectionFunction);
    }

    @Override
    public byte createMessageType(final @NonNull MessageType type) {
      return TYPE_ACTION_BAR;
    }

    @Override
    public void sendMessage(final @NonNull V viewer, final @NonNull String message) {
      this.sendMessage(viewer, message, MessageType.CHAT);
    }
  }

  public static class ActionBarTitle<V> extends ProtocolBased<V> implements Facet.ActionBar<V, String> {
    public ActionBarTitle(final @NonNull Class<? extends V> viewerClass, final @NonNull Function<V, UserConnection> connectionFunction) {
      super("1_11", "1_10", TitlePacket.PROTOCOL_ACTION_BAR, "TITLE", viewerClass, connectionFunction);
    }

    @Override
    public void sendMessage(final @NonNull V viewer, final @NonNull String message) {
      final PacketWrapper packet = this.createPacket(viewer);
      packet.write(Type.VAR_INT, TitlePacket.ACTION_ACTIONBAR);
      packet.write(Type.STRING, message);
      this.sendPacket(packet);
    }
  }

  public static class Title<V> extends ProtocolBased<V> implements Facet.TitlePacket<V, String, Consumer<V>> {
    protected Title(final @NonNull String fromProtocol, final @NonNull String toProtocol, final int minProtocol, final @NonNull Class<? extends V> viewerClass, final @NonNull Function<V, UserConnection> connectionFunction) {
      super(fromProtocol, toProtocol, minProtocol, "TITLE", viewerClass, connectionFunction);
    }

    public Title(final @NonNull Class<? extends V> viewerClass, final @NonNull Function<V, UserConnection> connectionFunction) {
      this("1_16", "1_15_2", PROTOCOL_HEX_COLOR, viewerClass, connectionFunction);
    }

    @NonNull
    @Override
    public Consumer<V> createTitle(final @Nullable String title, final @Nullable String subTitle, final int inTicks, final int stayTicks, final int outTicks) {
      return viewer -> {
        if(inTicks > -1 || stayTicks > -1 || outTicks > -1) {
          final PacketWrapper packet = this.createPacket(viewer);
          packet.write(Type.VAR_INT, ACTION_TIMES);
          packet.write(Type.INT, inTicks);
          packet.write(Type.INT, stayTicks);
          packet.write(Type.INT, outTicks);
          this.sendPacket(packet);
        }

        if(subTitle != null) {
          final PacketWrapper packet = this.createPacket(viewer);
          packet.write(Type.VAR_INT, ACTION_SUBTITLE);
          packet.write(Type.STRING, subTitle);
          this.sendPacket(packet);
        }

        if(title != null) {
          final PacketWrapper packet = this.createPacket(viewer);
          packet.write(Type.VAR_INT, ACTION_TITLE);
          packet.write(Type.STRING, title);
          this.sendPacket(packet);
        }
      };
    }

    @Override
    public void showTitle(final @NonNull V viewer, final @NonNull Consumer<V> title) {
      title.accept(viewer);
    }

    @Override
    public void clearTitle(final @NonNull V viewer) {
      final PacketWrapper packet = this.createPacket(viewer);
      packet.write(Type.VAR_INT, ACTION_CLEAR);
      this.sendPacket(packet);
    }

    @Override
    public void resetTitle(final @NonNull V viewer) {
      final PacketWrapper packet = this.createPacket(viewer);
      packet.write(Type.VAR_INT, ACTION_RESET);
      this.sendPacket(packet);
    }
  }

  public static final class BossBar<V> extends ProtocolBased<V> implements Facet.BossBarPacket<V> {
    private final Set<V> viewers;
    private UUID id;
    private String title;
    private float health;
    private int color;
    private int overlay;
    private byte flags;

    private BossBar(final @NonNull String fromProtocol, final @NonNull String toProtocol, final @NonNull Class<? extends V> viewerClass, final @NonNull Function<V, UserConnection> connectionFunction, final Collection<V> viewers) {
      super(fromProtocol, toProtocol, PROTOCOL_BOSS_BAR, "BOSSBAR", viewerClass, connectionFunction);
      this.viewers = new CopyOnWriteArraySet<>(viewers);
    }

    public static class Builder<V> extends ViaFacet<V> implements Facet.BossBar.Builder<V, Facet.BossBar<V>> {
      public Builder(final @NonNull Class<? extends V> viewerClass, final @NonNull Function<V, UserConnection> connectionFunction) {
        super(viewerClass, connectionFunction, PROTOCOL_HEX_COLOR);
      }

      @Override
      public Facet.@NonNull BossBar<V> createBossBar(final @NonNull Collection<V> viewer) {
        return new ViaFacet.BossBar<>("1_16", "1_15_2", this.viewerClass, this::findConnection, viewer);
      }
    }

    public static class Builder1_9_To_1_15<V> extends ViaFacet<V> implements Facet.BossBar.Builder<V, Facet.BossBar<V>> {
      public Builder1_9_To_1_15(final @NonNull Class<? extends V> viewerClass, final @NonNull Function<V, UserConnection> connectionFunction) {
        super(viewerClass, connectionFunction, PROTOCOL_BOSS_BAR);
      }

      @Override
      public Facet.@NonNull BossBar<V> createBossBar(final @NonNull Collection<V> viewer) {
        return new ViaFacet.BossBar<>("1_9", "1_8", this.viewerClass, this::findConnection, viewer);
      }
    }

    @Override
    public void bossBarInitialized(final net.kyori.adventure.bossbar.@NonNull BossBar bar) {
      Facet.BossBarPacket.super.bossBarInitialized(bar);
      this.id = UUID.randomUUID();
      this.broadcastPacket(ACTION_ADD);
    }

    @Override
    public void bossBarNameChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final @NonNull Component oldName, final @NonNull Component newName) {
      if(!this.viewers.isEmpty()) {
        this.title = this.createMessage(this.viewers.iterator().next(), newName);
        this.broadcastPacket(ACTION_TITLE);
      }
    }

    @Override
    public void bossBarPercentChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final float oldPercent, final float newPercent) {
      this.health = newPercent;
      this.broadcastPacket(ACTION_HEALTH);
    }

    @Override
    public void bossBarColorChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NonNull Color oldColor, final net.kyori.adventure.bossbar.BossBar.@NonNull Color newColor) {
      this.color = this.createColor(newColor);
      this.broadcastPacket(ACTION_STYLE);
    }

    @Override
    public void bossBarOverlayChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NonNull Overlay oldOverlay, final net.kyori.adventure.bossbar.BossBar.@NonNull Overlay newOverlay) {
      this.overlay = this.createOverlay(newOverlay);
      this.broadcastPacket(ACTION_STYLE);
    }

    @Override
    public void bossBarFlagsChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final @NonNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsAdded, final @NonNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsRemoved) {
      this.flags = this.createFlag(this.flags, flagsAdded, flagsRemoved);
      this.broadcastPacket(ACTION_FLAG);
    }

    public void sendPacket(final @NonNull V viewer, final int action) {
      final PacketWrapper packet = this.createPacket(viewer);
      packet.write(Type.UUID, this.id);
      packet.write(Type.VAR_INT, action);
      if(action == ACTION_ADD || action == ACTION_TITLE) {
        packet.write(Type.STRING, this.title);
      }
      if(action == ACTION_ADD || action == ACTION_HEALTH) {
        packet.write(Type.FLOAT, this.health);
      }
      if(action == ACTION_ADD || action == ACTION_STYLE) {
        packet.write(Type.VAR_INT, this.color);
        packet.write(Type.VAR_INT, this.overlay);
      }
      if(action == ACTION_ADD || action == ACTION_FLAG) {
        packet.write(Type.BYTE, this.flags);
      }
      this.sendPacket(packet);
    }

    public void broadcastPacket(final int action) {
      if(this.isEmpty()) return;
      for(final V viewer : this.viewers) {
        this.sendPacket(viewer, action);
      }
    }

    @Override
    public void addViewer(final @NonNull V viewer) {
      if(this.viewers.add(viewer)) {
        this.sendPacket(viewer, ACTION_ADD);
      }
    }

    @Override
    public void removeViewer(final @NonNull V viewer) {
      if(this.viewers.remove(viewer)) {
        this.sendPacket(viewer, ACTION_REMOVE);
      }
    }

    @Override
    public boolean isEmpty() {
      return this.id == null || this.viewers.isEmpty();
    }

    @Override
    public void close() {
      this.broadcastPacket(ACTION_REMOVE);
      this.viewers.clear();
    }
  }
}
