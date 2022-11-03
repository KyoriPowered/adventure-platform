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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetBase;
import net.kyori.adventure.sound.Sound.Emitter;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.util.Index;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findMethod;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findStaticMethod;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.hasField;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.lookup;
import static net.kyori.adventure.platform.facet.Knob.isEnabled;
import static net.kyori.adventure.platform.facet.Knob.logError;

class PaperFacet extends FacetBase<Object /* (native) Audience */> {
  static final Class<?> NATIVE_COMPONENT_CLASS = findClass(nativeClazz("text.Component"));
  private static final Class<?> NATIVE_AUDIENCE_CLASS = findClass(nativeClazz("audience.Audience"));
  private static final boolean SUPPORTED = isEnabled("paper", true) && !Component.class.equals(NATIVE_COMPONENT_CLASS);
  private static final Class<?> NATIVE_GSON_COMPONENT_SERIALIZER_CLASS = findClass(nativeClazz("text.serializer.gson.GsonComponentSerializer"));
  private static final Class<?> NATIVE_KEY_CLASS = findClass(nativeClazz("key.Key"));
  private static final MethodHandle NATIVE_GSON_COMPONENT_SERIALIZER_GSON_GETTER = findStaticMethod(NATIVE_GSON_COMPONENT_SERIALIZER_CLASS, "gson", NATIVE_GSON_COMPONENT_SERIALIZER_CLASS);
  private static final MethodHandle NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD = findNativeDeserializeMethod();
  private static final MethodHandle NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND = createBoundNativeDeserializeMethodHandle();
  private static final MethodHandle NATIVE_KEY_FACTORY = findStaticMethod(NATIVE_KEY_CLASS, "key", NATIVE_KEY_CLASS, String.class, String.class);

  private static @Nullable MethodHandle findNativeDeserializeMethod() {
    if (NATIVE_GSON_COMPONENT_SERIALIZER_CLASS == null) return null;

    try {
      final Method method = NATIVE_GSON_COMPONENT_SERIALIZER_CLASS.getDeclaredMethod("deserialize", String.class);
      method.setAccessible(true);
      return lookup().unreflect(method);
    } catch (final NoSuchMethodException | IllegalAccessException ex) {
      return null;
    }
  }

