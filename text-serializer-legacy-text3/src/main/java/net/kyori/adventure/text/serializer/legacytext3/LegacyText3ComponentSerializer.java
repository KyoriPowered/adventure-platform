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
package net.kyori.adventure.text.serializer.legacytext3;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

/**
 * A component serializer betweeen text 3.x's {@link net.kyori.text.Component} and adventure's {@link Component}.
 */
public final class LegacyText3ComponentSerializer implements ComponentSerializer<Component, Component, net.kyori.text.Component> {
  private static final LegacyText3ComponentSerializer INSTANCE = new LegacyText3ComponentSerializer();

  /**
   * Gets a component serializer for adapting text 3.x components to adventure.
   *
   * @return a component serializer
   */
  public static @NonNull LegacyText3ComponentSerializer get() {
    return INSTANCE;
  }

  private LegacyText3ComponentSerializer() {
  }

  @Override
  public @NonNull Component deserialize(final net.kyori.text.@NonNull Component input) {
    return GsonComponentSerializer.gson().deserialize(net.kyori.text.serializer.gson.GsonComponentSerializer.INSTANCE.serialize(requireNonNull(input, "text")));
  }

  @Override
  public net.kyori.text.@NonNull Component serialize(final @NonNull Component component) {
    return net.kyori.text.serializer.gson.GsonComponentSerializer.INSTANCE.deserialize(GsonComponentSerializer.colorDownsamplingGson().serialize(requireNonNull(component, "component")));
  }
}
