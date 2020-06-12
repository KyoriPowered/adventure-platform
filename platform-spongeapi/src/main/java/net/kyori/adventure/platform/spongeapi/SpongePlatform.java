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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.AdventurePlatform;
import net.kyori.adventure.platform.impl.HandledAudience;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import net.kyori.adventure.platform.impl.Knobs;
import net.kyori.adventure.platform.viaversion.ViaVersionHandlers;
import net.kyori.adventure.util.NameMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.channel.MessageReceiver;
import us.myles.ViaVersion.api.platform.ViaPlatform;

import static java.util.Objects.requireNonNull;

public class SpongePlatform implements AdventurePlatform {
  
  static { // init
    Knobs.logger(new Slf4jLogHandler());
  }

  private static final SpongePlatform INSTANCE = new SpongePlatform();

  public static AdventurePlatform provider() {
    return INSTANCE;
  }
  
  public static Audience audience(MessageReceiver receiver) {
    final SpongePlatform p = INSTANCE; // todo: integrate
    if(receiver instanceof Player) {
      return new HandledAudience<>((Player) receiver, p.chat, p.actionBar, p.title, p.bossBar, p.sound);
    } else if(receiver instanceof Viewer) {
      return new HandledAudience<>((Viewer & MessageReceiver) receiver, p.chat, p.actionBar, p.title, null, p.sound);
    } else {
      return new HandledAudience<>(receiver, p.chat, p.actionBar, null, null, null);
    }
  }

  public static MultiAudience audience(MessageReceiver... receiver) {
    final List<MessageReceiver> receivers = Arrays.asList(receiver);
    return new SpongeMultiAudience(() -> receivers);
  }

  public static MultiAudience audience(Collection<MessageReceiver> receivers) {
    return new SpongeMultiAudience(() -> receivers);
  }

  /* package */ static <K, S extends CatalogType> S sponge(final @NonNull Class<S> spongeType, final @NonNull K value, final @NonNull NameMap<K> elements)  {
    return Sponge.getRegistry().getType(spongeType, elements.name(requireNonNull(value, "value")))
      .orElseThrow(() -> new IllegalArgumentException("Value " + value + " could not be found in Sponge type " + spongeType));
  }

  /* package */ static <K, S extends CatalogType> K adventure(final @NonNull S sponge, final @NonNull NameMap<K> values) {
    return values.value(requireNonNull(sponge, "sponge").getId())
      .orElseThrow(() -> new IllegalArgumentException("Sponge CatalogType value " + sponge + " could not be converted to its Adventure equivalent"));
  }

  /* package */ static <S extends CatalogType> S sponge(final @NonNull Class<S> spongeType, final @NonNull Key identifier) {
    return Sponge.getRegistry().getType(spongeType, requireNonNull(identifier, "Identifier must be non-null").asString())
      .orElseThrow(() -> new IllegalArgumentException("Value for Key " + identifier + " could not be found in Sponge type " + spongeType));
  }
  
  private final HandlerCollection<MessageReceiver, Handler.Chat<MessageReceiver, ?>> chat;
  private final HandlerCollection<MessageReceiver, Handler.ActionBar<MessageReceiver, ?>> actionBar;
  private final HandlerCollection<Viewer, Handler.Titles<Viewer>> title;
  private final HandlerCollection<Player, Handler.BossBars<Player>> bossBar;
  private final HandlerCollection<Viewer, Handler.PlaySound<Viewer>> sound;

  private SpongePlatform() { 
    final SpongeViaProvider via = new SpongeViaProvider();
    this.chat = new HandlerCollection<>(new ViaVersionHandlers.Chat<>(via), new SpongeHandlers.Chat());
    this.actionBar = new HandlerCollection<>(new ViaVersionHandlers.ActionBar<>(via), new SpongeHandlers.ActionBar());
    this.title = new HandlerCollection<>(new ViaVersionHandlers.Titles<>(via), new SpongeHandlers.Titles());
    this.bossBar = new HandlerCollection<>(new ViaVersionHandlers.BossBars<>(via), new SpongeBossBarListener());
    this.sound = new HandlerCollection<>(new SpongeHandlers.PlaySound()); // don't include via since we don't target versions below 1.9
  }

  @Override
  public @NonNull Audience all() {
    return Audience.empty(); // TODO
  }

  @Override
  public @NonNull Audience console() {
    return new SpongeAudience<>(Sponge.getGame().getServer().getConsole());
  }

  @Override
  public @NonNull Audience players() {
    return Audience.empty(); // TODO
  }

  @Override
  public @NonNull Audience player(final @NonNull UUID playerId) {
    return null;
  }

  @Override
  public @NonNull Audience permission(final @NonNull String permission) {
    /*return new SpongeMultiAudience(() -> Sponge.getGame().getServiceManager().provide(PermissionService.class)
      .orElseThrow(() -> new IllegalArgumentException("Sponge must have a permissions service"))
      .getUserSubjects().getLoadedWithPermission(spongePerm).keySet());*/
    return Audience.empty(); // TODO
  }

  @Override
  public @NonNull Audience world(final @NonNull UUID worldId) {
    return Audience.empty(); // TODO
  }

  @Override
  public @NonNull Audience server(@NonNull String serverName) {
    return all();
  }
  
  /* package */ static class SpongeViaProvider implements ViaVersionHandlers.ViaAPIProvider<Object> { // too many interfaces :(

    @Override
    public boolean isAvailable() {
      return Sponge.getPluginManager().isLoaded("viaversion");
    }

    @Override
    public ViaPlatform<?> platform() {
      if(!isAvailable()) {
        return null;
      }
      final PluginContainer container = Sponge.getPluginManager().getPlugin("viaversion").orElse(null);
      if(container == null) return null;
      return (ViaPlatform<?>) container.getInstance().orElse(null);
    }

    @Override
    public @Nullable UUID id(final Object viewer) {
      if(!(viewer instanceof Player)) return null;
      
      return ((Player) viewer).getUniqueId();
    }
  }
}
