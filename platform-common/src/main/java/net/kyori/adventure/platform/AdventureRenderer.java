package net.kyori.adventure.platform;

import net.kyori.adventure.platform.audience.AdventurePlayerAudience;
import net.kyori.adventure.platform.audience.AdventureAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Comparator;

/**
 * A component renderer that customizes text for an {@link AdventureAudience}.
 */
public interface AdventureRenderer extends ComponentRenderer<AdventureAudience>, Comparator<AdventureAudience> {

  /**
   * Customize text for an audience.
   *
   * <p>Audience may also be an instance of {@link AdventurePlayerAudience}.</p>
   *
   * @param component a component
   * @param audience an audience
   * @return a customized component
   */
  @Override
  @NonNull Component render(@NonNull Component component, @NonNull AdventureAudience audience);

  /**
   * Determine whether any two audiences are considered "equivalent".
   *
   * <p>Can be used to cache text for a group of audiences, such as by locale.</p>
   *
   * @param a1 an audience
   * @param a2 another audience
   * @return {@code 0} if both audiences are "equivalent", {@code 1} or {@code -1} otherwise
   */
  @Override
  int compare(AdventureAudience a1, AdventureAudience a2);
}
