package net.kyori.adventure.platform;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * Contextual information about an {@link Audience}.
 */
public interface AudienceInfo {
    /**
     * Gets the locale.
     *
     * @return a locale or {@code null} if unknown
     */
    @Nullable Locale getLocale();

    /**
     * Gets the uuid, if a player.
     *
     * @return a player uuid or {@code null} if not a player
     */
    @Nullable UUID getId();

    /**
     * Gets the world identifier.
     *
     * @return a world id or {@code null} if unknown
     */
    @Nullable Key getWorld();

    /**
     * Gets the server name.
     *
     * @return a server name or {@code null} if unknown
     */
    @Nullable String getServer();

    /**
     * Checks whether there is permission.
     *
     * @param permission a permission node
     * @return if the audience has permission
     */
    boolean hasPermission(final @NonNull String permission);

    /**
     * Gets if a console.
     *
     * @return if the audience is a console
     */
    boolean isConsole();

    /**
     * Gets if a player.
     *
     * @return if the audience is a player
     */
    boolean isPlayer();
}
