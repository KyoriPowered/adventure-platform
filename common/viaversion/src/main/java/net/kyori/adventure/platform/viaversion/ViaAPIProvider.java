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

import java.util.UUID;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.platform.ViaPlatform;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;

/**
 * A provider for the ViaVersion api objects
 *
 * @param <V> native player type
 */
public interface ViaAPIProvider<V> {

  /**
   * Whether ViaVersion is available and should be used (generally true if the running game version is less than what is supported).
   *
   * @return if available
   */
  boolean isAvailable();

  /**
   * Get the active {@link ViaPlatform} instance
   *
   * @return the platform
   */
  ViaPlatform<? extends V> platform();

  default @Nullable UserConnection connection(final @NonNull V viewer) {
    final UUID viewerId = id(viewer);
    return viewerId == null ? null : platform().getConnectionManager().getConnectedClient(viewerId);
  }

  /**
   * Check that a viewer is a player, and if so return their UUID.
   *
   * @param viewer the viewer
   * @return its id, or null if not a player.
   */
  @Nullable UUID id(final @NonNull V viewer);

  /**
   * Get a versioned GSON serializer appropriate for the viewer's protocol version.
   *
   * <p>The default implementation of this method assumes that ViaVersion is available,
   * but implementations may be more defensive and return an appropriate value even when
   * ViaVersion is not available.</p>
   *
   * @param viewer The receiving viewer
   * @return a serializer
   */
  default @NonNull GsonComponentSerializer serializer(final @NonNull V viewer) {
    int protocolVersion = ProtocolRegistry.SERVER_PROTOCOL;
    final UUID id = id(viewer);
    if(id != null) {
      protocolVersion = platform().getApi().getPlayerVersion(id);
    }

    if(protocolVersion >= ProtocolVersion.v1_16.getId()) {
      return GsonComponentSerializer.gson();
    } else {
      return GsonComponentSerializer.colorDownsamplingGson();
    }
  }
}
