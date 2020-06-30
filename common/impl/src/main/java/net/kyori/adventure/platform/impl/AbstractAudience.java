package net.kyori.adventure.platform.impl;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.AudienceInfo;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.UUID;

public abstract class AbstractAudience implements Audience, AudienceInfo {
    @Override
    public @Nullable Locale getLocale() {
        return null;
    }

    @Override
    public @Nullable UUID getId() {
        return null;
    }

    @Override
    public @Nullable Key getWorld() {
        return null;
    }

    @Override
    public @Nullable String getServer() {
        return null;
    }

    @Override
    public boolean hasPermission(@NonNull String permission) {
        return false;
    }

    @Override
    public boolean isConsole() {
        return false;
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public void sendMessage(@NonNull Component message) {}

    @Override
    public void sendActionBar(@NonNull Component message) {}

    @Override
    public void showTitle(@NonNull Title title) {}

    @Override
    public void clearTitle() {}

    @Override
    public void resetTitle() {}

    @Override
    public void showBossBar(@NonNull BossBar bar) {}

    @Override
    public void hideBossBar(@NonNull BossBar bar) {}

    @Override
    public void playSound(@NonNull Sound sound) {}

    @Override
    public void playSound(@NonNull Sound sound, double x, double y, double z) {}

    @Override
    public void stopSound(@NonNull SoundStop stop) {}

    @Override
    public void openBook(@NonNull Book book) {}
}
