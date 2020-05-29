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
package net.kyori.adventure.platform.bukkit;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Excluder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class SpigotAdapter implements Adapter {
  private static final boolean BOUND = bind();

  @SuppressWarnings("unchecked")
  private static boolean bind() {
    try {
      final Field gsonField = Crafty.field(ComponentSerializer.class, "gson");
      return Crafty.injectGson((Gson) gsonField.get(null), builder -> {
        GsonComponentSerializer.GSON_BUILDER_CONFIGURER.accept(builder);
        builder.registerTypeAdapter(AdapterComponent.class, new Serializer());
      });
    } catch(NoSuchFieldException | IllegalAccessException ex) {
      return false;
    }
  }


  @Override
  public void sendMessage(final List<? extends CommandSender> viewers, final Component component) {
    if(!BOUND) {
      return;
    }
    send(viewers, component, (viewer, components) -> viewer.spigot().sendMessage(components));
  }

  @Override
  public void sendActionBar(final List<? extends CommandSender> viewers, final Component component) {
    if(!BOUND) {
      return;
    }
    send(viewers, component, (viewer, components) -> viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, components));
  }

  private static void send(final List<? extends CommandSender> viewers, final Component component, final BiConsumer<Player, BaseComponent[]> consumer) {
    if(!BOUND) {
      return;
    }
    final BaseComponent[] components = {new AdapterComponent(component)};
    for(final Iterator<? extends CommandSender> it = viewers.iterator(); it.hasNext(); ) {
      final CommandSender viewer = it.next();
      if(viewer instanceof Player) {
        try {
          consumer.accept((Player) viewer, components);
          it.remove();
        } catch(final Throwable e) {
          e.printStackTrace();
        }
      }
    }
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
