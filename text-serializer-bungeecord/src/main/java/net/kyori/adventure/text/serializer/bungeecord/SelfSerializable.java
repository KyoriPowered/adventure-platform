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
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

/**
 * A representation of a wrapper adapter, generally between and Adventure type and a native type.
 */
interface SelfSerializable {

  /**
   * Write this object to a json writer
   *
   * @param out writer to write to
   * @throws IOException when any serialization-related error occurs
   */
  void write(JsonWriter out) throws IOException;

  /**
   * Gson adapter factory that will appropriately unwrap wrapped values
   */
  class AdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
      if(!SelfSerializable.class.isAssignableFrom(type.getRawType())) {
        return null;
      }

      final TypeAdapter<T> delegated = gson.getDelegateAdapter(this, type);
      return new TypeAdapter<T>() {
        @Override
        public void write(final JsonWriter out, final T value) throws IOException {
          ((SelfSerializable) value).write(out);
        }

        @Override
        public T read(final JsonReader in) throws IOException {
          return delegated.read(in);
        }
      };
    }
  }
}
