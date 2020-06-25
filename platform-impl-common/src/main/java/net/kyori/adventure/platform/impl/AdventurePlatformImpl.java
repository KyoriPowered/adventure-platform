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
package net.kyori.adventure.platform.impl;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import net.kyori.adventure.platform.audience.PlayerAudience;
import net.kyori.adventure.platform.audience.SenderAudience;
import net.kyori.adventure.platform.AdventurePlatform;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * A base implementation of adventure platform.
 */
public abstract class AdventurePlatformImpl implements AdventurePlatform {

    private Audience all;
    private Audience console;
    private Audience players;
    private Map<UUID, PlayerAudience> playerMap;
    private Set<SenderAudience> senderSet;
    private Map<String, Audience> permissionMap;
    private Map<UUID, Audience> worldMap;
    private Map<String, Audience> serverMap;
    private volatile boolean closed;

    public AdventurePlatformImpl() {
        this.console = new ConsoleAudience();
        this.players = new PlayersAudience();
        this.senderSet = ConcurrentHashMap.newKeySet();
        this.all = (MultiAudience) () -> this.senderSet;
        this.playerMap = new ConcurrentSkipListMap<>(UUID::compareTo);
        this.permissionMap = new ConcurrentSkipListMap<>(String::compareTo);
        this.worldMap = new ConcurrentSkipListMap<>(UUID::compareTo);
        this.serverMap = new ConcurrentSkipListMap<>(String::compareTo);
        this.closed = false;
    }

    /**
     * Adds an audience to the registry.
     *
     * @param audience an audience
     */
    protected void add(SenderAudience audience) {
        // TODO: wrap audiences to inject custom rendering code
        // TODO: check if closed before forwarding messages
        this.senderSet.add(audience);
        if (audience instanceof PlayerAudience) {
            this.playerMap.put(((PlayerAudience) audience).getId(), (PlayerAudience) audience);
        }
    }

    /**
     * Removes an audience from the registry.
     *
     * @param playerId a player id
     */
    protected void remove(UUID playerId) {
        final Audience removed = this.playerMap.remove(playerId);
        this.senderSet.remove(removed);
    }

    @Override
    public @NonNull Audience all() {
        return all;
    }

    private class ConsoleAudience implements MultiAudience {
        @Override
        public @NonNull Iterable<? extends Audience> audiences() {
            return senderSet.stream().filter(SenderAudience::isConsole).collect(Collectors.toList());
        }
    }

    @Override
    public @NonNull Audience console() {
        return console;
    }

    private class PlayersAudience implements MultiAudience {
        @Override
        public @NonNull Iterable<? extends Audience> audiences() {
            return playerMap.values();
        }
    }

    @Override
    public @NonNull Audience players() {
        return players;
    }

    @Override
    public @NonNull Audience player(@NonNull UUID playerId) {
        final PlayerAudience player = playerMap.get(playerId);
        return player == null ? Audience.empty() : player;
    }

    private class PermissionAudience implements MultiAudience {
        private final String permission;

        private PermissionAudience(final @NonNull String permission) {
            this.permission = requireNonNull(permission, "permission");
        }

        private boolean hasPermission(SenderAudience audience) {
            return audience.hasPermission(this.permission);
        }

        @Override
        public @NonNull Iterable<? extends Audience> audiences() {
            return senderSet.stream().filter(this::hasPermission).collect(Collectors.toList());
        }
    }

    @Override
    public @NonNull Audience permission(@NonNull String permission) {
        Audience audience = this.permissionMap.get(permission);
        if (audience == null) {
            audience = this.permissionMap.computeIfAbsent(permission, PermissionAudience::new);
        }
        return audience;
    }

    private class WorldAudience implements MultiAudience {
        private final UUID worldId;

        private WorldAudience(final @NonNull UUID worldId) {
            this.worldId = requireNonNull(worldId, "world id");
        }

        private boolean isInWorld(PlayerAudience audience) {
            return worldId.equals(audience.getWorldId());
        }

        @Override
        public @NonNull Iterable<? extends Audience> audiences() {
            return playerMap.values().stream().filter(this::isInWorld).collect(Collectors.toList());
        }
    }

    @Override
    public @NonNull Audience world(@NonNull UUID worldId) {
        Audience audience = this.worldMap.get(worldId);
        if (audience == null) {
            audience = this.worldMap.computeIfAbsent(worldId, WorldAudience::new);
        }
        return audience;
    }

    private class ServerAudience implements MultiAudience {
        private final String serverName;

        private ServerAudience(final @NonNull String serverName) {
            this.serverName = requireNonNull(serverName, "server name");
        }

        private boolean isOnServer(PlayerAudience audience) {
            return serverName.equals(audience.getServerName());
        }

        @Override
        public @NonNull Iterable<? extends Audience> audiences() {
            return playerMap.values().stream().filter(this::isOnServer).collect(Collectors.toList());
        }
    }

    @Override
    public @NonNull Audience server(@NonNull String serverName) {
        Audience audience = this.serverMap.get(serverName);
        if (audience == null) {
            audience = this.serverMap.computeIfAbsent(serverName, ServerAudience::new);
        }
        return audience;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.all = Audience.empty();
            this.console = Audience.empty();
            this.players = Audience.empty();
            this.playerMap = Collections.emptyMap();
            this.senderSet = Collections.emptySet();
            this.permissionMap = Collections.emptyMap();
            this.worldMap = Collections.emptyMap();
            this.serverMap = Collections.emptyMap();
        }

        this.closed = true;
    }
}
