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
package net.kyori.text.adapter.bungeecord;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TreeTypeAdapter;
import com.google.gson.reflect.TypeToken;

import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An adapter for sending text {@link Component}s to BungeeCord objects.
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
    TextAdapter0.sendComponent(viewers, component);
  }
}

final class TextAdapter0 {
  private static final boolean BOUND = bind();

  @SuppressWarnings("unchecked")
  private static boolean bind() {
    try {
      final Field gsonField = ComponentSerializer.class.getDeclaredField("gson");
      gsonField.setAccessible(true);
      final Gson gson = (Gson) gsonField.get(null);
      final Field factoriesField = Gson.class.getDeclaredField("factories");
      factoriesField.setAccessible(true);

      final List<TypeAdapterFactory> factories = (List) factoriesField.get(gson);
      final List<TypeAdapterFactory> modifiedFactories = new ArrayList<>(factories);
      modifiedFactories.add(0, TreeTypeAdapter.newTypeHierarchyFactory(Component.class, GsonComponentSerializer.INSTANCE));
      modifiedFactories.add(0, TreeTypeAdapter.newFactoryWithMatchRawType(TypeToken.get(AdapterComponent.class), new Serializer()));
      factoriesField.set(gson, modifiedFactories);
      return true;
    } catch(final Exception e) {
      return false;
    }
  }

  static void sendComponent(final Iterable<? extends CommandSender> viewers, final Component component) {
    final BaseComponent[] components;
    if(BOUND) {
      components = new BaseComponent[]{new AdapterComponent(component)};
    } else {
      components = ComponentSerializer.parse(GsonComponentSerializer.INSTANCE.serialize(component));
    }
    for(final CommandSender viewer : viewers) {
      viewer.sendMessage(components);
    }
  }

  public static final class AdapterComponent extends BaseComponent {
    private final Component component;

    AdapterComponent(final Component component) {
      this.component = component;
    }

    @Override
    public String toLegacyText() {
      return LegacyComponentSerializer.INSTANCE.serialize(this.component);
    }

    @Override
    public BaseComponent duplicate() {
      return this;
    }
  }

  public static class Serializer implements JsonSerializer<AdapterComponent> {
    @Override
    public JsonElement serialize(final AdapterComponent src, final Type typeOfSrc, final JsonSerializationContext context) {
      return context.serialize(src.component);
    }
  }
}
