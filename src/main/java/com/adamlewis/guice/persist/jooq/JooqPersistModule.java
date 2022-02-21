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

import static com.google.inject.multibindings.OptionalBinder.newOptionalBinder;

import com.google.inject.Singleton;
import com.google.inject.persist.PersistModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import org.aopalliance.intercept.MethodInterceptor;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.conf.Settings;

/**
 * Jooq Factory provider for guice persist.
 *
 * @author Adam Lewis
 */
public final class JooqPersistModule extends PersistModule {
  private MethodInterceptor transactionInterceptor;

  @Override
  protected void configurePersistence() {
    newOptionalBinder(binder(), Settings.class);
    newOptionalBinder(binder(), Configuration.class);
    bind(JooqPersistService.class).in(Singleton.class);
    bind(PersistService.class).to(JooqPersistService.class);
    bind(UnitOfWork.class).to(JooqPersistService.class);
    bind(DSLContext.class).toProvider(JooqPersistService.class);

    transactionInterceptor = new JdbcLocalTxnInterceptor(getProvider(JooqPersistService.class),
                                                         getProvider(UnitOfWork.class));
    requestInjection(transactionInterceptor);
  }

  @Override
  protected MethodInterceptor getTransactionInterceptor() {
    return transactionInterceptor;
  }
}
