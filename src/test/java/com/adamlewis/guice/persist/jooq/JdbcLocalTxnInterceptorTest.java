package com.adamlewis.guice.persist.jooq;

import java.lang.reflect.Method;

import com.adamlewis.guice.persist.jooq.utils.MockConnection;
import com.adamlewis.guice.persist.jooq.utils.Providers;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;
import org.aopalliance.intercept.MethodInvocation;
import org.jooq.impl.DefaultConnectionProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JdbcLocalTxnInterceptorTest {
  @Mock
  private JooqPersistService jooqPersistService;
  @Mock
  private UnitOfWork unitOfWork;
  @Mock
  private MockConnection connection;
  @Mock
  private MethodInvocation methodInvocation;

  private JdbcLocalTxnInterceptor interceptor;

  @Transactional
  public void transaction() {
  }

  @Before
  public void setUp() throws Exception {
    interceptor = new JdbcLocalTxnInterceptor(Providers.of(jooqPersistService), Providers.of(unitOfWork));

    when(connection.getAutoCommit()).thenCallRealMethod();
    doCallRealMethod().when(connection).setAutoCommit(anyBoolean());
    connection.setAutoCommit(true);

    DefaultConnectionProvider connectionProvider = new DefaultConnectionProvider(connection);
    when(jooqPersistService.getConnectionWrapper()).thenReturn(connectionProvider);
    when(jooqPersistService.isWorking()).thenReturn(false);

    // Method is final. Mockito doesn't support mocking final classes. Using reflection
    Method defaultTransaction = JdbcLocalTxnInterceptorTest.class.getMethod("transaction");
    when(methodInvocation.getMethod()).thenReturn(defaultTransaction);
  }

  @Test
  public void unitOfWorkEnds() throws Throwable {
    interceptor.invoke(methodInvocation);

    verify(unitOfWork).begin();
    verify(connection).commit();
    verify(unitOfWork).end();
  }

  @Test
  public void unitOfWorkEndsOnException() throws Throwable {
    when(methodInvocation.proceed()).thenThrow(Exception.class);

    try {
      interceptor.invoke(methodInvocation);
      fail("exception expected");
    } catch (Exception ignored) {
    }

    verify(unitOfWork).begin();
    verify(connection).commit();
    verify(unitOfWork).end();
  }

  @Test
  public void unitOfWorkEndsOnRollbackException() throws Throwable {
    when(methodInvocation.proceed()).thenThrow(RuntimeException.class);

    try {
      interceptor.invoke(methodInvocation);
      fail("exception expected");
    } catch (RuntimeException ignored) {
    }

    verify(unitOfWork).begin();
    verify(connection).rollback();
    verify(unitOfWork).end();
  }
}