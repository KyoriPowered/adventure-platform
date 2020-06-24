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
package net.kyori.adventure.platform.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.VersionedGsonComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VersionedGsonComponentSerializerTest {

  @Test
  public void testSerializesPlain() {
    final Component text = TextComponent.of("honk");
    assertEquals("{\"text\":\"honk\"}", VersionedGsonComponentSerializer.MODERN.serialize(text));
    assertEquals("{\"text\":\"honk\"}", VersionedGsonComponentSerializer.PRE_1_16.serialize(text));
  }

  @Test
  public void testModernPassthrough() {
    final TextColor original = TextColor.of(0xAB2211);
    final Component test = TextComponent.of("meow", original);
    assertEquals("{\"text\":\"meow\",\"color\":\"#ab2211\"}", VersionedGsonComponentSerializer.MODERN.serialize(test));
  }

  @Test
  public void testPre116Downsamples() {
    final TextColor original = TextColor.of(0xAB2211);
    final NamedTextColor downsampled = NamedTextColor.nearestTo(original);
    final Component test = TextComponent.of("meow", original);
    assertEquals("{\"text\":\"meow\",\"color\":\"" + name(downsampled) + "\"}", VersionedGsonComponentSerializer.PRE_1_16.serialize(test));
  }

  @Test
  public void testPre116DownsamplesInChildren() {
    final TextColor original = TextColor.of(0xEC41AA);
    final NamedTextColor downsampled = NamedTextColor.nearestTo(original);
    final Component test = TextComponent.make("hey", builder -> builder.append(TextComponent.of("there", original)));

    assertEquals("{\"text\":\"hey\",\"extra\":[{\"text\":\"there\",\"color\":\"" + name(downsampled) + "\"}]}", VersionedGsonComponentSerializer.PRE_1_16.serialize(test));
  }

  private static String name(NamedTextColor color) {
    return NamedTextColor.NAMES.key(color);
  }
}
