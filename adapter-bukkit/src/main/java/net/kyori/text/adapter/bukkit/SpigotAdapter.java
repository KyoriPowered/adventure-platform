/*
 * This file is part of text-adapters, licensed under the MIT License.
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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TreeTypeAdapter;
import com.google.gson.reflect.TypeToken;
import net.kyori.text.Component;
import net.kyori.text.serializer.GsonComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class SpigotAdapter implements Adapter {
  private static final boolean SETUP;
  static {
    boolean setup = false;
    try {
      final Field gsonField = ComponentSerializer.class.getDeclaredField("gson");
      gsonField.setAccessible(true);
      final Gson gson = (Gson) gsonField.get(null);
      final Field factoriesField = Gson.class.getDeclaredField("factories");
      factoriesField.setAccessible(true);

      //noinspection unchecked
      final List<TypeAdapterFactory> factories = (List) factoriesField.get(gson);
      final List<TypeAdapterFactory> modifiedFactories = new ArrayList<>(factories);
      modifiedFactories.add(0, TreeTypeAdapter.newTypeHierarchyFactory(Component.class, new GsonComponentSerializer()));
      modifiedFactories.add(0, TreeTypeAdapter.newFactoryWithMatchRawType(TypeToken.get(AdapterComponent.class), new Serializer()));
      factoriesField.set(gson, modifiedFactories);
      setup = true;
    } catch(final Exception e) {
      // ignore
    }
    SETUP = setup;
  }

  @Override
  public void sendComponent(final List<? extends CommandSender> senders, final Component component) {
    if(!SETUP) {
      return;
    }
    final BaseComponent[] baseComponents = {new AdapterComponent(component)};
    for(final Iterator<? extends CommandSender> iterator = senders.iterator(); iterator.hasNext(); ) {
      final CommandSender sender = iterator.next();
      if(sender instanceof Player) {
        try {
          final Player player = (Player) sender;
          player.spigot().sendMessage(baseComponents);
          iterator.remove();
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
    public JsonElement serialize(final AdapterComponent adapter, final Type type, final JsonSerializationContext context) {
      return context.serialize(adapter.component);
    }
  }
}
