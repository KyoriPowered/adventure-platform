package net.kyori.adventure.text.serializer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import static java.util.Objects.requireNonNull;

public final class SpongeComponentSerializer implements ComponentSerializer<Component, Component, Text> {
    public static final SpongeComponentSerializer INSTANCE = new SpongeComponentSerializer();

    @NonNull
    @Override
    public Component deserialize(@NonNull Text input) {
        return GsonComponentSerializer.INSTANCE.deserialize(TextSerializers.JSON.serialize(requireNonNull(input, "text")));
    }

    @NonNull
    @Override
    public Text serialize(@NonNull Component component) {
        return TextSerializers.JSON.deserialize(VersionedGsonComponentSerializer.PRE_1_16.serialize(requireNonNull(component, "component")));
    }

}
