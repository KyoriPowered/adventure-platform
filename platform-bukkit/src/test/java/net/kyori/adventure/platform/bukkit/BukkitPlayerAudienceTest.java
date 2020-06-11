package net.kyori.adventure.platform.bukkit;

import java.util.Locale;
import org.junit.jupiter.api.Test;

import static net.kyori.adventure.platform.bukkit.BukkitPlayerAudience.toLocale;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BukkitPlayerAudienceTest {
  @Test
  public void testLocaleFromEmptyStringOrNullIsDefault() {
    assertEquals(Locale.getDefault(), toLocale(""));
    assertEquals(Locale.getDefault(), toLocale(null));
  }

  @Test
  public void testStandardLocales() {
    assertEquals(Locale.ENGLISH, toLocale("en"));
    assertEquals(Locale.CANADA_FRENCH, toLocale("fr_ca"));
    assertEquals(new Locale("en", "UK", "basiceng"), toLocale("en_uk_basiceng"));
  }

  @Test
  public void testImaginaryLocales() {
    assertEquals(new Locale("en", "ud"), toLocale("en_ud")); // upside down
    assertEquals(new Locale("enws"), toLocale("enws")); // shakespearean
  }

  @Test
  public void testInvalidSyntaxAccepted() {
    assertEquals(new Locale("hello world"), toLocale("hello world"));
    assertEquals(new Locale("", "", "___"), toLocale("_____"));
  }

}
