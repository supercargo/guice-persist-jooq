package com.adamlewis.guice.persist.jooq;

import java.util.*;

import com.adamlewis.guice.persist.jooq.modules.ConfigurationModule;
import com.adamlewis.guice.persist.jooq.modules.DataSourceModule;
import com.adamlewis.guice.persist.jooq.modules.SettingsModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.jooq.Configuration;
import org.jooq.conf.BackslashEscaping;
import org.junit.Before;
import org.junit.Test;

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
    jooqPersistService.begin();

    assertEquals(DataSourceModule.DEFAULT_DIALECT, jooqPersistService.get().configuration().dialect());
  }

  @Test
  public void canProvideAConfiguration() {
    JooqPersistService jooqPersistService = givenJooqPersistServiceWithModule(new ConfigurationModule());
    jooqPersistService.begin();

    assertEquals(injector.getInstance(Configuration.class), jooqPersistService.get().configuration());
  }

  @Test
  public void canProvideSettings() {
    JooqPersistService jooqPersistService = givenJooqPersistServiceWithModule(new SettingsModule());
    jooqPersistService.begin();

    // We can't assert on Settings.equals() because jooq clones the Settings instance and Settings does not override equals().
    assertEquals(SettingsModule.ESCAPING, jooqPersistService.get().settings().getBackslashEscaping());
  }

  @Test
  public void canProvideSettingsAndConfigurationButSettingsIsIgnored() {
    JooqPersistService jooqPersistService = givenJooqPersistServiceWithModule(new ConfigurationModule(), new SettingsModule());
    jooqPersistService.begin();

    assertEquals(injector.getInstance(Configuration.class), jooqPersistService.get().configuration());
    assertEquals(BackslashEscaping.DEFAULT, jooqPersistService.get().settings().getBackslashEscaping());
  }

  @Test(expected = IllegalStateException.class)
  public void throwsIfUnitOfWorkIsNotStarted() {
    JooqPersistService jooqPersistService = givenJooqPersistServiceWithModule();

    jooqPersistService.get();
  }

  private JooqPersistService givenJooqPersistServiceWithModule(Module... modules) {
    Set<Module> moduleList = new HashSet<Module>(Arrays.asList(modules));
    moduleList.add(new JooqPersistModule());
    moduleList.add(new DataSourceModule());
    injector = Guice.createInjector(moduleList);
    return injector.getInstance(JooqPersistService.class);
  }
}