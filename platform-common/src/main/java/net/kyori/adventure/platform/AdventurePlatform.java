/*
 * This file is part of adventure, licensed under the MIT License.
 *
 * Copyright (c) 2017-2020 KyoriPowered
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
package net.kyori.adventure.platform;

import java.util.Arrays;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * SPI interface, to be implemented only by platform implementers
 */
public interface AdventurePlatform {

  /**
   * A friendly name to give information about this provider
   *
   * @return display name
   */
  @NonNull String name();

  /**
   * Get the support level of this provider. Higher support levels will be preferred.
   *
   * @return provider support level
   */
  @NonNull ProviderSupport supportLevel();

  /**
   * Create an audience that will only send to the server console.
   *
   * @return server console audience
   */
  @NonNull Audience console();

  /**
   * Create an audience that will send to all of the provided audiences.
   *
   * @param audiences child audiences
   * @return sew audience containing all child audiences
   */
  default @NonNull MultiAudience audience(@NonNull Audience @NonNull... audiences) {
    return audience(Arrays.asList(audiences));
  }

  /**
   * Create an audience that will send to all of the provided audiences.
   *
   * @param audiences child audiences
   * @return new audience containing all child audiences
   */
  @NonNull MultiAudience audience(@NonNull Iterable<@NonNull Audience> audiences);

  /**
   * Create a new audience containing all users with the provided permission.
   *
   * <p>The returned audience will dynamically update as viewer permissions change.
   *
   * @param permission permission to filter sending to
   * @return new audience
   */
  default @NonNull MultiAudience permission(@NonNull Key permission) {
    return permission(permission.namespace() + '.' + permission.value());
  }

  /**
   * Create a new audience containing all users with the provided permission.
   *
   * <p>The returned audience will dynamically update as viewer permissions change.
   *
   * @param permission permission to filter sending to
   * @return new audience
   */
  @NonNull MultiAudience permission(@NonNull String permission);

  /**
   * Create a new audience containing all online viewers. This audience does not contain any console.
   *
   * <p>The returned audience will dynamically update as the online viewers change.
   *
   * @return audience, may be a shared instance
   */
  @NonNull MultiAudience online();
}
