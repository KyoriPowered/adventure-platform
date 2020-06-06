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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.TreeTypeAdapter;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An adapter for sending and converting text {@link Component}s to BungeeCord objects.
 */
public interface TextAdapter {
  /**
   * Sends {@code component} to the given {@code viewer}.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   */
  static void sendMessage(final @NonNull CommandSender viewer, final @NonNull Component component) {
    viewer.sendMessage(toBungeeCord(component));
  }

  /**
   * Sends {@code component} to the given {@code viewers}.
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   */
  static void sendMessage(final @NonNull Iterable<? extends CommandSender> viewers, final @NonNull Component component) {
    final BaseComponent[] components = toBungeeCord(component);
    for(final CommandSender viewer : viewers) {
      viewer.sendMessage(components);
    }
  }

  /**
   * Sends {@code component} to the given {@code viewer}.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   * @deprecated use {@link #sendMessage(CommandSender, Component)}
   */
  @Deprecated
  static void sendComponent(final @NonNull CommandSender viewer, final @NonNull Component component) {
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
  static void sendComponent(final @NonNull Iterable<? extends CommandSender> viewers, final @NonNull Component component) {
    sendMessage(viewers, component);
  }

  /**
   * Converts {@code component} to the {@link BaseComponent} format used by BungeeCord.
   *
   * <p>The adapter makes no guarantees about the underlying structure/type of the components.
   * i.e. is it not guaranteed that a {@link net.kyori.text.TextComponent} will map to a
   * {@link net.md_5.bungee.api.chat.TextComponent}.</p>
   *
   * <p>The {@code sendComponent} methods should be used instead of this method when possible.</p>
   *
   * @param component the component
   * @return the Text representation of the component
   */
  static @NonNull BaseComponent[] toBungeeCord(final @NonNull Component component) {
    return TextAdapter0.toBungeeCord(component);
  }
}

final class TextAdapter0 {
  static final boolean BOUND = bind();

  @SuppressWarnings("unchecked")
  private static boolean bind() {
    try {
      final Field gsonField = field(ComponentSerializer.class, "gson");
      final Field factoriesField = field(Gson.class, "factories");
      final Field builderFactoriesField = field(GsonBuilder.class, "factories");
      final Field builderHierarchyFactoriesField = field(GsonBuilder.class, "hierarchyFactories");

      final Gson gson = (Gson) gsonField.get(null);
      final GsonBuilder builder = GsonComponentSerializer.populate(new GsonBuilder());

      final List<TypeAdapterFactory> existingFactories = (List<TypeAdapterFactory>) factoriesField.get(gson);
      final List<TypeAdapterFactory> newFactories = new ArrayList<>();
      newFactories.addAll((List<TypeAdapterFactory>) builderFactoriesField.get(builder));
      Collections.reverse(newFactories);
      newFactories.addAll((List<TypeAdapterFactory>) builderHierarchyFactoriesField.get(builder));

      final List<TypeAdapterFactory> modifiedFactories = new ArrayList<>(existingFactories);

      // the excluder must precede all adapters that handle user-defined types
      final int index = findExcluderIndex(modifiedFactories);

      for(final TypeAdapterFactory newFactory : Lists.reverse(newFactories)) {
        modifiedFactories.add(index, newFactory);
      }

      modifiedFactories.add(index, TreeTypeAdapter.newFactoryWithMatchRawType(TypeToken.get(AdapterComponent.class), new Serializer()));

      factoriesField.set(gson, modifiedFactories);
      return true;
    } catch(final Exception e) {
      return false;
    }
  }

  private static Field field(final Class<?> klass, final String name) throws NoSuchFieldException {
    final Field field = klass.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  private static int findExcluderIndex(final List<TypeAdapterFactory> factories) {
    for(int i = 0, size = factories.size(); i < size; i++) {
      final TypeAdapterFactory factory = factories.get(i);
      if(factory instanceof Excluder) {
        return i + 1;
      }
    }
    return 0;
  }

  static BaseComponent[] toBungeeCord(final Component component) {
    if(BOUND) {
      return new BaseComponent[]{new AdapterComponent(component)};
    } else {
      return ComponentSerializer.parse(GsonComponentSerializer.INSTANCE.serialize(component));
    }
  }

  public static final class AdapterComponent extends BaseComponent {
    private final Component component;

    @SuppressWarnings("deprecation") // weeeee
    AdapterComponent(final Component component) {
      this.component = component;
    }

    @Override
    public String toLegacyText() {
      return LegacyComponentSerializer.legacy().serialize(this.component);
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
