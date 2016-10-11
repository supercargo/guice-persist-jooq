package com.adamlewis.guice.persist.jooq.modules;

import com.google.inject.AbstractModule;
import org.jooq.conf.BackslashEscaping;
import org.jooq.conf.Settings;

public class SettingsModule extends AbstractModule {
  public static final BackslashEscaping ESCAPING = BackslashEscaping.OFF;

  protected void configure() {
    Settings settings= new Settings();
    settings.setBackslashEscaping(ESCAPING);
    binder().bind(Settings.class).toInstance(settings);
  }
}
