package com.adamlewis.guice.persist.jooq;

import org.aopalliance.intercept.MethodInterceptor;
import org.jooq.DSLContext;

import com.google.inject.Singleton;
import com.google.inject.persist.PersistModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

/**
 * Jooq Factory provider for guice persist.
 *
 * @author Adam Lewis
 */
public final class JooqPersistModule extends PersistModule {

  public JooqPersistModule() {
    
  }

  private MethodInterceptor transactionInterceptor;

  @Override protected void configurePersistence() {
    
    bind(JooqPersistService.class).in(Singleton.class);

    bind(PersistService.class).to(JooqPersistService.class);
    bind(UnitOfWork.class).to(JooqPersistService.class);
    bind(DSLContext.class).toProvider(JooqPersistService.class);
    
    transactionInterceptor = new JdbcLocalTxnInterceptor();
    requestInjection(transactionInterceptor);

    
  }

  @Override protected MethodInterceptor getTransactionInterceptor() {
    return transactionInterceptor;
  }

  
  
}
