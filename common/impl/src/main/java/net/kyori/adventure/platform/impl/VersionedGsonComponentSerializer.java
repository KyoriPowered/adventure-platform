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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class VersionedGsonComponentSerializer implements ComponentSerializer<Component, Component, String> {
  private final Gson serializer;
  private final Consumer<GsonBuilder> populator;

  private VersionedGsonComponentSerializer(final Consumer<GsonBuilder> populator) {
    this.populator = populator;
    final GsonBuilder builder = new GsonBuilder();
    populator.accept(builder);
    this.serializer = builder.create();
  }

  /**
   * A consumer that when applied to a {@link GsonBuilder} will populate it with the appropriate
   * type adapters to serialize Components at this serializer's compatibility level.
   *
   * @return populator
   */
  public Consumer<GsonBuilder> populator() {
    return this.populator;
  }

  public TypeAdapter<Component> typeAdapter() {
    return this.serializer.getAdapter(Component.class);
  }

  @Override
  public @NonNull Component deserialize(final @NonNull String input) {
    return this.serializer.fromJson(input, Component.class);
  }

  public @NonNull Component deserialize(final @NonNull JsonElement input) {
    return this.serializer.fromJson(input, Component.class);
  }

  @Override
  public @NonNull String serialize(final @NonNull Component component) {
    return this.serializer.toJson(component);
  }

  public @NonNull JsonElement serializeToTree(final @NonNull Component component) {
    return this.serializer.toJsonTree(component);
  }

  // Compatibility -- downsample text

  /**
   * A serializer that will attempt minimal backwards compatibility measures
   */
  public static final VersionedGsonComponentSerializer MODERN;

  /**
   * A serializer that will downsample RGB colors to remain compatible with older clients
   */
  public static final VersionedGsonComponentSerializer PRE_1_16;

  static {
    MODERN = new VersionedGsonComponentSerializer(GsonComponentSerializer.GSON_BUILDER_CONFIGURER);
    PRE_1_16 = new VersionedGsonComponentSerializer(builder -> {
      GsonComponentSerializer.GSON_BUILDER_CONFIGURER.accept(builder);
      builder.registerTypeAdapterFactory(new DownsamplingTypeAdapterFactory());
    });
  }

  /**
   * Overrides the {@link TypeAdapter} for a {@link Style} to replace its color with a version-appropriate one
   */
  static class DownsamplingTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
      if(!TextColor.class.isAssignableFrom(type.getRawType())) {
        return null;
      }
      final TypeAdapter<T> delegated = gson.getDelegateAdapter(this, type);

      class DownsamplingTypeAdapter extends TypeAdapter<T> {
        @Override
        @SuppressWarnings("unchecked")
        public void write(final JsonWriter out, final T value) throws IOException {
          TextColor color = (TextColor) value; // we already filtered ourselves to a TextColor at the beginning of create()
          if(color != null && !(color instanceof NamedTextColor)) {
            color = NamedTextColor.nearestTo(color);
          }
          delegated.write(out, (T) color);
        }

        @Override
        public T read(final JsonReader in) throws IOException {
          return delegated.read(in);
        }
      }
      return new DownsamplingTypeAdapter();
    }
  }
}
