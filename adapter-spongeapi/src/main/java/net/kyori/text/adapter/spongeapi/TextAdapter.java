/*
 * This file is part of text-extras, licensed under the MIT License.
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
package net.kyori.text.adapter.spongeapi;

import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.ChatTypeMessageReceiver;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.serializer.TextSerializers;

/**
 * An adapter for sending and converting text {@link Component}s to Sponge objects.
 */
public interface TextAdapter {
  /**
   * Sends {@code component} to the given {@code viewer}.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   */
  static void sendMessage(final @NonNull MessageReceiver viewer, final @NonNull Component component) {
    viewer.sendMessage(toSponge(component));
  }

  /**
   * Sends {@code component} to the given {@code viewers}.
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   */
  static void sendMessage(final @NonNull Iterable<? extends MessageReceiver> viewers, final @NonNull Component component) {
    final Text text = toSponge(component);
    for(final MessageReceiver viewer : viewers) {
      viewer.sendMessage(text);
    }
  }
  /**
   * Sends {@code component} to the given {@code viewer}.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   * @param type the type
   */
  static void sendMessage(final @NonNull ChatTypeMessageReceiver viewer, final @NonNull Component component, final @NonNull ChatType type) {
    viewer.sendMessage(type, toSponge(component));
  }

  /**
   * Sends {@code component} to the given {@code viewers}.
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   * @param type the type
   */
  static void sendMessage(final @NonNull Iterable<? extends ChatTypeMessageReceiver> viewers, final @NonNull Component component, final @NonNull ChatType type) {
    final Text text = toSponge(component);
    for(final ChatTypeMessageReceiver viewer : viewers) {
      viewer.sendMessage(type, text);
    }
  }

  /**
   * Sends {@code component} to the given {@code viewer}.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   * @deprecated use {@link #sendMessage(MessageReceiver, Component)}
   */
  @Deprecated
  static void sendComponent(final @NonNull MessageReceiver viewer, final @NonNull Component component) {
    sendMessage(viewer, component);
  }

  /**
   * Sends {@code component} to the given {@code viewers}.
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   * @deprecated use {@link #sendMessage(Iterable, Component)}
   */
  @Deprecated
  static void sendComponent(final @NonNull Iterable<? extends MessageReceiver> viewers, final @NonNull Component component) {
    sendMessage(viewers, component);
  }
  /**
   * Sends {@code component} to the given {@code viewer}.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   * @param type the type
   * @deprecated use {@link #sendMessage(ChatTypeMessageReceiver, Component, ChatType)}
   */
  @Deprecated
  static void sendComponent(final @NonNull ChatTypeMessageReceiver viewer, final @NonNull Component component, final @NonNull ChatType type) {
    sendMessage(viewer, component, type);
  }

  /**
   * Sends {@code component} to the given {@code viewers}.
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   * @param type the type
   * @deprecated use {@link #sendMessage(Iterable, Component, ChatType)}
   */
  @Deprecated
  static void sendComponent(final @NonNull Iterable<? extends ChatTypeMessageReceiver> viewers, final @NonNull Component component, final @NonNull ChatType type) {
    sendMessage(viewers, component, type);
  }

  /**
   * Converts {@code component} to the {@link Text} format used by Sponge.
   *
   * <p>The adapter makes no guarantees about the underlying structure/type of the components.
   * i.e. is it not guaranteed that a {@link net.kyori.text.TextComponent} will map to a
   * {@link org.spongepowered.api.text.LiteralText}.</p>
   *
   * <p>The {@code sendComponent} methods should be used instead of this method when possible.</p>
   *
   * @param component the component
   * @return the Text representation of the component
   */
  static @NonNull Text toSponge(final @NonNull Component component) {
    return TextSerializers.JSON.deserialize(GsonComponentSerializer.INSTANCE.serialize(component));
  }
}