  private static @Nullable MethodHandle createBoundNativeDeserializeMethodHandle() {
    if (NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD != null) {
      try {
        return NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD.bindTo(NATIVE_GSON_COMPONENT_SERIALIZER_GSON_GETTER.invoke());
      } catch (final Throwable throwable) {
        logError(throwable, "Failed to access native GsonComponentSerializer");
        return null;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static <T extends Enum<T>> Index<T, Object> createNativeEnumMapping(final Class<T> ourClass, final Class<?> nativeClass) {
    if (nativeClass == null) {
      return null;
    }
    return Index.create(nativeClass.asSubclass(Enum.class), k -> Enum.valueOf(ourClass, k.name()));
  }

  private static String nativeClazz(final String relativeName) {
    return String.join(".", "net", "kyori", "adventure", relativeName);
  }

  protected PaperFacet() {
    this(NATIVE_AUDIENCE_CLASS);
  }

  protected PaperFacet(final @Nullable Class<?> viewerClass) {
    super(viewerClass);
  }

  @Override
  public boolean isSupported() {
    return super.isSupported() && SUPPORTED;
  }

  public final @Nullable Object createMessage(final @NotNull Object viewer, final @NotNull Component message) {
    return componentToNative(message);
  }

  // Convert

  static Object componentToNative(final Component component) {
    if (NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND != null) {
      try {
        return NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND.invoke(GsonComponentSerializer.gson().serialize(component));
      } catch (final Throwable throwable) {
        logError(throwable, "Failed to create native Component message");
      }
    }
    return null;
  }

  static Object keyToNative(final Key key) {
    try {
      return NATIVE_KEY_FACTORY.invoke(key.namespace(), key.value());
    } catch (final Throwable ex) {
        logError(ex, "Failed to create native Key message");
        return null;
    }
  }

  // Facet implementations

  static final class Chat extends PaperFacet implements Facet.Chat<Object, Object> {
    private static final Class<?> NATIVE_MESSAGE_TYPE = findClass(nativeClazz("audience.MessageType"));
    private static final Index<MessageType, Object> MESSAGE_TYPE_TO_NATIVE = createNativeEnumMapping(MessageType.class, NATIVE_MESSAGE_TYPE);
    private static final Class<?> NATIVE_IDENTITY_TYPE = findClass(nativeClazz("identity.Identity"));
    private static final MethodHandle AUDIENCE_SEND_MESSAGE = findMethod(NATIVE_AUDIENCE_CLASS, "sendMessage", void.class, NATIVE_IDENTITY_TYPE, NATIVE_COMPONENT_CLASS, NATIVE_MESSAGE_TYPE);

    @Override
    public boolean isSupported() {
      return super.isSupported() && AUDIENCE_SEND_MESSAGE != null && MESSAGE_TYPE_TO_NATIVE != null;
    }

    @Override
    public void sendMessage(final @NotNull Object viewer, final @NotNull Identity source, final @NotNull Object message, final @NotNull MessageType type) {
      try {
        AUDIENCE_SEND_MESSAGE.invoke(viewer, source, message, MESSAGE_TYPE_TO_NATIVE.value(type));
      } catch (final Throwable ex) {
        logError(ex, "While sending message to {}", viewer);
      }
    }
  }

  static final class ActionBar extends PaperFacet implements Facet.ActionBar<Object, Object> {
    private static final MethodHandle AUDIENCE_SEND_ACTION_BAR = findMethod(NATIVE_AUDIENCE_CLASS, "sendActionBar", void.class, NATIVE_COMPONENT_CLASS);

    ActionBar() {
    }

    @Override
    public boolean isSupported() {
      return super.isSupported() && AUDIENCE_SEND_ACTION_BAR != null;
    }

    @Override
    public void sendMessage(final @NotNull Object viewer, final @NotNull Object message) {
      try {
        AUDIENCE_SEND_ACTION_BAR.invoke(viewer, message);
      } catch (final Throwable ex) {
        logError(ex, "Failed to send actuon bar to {}", viewer);
      }
    }
  }

  static final class Title extends PaperFacet implements Facet.Title<Object, Object, List<Consumer<Object>>, List<Consumer<Object>>> {
    private static final Class<?> NATIVE_TITLE_PART_CLASS = findClass(nativeClazz("title.TitlePart"));
    private static final Class<?> NATIVE_TITLE_TIMES_CLASS = findClass(nativeClazz("title.Title$Times"));
    private static final MethodHandle AUDIENCE_SEND_TITLE_PART = findMethod(NATIVE_AUDIENCE_CLASS, "sendTitlePart", void.class, NATIVE_TITLE_PART_CLASS, Object.class);
    private static final MethodHandle AUDIENCE_CLEAR_TITLE = findMethod(NATIVE_AUDIENCE_CLASS, "clearTitle", void.class);
    private static final MethodHandle AUDIENCE_RESET_TITLE = findMethod(NATIVE_AUDIENCE_CLASS, "resetTitle", void.class);
    private static final MethodHandle TITLE_TIMES_TIMES = findStaticMethod(NATIVE_TITLE_TIMES_CLASS, "times", NATIVE_TITLE_TIMES_CLASS, Duration.class, Duration.class, Duration.class);
    private static final Object TITLE_PART_TITLE = MinecraftReflection.findConstantFieldValue(NATIVE_TITLE_PART_CLASS, "TITLE");
    private static final Object TITLE_PART_SUBTITLE = MinecraftReflection.findConstantFieldValue(NATIVE_TITLE_PART_CLASS, "SUBTITLE");
    private static final Object TITLE_PART_TIMES = MinecraftReflection.findConstantFieldValue(NATIVE_TITLE_PART_CLASS, "TIMES");

    @Override
    public boolean isSupported() {
      return super.isSupported() && TITLE_PART_TITLE != null && AUDIENCE_SEND_TITLE_PART != null && AUDIENCE_CLEAR_TITLE != null && AUDIENCE_RESET_TITLE != null && TITLE_TIMES_TIMES != null;
    }

    @Override
    public @NotNull List<Consumer<Object>> createTitleCollection() {
      return new ArrayList<>(3);
    }

    private void addSendTitlePart(final List<Consumer<Object>> items, final Object titlePart, final Object value) {
      items.add(viewer -> {
        try {
          AUDIENCE_SEND_TITLE_PART.invoke(viewer, titlePart, value);
        } catch (final Throwable ex) {
          logError(ex, "Failed to send title part {} to {}", titlePart, viewer);
        }
      });
    }

    @Override
    public void contributeTitle(final @NotNull List<Consumer<Object>> coll, final @NotNull Object title) {
      this.addSendTitlePart(coll, TITLE_PART_TITLE, title);
    }

    @Override
    public void contributeSubtitle(final @NotNull List<Consumer<Object>> coll, final @NotNull Object subtitle) {
      this.addSendTitlePart(coll, TITLE_PART_SUBTITLE, subtitle);
    }

    @Override
    public void contributeTimes(final @NotNull List<Consumer<Object>> coll, final int inTicks, final int stayTicks, final int outTicks) {
      Object nativeTimes;
      try {
        nativeTimes = TITLE_TIMES_TIMES.invoke(
          Duration.ofSeconds(inTicks / 20),
          Duration.ofSeconds(stayTicks / 20),
          Duration.ofSeconds(outTicks / 20)
        );
      } catch (final Throwable ex) {
        logError(ex, "Failed to create a title times instance", ex);
        return;
      }

      this.addSendTitlePart(coll, TITLE_PART_TIMES, nativeTimes);
    }

    @Override
    public @Nullable List<Consumer<Object>> completeTitle(final @NotNull List<Consumer<Object>> coll) {
      return coll;
    }

    @Override
    public void showTitle(final @NotNull Object viewer, final @NotNull List<Consumer<Object>> title) {
      for (final Consumer<Object> run : title) {
        run.accept(viewer);
      }
    }

    @Override
    public void clearTitle(final @NotNull Object viewer) {
      try {
        AUDIENCE_CLEAR_TITLE.invoke(viewer);
      } catch (final Throwable ex) {
        logError(ex, "Failed to clear title");
      }
    }

    @Override
    public void resetTitle(final @NotNull Object viewer) {
      try {
        AUDIENCE_RESET_TITLE.invoke(viewer);
      } catch (final Throwable ex) {
        logError(ex, "Failed to reset title");
      }
    }
  }

  static final class Sound extends PaperFacet implements Facet.Sound<Object, Vector> {
    private static final Class<?> NATIVE_SOUND = findClass(nativeClazz("sound.Sound"));
    private static final Class<?> NATIVE_SOUND_STOP = findClass(nativeClazz("sound.SoundStop"));
    private static final Class<?> NATIVE_SOUND_SOURCE_CLASS = findClass(nativeClazz("sound.Sound$Source"));
    private static final Index<net.kyori.adventure.sound.Sound.Source, Object> SOURCE_TO_NATIVE_SOURCE = createNativeEnumMapping(net.kyori.adventure.sound.Sound.Source.class, NATIVE_SOUND_SOURCE_CLASS);

    private static final MethodHandle AUDIENCE_PLAY_SOUND = findMethod(NATIVE_AUDIENCE_CLASS, "playSound", void.class, NATIVE_SOUND, double.class, double.class, double.class);
    private static final MethodHandle AUDIENCE_STOP_SOUND = findMethod(NATIVE_AUDIENCE_CLASS, "stopSound", void.class, NATIVE_SOUND_STOP);
    private static final MethodHandle SOUND_SOUND = findStaticMethod(NATIVE_SOUND, "sound", NATIVE_SOUND, NATIVE_KEY_CLASS, NATIVE_SOUND_SOURCE_CLASS, float.class, float.class);
    private static final MethodHandle SOUND_STOP_NAMED = findStaticMethod(NATIVE_SOUND_STOP, "named", NATIVE_SOUND_STOP, NATIVE_KEY_CLASS);
    private static final MethodHandle SOUND_STOP_SOURCE = findStaticMethod(NATIVE_SOUND_STOP, "source", NATIVE_SOUND_STOP, NATIVE_SOUND_SOURCE_CLASS);
    private static final MethodHandle SOUND_STOP_NAMED_ON_SOURCE = findStaticMethod(NATIVE_SOUND_SOURCE_CLASS, "namedOnSource", NATIVE_SOUND_STOP, NATIVE_KEY_CLASS, NATIVE_SOUND_SOURCE_CLASS);

    private static final Object SOUND_STOP_ALL;

    static {
      Object soundStopAll = null;
      try {
        soundStopAll = findStaticMethod(NATIVE_SOUND_STOP, "all", NATIVE_SOUND_STOP).invoke();
      } catch (final Throwable ex) {
        // no-op
      }

      SOUND_STOP_ALL = soundStopAll;
    }

    @Override
    public boolean isSupported() {
      return super.isSupported() && SOUND_SOUND != null && AUDIENCE_PLAY_SOUND != null && AUDIENCE_STOP_SOUND != null;
    }

    @Override
    public @Nullable Vector createPosition(final @NotNull Object viewer) {
      if (viewer instanceof Entity) {
        return ((Entity) viewer).getLocation().toVector();
      }
      return null;
    }

    @Override
    public @NotNull Vector createPosition(final double x, final double y, final double z) {
      return new Vector(x, y, z);
    }

    @Override
    public void playSound(final @NotNull Object viewer, final net.kyori.adventure.sound.@NotNull Sound sound, final @NotNull Vector position) {
      try {
        final Object nativeSound = SOUND_SOUND.invoke(
          keyToNative(sound.name()),
          SOURCE_TO_NATIVE_SOURCE.value(sound.source()),
          sound.volume(),
          sound.pitch()
        );

        AUDIENCE_PLAY_SOUND.invoke(viewer, nativeSound, position.getX(), position.getY(), position.getZ());
      } catch (final Throwable ex) {
        logError(ex, "Failed to play sound {}", sound);
      }
    }

    @Override
    public void stopSound(final @NotNull Object viewer, final @NotNull SoundStop sound) {
      final boolean hasSound = sound.sound() != null;
      final boolean hasSource = sound.source() != null;

      final Object stop;
      try {
        if (hasSound && hasSource) {
          stop = SOUND_STOP_NAMED_ON_SOURCE.invoke(keyToNative(sound.sound()), SOURCE_TO_NATIVE_SOURCE.value(sound.source()));
        } else if (hasSound) {
          stop = SOUND_STOP_NAMED.invoke(keyToNative(sound.sound()));
        } else if (hasSource) {
          stop = SOUND_STOP_SOURCE.invoke(SOURCE_TO_NATIVE_SOURCE.value(sound.source()));
        } else {
          stop = SOUND_STOP_ALL;
        }

        AUDIENCE_STOP_SOUND.invoke(viewer, stop);
      } catch (final Throwable ex) {
        logError(ex, "Failed to stop sound {}", sound);
      }
    }
  }

  static final class EntitySound extends PaperFacet implements Facet.EntitySound<Object, MethodHandle> {
    @Override
    public MethodHandle createForSelf(final Object viewer, final net.kyori.adventure.sound.@NotNull Sound sound) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public MethodHandle createForEmitter(final net.kyori.adventure.sound.@NotNull Sound sound, final @NotNull Emitter emitter) {
      if (emitter instanceof BukkitEmitter) {
        final Entity entity = ((BukkitEmitter) emitter).entity;
        // AUDIENCE_PLAY_SOUND(viewer, entity, sound)
      }
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void playSound(final @NotNull Object viewer, final MethodHandle message) {
      // TODO Auto-generated method stub
    }
  }

  static final class Book extends PaperFacet implements Facet.Book<Object, Object, Object> {
    @Override
    public @Nullable Object createBook(final @NotNull String title, final @NotNull String author, final @NotNull Iterable<Object> pages) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void openBook(final @NotNull Object viewer, final @NotNull Object book) {
      // TODO Auto-generated method stub

    }

  }

  static final class BossBarBuilder extends PaperFacet implements Facet.BossBar.Builder<Object, BossBar> {
    @Override
    public @NotNull PaperFacet.BossBar createBossBar(final @NotNull Collection<Object> viewer) {
      return new PaperFacet.BossBar(viewer);
    }
  }

  static final class BossBar extends PaperFacet implements Facet.BossBar<Object> {
    BossBar(final Collection<Object> viewer) {

    }

    @Override
    public void addViewer(final @NotNull Object viewer) {
      // TODO Auto-generated method stub

    }

    @Override
    public void removeViewer(final @NotNull Object viewer) {
      // TODO Auto-generated method stub

    }

    @Override
    public boolean isEmpty() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public void close() {
      // TODO Auto-generated method stub

    }
  }

  static class TabList extends CraftBukkitFacet.TabList {
    private static final boolean SUPPORTED = hasField(CLASS_CRAFT_PLAYER, NATIVE_COMPONENT_CLASS, "playerListHeader") && hasField(CLASS_CRAFT_PLAYER, NATIVE_COMPONENT_CLASS, "playerListFooter");
    private static final MethodHandle NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND = createBoundNativeDeserializeMethodHandle();

    private static @Nullable MethodHandle createBoundNativeDeserializeMethodHandle() {
      if (SUPPORTED) {
        try {
          return NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD.bindTo(NATIVE_GSON_COMPONENT_SERIALIZER_GSON_GETTER.invoke());
        } catch (final Throwable throwable) {
          logError(throwable, "Failed to access native GsonComponentSerializer");
          return null;
        }
      }
      return null;
    }

    @Override
    public boolean isSupported() {
      return SUPPORTED && super.isSupported() && CLIENTBOUND_TAB_LIST_PACKET_SET_HEADER != null && CLIENTBOUND_TAB_LIST_PACKET_SET_FOOTER != null;
    }

    @Override
    protected Object create117Packet(final Player viewer, final @Nullable Object header, final @Nullable Object footer) throws Throwable {
      final Object packet = CLIENTBOUND_TAB_LIST_PACKET_CTOR.invoke(null, null);
      CLIENTBOUND_TAB_LIST_PACKET_SET_HEADER.invoke(packet, header == null ? this.createMessage(viewer, Component.empty()) : header);
      CLIENTBOUND_TAB_LIST_PACKET_SET_FOOTER.invoke(packet, footer == null ? this.createMessage(viewer, Component.empty()) : footer);
      return packet;
    }

    @Override
    public @Nullable Object createMessage(final @NotNull Player viewer, final @NotNull Component message) {
      try {
        return NATIVE_GSON_COMPONENT_SERIALIZER_DESERIALIZE_METHOD_BOUND.invoke(GsonComponentSerializer.gson().serialize(message));
      } catch (final Throwable throwable) {
        logError(throwable, "Failed to create native Component message");
        return null;
      }
    }
  }
}
