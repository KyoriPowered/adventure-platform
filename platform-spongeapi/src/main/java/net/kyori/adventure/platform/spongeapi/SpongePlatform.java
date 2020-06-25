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
package net.kyori.adventure.platform.spongeapi;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.AdventurePlatformImpl;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import net.kyori.adventure.platform.impl.Knobs;
import net.kyori.adventure.platform.viaversion.ViaVersionHandlers;
import net.kyori.adventure.util.Index;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.text.channel.MessageReceiver;

import static java.util.Objects.requireNonNull;

@Singleton // one instance per plugin module
public final class SpongePlatform extends AdventurePlatformImpl {
  
  static { // init
    Knobs.logger(new Slf4jLogHandler());
  }

  /* package */ static <K, S extends CatalogType> S sponge(final @NonNull Class<S> spongeType, final @NonNull K value, final @NonNull Index<String, K> elements)  {
    return Sponge.getRegistry().getType(spongeType, elements.key(requireNonNull(value, "value")))
      .orElseThrow(() -> new IllegalArgumentException("Value " + value + " could not be found in Sponge type " + spongeType));
  }

  /* package */ static <K, S extends CatalogType> K adventure(final @NonNull S sponge, final @NonNull Index<String, K> values) {
    K value = values.value(requireNonNull(sponge, "sponge").getId());
    if(value == null) {
      throw new IllegalArgumentException("Sponge CatalogType value " + sponge + " could not be converted to its Adventure equivalent");
    }
    return value;
  }

  /* package */ static <S extends CatalogType> S sponge(final @NonNull Class<S> spongeType, final @NonNull Key identifier) {
    return Sponge.getRegistry().getType(spongeType, requireNonNull(identifier, "Identifier must be non-null").asString())
      .orElseThrow(() -> new IllegalArgumentException("Value for Key " + identifier + " could not be found in Sponge type " + spongeType));
  }

  /**
   * Create a new platform for a plugin. A guice-created instance should be preferred.
   *
   * @param container plugin requesting this platform instance
   * @param game the game
   * @return platform
   */
  public static SpongePlatform of(final @NonNull PluginContainer container, final Game game) {
    final SpongePlatform platform = new SpongePlatform(game.getEventManager(), game.getPluginManager(), game);
    platform.init(container);
    return platform;
  }

  private final EventManager eventManager;
  private final Events events;
  private final PluginManager plugins;

  private HandlerCollection<MessageReceiver, Handler.Chat<MessageReceiver, ?>> chat;
  private HandlerCollection<MessageReceiver, Handler.ActionBar<MessageReceiver, ?>> actionBar;
  private HandlerCollection<Viewer, Handler.Titles<Viewer>> title;
  private HandlerCollection<Player, Handler.BossBars<Player>> bossBar;
  private HandlerCollection<Viewer, Handler.PlaySound<Viewer>> sound;
  private HandlerCollection<Viewer, Handler.Books<Viewer>> books;

  @Inject
  /* package */ SpongePlatform(final @NonNull EventManager eventManager, final @NonNull PluginManager plugins, final @NonNull Game game) {
    this.eventManager = eventManager;
    this.plugins = plugins;
    this.events = new Events(game);
    if(Sponge.getGame().getState().compareTo(GameState.POST_INITIALIZATION) > 0) { // if we've already post-initialized
      setupHandlers();
    }
  }

  @Inject
  void init(final PluginContainer container) {
    this.eventManager.registerListeners(container, this.events);
  }

  /* package */ void setupHandlers() {
    final SpongeViaProvider via = new SpongeViaProvider(this.plugins);

    this.chat = HandlerCollection.of(
      new ViaVersionHandlers.Chat<>(via),
      new SpongeHandlers.Chat());
    this.actionBar = HandlerCollection.of(
      new ViaVersionHandlers.ActionBar<>(via),
      new SpongeHandlers.ActionBar());
    this.title = HandlerCollection.of(
      new ViaVersionHandlers.Titles<>(via),
      new SpongeHandlers.Titles());
    this.bossBar = HandlerCollection.of(
      new ViaVersionHandlers.BossBars_1_16<>(via),
      new ViaVersionHandlers.BossBars_1_9_1_15<>(via),
      new SpongeBossBarListener());
    this.sound = HandlerCollection.of(new SpongeHandlers.PlaySound()); // don't include via since we don't target versions below 1.9
    this.books = HandlerCollection.of(new SpongeHandlers.Books());
  }

  /**
   * Internal event wrapper class, do not use.
   */
  public class Events {

    private final Game game;

    Events(final @NonNull Game game) {
      this.game = game;
    }

    @Listener
    public void setupHandlers(final @NonNull GamePostInitializationEvent event) { // set up handlers so that we don't start too early for ViaVersion
      SpongePlatform.this.setupHandlers();
    }

    @Listener(order = Order.FIRST)
    public void join(final ClientConnectionEvent.@NonNull Join event) {
      SpongePlatform.this.add(new SpongePlayerAudience(event.getTargetEntity(), chat, actionBar, title, bossBar, sound, books));
    }

    @Listener(order = Order.LAST)
    public void quit(final ClientConnectionEvent.@NonNull Disconnect event) {
      SpongePlatform.this.remove(event.getTargetEntity().getUniqueId());
      if(bossBar != null) {
        for(Handler.BossBars<Player> handler : bossBar) {
          handler.hideAll(event.getTargetEntity());
        }
      }
    }

    @Listener
    public void serverStart(final @NonNull GameStartingServerEvent event) {
      SpongePlatform.this.add(new SpongeSenderAudience<>(this.game.getServer().getConsole(), chat, actionBar, null, null, null, null));
    }

    @Listener
    public void serverStop(final @NonNull GameStoppedServerEvent event) {
      // todo: remove server
    }
  }

  public @NonNull Audience audience(final @NonNull MessageReceiver source) {
    if(source instanceof Player) {
      return player(((Player) source).getUniqueId());
    } else if(source instanceof ConsoleSource) {
      return console();
    } else if(source instanceof Viewer) {
      return new SpongeSenderAudience<>((Viewer & MessageReceiver) source, this.chat, this.actionBar, this.title, null, this.sound, this.books);
    } else {
      return new SpongeSenderAudience<>(source, this.chat, this.actionBar, null, null, null, null);
    }
  }

  @Override
  public void close() {
    this.eventManager.unregisterListeners(this.events);
    for(Handler.BossBars<Player> handler : this.bossBar) {
      handler.hideAll();
    }
    super.close();
  }
}
