package com.adamlewis.guice.persist.jooq;

import com.adamlewis.guice.persist.jooq.modules.ConfigurationModule;
import com.adamlewis.guice.persist.jooq.modules.DataSourceModule;
import com.adamlewis.guice.persist.jooq.modules.SettingsModule;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.jooq.Configuration;
import org.jooq.conf.BackslashEscaping;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class JooqPersistServiceTest {

  private Injector injector;

  @Before
  public void setup() {
    injector = null;
  }

  @Test
  public void canCreateWithoutConfiguration() {
    JooqPersistService jooqPersistService = givenJooqPersistServiceWithModule();

    assertEquals(DataSourceModule.DEFAULT_DIALECT, jooqPersistService.get().configuration().dialect());
  }

  @Test
  public void canProvideAConfiguration() {
    JooqPersistService jooqPersistService = givenJooqPersistServiceWithModule(new ConfigurationModule());

    assertEquals(injector.getInstance(Configuration.class), jooqPersistService.get().configuration());
  }

  @Test
  public void canProvideSettings() {
    JooqPersistService jooqPersistService = givenJooqPersistServiceWithModule(new SettingsModule());

    // We can't assert on Settings.equals() because jooq clones the Settings instance and Settings does not override equals().
    assertEquals(SettingsModule.ESCAPING, jooqPersistService.get().settings().getBackslashEscaping());
  }

  @Test
  public void canProvideSettingsAndConfigurationButSettingsIsIgnored() {
    JooqPersistService jooqPersistService = givenJooqPersistServiceWithModule(new ConfigurationModule(), new SettingsModule());

    assertEquals(injector.getInstance(Configuration.class), jooqPersistService.get().configuration());
    assertEquals(BackslashEscaping.DEFAULT, jooqPersistService.get().settings().getBackslashEscaping());
  }

  private JooqPersistService givenJooqPersistServiceWithModule(Module... modules) {
    Set<Module> moduleList = Sets.newHashSet(modules);
    moduleList.add(new JooqPersistModule());
    moduleList.add(new DataSourceModule());
    injector = Guice.createInjector(moduleList);
    return injector.getInstance(JooqPersistService.class);
  }
}