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
package net.kyori.adventure.platform.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Collection, that exposes the first available handler.
 *
 * @param <H> handler type
 */
public class HandlerCollection<V, H extends Handler<V>> implements Iterable<H> {
  private final @NonNull List<H> handlers;

  @SafeVarargs
  public static <V, H extends Handler<V>> HandlerCollection<V, H> of(final @Nullable H@NonNull... handlers) {
    final List<H> handlerList = Stream.of(handlers)
      .filter(Objects::nonNull)
      .filter(Handler::isAvailable)
      .collect(Collectors.toList());
    if(handlerList.isEmpty()) {
      return new HandlerCollection<>(Collections.emptyList());
    } else if(handlerList.size() == 1) {
      return new HandlerCollection<>(Collections.singletonList(handlerList.get(0)));
    } else {
      return new HandlerCollection<>(Collections.unmodifiableList(handlerList));
    }
  }

  private HandlerCollection(final @NonNull List<H> handlers) {
    this.handlers = handlers;
  }

  public @Nullable H get(final V viewer) {
    for(final H handler : this.handlers) {
      if(handler.isAvailable(viewer)) {
        Knobs.logChosenHandler(viewer, handler);
        return handler;
      }
    }
    Knobs.logChosenHandler(viewer, null);
    return null;
  }

  @Override
  public @NonNull Iterator<H> iterator() {
    return this.handlers.iterator();
  }

  @Override
  public void forEach(final Consumer<? super H> action) {
    this.handlers.forEach(action);
  }

  @Override
  public Spliterator<H> spliterator() {
    return this.handlers.spliterator();
  }
}
