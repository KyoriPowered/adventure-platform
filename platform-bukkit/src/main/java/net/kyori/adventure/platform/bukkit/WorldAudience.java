package net.kyori.adventure.platform.bukkit;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import org.bukkit.Server;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.ref.WeakReference;
import java.util.Collections;

import static com.google.common.collect.Iterables.transform;
import static java.util.Objects.requireNonNull;

final class WorldAudience implements MultiAudience {

    private final Server server;
    private final String worldName;
    private WeakReference<World> worldRef;

    public WorldAudience(final @NonNull Server server, final @NonNull String worldName) {
        this.server = requireNonNull(server, "server");
        this.worldName = requireNonNull(worldName, "world name");
        this.worldRef = new WeakReference<>(null);
    }

    @Override
    public @NonNull Iterable<? extends Audience> audiences() {
        World world = worldRef.get();
        if (world == null) {
            world = server.getWorld(worldName);
            if (world != null) {
                worldRef = new WeakReference<>(world);
            }
        }

        if (world == null) {
            return Collections.emptyList();
        }

        // TODO: include console
        return transform(world.getPlayers(), BukkitPlatform::audience);
    }
}
