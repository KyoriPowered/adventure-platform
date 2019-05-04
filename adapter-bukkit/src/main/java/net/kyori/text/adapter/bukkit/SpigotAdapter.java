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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

final class SpigotAdapter implements Adapter {
  private static final boolean BOUND = bind();

  @SuppressWarnings("unchecked")
  private static boolean bind() {
    try {
      final Field gsonField = ComponentSerializer.class.getDeclaredField("gson");
      gsonField.setAccessible(true);
      final Field factoriesField = Gson.class.getDeclaredField("factories");
      factoriesField.setAccessible(true);

      final Gson componentSerializerGson = (Gson) gsonField.get(null);
      final Gson textGson = GsonComponentSerializer.populate(new GsonBuilder()).create();

      final List<TypeAdapterFactory> existingFactories = (List) factoriesField.get(componentSerializerGson);
      final List<TypeAdapterFactory> newFactories = (List) factoriesField.get(textGson);

      final List<TypeAdapterFactory> modifiedFactories = new LinkedList<>(existingFactories);
      for(final TypeAdapterFactory newFactory : Lists.reverse(newFactories)) {
          modifiedFactories.add(0, newFactory);
      }

      Class<?> treeTypeAdapterClass;
      try {
        // newer gson releases
        treeTypeAdapterClass = Class.forName("com.google.gson.internal.bind.TreeTypeAdapter");
      } catch (final ClassNotFoundException e) {
        // old gson releases
        treeTypeAdapterClass = Class.forName("com.google.gson.TreeTypeAdapter");
      }

      final Method newFactoryWithMatchRawTypeMethod = treeTypeAdapterClass.getMethod("newFactoryWithMatchRawType", TypeToken.class, Object.class);
      final TypeAdapterFactory adapterComponentFactory = (TypeAdapterFactory) newFactoryWithMatchRawTypeMethod.invoke(null, TypeToken.get(AdapterComponent.class), new Serializer());
      modifiedFactories.add(0, adapterComponentFactory);

      factoriesField.set(componentSerializerGson, modifiedFactories);
      return true;
    } catch(final Throwable e) {
      return false;
    }
  }

  @Override
  public void sendComponent(final List<? extends CommandSender> viewers, final Component component, final boolean actionBar) {
    if(!BOUND) {
      return;
    }
    final BaseComponent[] components = {new AdapterComponent(component)};
    for(final Iterator<? extends CommandSender> it = viewers.iterator(); it.hasNext(); ) {
      final CommandSender viewer = it.next();
      if(viewer instanceof Player) {
        try {
          final Player player = (Player) viewer;
          if(actionBar) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
          } else {
            player.spigot().sendMessage(components);
          }
          it.remove();
        } catch(final Throwable e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static final class AdapterComponent extends BaseComponent {
    private final Component component;

    AdapterComponent(final Component component) {
      this.component = component;
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
