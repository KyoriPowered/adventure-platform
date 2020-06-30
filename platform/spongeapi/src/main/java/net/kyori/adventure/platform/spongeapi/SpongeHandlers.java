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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.spongeapi.SpongeApiComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.effect.sound.SoundCategory;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.ChatTypeMessageReceiver;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.world.Locatable;

/* package */ final class SpongeHandlers {
  
  private SpongeHandlers() {}
  
  /* package */ static class Chat implements Handler.Chat<MessageReceiver, Text> {

    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    public Text initState(final @NonNull Component component) {
      return SpongeApiComponentSerializer.get().serialize(component);
    }

    @Override
    public void sendMessage(final @NonNull MessageReceiver target, final @NonNull Text message) {
      target.sendMessage(message);
    }
  }
  
  /* package */ static class ActionBar implements Handler.ActionBar<MessageReceiver, Text> {

    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    public Text initState(final @NonNull Component message) {
      return SpongeApiComponentSerializer.get().serialize(message);
    }

    @Override
    public void sendActionBar(final @NonNull MessageReceiver viewer, final @NonNull Text message) {
      if(viewer instanceof ChatTypeMessageReceiver) {
        ((ChatTypeMessageReceiver) viewer).sendMessage(ChatTypes.ACTION_BAR, message);
      } else {
        viewer.sendMessage(message);
      }
    }
  }
  
  /* package */ static class Titles implements Handler.Titles<Viewer> {

    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    public void showTitle(@NonNull Viewer viewer, @NonNull Component title, @NonNull Component subtitle, int inTicks, int stayTicks, int outTicks) {
      viewer.sendTitle(org.spongepowered.api.text.title.Title.builder()
              .title(SpongeApiComponentSerializer.get().serialize(title))
              .subtitle(SpongeApiComponentSerializer.get().serialize(title))
              .fadeIn(inTicks)
              .stay(stayTicks)
              .fadeOut(outTicks)
              .build());
    }

    @Override
    public void clearTitle(final @NonNull Viewer viewer) {
      viewer.clearTitle();
    }

    @Override
    public void resetTitle(final @NonNull Viewer viewer) {
      viewer.resetTitle();
    }
  }

  /* package */ static class PlaySound implements Handler.PlaySound<Viewer> {

    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    public void playSound(final @NonNull Viewer viewer, final @NonNull Sound sound) {
      Vector3d loc = Vector3d.ZERO;
      if(viewer instanceof Locatable) {
        loc = ((Locatable) viewer).getLocation().getPosition();
      }
      playSound(viewer, sound, loc);
    }

    @Override
    public void playSound(final @NonNull Viewer viewer, final @NonNull Sound sound, final double x, final double y, final double z) {
      playSound(viewer, sound, new Vector3d(x, y, z));
    }

    private void playSound(final @NonNull Viewer viewer, final @NonNull Sound sound, final @NonNull Vector3d position) {
      final SoundType type = sponge(sound.name());
      final SoundCategory category = sponge(sound.source());
      viewer.playSound(type, category, position, sound.volume(), sound.pitch());
    }

    @Override
    public void stopSound(final @NonNull Viewer viewer, final @NonNull SoundStop stop) {
      final SoundType type = sponge(stop.sound());
      final SoundCategory category = sponge(stop.source());

      if(type != null && category != null) {
        viewer.stopSounds(type, category);
      } else if(type != null) {
        viewer.stopSounds(type);
      } else if(category != null) {
        viewer.stopSounds(category);
      } else {
        viewer.stopSounds();
      }
    }

    private static SoundType sponge(final @Nullable Key sound) {
      return sound == null ? null : SpongePlatform.sponge(SoundType.class, sound);
    }

    private static SoundCategory sponge(final Sound.@Nullable Source source) {
      return source == null ? null : SpongePlatform.sponge(SoundCategory.class, source, Sound.Source.NAMES);
    }
  }

  /* package */ static class Books implements Handler.Books<Viewer> {

    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    public void openBook(@NonNull Viewer viewer, @NonNull Component title, @NonNull Component author, @NonNull Iterable<Component> pages) {
      final BookView.Builder view = BookView.builder()
              .title(SpongeApiComponentSerializer.get().serialize(title))
              .author(SpongeApiComponentSerializer.get().serialize(author));
      for(final Component page : pages) {
        view.addPage(SpongeApiComponentSerializer.get().serialize(page));
      }
      viewer.sendBookView(view.build());
    }
  }
}
