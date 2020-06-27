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
package net.kyori.adventure.platform;

import net.kyori.adventure.platform.audience.AdventurePlayerAudience;
import net.kyori.adventure.platform.audience.AdventureAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Comparator;

/**
 * A component renderer that customizes text for an {@link AdventureAudience}.
 */
public interface AdventureRenderer extends ComponentRenderer<AdventureAudience>, Comparator<AdventureAudience> {

  /**
   * Customize text for an audience.
   *
   * <p>Audience may also be an instance of {@link AdventurePlayerAudience}.</p>
   *
   * @param component a component
   * @param audience an audience
   * @return a customized component
   */
  @Override
  @NonNull Component render(@NonNull Component component, @NonNull AdventureAudience audience);

  /**
   * Determine whether any two audiences are considered "equivalent".
   *
   * <p>Can be used to cache text for a group of audiences, such as by locale.</p>
   *
   * @param a1 an audience
   * @param a2 another audience
   * @return {@code 0} if both audiences are "equivalent", {@code 1} or {@code -1} otherwise
   */
  @Override
  int compare(AdventureAudience a1, AdventureAudience a2);
}
