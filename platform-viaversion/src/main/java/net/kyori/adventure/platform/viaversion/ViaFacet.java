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

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetBase;
import net.kyori.adventure.platform.facet.Knob;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.platform.facet.Knob.logError;
import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.colorDownsamplingGson;
import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;

// Non-API
@SuppressWarnings({"checkstyle:FilteringWriteTag", "checkstyle:MissingJavadocType", "checkstyle:MissingJavadocMethod"})
public class ViaFacet<V> extends FacetBase<V> implements Facet.Message<V, String> {
  private static final String PACKAGE = "com.viaversion.viaversion";
  private static final boolean SUPPORTED;

  static {
    boolean supported = false;
    try {
      // Check if the ViaVersion API is present
      Class.forName(PACKAGE + ".api.ViaManager");
      supported = true;
    } catch (final Throwable error) {
      // ignore
    }
    SUPPORTED = supported && Knob.isEnabled("viaversion", true);
  }

  private final Function<V, UserConnection> connectionFunction;
  private final int minProtocol;

  public ViaFacet(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction, final int minProtocol) {
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
  public boolean isApplicable(final @NotNull V viewer) {
    return super.isApplicable(viewer)
      && this.minProtocol > Via.getAPI().getServerVersion().lowestSupportedVersion()
      && this.findProtocol(viewer) >= this.minProtocol;
  }

  public @Nullable UserConnection findConnection(final @NotNull V viewer) {
    return this.connectionFunction.apply(viewer);
  }

  public int findProtocol(final @NotNull V viewer) {
    final UserConnection connection = this.findConnection(viewer);
    if (connection != null) {
      return connection.getProtocolInfo().getProtocolVersion();
    }
    return -1;
  }

  @NotNull
  @Override
  public String createMessage(final @NotNull V viewer, final @NotNull Component message) {
    final int protocol = this.findProtocol(viewer);
    if (protocol >= PROTOCOL_HEX_COLOR) {
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
    protected ProtocolBased(final @NotNull String fromProtocol, final @NotNull String toProtocol, final int minProtocol, final @NotNull String packetName, final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
      super(viewerClass, connectionFunction, minProtocol);

      final String protocolClassName = MessageFormat.format("{0}.protocols.protocol{1}to{2}.Protocol{1}To{2}", PACKAGE, fromProtocol, toProtocol);
      final String packetClassName = MessageFormat.format("{0}.protocols.protocol{1}to{2}.ClientboundPackets{1}", PACKAGE, fromProtocol, toProtocol);

      Class<? extends Protocol<?, ?, ?, ?>> protocolClass = null;
      Class<? extends ClientboundPacketType> packetClass = null;
      int packetId = -1;
      try {
        protocolClass = (Class<? extends Protocol<?, ?, ?, ?>>) Class.forName(protocolClassName);
        packetClass = (Class<? extends ClientboundPacketType>) Class.forName(packetClassName);
        for (final ClientboundPacketType type : packetClass.getEnumConstants()) {
          if (type.getName().equals(packetName)) {
            packetId = type.getId();
            break;
          }
        }
      } catch (final Throwable error) {
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

    public PacketWrapper createPacket(final @NotNull V viewer) {
      return PacketWrapper.create(this.packetId, null, this.findConnection(viewer));
    }

    public void sendPacket(final @NotNull PacketWrapper packet) {
      if (packet.user() == null) return;
      try {
        packet.send(this.protocolClass);
      } catch (final Throwable error) {
        logError(error, "Failed to send ViaVersion packet: %s %s", packet.user(), packet);
      }
    }
  }

  public static class Chat<V> extends ProtocolBased<V> implements ChatPacket<V, String> {
    public Chat(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
      super("1_16", "1_15_2", PROTOCOL_HEX_COLOR, "CHAT_MESSAGE", viewerClass, connectionFunction);
    }

    @Override
    public void sendMessage(final @NotNull V viewer, final @NotNull Identity source, final @NotNull String message, final @NotNull MessageType type) {
      final PacketWrapper packet = this.createPacket(viewer);
      packet.write(Type.STRING, message);
      packet.write(Type.BYTE, this.createMessageType(type));
      packet.write(Type.UUID, source.uuid());
      this.sendPacket(packet);
    }
  }

  public static class ActionBar<V> extends Chat<V> implements Facet.ActionBar<V, String> {
    public ActionBar(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
      super(viewerClass, connectionFunction);
    }

    @Override
    public byte createMessageType(final @NotNull MessageType type) {
      return TYPE_ACTION_BAR;
    }

    @Override
    public void sendMessage(final @NotNull V viewer, final @NotNull String message) {
      this.sendMessage(viewer, Identity.nil(), message, MessageType.CHAT);
    }
  }

  public static class ActionBarTitle<V> extends ProtocolBased<V> implements Facet.ActionBar<V, String> {
    public ActionBarTitle(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
      super("1_11", "1_10", TitlePacket.PROTOCOL_ACTION_BAR, "TITLE", viewerClass, connectionFunction);
    }

    @Override
    public void sendMessage(final @NotNull V viewer, final @NotNull String message) {
      final PacketWrapper packet = this.createPacket(viewer);
      packet.write(Type.VAR_INT, TitlePacket.ACTION_ACTIONBAR);
      packet.write(Type.STRING, message);
      this.sendPacket(packet);
    }
  }

  public static class Title<V> extends ProtocolBased<V> implements Facet.TitlePacket<V, String, List<Consumer<PacketWrapper>>, Consumer<V>> {
    protected Title(final @NotNull String fromProtocol, final @NotNull String toProtocol, final int minProtocol, final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
      super(fromProtocol, toProtocol, minProtocol, "TITLE", viewerClass, connectionFunction);
    }

    public Title(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
      this("1_16", "1_15_2", PROTOCOL_HEX_COLOR, viewerClass, connectionFunction);
    }

    @Override
    public @NotNull List<Consumer<PacketWrapper>> createTitleCollection() {
      return new ArrayList<>();
    }

    @Override
    public void contributeTitle(final @NotNull List<Consumer<PacketWrapper>> coll, final @NotNull String title) {
      coll.add(packet -> {
        packet.write(Type.VAR_INT, ACTION_TITLE);
        packet.write(Type.STRING, title);
      });
    }

    @Override
    public void contributeSubtitle(final @NotNull List<Consumer<PacketWrapper>> coll, final @NotNull String subtitle) {
      coll.add(packet -> {
        packet.write(Type.VAR_INT, ACTION_SUBTITLE);
        packet.write(Type.STRING, subtitle);
      });
    }

    @Override
    public void contributeTimes(final @NotNull List<Consumer<PacketWrapper>> coll, final int inTicks, final int stayTicks, final int outTicks) {
      coll.add(packet -> {
        packet.write(Type.VAR_INT, ACTION_TIMES);
        packet.write(Type.INT, inTicks);
        packet.write(Type.INT, stayTicks);
        packet.write(Type.INT, outTicks);
      });
    }

    @Override
    public @Nullable Consumer<V> completeTitle(final @NotNull List<Consumer<PacketWrapper>> coll) {
      return v -> {
        for (int i = 0, length = coll.size(); i < length; i++) {
          final PacketWrapper pkt = this.createPacket(v);
          coll.get(i).accept(pkt);
          this.sendPacket(pkt);
        }
      };
    }

    @Override
    public void showTitle(final @NotNull V viewer, final @NotNull Consumer<V> title) {
      title.accept(viewer);
    }

    @Override
    public void clearTitle(final @NotNull V viewer) {
      final PacketWrapper packet = this.createPacket(viewer);
      packet.write(Type.VAR_INT, ACTION_CLEAR);
      this.sendPacket(packet);
    }

    @Override
    public void resetTitle(final @NotNull V viewer) {
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

    private BossBar(final @NotNull String fromProtocol, final @NotNull String toProtocol, final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction, final Collection<V> viewers) {
      super(fromProtocol, toProtocol, PROTOCOL_BOSS_BAR, "BOSSBAR", viewerClass, connectionFunction);
      this.viewers = new CopyOnWriteArraySet<>(viewers);
    }

    public static class Builder<V> extends ViaFacet<V> implements Facet.BossBar.Builder<V, Facet.BossBar<V>> {
      public Builder(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
        super(viewerClass, connectionFunction, PROTOCOL_HEX_COLOR);
      }

      @Override
      public Facet.@NotNull BossBar<V> createBossBar(final @NotNull Collection<V> viewer) {
        return new ViaFacet.BossBar<>("1_16", "1_15_2", this.viewerClass, this::findConnection, viewer);
      }
    }

    public static class Builder1_9_To_1_15<V> extends ViaFacet<V> implements Facet.BossBar.Builder<V, Facet.BossBar<V>> {
      public Builder1_9_To_1_15(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
        super(viewerClass, connectionFunction, PROTOCOL_BOSS_BAR);
      }

      @Override
      public Facet.@NotNull BossBar<V> createBossBar(final @NotNull Collection<V> viewer) {
        return new ViaFacet.BossBar<>("1_9", "1_8", this.viewerClass, this::findConnection, viewer);
      }
    }

    @Override
    public void bossBarInitialized(final net.kyori.adventure.bossbar.@NotNull BossBar bar) {
      Facet.BossBarPacket.super.bossBarInitialized(bar);
      this.id = UUID.randomUUID();
      this.broadcastPacket(ACTION_ADD);
    }

    @Override
    public void bossBarNameChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final @NotNull Component oldName, final @NotNull Component newName) {
      if (!this.viewers.isEmpty()) {
        this.title = this.createMessage(this.viewers.iterator().next(), newName);
        this.broadcastPacket(ACTION_TITLE);
      }
    }

    @Override
    public void bossBarProgressChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final float oldPercent, final float newPercent) {
      this.health = newPercent;
      this.broadcastPacket(ACTION_HEALTH);
    }

    @Override
    public void bossBarColorChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NotNull Color oldColor, final net.kyori.adventure.bossbar.BossBar.@NotNull Color newColor) {
      this.color = this.createColor(newColor);
      this.broadcastPacket(ACTION_STYLE);
    }

    @Override
    public void bossBarOverlayChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NotNull Overlay oldOverlay, final net.kyori.adventure.bossbar.BossBar.@NotNull Overlay newOverlay) {
      this.overlay = this.createOverlay(newOverlay);
      this.broadcastPacket(ACTION_STYLE);
    }

    @Override
    public void bossBarFlagsChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsAdded, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsRemoved) {
      this.flags = this.createFlag(this.flags, flagsAdded, flagsRemoved);
      this.broadcastPacket(ACTION_FLAG);
    }

    public void sendPacket(final @NotNull V viewer, final int action) {
      final PacketWrapper packet = this.createPacket(viewer);
      packet.write(Type.UUID, this.id);
      packet.write(Type.VAR_INT, action);
      if (action == ACTION_ADD || action == ACTION_TITLE) {
        packet.write(Type.STRING, this.title);
      }
      if (action == ACTION_ADD || action == ACTION_HEALTH) {
        packet.write(Type.FLOAT, this.health);
      }
      if (action == ACTION_ADD || action == ACTION_STYLE) {
        packet.write(Type.VAR_INT, this.color);
        packet.write(Type.VAR_INT, this.overlay);
      }
      if (action == ACTION_ADD || action == ACTION_FLAG) {
        packet.write(Type.BYTE, this.flags);
      }
      this.sendPacket(packet);
    }

    public void broadcastPacket(final int action) {
      if (this.isEmpty()) return;
      for (final V viewer : this.viewers) {
        this.sendPacket(viewer, action);
      }
    }

    @Override
    public void addViewer(final @NotNull V viewer) {
      if (this.viewers.add(viewer)) {
        this.sendPacket(viewer, ACTION_ADD);
      }
    }

    @Override
    public void removeViewer(final @NotNull V viewer) {
      if (this.viewers.remove(viewer)) {
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

  public static final class TabList<V> extends ProtocolBased<V> implements Facet.TabList<V, String> {

    public TabList(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> userConnection) {
      super("1_16", "1_15_2", PROTOCOL_HEX_COLOR, "TAB_LIST", viewerClass, userConnection);
    }

    @Override
    public void send(final V viewer, final @Nullable String header, final @Nullable String footer) {
      final PacketWrapper packet = this.createPacket(viewer);
      packet.write(Type.STRING, header);
      packet.write(Type.STRING, footer);
      this.sendPacket(packet);
    }
  }
}
