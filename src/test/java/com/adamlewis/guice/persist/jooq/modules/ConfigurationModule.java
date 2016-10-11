package com.adamlewis.guice.persist.jooq.modules;

import com.google.inject.AbstractModule;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

public class ConfigurationModule extends AbstractModule {
  public static final SQLDialect DIALECT = SQLDialect.CUBRID;

  protected void configure() {
    DefaultConfiguration configuration = new DefaultConfiguration();
    configuration.setSQLDialect(DIALECT);
    binder().bind(Configuration.class).toInstance(configuration);
  }
}
