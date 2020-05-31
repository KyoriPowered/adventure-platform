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
package net.kyori.adventure.platform;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public class Adventure {
  private static final AdventurePlatform PROVIDER = AdventureProvider0.provide();

  /**
   * Create an audience that will only send to the server console.
   *
   * @return server console audience
   */
  public static Audience console() {
    return PROVIDER.console();
  }

  /**
   * Create an audience that will send to all of the provided audiences.
   *
   * @param audiences child audiences
   * @return sew audience containing all child audiences
   */
  public static MultiAudience audience(Audience... audiences) {
    return PROVIDER.audience(audiences);
  }

  /**
   * Create an audience that will send to all of the provided audiences.
   *
   * @param audiences child audiences
   * @return new audience containing all child audiences
   */
  public static MultiAudience audience(Iterable<Audience> audiences) {
    return PROVIDER.audience(audiences);
  }

  /**
   * Create a new audience containing all users with the provided permission.
   *
   * <p>The returned audience will dynamically update as viewer permissions change.
   *
   * @param permission permission to filter sending to
   * @return new audience
   */
  public static MultiAudience permission(Key permission) {
    return PROVIDER.permission(permission);
  }

  /**
   * Create a new audience containing all online viewers. This audience does not contain any console.
   *
   * <p>The returned audience will dynamically update as the online viewers change.
   *
   * @return audience, may be a shared instance
   */
  public static MultiAudience online() {
    return PROVIDER.online();
  }

}
