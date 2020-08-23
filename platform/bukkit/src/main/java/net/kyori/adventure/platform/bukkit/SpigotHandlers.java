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

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.platform.common.Handler;
import net.kyori.adventure.platform.common.Knobs;
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

final class SpigotHandlers {
  // net.kyori.adventure.enableBungeeCordAdapters
  private static final boolean FORCE_ENABLE = Boolean.getBoolean("net.kyo".concat("ri.adventure.enableBungeeCordAdapters"));
  static final boolean BOUND = FORCE_ENABLE && Knobs.enabled("spigot") && BungeeCordComponentSerializer.nativeSupport();

  static final BungeeCordComponentSerializer SERIALIZER = BungeeCordComponentSerializer.of(BukkitAudienceProvider.GSON_SERIALIZER, BukkitAudienceProvider.LEGACY_SERIALIZER);

  private static final Class<?> BUNGEE_CHAT_MESSAGE_TYPE = Crafty.findClass("net.md_5.bungee.api.ChatMessageType");
  private static final Class<?> BUNGEE_COMPONENT_TYPE = Crafty.findClass("net.md_5.bungee.api.chat.BaseComponent");

  private SpigotHandlers() {
  }

  private static class WithBungeeText<T extends CommandSender> implements Handler<T> {

    @Override
    public boolean isAvailable() {
      return BOUND;
    }

    public BaseComponent[] initState(final @NonNull Component message) {
      return SERIALIZER.serialize(message);
    }

    public BaseComponent[] initState(final @NonNull Component message, final MessageType type) {
      return this.initState(message);
    }
  }

  static final class Chat extends WithBungeeText<CommandSender> implements Handler.Chat<CommandSender, BaseComponent[]> {
    private static final boolean HAS_COMMANDSENDER_SPIGOT = Crafty.hasClass("org.bukkit.command.CommandSender$Spigot");

    @Override
    public boolean isAvailable() {
      return super.isAvailable() && HAS_COMMANDSENDER_SPIGOT;
    }

    @Override
    public void send(@NonNull final CommandSender target, final BaseComponent @NonNull [] message, final @NonNull MessageType type) {
      target.spigot().sendMessage(message);
    }
  }

  static final class Chat_PlayerWithType extends WithBungeeText<CommandSender> implements Handler.Chat<CommandSender, BaseComponent[]> {
    private static final Class<?> PLAYER_SPIGOT = Crafty.findClass("org.bukkit.entity.Player$Spigot");
    private static final boolean HAS_TYPE = Crafty.hasMethod(PLAYER_SPIGOT, "sendMessage", BUNGEE_CHAT_MESSAGE_TYPE, BUNGEE_COMPONENT_TYPE);

    @Override
    public boolean isAvailable(final @NonNull CommandSender viewer) {
      return super.isAvailable() && viewer instanceof Player && HAS_TYPE;
    }

    @Override
    @SuppressWarnings("deprecation") // this is stupid.
    public void send(final @NonNull CommandSender target, final BaseComponent @NonNull [] message, final MessageType type) {
      ((Player) target).spigot().sendMessage(this.messageType(type), message);
    }

    private ChatMessageType messageType(final MessageType type) {
      if(type == MessageType.CHAT) {
        return ChatMessageType.CHAT;
      } else {
        return ChatMessageType.SYSTEM;
      }
    }
  }

  static final class Chat_PlayerOnly extends WithBungeeText<CommandSender> implements Handler.Chat<CommandSender, BaseComponent[]> {

    @Override
    public boolean isAvailable(final @NonNull CommandSender viewer) {
      return viewer instanceof Player;
    }

    @Override
    public void send(final @NonNull CommandSender target, final BaseComponent @NonNull [] message, final MessageType type) {
      ((Player) target).spigot().sendMessage(message);
    }
  }

  static final class ActionBar extends WithBungeeText<Player> implements Handler.ActionBar<Player, BaseComponent[]> {

    @Override
    public boolean isAvailable() {
      if(!super.isAvailable() || Crafty.isCraftBukkit()) {
        return false;
      }
      try {
        final Class<?> spigotClass = Player.class.getMethod("spigot").getReturnType();
        if(BUNGEE_CHAT_MESSAGE_TYPE == null || BUNGEE_COMPONENT_TYPE == null) {
          return false;
        }
        return Crafty.hasMethod(spigotClass, "sendMessage", BUNGEE_CHAT_MESSAGE_TYPE, BUNGEE_COMPONENT_TYPE);
      } catch(final NoSuchMethodException e) {
        return false;
      }
    }

    @Override
    @SuppressWarnings("deprecation") // pls stop
    public void send(final @NonNull Player viewer, final BaseComponent @NonNull [] message) {
      viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
    }
  }

  static final class OpenBook implements Handler.Books<Player> {
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
          spigot.spigot().addPage(SERIALIZER.serialize(page));
        }
        spigot.setAuthor(BukkitAudienceProvider.LEGACY_SERIALIZER.serialize(book.author()));
        spigot.setTitle(BukkitAudienceProvider.LEGACY_SERIALIZER.serialize(book.title())); // todo: don't use legacy
        stack.setItemMeta(spigot);
      }
      return stack;
    }

    @Override
    public void openBook(final @NonNull Player viewer, final @NonNull Book book) {
      viewer.openBook(this.createBook(book));
    }
  }
}
