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

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetBase;
import net.kyori.adventure.platform.facet.FacetComponentFlattener;
import net.kyori.adventure.platform.facet.FacetPointers;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.Index;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.boss.BossBarColor;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlay;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.effect.sound.SoundCategory;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.ChatTypeMessageReceiver;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Locatable;

import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.platform.facet.Knob.logUnsupported;
import static net.kyori.adventure.text.serializer.spongeapi.SpongeComponentSerializer.get;

class SpongeFacet<V> extends FacetBase<V> {
  protected SpongeFacet(final @Nullable Class<? extends V> viewerClass) {
    super(viewerClass);
  }

  public <K, S extends CatalogType> @Nullable S sponge(final @NotNull Class<S> spongeType, final @NotNull K value, final @NotNull Index<String, K> elements) {
    return Sponge.getRegistry()
      .getType(spongeType, elements.key(requireNonNull(value, "value")))
      .orElseGet(() -> {
        logUnsupported(this, value);
        return null;
      });
  }

  public <S extends CatalogType> @Nullable S sponge(final @NotNull Class<S> spongeType, final @NotNull Key identifier) {
    return Sponge.getRegistry()
      .getType(spongeType, requireNonNull(identifier, "Identifier must be non-null").asString())
      .orElseGet(() -> {
        logUnsupported(this, identifier);
        return null;
      });
  }

  static class Message<V> extends SpongeFacet<V> implements Facet.Message<V, Text> {
    protected Message(final @Nullable Class<? extends V> viewerClass) {
      super(viewerClass);
    }

    @Override
    public @NotNull Text createMessage(final @NotNull V viewer, final @NotNull Component message) {
      return get().serialize(message);
    }
  }

  static class Chat extends Message<MessageReceiver> implements Facet.Chat<MessageReceiver, Text> {
    protected Chat() {
      super(MessageReceiver.class);
    }

    @Override
    public void sendMessage(final @NotNull MessageReceiver viewer, final @NotNull Identity source, final @NotNull Text message, final @NotNull MessageType type) {
      viewer.sendMessage(message);
    }
  }

  static class ChatWithType extends Message<ChatTypeMessageReceiver> implements Facet.Chat<ChatTypeMessageReceiver, Text> {
    protected ChatWithType() {
      super(ChatTypeMessageReceiver.class);
    }

    private @Nullable ChatType type(final @NotNull MessageType type) {
      if (type == MessageType.CHAT) {
        return ChatTypes.CHAT;
      } else if (type == MessageType.SYSTEM) {
        return ChatTypes.SYSTEM;
      }
      logUnsupported(this, type);
      return null;
    }

    @Override
    public void sendMessage(final @NotNull ChatTypeMessageReceiver viewer, final @NotNull Identity source, final @NotNull Text message, final @NotNull MessageType type) {
      final ChatType chat = this.type(type);
      if (chat != null) {
        viewer.sendMessage(chat, message);
      }
    }
  }

  static class ActionBar extends Message<ChatTypeMessageReceiver> implements Facet.ActionBar<ChatTypeMessageReceiver, Text> {
    protected ActionBar() {
      super(ChatTypeMessageReceiver.class);
    }

    @Override
    public void sendMessage(final @NotNull ChatTypeMessageReceiver viewer, final @NotNull Text message) {
      viewer.sendMessage(ChatTypes.ACTION_BAR, message);
    }
  }

  static class Title extends Message<Viewer> implements Facet.Title<Viewer, Text, org.spongepowered.api.text.title.Title.Builder, org.spongepowered.api.text.title.Title> {
    protected Title() {
      super(Viewer.class);
    }

    @Override
    public org.spongepowered.api.text.title.Title.@NotNull Builder createTitleCollection() {
      return org.spongepowered.api.text.title.Title.builder();
    }

    @Override
    public void contributeTitle(final org.spongepowered.api.text.title.Title.@NotNull Builder coll, final @NotNull Text title) {
      coll.title(title);
    }

