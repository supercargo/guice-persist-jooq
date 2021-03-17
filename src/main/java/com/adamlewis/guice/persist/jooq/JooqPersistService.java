/*
 * Copyright 2014 Adam L. Lewis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.adamlewis.guice.persist.jooq;

import javax.sql.DataSource;
import java.sql.SQLException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Based on the JPA Persistence Service by Dhanji R. Prasanna (dhanji@gmail.com)
 *
 * @author Adam Lewis (github@adamlewis.com)
 */
@Singleton
class JooqPersistService implements Provider<DSLContext>, UnitOfWork, PersistService {

  private static final Logger logger = LoggerFactory.getLogger(JooqPersistService.class);

  private final ThreadLocal<DSLContext> threadFactory = new ThreadLocal<DSLContext>();
  private final ThreadLocal<DefaultConnectionProvider> threadConnection = new ThreadLocal<DefaultConnectionProvider>();
  private final Provider<DataSource> jdbcSource;
  private final SQLDialect sqlDialect;

  @Inject(optional = true)
  private Settings jooqSettings = null;

  @Inject(optional = true)
  private Configuration configuration = null;

  @Inject
  public JooqPersistService(final Provider<DataSource> jdbcSource, final SQLDialect sqlDialect) {
    this.jdbcSource = jdbcSource;
    this.sqlDialect = sqlDialect;
  }

  public DSLContext get() {
    DSLContext factory = threadFactory.get();
    if(null == factory) {
      throw new IllegalStateException("Requested Factory outside work unit. "
              + "Try calling UnitOfWork.begin() first, use @Transactional annotation"
              + "or use a PersistFilter if you are inside a servlet environment.");
    }

    return factory;
  }

  public DefaultConnectionProvider getConnectionWrapper() {
	  return threadConnection.get();
  }

  public boolean isWorking() {
    return threadFactory.get() != null;
  }

  public void begin() {
    if(null != threadFactory.get()) {
      throw new IllegalStateException("Work already begun on this thread. "
              + "It looks like you have called UnitOfWork.begin() twice"
              + " without a balancing call to end() in between.");
    }

    DefaultConnectionProvider conn;
    try {
      logger.debug("Getting JDBC connection");
      DataSource dataSource = jdbcSource.get();
      if (dataSource == null) {
        throw new RuntimeException("Datasource not available from provider");
      }
      conn = new DefaultConnectionProvider(dataSource.getConnection());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    DSLContext jooqFactory;

    if (configuration != null) {
      logger.debug("Creating factory from configuration having dialect {}", configuration.dialect());
      if (jooqSettings != null) {
        logger.warn("@Injected org.jooq.conf.Settings is being ignored since a full org.jooq.Configuration was supplied");
      }
      jooqFactory = DSL.using(configuration);
    } else {
      if (jooqSettings == null) {
        logger.debug("Creating factory with dialect {}", sqlDialect);
        jooqFactory = DSL.using(conn, sqlDialect);
      } else {
        logger.debug("Creating factory with dialect {} and settings.", sqlDialect);
        jooqFactory = DSL.using(conn, sqlDialect, jooqSettings);
      }
    }
    threadConnection.set(conn);
    threadFactory.set(jooqFactory);
  }

  public void end() {
	  DSLContext jooqFactory = threadFactory.get();
	  DefaultConnectionProvider conn = threadConnection.get();
    // Let's not penalize users for calling end() multiple times.
    if (null == jooqFactory) {
      return;
    }

    try {
      logger.debug("Closing JDBC connection");
      conn.acquire().close();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    threadFactory.remove();
    threadConnection.remove();
  }


  public synchronized void start() {
	  //nothing to do on start
  }

  public synchronized void stop() {
	  //nothing to do on stop
  }


}
