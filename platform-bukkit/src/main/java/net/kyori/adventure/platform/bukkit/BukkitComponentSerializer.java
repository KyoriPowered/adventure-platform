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
package net.kyori.adventure.platform.bukkit;

import java.util.Collection;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetComponentFlattener;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.kyori.adventure.text.serializer.json.legacyimpl.NBTLegacyHoverEventSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findEnum;

/**
 * A pair of component serializers for {@link org.bukkit.Bukkit}.
 *
 * @since 4.0.0
 */
public final class BukkitComponentSerializer {
  private BukkitComponentSerializer() {
  }

  private static final boolean IS_1_13 = findEnum(Material.class, "BLUE_ICE") != null;
  private static final boolean IS_1_16 = findEnum(Material.class, "NETHERITE_PICKAXE") != null;

  private static final Collection<FacetComponentFlattener.Translator<Server>> TRANSLATORS = Facet.of(
    SpigotFacet.Translator::new,
    CraftBukkitFacet.Translator::new
  );
  private static final LegacyComponentSerializer LEGACY_SERIALIZER;
  private static final GsonComponentSerializer GSON_SERIALIZER;
  static final ComponentFlattener FLATTENER;

  static {
    FLATTENER = FacetComponentFlattener.get(Bukkit.getServer(), TRANSLATORS);

    if (IS_1_13) {
      GSON_SERIALIZER = GsonComponentSerializer.builder()
              .options(JSONOptions.byDataVersion().at(Bukkit.getUnsafe().getDataVersion()))
              .build();
    } else {
      GSON_SERIALIZER = GsonComponentSerializer.builder()
              .legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.get())
              .options(JSONOptions.byDataVersion().at(0))
              .build();
    }

    if (IS_1_16) {
      LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .flattener(FLATTENER)
        .build();
    } else {
      LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
        .character(LegacyComponentSerializer.SECTION_CHAR)
        .flattener(FLATTENER)
        .build();
    }
  }

  /**
   * Gets the legacy component serializer.
   *
   * @return a legacy component serializer
   * @since 4.0.0
   */
  public static @NotNull LegacyComponentSerializer legacy() {
    return LEGACY_SERIALIZER;
  }

  /**
   * Gets the gson component serializer.
   *
   * <p>Not available on servers before 1.7.2, will be {@code null}.</p>
   *
   * @return a gson component serializer
   * @since 4.0.0
   */
  public static @NotNull GsonComponentSerializer gson() {
    return GSON_SERIALIZER;
  }
}
