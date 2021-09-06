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

import java.util.Collection;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetAudience;
import net.kyori.adventure.platform.facet.FacetAudienceProvider;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.channel.ChatTypeMessageReceiver;
import org.spongepowered.api.text.channel.MessageReceiver;

final class SpongeAudience extends FacetAudience<MessageReceiver> {
  // private static final Function<Player, UserConnection> VIA = new SpongeFacet.ViaHook();
  private static final Collection<Facet.Chat<?, ?>> CHAT = Facet.of(
    // () -> new ViaFacet.Chat<>(Player.class, VIA),
    SpongeFacet.ChatWithType::new,
    SpongeFacet.Chat::new);
  private static final Collection<Facet.ActionBar<? extends ChatTypeMessageReceiver, ?>> ACTION_BAR = Facet.of(
    // () -> new ViaFacet.ActionBarTitle<>(Player.class, VIA),
    // () -> new ViaFacet.ActionBar<>(Player.class, VIA),
    SpongeFacet.ActionBar::new);
  private static final Collection<Facet.Title<Viewer, ?, ?, ?>> TITLE = Facet.of(
    SpongeFacet.Title::new);
  private static final Collection<Facet.Sound<Viewer, ?>> SOUND = Facet.of(
    SpongeFacet.Sound::new);
  private static final Collection<Facet.Book<Viewer, ?, ?>> BOOK = Facet.of(
    SpongeFacet.Book::new);
  private static final Collection<Facet.BossBar.Builder<Player, ? extends Facet.BossBar<Player>>> BOSS_BAR = Facet.of(
    // () -> new ViaFacet.BossBar.Builder<>(Player.class, VIA),
    SpongeFacet.BossBarBuilder::new);
  private static final Collection<Facet.TabList<Player, ?>> TAB_LIST = Facet.of(
    // () -> new ViaFacet.TabList<>(Player.class, VIA),
    SpongeFacet.TabList::new
  );
  private static final Collection<Facet.Pointers<?>> POINTERS = Facet.of(
    SpongeFacet.CommandSourcePointers::new,
    SpongeFacet.SubjectPointers::new,
    SpongeFacet.IdentifiablePointers::new,
    SpongeFacet.ConsoleSourcePointers::new,
    SpongeFacet.PlayerPointers::new,
    SpongeFacet.LocatablePointers::new
  );

  SpongeAudience(final FacetAudienceProvider<?, ?> provider, final @NotNull Collection<MessageReceiver> viewers) {
    super(provider, viewers, CHAT, ACTION_BAR, TITLE, SOUND, null, BOOK, BOSS_BAR, TAB_LIST, POINTERS);
  }
}