    @Override
    public void contributeSubtitle(final org.spongepowered.api.text.title.Title.@NotNull Builder coll, final @NotNull Text subtitle) {
      coll.subtitle(subtitle);
    }

    @Override
    public void contributeTimes(final org.spongepowered.api.text.title.Title.@NotNull Builder coll, final int inTicks, final int stayTicks, final int outTicks) {
      if (inTicks > -1) coll.fadeIn(inTicks);
      if (stayTicks > -1) coll.stay(stayTicks);
      if (outTicks > -1) coll.fadeOut(outTicks);
    }

    @Nullable
    @Override
    public org.spongepowered.api.text.title.Title completeTitle(final org.spongepowered.api.text.title.Title.@NotNull Builder coll) {
      return coll.build();
    }

    @Override
    public void showTitle(final @NotNull Viewer viewer, final org.spongepowered.api.text.title.@NotNull Title title) {
      viewer.sendTitle(title);
    }

    @Override
    public void clearTitle(final @NotNull Viewer viewer) {
      viewer.clearTitle();
    }

    @Override
    public void resetTitle(final @NotNull Viewer viewer) {
      viewer.resetTitle();
    }
  }

  static class Position extends SpongeFacet<Viewer> implements Facet.Position<Viewer, Vector3d> {
    protected Position() {
      super(Viewer.class);
    }

    @Override
    public boolean isApplicable(final @NotNull Viewer viewer) {
      return super.isApplicable(viewer) && viewer instanceof Locatable;
    }

    @Nullable
    @Override
    public Vector3d createPosition(final @NotNull Viewer viewer) {
      if (viewer instanceof Locatable) {
        return ((Locatable) viewer).getLocation().getPosition();
      }
      return null;
    }

    @NotNull
    @Override
    public Vector3d createPosition(final double x, final double y, final double z) {
      return new Vector3d(x, y, z);
    }
  }

  static class Sound extends Position implements Facet.Sound<Viewer, Vector3d> {
    @Override
    public void playSound(final @NotNull Viewer viewer, final net.kyori.adventure.sound.@NotNull Sound sound, final @NotNull Vector3d vector) {
      final SoundType type = this.type(sound.name());
      final SoundCategory category = this.category(sound.source());

      if (type != null && category != null) {
        viewer.playSound(type, category, vector, sound.volume(), sound.pitch());
      } else if (type != null) {
        viewer.playSound(type, vector, sound.volume(), sound.pitch());
      }
    }

    @Override
    public void stopSound(final @NotNull Viewer viewer, final @NotNull SoundStop stop) {
      final SoundType type = this.type(stop.sound());
      final SoundCategory category = this.category(stop.source());

      if (type != null && category != null) {
        viewer.stopSounds(type, category);
      } else if (type != null) {
        viewer.stopSounds(type);
      } else if (category != null) {
        viewer.stopSounds(category);
      } else {
        viewer.stopSounds();
      }
    }

    public @Nullable SoundType type(final @Nullable Key sound) {
      return sound == null ? null : this.sponge(SoundType.class, sound);
    }

    public @Nullable SoundCategory category(final net.kyori.adventure.sound.Sound.@Nullable Source source) {
      return source == null ? null : this.sponge(SoundCategory.class, source, net.kyori.adventure.sound.Sound.Source.NAMES);
    }
  }

  static class Book extends Message<Viewer> implements Facet.Book<Viewer, Text, BookView> {
    protected Book() {
      super(Viewer.class);
    }

    @NotNull
    @Override
    public BookView createBook(final @NotNull String title, final @NotNull String author, final @NotNull Iterable<Text> pages) {
      return BookView.builder().title(Text.of(title)).author(Text.of(author)).addPages(Lists.newArrayList(pages)).build();
    }

    @Override
    public void openBook(final @NotNull Viewer viewer, final @NotNull BookView book) {
      viewer.sendBookView(book);
    }
  }

