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

import java.util.List;
import net.kyori.text.Component;
import org.bukkit.command.CommandSender;

interface Adapter {
  /**
   * Attempts to send the {@code component} to each sender in the given list, removing
   * viewers from the list if the adapter was able to successfully send the component.
   *
   * @param viewers the viewers
   * @param component the component
   */
  void sendMessage(final List<? extends CommandSender> viewers, final Component component);

  /**
   * Attempts to send the {@code component} to each sender in the given list, removing
   * viewers from the list if the adapter was able to successfully send the component.
   *
   * @param viewers the viewers
   * @param component the component
   */
  void sendActionBar(final List<? extends CommandSender> viewers, final Component component);
}
