package net.kyori.adventure.platform.bukkit;

import net.kyori.adventure.sound.Sound;
import org.bukkit.SoundCategory;
import org.checkerframework.checker.nullness.qual.NonNull;

final class BukkitPlatform {
  static SoundCategory category(final Sound.@NonNull Source source) {
    switch(source) {
      case MASTER: return SoundCategory.MASTER;
      case MUSIC: return SoundCategory.MUSIC;
      case RECORD: return SoundCategory.RECORDS;
      case WEATHER: return SoundCategory.WEATHER;
      case BLOCK: return SoundCategory.BLOCKS;
      case HOSTILE: return SoundCategory.HOSTILE;
      case NEUTRAL: return SoundCategory.NEUTRAL;
      case PLAYER: return SoundCategory.PLAYERS;
      case AMBIENT: return SoundCategory.AMBIENT;
      case VOICE: return SoundCategory.VOICE;
      default: throw new IllegalArgumentException("Unknown sound source " + source);
    }
  }
}
