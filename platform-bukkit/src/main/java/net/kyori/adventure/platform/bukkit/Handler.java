package net.kyori.adventure.platform.bukkit;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A handler for a unit of functionality that may differ between platforms
 */
interface Handler {
  boolean isAvailable();

  /**
   * Collection, that exposes the first available handler
   *
   * @param <H> handler type
   */
  class Collection<H extends Handler> {
    private final @NonNull H activeHandler;

    @SafeVarargs
    Collection(final @NonNull H @NonNull... options) {
      for(H handler : options) {
        if(handler.isAvailable()) {
          this.activeHandler = handler;
          return;
        }
      }
      throw new IllegalArgumentException("No handler of " + Arrays.toString(options) + " was available");
    }

    @NonNull H get() {
      return this.activeHandler;
    }
  }

  interface Chat extends Handler {
    void send(@NonNull CommandSender sender, @NonNull Component message);
  }

  interface ActionBar extends Handler {
    void send(@NonNull Player player, @NonNull Component message);
  }

  interface Title extends Handler {
    void send(@NonNull Player player, net.kyori.adventure.title.@NonNull Title title);

    void clear(@NonNull Player player);

    void reset(@NonNull Player player);
  }

  interface BossBar extends Handler {
    void show(@NonNull Player player, net.kyori.adventure.bossbar.@NonNull BossBar bar);
    void hide(@NonNull Player player, net.kyori.adventure.bossbar.@NonNull BossBar bar);
  }

}
