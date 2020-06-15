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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.Knobs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

/* package */ class SpigotHandlers {

  /* package */ static final boolean BOUND = Knobs.enabled("spigot") && bind();

  private static boolean bind() {
    try {
      final Field gsonField = Crafty.field(ComponentSerializer.class, "gson");
      return Crafty.injectGson((Gson) gsonField.get(null), builder -> {
        BukkitPlatform.GSON_SERIALIZER.populator().accept(builder);
        builder.registerTypeAdapter(AdapterComponent.class, new Serializer());
      });
    } catch(NoSuchFieldException | IllegalAccessException ex) {
      return false;
    }
  }

  private static class WithBungeeText<T extends CommandSender> implements Handler<T> {

    @Override
    public boolean isAvailable() {
      return BOUND;
    }

    public BaseComponent[] initState(final @NonNull Component message) {
      return toBungeeCord(message);
    }
  }

  /* package */ static final class Chat extends WithBungeeText<CommandSender> implements Handler.Chat<CommandSender, BaseComponent[]> {
    private static final boolean HAS_COMMANDSENDER_SPIGOT = Crafty.hasClass("org.bukkit.command.CommandSender$Spigot");

    @Override
    public boolean isAvailable(final @NonNull CommandSender viewer) {
      return HAS_COMMANDSENDER_SPIGOT || viewer instanceof Player;
    }

    @Override
    public void send(@NonNull final CommandSender target, final BaseComponent @NonNull [] message) {
      target.spigot().sendMessage(message);
    }
  }

  /* package */ static final class ActionBar extends WithBungeeText<Player> implements Handler.ActionBar<Player, BaseComponent[]> {

    @Override
    public boolean isAvailable() {
      if (!super.isAvailable() || Crafty.hasCraftBukkit()) {
        return false;
      }
      try {
        final Class<?> spigotClass = Player.class.getMethod("spigot").getReturnType();
        final Class<?> chatMessageType = Crafty.findClass("net.md_5.bungee.api.ChatMessageType");
        final Class<?> baseComponent = Crafty.findClass("net.md_5.bungee.api.chat.BaseComponent");
        if(chatMessageType == null || baseComponent == null) {
          return false;
        }
        return Crafty.hasMethod(spigotClass, "sendMessage", chatMessageType, baseComponent);
      } catch(NoSuchMethodException e) {
        return false;
      }
    }

    @Override
    @SuppressWarnings("deprecation") // pls stop
    public void send(final @NonNull Player viewer, final BaseComponent @NonNull [] message) {
      viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
    }
  }

  static BaseComponent[] toBungeeCord(final @NonNull Component component) {
    requireNonNull(component, "component");
    if(BOUND) {
      return new BaseComponent[]{new AdapterComponent(component)};
    } else {
      return ComponentSerializer.parse(BukkitPlatform.GSON_SERIALIZER.serialize(component));
    }
  }

  /* package */ static final class OpenBook implements Handler.Books<Player> {
    private static final boolean SUPPORTED = Crafty.hasMethod(Player.class, "openBook", ItemStack.class); // Added June 2019

    @Override
    public boolean isAvailable() {
      return BOUND & SUPPORTED;
    }

    public ItemStack createBook(final @NonNull Book book) {
      final ItemStack stack = new ItemStack(Material.WRITTEN_BOOK);
      final ItemMeta meta = stack.getItemMeta();
      if(meta instanceof BookMeta) {
        final BookMeta spigot = (BookMeta) meta;
        for(final Component page : book.pages()) {
          spigot.spigot().addPage(toBungeeCord(page));
        }
        spigot.setAuthor(LegacyComponentSerializer.legacy().serialize(book.author()));
        spigot.setTitle(LegacyComponentSerializer.legacy().serialize(book.title())); // todo: don't use legacy
        stack.setItemMeta(spigot);
      }
      return stack;
    }

    @Override
    public void openBook(final @NonNull Player viewer, final @NonNull Book book) {
      viewer.openBook(createBook(book));
    }
  }

  /* package */ static final class AdapterComponent extends BaseComponent {
    private final Component component;

    /* package */ AdapterComponent(final Component component) {
      this.component = component;
    }

    @Override
    public BaseComponent duplicate() {
      return this;
    }

    @Override
    public String toLegacyText() {
      return LegacyComponentSerializer.legacy().serialize(this.component);
    }
  }

  /* package */ static class Serializer implements JsonSerializer<AdapterComponent> {
    @Override
    public JsonElement serialize(final AdapterComponent src, final Type typeOfSrc, final JsonSerializationContext context) {
      return context.serialize(src.component);
    }
  }
}
