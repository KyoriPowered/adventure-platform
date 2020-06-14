package net.kyori.adventure.platform.spongeapi;

import java.util.UUID;
import net.kyori.adventure.platform.viaversion.ViaVersionHandlers;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginManager;
import us.myles.ViaVersion.api.platform.ViaPlatform;

/**
 * Sponge provider for ViaVersion API
 */
/* package */  class SpongeViaProvider implements ViaVersionHandlers.ViaAPIProvider<Object> { // too many interfaces :(

  private final PluginManager plugins;
  private volatile ViaPlatform<?> platform = null;

  SpongeViaProvider(final PluginManager plugins) {
    this.plugins = plugins;
  }

  @Override
  public boolean isAvailable() {
    return this.plugins.isLoaded("viaversion");
  }

  @Override
  public ViaPlatform<?> platform() {
    if(!isAvailable()) {
      return null;
    }
    ViaPlatform<?> platform = this.platform;
    if(platform == null) {
      final PluginContainer container = this.plugins.getPlugin("viaversion").orElse(null);
      if(container == null) return null;
      this.platform = platform = (ViaPlatform<?>) container.getInstance().orElse(null);
    }
    return platform;
  }

  @Override
  public @Nullable UUID id(final Object viewer) {
    if(!(viewer instanceof Player)) return null;

    return ((Player) viewer).getUniqueId();
  }
}
