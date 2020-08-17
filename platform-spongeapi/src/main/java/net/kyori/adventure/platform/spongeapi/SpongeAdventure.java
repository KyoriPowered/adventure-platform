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
package net.kyori.adventure.platform.spongeapi;

import net.kyori.adventure.platform.facet.Knob;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The entrypoint for Adventure in SpongeAPI.
 *
 * @see #of(PluginContainer, Game)
 * @see SpongeAudienceProvider
 */
public final class SpongeAdventure {
  private SpongeAdventure() {
  }

  static {
    final Logger logger = LoggerFactory.getLogger(SpongeAudienceProvider.class);
    Knob.OUT = logger::debug;
    Knob.ERR = logger::warn;
  }

  static final Map<PluginContainer, SpongeAudienceProvider> INSTANCES = Collections.synchronizedMap(new IdentityHashMap<>(4));

  /**
   * Creates an audience provider for a plugin.
   *
   * <p>There will only be one provider for each plugin.</p>
   *
   * @param plugin a plugin container
   * @param game a game
   * @return an audience provider
   */
  public static SpongeAudienceProvider of(final @NonNull PluginContainer plugin, final @NonNull Game game) {
    return INSTANCES.computeIfAbsent(plugin, container -> {
      final SpongeAudienceProviderImpl provider = new SpongeAudienceProviderImpl(game);
      provider.init(container);
      return provider;
    });
  }
}
