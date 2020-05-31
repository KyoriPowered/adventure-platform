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
package net.kyori.adventure.platform.spongeapi;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import net.kyori.adventure.platform.AdventurePlatform;
import net.kyori.adventure.platform.ProviderSupport;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;

public class SpongePlatform implements AdventurePlatform {
  @Override
  public @NonNull String name() {
    return "Sponge";
  }

  @Override
  public @NonNull ProviderSupport supportLevel() {
    return ProviderSupport.FULL;
  }

  @Override
  public @NonNull Audience console() {
    return new SpongeAudience(Sponge.getGame().getServer().getConsole());
  }

  @Override
  public @NonNull MultiAudience audience(final @NonNull Iterable<@NonNull Audience> audiences) {
    return MultiAudience.of(audiences);
  }

  @Override
  public @NonNull MultiAudience permission(final @NonNull String permission) {
    /*return new SpongeMultiAudience(() -> Sponge.getGame().getServiceManager().provide(PermissionService.class)
      .orElseThrow(() -> new IllegalArgumentException("Sponge must have a permissions service"))
      .getUserSubjects().getLoadedWithPermission(spongePerm).keySet());*/
    return null;
  }

  @Override
  public @NonNull MultiAudience online() {
    return new SpongeMultiAudience(Sponge.getServer()::getOnlinePlayers);
  }
}
