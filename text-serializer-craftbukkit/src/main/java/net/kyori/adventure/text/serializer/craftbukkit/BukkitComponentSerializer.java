/*
 * This file is part of adventure-platform, licensed under the MIT License.
 *
 * Copyright (c) 2018-2020 KyoriPowered
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
package net.kyori.adventure.text.serializer.craftbukkit;

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.NonNull;

import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findEnum;

/**
 * A pair of component serializers for {@link org.bukkit.Bukkit}.
 */
public final class BukkitComponentSerializer {
  private BukkitComponentSerializer() {
  }

  private static final boolean IS_1_16 = findEnum(Material.class, "NETHERITE_PICKAXE") != null;
  private static final boolean IS_1_8 = findEnum(Material.class, "PRISMARINE") != null;

  private static final LegacyComponentSerializer LEGACY_SERIALIZER;
  private static final GsonComponentSerializer GSON_SERIALIZER;

  static {
    if(IS_1_16) {
      LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
              .hexColors()
              .useUnusualXRepeatedCharacterHexFormat()
              .build();
      GSON_SERIALIZER = GsonComponentSerializer.builder()
              .legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.INSTANCE)
              .build();
    } else if(IS_1_8) {
      LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
      GSON_SERIALIZER = GsonComponentSerializer.builder()
              .legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.INSTANCE)
              .emitLegacyHoverEvent()
              .downsampleColors()
              .build();
    } else {
      LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
      GSON_SERIALIZER = null;
    }
  }

  /**
   * Gets the legacy component serializer.
   *
   * @return a legacy component serializer
   */
  public static @NonNull LegacyComponentSerializer legacy() {
    return LEGACY_SERIALIZER;
  }

  /**
   * Gets the gson component serializer.
   *
   * <p>Not available on servers before 1.8, will be {@code null}.</p>
   *
   * @return a gson component serializer
   */
  public static @NonNull GsonComponentSerializer gson() {
    return GSON_SERIALIZER;
  }
}
