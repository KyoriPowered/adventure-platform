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

import net.md_5.bungee.api.chat.BaseComponent;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * The {@link Server#getConsoleSender()} method returns null during onLoad
 * in older CraftBukkit builds.
 */
class LazyConsole implements ConsoleCommandSender {
    private final Server server;

    LazyConsole(Server server) {
        this.server = server;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void sendMessage(@NonNull String message) {
        ConsoleCommandSender console = this.server.getConsoleSender();
        if (console != null) {
            console.sendMessage(message);
        } else {
            this.server.getLogger().info(ChatColor.stripColor(message));
        }
    }

    @Override public void sendMessage(@NotNull String[] messages) { throw new UnsupportedOperationException(); }
    @Override public @NotNull Server getServer() { throw new UnsupportedOperationException(); }
    @Override public @NotNull String getName() { throw new UnsupportedOperationException(); }
    @Override public @NotNull Spigot spigot() { throw new UnsupportedOperationException(); }
    @Override public boolean isConversing() { throw new UnsupportedOperationException(); }
    @Override public void acceptConversationInput(@NotNull String input) { throw new UnsupportedOperationException(); }
    @Override public boolean beginConversation(@NotNull Conversation conversation) { throw new UnsupportedOperationException(); }
    @Override public void abandonConversation(@NotNull Conversation conversation) { throw new UnsupportedOperationException(); }
    @Override public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent details) { throw new UnsupportedOperationException(); }
    @Override public void sendRawMessage(@NotNull String message) { throw new UnsupportedOperationException(); }
    @Override public boolean isPermissionSet(@NotNull String name) { throw new UnsupportedOperationException(); }
    @Override public boolean isPermissionSet(@NotNull Permission perm) { throw new UnsupportedOperationException(); }
    @Override public boolean hasPermission(@NotNull String name) { throw new UnsupportedOperationException(); }
    @Override public boolean hasPermission(@NotNull Permission perm) { throw new UnsupportedOperationException(); }
    @Override public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) { throw new UnsupportedOperationException(); }
    @Override public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) { throw new UnsupportedOperationException(); }
    @Override public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) { throw new UnsupportedOperationException(); }
    @Override public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) { throw new UnsupportedOperationException(); }
    @Override public void removeAttachment(@NotNull PermissionAttachment attachment) { throw new UnsupportedOperationException(); }
    @Override public void recalculatePermissions() { throw new UnsupportedOperationException(); }
    @Override public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() { throw new UnsupportedOperationException(); }
    @Override public boolean isOp() { throw new UnsupportedOperationException(); }
    @Override public void setOp(boolean value) { throw new UnsupportedOperationException(); }
    @Override public void sendMessage(@NotNull BaseComponent component) { throw new UnsupportedOperationException(); }
    @Override public void sendMessage(@NotNull BaseComponent... components) { throw new UnsupportedOperationException(); }

}
