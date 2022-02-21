package com.adamlewis.guice.persist.jooq;

import java.sql.Connection;
import java.util.*;

import com.adamlewis.guice.persist.jooq.modules.ConfigurationModule;
import com.adamlewis.guice.persist.jooq.modules.DataSourceModule;
import com.adamlewis.guice.persist.jooq.modules.SettingsModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import javax.sql.DataSource;
import org.jooq.Configuration;
import org.jooq.conf.BackslashEscaping;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
  public void canProvideAConfiguration() throws Exception {
    JooqPersistService jooqPersistService = givenJooqPersistServiceWithModule(new ConfigurationModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    Connection connectionMock = mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connectionMock);

    jooqPersistService.begin();

    Configuration configuration = injector.getInstance(Configuration.class);
    Configuration transactionConfiguration = jooqPersistService.get().configuration();
    assertNotNull(transactionConfiguration);
    // the connection must be provided for each transaction configuration hence it will differ from default configuration
    assertNotEquals(configuration, transactionConfiguration);
    assertEquals(configuration.dialect(), transactionConfiguration.dialect());
    assertEquals(configuration.settings(), transactionConfiguration.settings());
    assertEquals(connectionMock, transactionConfiguration.connectionProvider().acquire());
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

    Configuration configuration = injector.getInstance(Configuration.class);
    Configuration transactionConfiguration = jooqPersistService.get().configuration();
    assertNotNull(transactionConfiguration);
    assertNotEquals(configuration, transactionConfiguration);
    assertEquals(configuration.dialect(), transactionConfiguration.dialect());
    assertEquals(configuration.settings(), transactionConfiguration.settings());
    assertEquals(BackslashEscaping.DEFAULT, transactionConfiguration.settings().getBackslashEscaping());
  }

  @Test(expected = IllegalStateException.class)
  public void throwsIfUnitOfWorkIsNotStarted() {
    JooqPersistService jooqPersistService = givenJooqPersistServiceWithModule();

    jooqPersistService.get();
  }

  private JooqPersistService givenJooqPersistServiceWithModule(Module... modules) {
    Set<Module> moduleList = new HashSet<>(Arrays.asList(modules));
    moduleList.add(new JooqPersistModule());
    moduleList.add(new DataSourceModule());
    injector = Guice.createInjector(moduleList);
    return injector.getInstance(JooqPersistService.class);
  }
}