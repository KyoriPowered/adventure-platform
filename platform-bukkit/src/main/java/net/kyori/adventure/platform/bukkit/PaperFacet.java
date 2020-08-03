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
package net.kyori.adventure.platform.bukkit;

import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetBase;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static net.kyori.adventure.platform.facet.Knob.isEnabled;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.hasClass;

/* package */ class PaperFacet<V extends CommandSender> extends FacetBase<V> {
  private static final boolean SUPPORTED = isEnabled("paper");

  protected PaperFacet(final @Nullable Class<? extends V> viewerClass) {
    super(viewerClass);
  }

  @Override
  public boolean isSupported() {
    return super.isSupported() && SUPPORTED;
  }

  /* package */ static class Title extends SpigotFacet.Message<Player> implements Facet.Title<Player, BaseComponent[], com.destroystokyo.paper.Title> {
    private final static boolean SUPPORTED = hasClass("com.destroystokyo.paper.Title");

    protected Title() {
      super(Player.class);
    }

    @Override
    public boolean isSupported() {
      return super.isSupported() && SUPPORTED;
    }

    @Override
    public com.destroystokyo.paper.@NonNull Title createTitle(final BaseComponent @Nullable[] title, final BaseComponent @Nullable[] subTitle, final int inTicks, final int stayTicks, final int outTicks) {
      final com.destroystokyo.paper.Title.Builder builder = com.destroystokyo.paper.Title.builder();

      if(title != null) builder.title(title);
      if(subTitle != null) builder.subtitle(subTitle);
      if(inTicks > -1) builder.fadeIn(inTicks);
      if(stayTicks > -1) builder.stay(stayTicks);
      if(outTicks > -1) builder.fadeOut(outTicks);

      return builder.build();
    }

    @Override
    public void showTitle(final @NonNull Player viewer, final com.destroystokyo.paper.@NonNull Title title) {
      viewer.sendTitle(title);
    }

    @Override
    public void clearTitle(final @NonNull Player viewer) {
      viewer.hideTitle();
    }

    @Override
    public void resetTitle(final @NonNull Player viewer) {
      viewer.resetTitle();
    }
  }
}
