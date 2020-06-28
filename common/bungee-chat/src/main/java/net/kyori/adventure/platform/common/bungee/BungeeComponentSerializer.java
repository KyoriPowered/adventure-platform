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
package net.kyori.adventure.platform.common.bungee;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.function.Function;
import net.kyori.adventure.platform.PlatformComponentSerializer;
import net.kyori.adventure.platform.impl.gson.GsonInjections;
import net.kyori.adventure.platform.impl.gson.SelfSerializable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

public class BungeeComponentSerializer implements PlatformComponentSerializer<BaseComponent[]> {
  public static final boolean SUPPORTED = bind();
  public static final BungeeComponentSerializer MODERN = new BungeeComponentSerializer(GsonComponentSerializer.gson(), AdapterComponent::new);
  public static final BungeeComponentSerializer PRE_1_16 = new BungeeComponentSerializer(GsonComponentSerializer.colorDownsamplingGson(), DownsamplingAdapterComponent::new);

  private final GsonComponentSerializer serializer;
  private final Function<Component, AdapterComponent> maker;

  private BungeeComponentSerializer(final GsonComponentSerializer serializer, final Function<Component, AdapterComponent> maker) {
    this.serializer = serializer;
    this.maker = maker;
  }

  private static boolean bind() {
    try {
      final Field gsonField = GsonInjections.field(net.md_5.bungee.chat.ComponentSerializer.class, "gson");
      return GsonInjections.injectGson((Gson) gsonField.get(null), builder -> {
        GsonComponentSerializer.gson().populator().apply(builder); // TODO: this might be unused?
        builder.registerTypeAdapterFactory(new SelfSerializable.AdapterFactory());
      });
    } catch(Exception ex) {
      return false;
    }
  }

  @Override
  public boolean supported() {
    return SUPPORTED;
  }

  @Override
  public @NonNull Component deserialize(final @NonNull BaseComponent@NonNull[] input) {
    requireNonNull(input, "input");
    if(input.length == 1 && input[0] instanceof AdapterComponent) {
      return ((AdapterComponent) input[0]).component();
    } else {
      return GsonComponentSerializer.gson().deserialize(net.md_5.bungee.chat.ComponentSerializer.toString(input));
    }
  }

  @Override
  public @NonNull BaseComponent@NonNull[] serialize(final @NonNull Component component) {
    requireNonNull(component, "component");
    if(SUPPORTED) {
      return new BaseComponent[] {this.maker.apply(component)};
    } else {
      return net.md_5.bungee.chat.ComponentSerializer.parse(this.serializer.serialize(component));
    }
  }
}

class AdapterComponent extends BaseComponent implements SelfSerializable {
  private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().hexColors().build();

  private final Component component;
  private volatile String legacy;

  @SuppressWarnings("deprecation") // TODO: when/if bungee removes this, ???
  AdapterComponent(final Component component) {
    this.component = component;
  }

  protected GsonComponentSerializer gson() {
    return GSON;
  }

  protected LegacyComponentSerializer legacy() {
    return LEGACY;
  }

  @Override
  public String toLegacyText() {
    if (this.legacy == null) {
      this.legacy = legacy().serialize(this.component);
    }
    return this.legacy;
  }

  @Override
  public @NonNull BaseComponent duplicate() {
    return this;
  }

  public @NonNull Component component() {
    return this.component;
  }

  @Override
  public void write(final JsonWriter out) throws IOException {
    gson().serializer().getAdapter(Component.class).write(out, this.component);
  }
}

final class DownsamplingAdapterComponent extends AdapterComponent {
  private static final GsonComponentSerializer GSON = GsonComponentSerializer.colorDownsamplingGson();
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacy();

  DownsamplingAdapterComponent(final Component component) {
    super(component);
  }

  @Override
  protected GsonComponentSerializer gson() {
    return GSON;
  }

  @Override
  protected LegacyComponentSerializer legacy() {
    return LEGACY;
  }
}
