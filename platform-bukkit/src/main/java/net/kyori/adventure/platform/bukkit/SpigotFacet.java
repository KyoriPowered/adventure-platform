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
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetBase;
import net.kyori.adventure.platform.facet.FacetComponentFlattener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.TranslationRegistry;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.platform.bukkit.BukkitComponentSerializer.gson;
import static net.kyori.adventure.platform.bukkit.BukkitComponentSerializer.legacy;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.hasClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.hasMethod;
import static net.kyori.adventure.platform.facet.Knob.isEnabled;
import static net.kyori.adventure.platform.facet.Knob.logUnsupported;
import static net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.isNative;

class SpigotFacet<V extends CommandSender> extends FacetBase<V> {
  private static final boolean SUPPORTED = isEnabled("spigot", true) && isNative();

  protected SpigotFacet(final @Nullable Class<? extends V> viewerClass) {
    super(viewerClass);
  }

  @Override
  public boolean isSupported() {
    return super.isSupported() && SUPPORTED;
  }

  private static final Class<?> BUNGEE_CHAT_MESSAGE_TYPE = findClass("net.md_5.bungee.api.ChatMessageType");
  static final Class<?> BUNGEE_COMPONENT_TYPE = findClass("net.md_5.bungee.api.chat.BaseComponent");

  static class Message<V extends CommandSender> extends SpigotFacet<V> implements Facet.Message<V, BaseComponent[]> {
    private static final BungeeComponentSerializer SERIALIZER = BungeeComponentSerializer.of(gson(), legacy());

    protected Message(final @Nullable Class<? extends V> viewerClass) {
      super(viewerClass);
    }

    @Override
    public @NotNull BaseComponent@NotNull[] createMessage(final @NotNull V viewer, final @NotNull Component message) {
      return SERIALIZER.serialize(message);
    }
  }

  static final class Chat extends Message<CommandSender> implements Facet.Chat<CommandSender, BaseComponent[]> {
    private static final boolean SUPPORTED = hasClass("org.bukkit.command.CommandSender$Spigot");

    protected Chat() {
      super(CommandSender.class);
    }

    @Override
    public boolean isSupported() {
      return super.isSupported() && SUPPORTED;
    }

    @Override
    public void sendMessage(final @NotNull CommandSender viewer, final @NotNull Identity source, final BaseComponent @NotNull[] message, final @NotNull MessageType type) {
      viewer.spigot().sendMessage(message);
    }
  }

  static class ChatWithType extends Message<Player> implements Facet.Chat<Player, BaseComponent[]> {
    private static final Class<?> PLAYER_CLASS = findClass("org.bukkit.entity.Player$Spigot");
    private static final boolean SUPPORTED = hasMethod(PLAYER_CLASS, "sendMessage", BUNGEE_CHAT_MESSAGE_TYPE, BUNGEE_COMPONENT_TYPE);

    protected ChatWithType() {
      super(Player.class);
    }

    @Override
    public boolean isSupported() {
      return super.isSupported() && SUPPORTED;
    }

    private @Nullable ChatMessageType createType(final @NotNull MessageType type) {
      if (type == MessageType.CHAT) {
        return ChatMessageType.CHAT;
      } else if (type == MessageType.SYSTEM) {
        return ChatMessageType.SYSTEM;
      }
      logUnsupported(this, type);
      return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void sendMessage(final @NotNull Player viewer, final @NotNull Identity source, final BaseComponent @NotNull[] message, final @NotNull MessageType type) {
      final ChatMessageType chat = this.createType(type);
      if (chat != null) {
        viewer.spigot().sendMessage(chat, message);
      }
    }
  }

  static final class ActionBar extends ChatWithType implements Facet.ActionBar<Player, BaseComponent[]> {
    @Override
    @SuppressWarnings("deprecation")
    public void sendMessage(final @NotNull Player viewer, final BaseComponent @NotNull[] message) {
      viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
    }
  }

  static final class Book extends Message<Player> implements Facet.Book<Player, BaseComponent[], ItemStack> {
    private static final boolean SUPPORTED = hasMethod(Player.class, "openBook", ItemStack.class); // Added June 2019

    protected Book() {
      super(Player.class);
    }

    @Override
    public boolean isSupported() {
      return super.isSupported() && SUPPORTED;
    }

    @Override
    public @NotNull ItemStack createBook(final @NotNull String title, final @NotNull String author, final @NotNull Iterable<BaseComponent[]> pages) {
      final ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
      final ItemMeta meta = book.getItemMeta();
      if (meta instanceof BookMeta) {
        final BookMeta spigot = (BookMeta) meta;
        for (final BaseComponent[] page : pages) {
          spigot.spigot().addPage(page);
        }
        spigot.setTitle(title);
        spigot.setAuthor(author);
        book.setItemMeta(spigot);
      }
      return book;
    }

    @Override
    public void openBook(final @NotNull Player viewer, final @NotNull ItemStack book) {
      viewer.openBook(book);
    }
  }

  static class Translator extends FacetBase<Server> implements FacetComponentFlattener.Translator<Server> {
    private static final boolean SUPPORTED = MinecraftReflection.hasClass("net.md_5.bungee.chat.TranslationRegistry");

    Translator() {
      super(Server.class);
    }

    @Override
    public boolean isSupported() {
      return super.isSupported() && SUPPORTED;
    }

    @Override
    public @NotNull String valueOrDefault(final @NotNull Server game, final @NotNull String key) {
      return TranslationRegistry.INSTANCE.translate(key);
    }
  }
}
