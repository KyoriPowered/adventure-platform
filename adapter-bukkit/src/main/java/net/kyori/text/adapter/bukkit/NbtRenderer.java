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

import java.lang.reflect.Method;
import java.util.Optional;
import net.kyori.text.BlockNbtComponent;
import net.kyori.text.Component;
import net.kyori.text.EntityNbtComponent;
import net.kyori.text.renderer.AbstractDeepComponentRenderer;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class NbtRenderer extends AbstractDeepComponentRenderer<Entity> {
  static final NbtRenderer RENDERER = tryMake();
  private final Class<?> chatComponentContextualClass;
  private final Method handle;
  private final Method render;
  private final Method createStack;

  private static @Nullable NbtRenderer tryMake() {
    try {
      final Class<?> chatComponentContextualClass = Shiny.vanillaClass("ChatComponentContextual");
      final Class<?> commandListenerWrapperClass = Shiny.vanillaClass("CommandListenerWrapper");
      final Class<?> entityClass = Shiny.vanillaClass("Entity");
      final Method handle = Shiny.craftClass("entity.CraftEntity").getMethod("getHandle");
      final Method renderMethod = chatComponentContextualClass.getMethod("a", commandListenerWrapperClass, entityClass, int.class);
      final Method createStackMethod = entityClass.getMethod("getCommandListener");
      return new NbtRenderer(chatComponentContextualClass, handle, renderMethod, createStackMethod);
    } catch(final Throwable t) {
      return null;
    }
  }

  private NbtRenderer(final Class<?> chatComponentContextualClass, final Method handle, final Method render, final Method createStack) {
    this.chatComponentContextualClass = chatComponentContextualClass;
    this.handle = handle;
    this.render = render;
    this.createStack = createStack;
  }

  public static Component tryRender(final Component component, final Entity context) {
    if(RENDERER != null) {
      return RENDERER.render(component, context);
    }
    return component;
  }

  @Override
  protected @NonNull Component render(@NonNull final BlockNbtComponent component, @NonNull final Entity context) {
    Object vanilla = CraftBukkitAdapter.BINDING.asVanilla(component);
    vanilla = this.render(context, vanilla);
    return Optional.ofNullable(CraftBukkitAdapter.BINDING.asKyori(vanilla)).orElse(component);
  }

  @Override
  protected @NonNull Component render(@NonNull final EntityNbtComponent component, @NonNull final Entity context) {
    Object vanilla = CraftBukkitAdapter.BINDING.asVanilla(component);
    vanilla = this.render(context, vanilla);
    return Optional.ofNullable(CraftBukkitAdapter.BINDING.asKyori(vanilla)).orElse(component);
  }

  private Object render(final Entity viewer, final Object component) {
    if(this.chatComponentContextualClass.isInstance(component)) {
      try {
        final Object stack = this.createStack.invoke(this.handle.invoke(viewer));
        return this.render.invoke(component, stack, null, 0);
      } catch(final Throwable t) {
        // TODO
      }
    }
    return component;
  }
}
