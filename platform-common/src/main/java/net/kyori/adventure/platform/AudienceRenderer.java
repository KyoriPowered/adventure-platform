package net.kyori.adventure.platform;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

/**
 * A component renderer that customizes text for an audience.
 */
// TODO: should this be in text?
public interface AudienceRenderer extends ComponentRenderer<AudienceRenderer.Partition> {

    /**
     * Customizes a component for an audience.
     *
     * @param component a component
     * @param partition an audience partition
     * @return a customized component
     */
    @Override
    @NonNull Component render(@NonNull Component component, @NonNull Partition partition);

    /**
     * Gets the strategy for partitioning audiences.
     *
     * @return a partition strategy
     */
    @NonNull PartitionBy partitionBy();

    /**
     * A strategy for partitioning audiences.
     */
    enum PartitionBy {
        /**
         * Each audience is treated as a separate partition.
         */
        INDIVIDUAL,
        /**
         * Audiences with the same locale are grouped into the same partition.
         */
        LOCALE;
    }

    /**
     * A partition of audiences.
     */
    interface Partition {
        /**
         * Gets the locale of the audience.
         *
         * @return a locale, or null if unknown
         */
        @Nullable Locale getLocale();
    }
}
