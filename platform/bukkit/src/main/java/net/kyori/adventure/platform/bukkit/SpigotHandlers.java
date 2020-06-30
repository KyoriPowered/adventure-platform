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
package net.kyori.adventure.platform.bukkit;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.Knobs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeCordComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/* package */ class SpigotHandlers {

  /* package */ static final boolean BOUND = Knobs.enabled("spigot") && BungeeCordComponentSerializer.nativeSupport();

  /* package */ static final BungeeCordComponentSerializer SERIALIZER = BukkitPlatform.IS_1_16 ? BungeeCordComponentSerializer.get() : BungeeCordComponentSerializer.legacy();

  private static class WithBungeeText<T extends CommandSender> implements Handler<T> {

    @Override
    public boolean isAvailable() {
      return BOUND;
    }

    public BaseComponent[] initState(final @NonNull Component message) {
      return SERIALIZER.serialize(message);
    }
  }

  /* package */ static final class Chat extends WithBungeeText<CommandSender> implements Handler.Chat<CommandSender, BaseComponent[]> {
    private static final boolean HAS_COMMANDSENDER_SPIGOT = Crafty.hasClass("org.bukkit.command.CommandSender$Spigot");

    @Override
    public boolean isAvailable() {
      return super.isAvailable() && HAS_COMMANDSENDER_SPIGOT;
    }

    @Override
    public void send(@NonNull final CommandSender target, final BaseComponent @NonNull [] message) {
      target.spigot().sendMessage(message);
    }
  }

  /* package */ static final class Chat_PlayerOnly extends WithBungeeText<CommandSender> implements Handler.Chat<CommandSender, BaseComponent[]> {

    @Override
    public boolean isAvailable(final @NonNull CommandSender viewer) {
      return viewer instanceof Player;
    }

    @Override
    public void send(final @NonNull CommandSender target, final BaseComponent @NonNull [] message) {
      ((Player) target).spigot().sendMessage(message);
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

  /* package */ static final class OpenBook implements Handler.Books<Player> {
    private static final boolean SUPPORTED = Crafty.hasMethod(Player.class, "openBook", ItemStack.class); // Added June 2019

    @Override
    public boolean isAvailable() {
      return BOUND & SUPPORTED;
    }

    @Override
    public void openBook(@NonNull Player viewer, @NonNull Component title, @NonNull Component author, @NonNull Iterable<Component> pages) {
      final ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
      final ItemMeta meta = book.getItemMeta();
      if(meta instanceof BookMeta) {
        final BookMeta spigot = (BookMeta) meta;
        for(final Component page : pages) {
          spigot.spigot().addPage(SERIALIZER.serialize(page));
        }
        spigot.setAuthor(BukkitPlatform.LEGACY_SERIALIZER.serialize(author));
        spigot.setTitle(BukkitPlatform.LEGACY_SERIALIZER.serialize(title)); // TODO: don't use legacy
        book.setItemMeta(spigot);
      }

      viewer.openBook(book);
    }
  }
}
