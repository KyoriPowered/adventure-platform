package net.kyori.adventure.platform.spongeapi;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.text.channel.MessageReceiver;

final class SpongeMultiAudience implements MultiAudience {
  private final Supplier<Collection<? extends MessageReceiver>> viewers;

  SpongeMultiAudience(final Supplier<Collection<? extends MessageReceiver>> viewers) {
    this.viewers = viewers;
  }

  @Override
  public @NonNull Iterable<Audience> audiences() {
    return viewers.get().stream()
      .map(viewer -> {
        if (viewer instanceof Viewer) {
          return new SpongeFullAudience((Viewer & MessageReceiver) viewer);
        } else {
          return new SpongeAudience(viewer);
        }
      }).collect(Collectors.toList());
  }
}
