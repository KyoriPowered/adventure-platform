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
package net.kyori.text.adapter.bukkit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.kyori.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * An adapter for sending text {@link Component}s to Bukkit objects.
 */
public interface TextAdapter {
  /**
   * Sends {@code component} to the given {@code viewer}.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   */
  static void sendComponent(final @NonNull CommandSender viewer, final @NonNull Component component) {
    sendComponent(Collections.singleton(viewer), component);
  }

  /**
   * Sends {@code component} to the given {@code viewers}.
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   */
  static void sendComponent(final @NonNull Iterable<? extends CommandSender> viewers, final @NonNull Component component) {
    TextAdapter0.sendComponent(viewers, component, false);
  }

  /**
   * Sends {@code component} to the given {@code viewer}'s action bar.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   */
  static void sendActionBar(final @NonNull CommandSender viewer, final @NonNull Component component) {
    sendActionBar(Collections.singleton(viewer), component);
  }

  /**
   * Sends {@code component} to the given {@code viewers}'s action bar.
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   */
  static void sendActionBar(final @NonNull Iterable<? extends CommandSender> viewers, final @NonNull Component component) {
    TextAdapter0.sendComponent(viewers, component, true);
  }
}

final class TextAdapter0 {
  private static final List<Adapter> ADAPTERS = pickAdapters();

  private static List<Adapter> pickAdapters() {
    final ImmutableList.Builder<Adapter> adapters = ImmutableList.builder();
    if(isSpigotAdapterSupported()) {
      adapters.add(new SpigotAdapter());
    }
    adapters.add(new CraftBukkitAdapter());
    adapters.add(new LegacyAdapter());
    return adapters.build();
  }

  private static boolean isSpigotAdapterSupported() {
    try {
      Player.class.getMethod("spigot");
      return true;
    } catch(final NoSuchMethodException e) {
      return false;
    }
  }

  static void sendComponent(final Iterable<? extends CommandSender> viewers, final Component component, final boolean actionBar) {
    final List<CommandSender> list = new LinkedList<>();
    Iterables.addAll(list, viewers);
    for(final Iterator<Adapter> it = ADAPTERS.iterator(); it.hasNext() && !list.isEmpty(); ) {
      final Adapter adapter = it.next();
      adapter.sendComponent(list, component, actionBar);
    }
  }
}