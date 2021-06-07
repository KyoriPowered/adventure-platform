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
package net.kyori.adventure.text.serializer.bungeecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Excluder;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.NonNull;

final class GsonInjections {
  private GsonInjections() {
  }

  /**
   * Get a field from {@code klass} and make it accessible .
   *
   * @param klass containing class
   * @param name field name
   * @return the field
   * @throws NoSuchFieldException when thrown by {@link Class#getDeclaredField(String)}
   */
  public static Field field(final @NonNull Class<?> klass, final @NonNull String name) throws NoSuchFieldException {
    final Field field = klass.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  // Gson //

  @SuppressWarnings("unchecked")
  public static boolean injectGson(final @NonNull Gson existing, final @NonNull Consumer<GsonBuilder> accepter) {
    try {
      final Field factoriesField = field(Gson.class, "factories");
      final Field builderFactoriesField = field(GsonBuilder.class, "factories");
      final Field builderHierarchyFactoriesField = field(GsonBuilder.class, "hierarchyFactories");

      final GsonBuilder builder = new GsonBuilder();
      accepter.accept(builder);

      final List<TypeAdapterFactory> existingFactories = (List<TypeAdapterFactory>) factoriesField.get(existing);
      final List<TypeAdapterFactory> newFactories = new ArrayList<>();
      newFactories.addAll((List<TypeAdapterFactory>) builderFactoriesField.get(builder));
      Collections.reverse(newFactories);
      newFactories.addAll((List<TypeAdapterFactory>) builderHierarchyFactoriesField.get(builder));

      final List<TypeAdapterFactory> modifiedFactories = new ArrayList<>(existingFactories);

      // the excluder must precede all adapters that handle user-defined types
      final int index = findExcluderIndex(modifiedFactories);

      Collections.reverse(newFactories);
      for(final TypeAdapterFactory newFactory : newFactories) {
        modifiedFactories.add(index, newFactory);
      }

      factoriesField.set(existing, modifiedFactories);
      return true;
    } catch(final NoSuchFieldException | IllegalAccessException ex) {
      return false;
    }
  }

  private static int findExcluderIndex(final @NonNull List<TypeAdapterFactory> factories) {
    for(int i = 0, size = factories.size(); i < size; i++) {
      final TypeAdapterFactory factory = factories.get(i);
      if(factory instanceof Excluder) {
        return i + 1;
      }
    }
    return 0;
  }
}
