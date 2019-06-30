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
package net.kyori.text.adapter.bukkit;

import org.bukkit.Bukkit;

@SuppressWarnings("SpellCheckingInspection")
final class Shiny {
  private static final String PAKACGE_OBC = "org.bukkit.craftbukkit";
  private static final String PAKACGE_NMS = "net.minecraft.server";
  private static final String VERSION = getVersion();

  static boolean isCompatibleServer() {
    final Class<?> server = Bukkit.getServer().getClass();
    return server.getPackage().getName().startsWith(PAKACGE_OBC) && server.getSimpleName().equals("CraftServer");
  }

  private static String getVersion() {
    final Class<?> server = Bukkit.getServer().getClass();
    final String version = server.getPackage().getName().substring("org.bukkit.craftbukkit".length());
    if(version.isEmpty()) {
      return "";
    } else if(version.charAt(0) == '.') {
      return version.substring(1) + '.';
    }
    throw new IllegalArgumentException("Unknown version " + version);
  }

  static Class<?> craftClass(final String name) throws ClassNotFoundException {
    return Class.forName(getPackage(PAKACGE_OBC) + name);
  }

  static Class<?> vanillaClass(final String name) throws ClassNotFoundException {
    return Class.forName(getPackage(PAKACGE_NMS) + name);
  }

  static Class<?> maybeVanillaClass(final String name) {
    try {
      return vanillaClass(name);
    } catch(final ClassNotFoundException e) {
      return null;
    }
  }

  private static String getPackage(final String type) {
    return type + '.' + VERSION;
  }
}