  static class BossBarBuilder extends SpongeFacet<Player> implements Facet.BossBar.Builder<Player, SpongeFacet.BossBar> {
    protected BossBarBuilder() {
      super(Player.class);
    }

    @Override
    public SpongeFacet.@NotNull BossBar createBossBar(final @NotNull Collection<Player> viewers) {
      return new SpongeFacet.BossBar(viewers);
    }
  }

  static class BossBar extends Message<Player> implements Facet.BossBar<Player> {
    private final ServerBossBar bar;

    protected BossBar(final @NotNull Collection<Player> viewers) {
      super(Player.class);
      this.bar = ServerBossBar.builder().name(Text.of()).color(BossBarColors.PINK).overlay(BossBarOverlays.PROGRESS).visible(false).build();
      this.bar.addPlayers(viewers);
    }

    @Override
    public void bossBarInitialized(final net.kyori.adventure.bossbar.@NotNull BossBar bar) {
      Facet.BossBar.super.bossBarInitialized(bar);
      this.bar.setVisible(true);
    }

    @Override
    public void bossBarNameChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final @NotNull Component oldName, final @NotNull Component newName) {
      if (!this.bar.getPlayers().isEmpty()) {
        this.bar.setName(this.createMessage(this.bar.getPlayers().iterator().next(), newName));
      }
    }

