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

import java.sql.SQLException;

import javax.sql.DataSource;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

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
  private final DataSource jdbcSource;
  private final SQLDialect sqlDialect;
  
  @Inject(optional = true)
  private Settings jooqSettings = null;

  @Inject(optional = true)
  private Configuration configuration = null;
  
  @Inject
  public JooqPersistService(final DataSource jdbcSource, final SQLDialect sqlDialect) {
    this.jdbcSource = jdbcSource;
    this.sqlDialect = sqlDialect;
  }

  public DSLContext get() {
    if (!isWorking()) {
      begin();
    }

    DSLContext factory = threadFactory.get();
    Preconditions.checkState(null != factory, "Requested Factory outside work unit. "
        + "Try calling UnitOfWork.begin() first, or use a PersistFilter if you "
        + "are inside a servlet environment.");

    return factory;
  }
  
  public DefaultConnectionProvider getConnectionWrapper() {
	  return threadConnection.get();
  }

  public boolean isWorking() {
    return threadFactory.get() != null;
  }

  public void begin() {
    Preconditions.checkState(null == threadFactory.get(),
        "Work already begun on this thread. Looks like you have called UnitOfWork.begin() twice"
         + " without a balancing call to end() in between.");

    DefaultConnectionProvider conn;
    try {
      logger.debug("Getting JDBC connection");
      conn = new DefaultConnectionProvider(jdbcSource.getConnection());
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
