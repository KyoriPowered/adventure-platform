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
package net.kyori.text.adapter.fabric;

import java.util.Collections;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An adapter for sending text {@link Component}s to Fabric objects.
 */
public interface TextAdapter {
  /**
   * Sends {@code component} to the given {@code viewer}.
   *
   * <p>Note that all Entities implement {@link CommandOutput}.</p>
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   */
  static void sendComponent(final @NonNull CommandOutput viewer, final @NonNull Component component) {
    sendComponent(Collections.singleton(viewer), component);
  }

  /**
   * Sends {@code component} to the given {@code viewers}.
   *
   * <p>Note that all Entities implement {@link CommandOutput}.</p>
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   */
  static void sendComponent(final @NonNull Iterable<? extends CommandOutput> viewers, final @NonNull Component component) {
    final Text text = Text.Serializer.fromJson(GsonComponentSerializer.INSTANCE.serialize(component));
    for(final CommandOutput viewer : viewers) {
      viewer.sendMessage(text);
    }
  }
  /**
   * Sends {@code component} to the given {@code viewer}.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   * @param broadcastToOperators whether this component should be broadcast to operators
   */
  static void sendComponent(final @NonNull ServerCommandSource viewer, final @NonNull Component component, final boolean broadcastToOperators) {
    sendComponent(Collections.singleton(viewer), component, broadcastToOperators);
  }

  /**
   * Sends {@code component} to the given {@code viewers}.
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   * @param broadcastToOperators whether this component should be broadcast to operators
   */
  static void sendComponent(final @NonNull Iterable<? extends ServerCommandSource> viewers, final @NonNull Component component, final boolean broadcastToOperators) {
    final Text text = Text.Serializer.fromJson(GsonComponentSerializer.INSTANCE.serialize(component));
    for(final ServerCommandSource viewer : viewers) {
      viewer.sendFeedback(text, broadcastToOperators);
    }
  }
}
