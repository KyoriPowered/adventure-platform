/*
 * This file is part of text-adapters, licensed under the MIT License.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * An adapter for sending text {@link Component}s to Bukkit objects.
 */
public interface TextAdapter {
  /**
   * Sends {@code component} to the given {@code sender}.
   *
   * @param sender the sender to send the component to
   * @param component the component
   */
  static void sendComponent(final @NonNull CommandSender sender, final @NonNull Component component) {
    sendComponent(Collections.singleton(sender), component);
  }

  /**
   * Sends {@code component} to the given {@code senders}.
   *
   * @param senders the senders to send the component to
   * @param component the component
   */
  static void sendComponent(final @NonNull Iterable<? extends CommandSender> senders, final @NonNull Component component) {
    TextAdapter0.sendComponent(senders, component);
  }
}

final class TextAdapter0 {
  private static final List<Adapter> ADAPTERS;
  static {
    final ImmutableList.Builder<Adapter> adapters = ImmutableList.builder();
    if(isSpigotAdapterSupported()) {
      adapters.add(new SpigotAdapter());
    }
    adapters.add(new CraftBukkitAdapter());
    adapters.add(new LegacyAdapter());
    ADAPTERS = adapters.build();
  }

  private static boolean isSpigotAdapterSupported() {
    try {
      Player.class.getMethod("spigot");
      return true;
    } catch(final NoSuchMethodException e) {
      return false;
    }
  }

  static void sendComponent(final Iterable<? extends CommandSender> senders, final Component component) {
    final List<CommandSender> list = new ArrayList<>();
    Iterables.addAll(list, senders);
    for(final Iterator<Adapter> iterator = ADAPTERS.iterator(); iterator.hasNext() && !list.isEmpty(); ) {
      final Adapter adapter = iterator.next();
      adapter.sendComponent(list, component);
    }
  }
}