    @Override
    public void bossBarProgressChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final float oldPercent, final float newPercent) {
      this.bar.setPercent(newPercent);
    }

    @Override
    public void bossBarColorChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NotNull Color oldColor, final net.kyori.adventure.bossbar.BossBar.@NotNull Color newColor) {
      final BossBarColor color = this.sponge(BossBarColor.class, newColor, net.kyori.adventure.bossbar.BossBar.Color.NAMES);
      if (color != null) {
        this.bar.setColor(color);
      }
    }

    @Override
    public void bossBarOverlayChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NotNull Overlay oldOverlay, final net.kyori.adventure.bossbar.BossBar.@NotNull Overlay newOverlay) {
      final BossBarOverlay overlay = this.sponge(BossBarOverlay.class, newOverlay, net.kyori.adventure.bossbar.BossBar.Overlay.NAMES);
      if (overlay != null) {
        this.bar.setOverlay(overlay);
      }
    }

    @Override
    public void bossBarFlagsChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> removedFlags, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> addedFlags) {
      final Boolean fog = this.hasFlag(net.kyori.adventure.bossbar.BossBar.Flag.CREATE_WORLD_FOG, removedFlags, addedFlags);
      if (fog != null) this.bar.setCreateFog(fog);
      final Boolean darkenScreen = this.hasFlag(net.kyori.adventure.bossbar.BossBar.Flag.DARKEN_SCREEN, removedFlags, addedFlags);
      if (darkenScreen != null) this.bar.setDarkenSky(darkenScreen);
      final Boolean bossMusic = this.hasFlag(net.kyori.adventure.bossbar.BossBar.Flag.PLAY_BOSS_MUSIC, removedFlags, addedFlags);
      if (bossMusic != null) this.bar.setPlayEndBossMusic(bossMusic);
    }

    private @Nullable Boolean hasFlag(final net.kyori.adventure.bossbar.BossBar.@NotNull Flag flag, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> removedFlags, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> addedFlags) {
      if (addedFlags.contains(flag)) return true;
      if (removedFlags.contains(flag)) return false;
      return null;
    }

    @Override
    public void addViewer(final @NotNull Player viewer) {
      this.bar.addPlayer(viewer);
    }

    @Override
    public void removeViewer(final @NotNull Player viewer) {
      this.bar.removePlayer(viewer);
    }

    @Override
    public boolean isEmpty() {
      return !this.bar.isVisible() || this.bar.getPlayers().isEmpty();
    }

    @Override
    public void close() {
      this.bar.removePlayers(this.bar.getPlayers());
    }
  }

  /*static class ViaHook implements Function<Player, UserConnection> {
    @Override
    public UserConnection apply(final @NotNull Player player) {
      return Via.getManager().getConnection(player.getUniqueId());
    }
  } */

  static class TabList extends Message<Player> implements Facet.TabList<Player, Text> {

    TabList() {
      super(Player.class);
    }

    @Override
    public void send(final Player viewer, final @Nullable Text header, final @Nullable Text footer) {
      if (header != null && footer != null) {
        viewer.getTabList().setHeaderAndFooter(header, footer);
      } else if (header != null) {
        viewer.getTabList().setHeader(header);
      } else if (footer != null) {
        viewer.getTabList().setFooter(footer);
      }
    }
  }

  static final class SubjectPointers extends SpongeFacet<Subject> implements Facet.Pointers<Subject> {
    SubjectPointers() {
      super(Subject.class);
    }

    @Override
    public void contributePointers(final Subject viewer, final net.kyori.adventure.pointer.Pointers.Builder builder) {
      builder.withStatic(PermissionChecker.POINTER, perm -> {
        final Tristate sponge = viewer.getPermissionValue(viewer.getActiveContexts(), perm);
        if (sponge == Tristate.UNDEFINED) {
          return TriState.NOT_SET;
        } else {
          return TriState.byBoolean(sponge.asBoolean());
        }
      });
    }
  }

  static final class ConsoleSourcePointers extends SpongeFacet<ConsoleSource> implements Facet.Pointers<ConsoleSource> {
    ConsoleSourcePointers() {
      super(ConsoleSource.class);
    }

    @Override
    public void contributePointers(final ConsoleSource viewer, final net.kyori.adventure.pointer.Pointers.Builder builder) {
      builder.withStatic(FacetPointers.TYPE, FacetPointers.Type.CONSOLE);
    }
  }

  static final class CommandSourcePointers extends SpongeFacet<CommandSource> implements Facet.Pointers<CommandSource> {
    CommandSourcePointers() {
      super(CommandSource.class);
    }

    @Override
    public void contributePointers(final CommandSource viewer, final net.kyori.adventure.pointer.Pointers.Builder builder) {
      builder.withStatic(Identity.NAME, viewer.getName());
      builder.withDynamic(Identity.LOCALE, viewer::getLocale);
    }
  }

  static final class LocatablePointers extends SpongeFacet<Locatable> implements Facet.Pointers<Locatable> {
    LocatablePointers() {
      super(Locatable.class);
    }

    @Override
    public void contributePointers(final Locatable viewer, final net.kyori.adventure.pointer.Pointers.Builder builder) {
      builder.withDynamic(FacetPointers.WORLD, () -> Key.key(viewer.getWorld().getName()));
    }
  }

  static final class PlayerPointers extends SpongeFacet<Player> implements Facet.Pointers<Player> {
    PlayerPointers() {
      super(Player.class);
    }

    @Override
    public void contributePointers(final Player viewer, final net.kyori.adventure.pointer.Pointers.Builder builder) {
      builder.withStatic(FacetPointers.TYPE, FacetPointers.Type.PLAYER);
    }
  }

  static final class IdentifiablePointers extends SpongeFacet<Identifiable> implements Facet.Pointers<Identifiable> {
    IdentifiablePointers() {
      super(Identifiable.class);
    }

    @Override
    public void contributePointers(final Identifiable viewer, final net.kyori.adventure.pointer.Pointers.Builder builder) {
      builder.withDynamic(Identity.UUID, viewer::getUniqueId);
    }
  }

  static final class Translator extends SpongeFacet<Game> implements FacetComponentFlattener.Translator<Game> {

    Translator() {
      super(Game.class);
    }

    @Override
    public @NotNull String valueOrDefault(final @NotNull Game game, final @NotNull String key) {
      final Optional<Translation> tr = game.getRegistry().getTranslationById(key);
      return tr.isPresent() ? tr.get().get() : key;
    }
  }
}
