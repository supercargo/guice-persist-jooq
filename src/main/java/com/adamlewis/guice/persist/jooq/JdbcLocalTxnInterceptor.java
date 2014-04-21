package com.adamlewis.guice.persist.jooq;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jooq.impl.DefaultConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;

/**
 * @author Adam Lewis
 */
class JdbcLocalTxnInterceptor implements MethodInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(JdbcLocalTxnInterceptor.class);
  
  @Inject
  private final JooqPersistService jooqProvider = null;

  @Inject
  private final UnitOfWork unitOfWork = null;

  @Transactional
  private static class Internal {}

  // Tracks if the unit of work was begun implicitly by this transaction.
  private final ThreadLocal<Boolean> didWeStartWork = new ThreadLocal<Boolean>();

  public Object invoke(final MethodInvocation methodInvocation) throws Throwable {

    // Should we start a unit of work?
    if (!jooqProvider.isWorking()) {
      jooqProvider.begin();
      didWeStartWork.set(true);
    }

    Transactional transactional = readTransactionMetadata(methodInvocation);
    DefaultConnectionProvider conn = this.jooqProvider.getConnectionWrapper();

    // Allow 'joining' of transactions if there is an enclosing @Transactional method.
    if (!conn.getAutoCommit()) {
      return methodInvocation.proceed();
    }
    logger.debug("Disabling JDBC auto commit for this thread");
    conn.setAutoCommit(false);

    Object result;
    try {
      result = methodInvocation.proceed();

    } catch (Exception e) {
      //commit transaction only if rollback didn't occur
      if (rollbackIfNecessary(transactional, e, conn)) {
        logger.debug("Committing JDBC transaction");
        conn.commit();
        logger.debug("Enabling auto commit for this thread");
        conn.setAutoCommit(true);
      }

      //propagate whatever exception is thrown anyway
      throw e;
    } finally {
      // Close the em if necessary (guarded so this code doesn't run unless catch fired).
      if (null != didWeStartWork.get() && conn.getAutoCommit()) {
        didWeStartWork.remove();
        unitOfWork.end();
      }
    }

    //everything was normal so commit the txn (do not move into try block above as it
    //  interferes with the advised method's throwing semantics)
    try {
      logger.debug("Committing JDBC transaction");
      conn.commit();
      logger.debug("Enabling auto commit for this thread");
      conn.setAutoCommit(true);
    } finally {
      //close the em if necessary
      if (null != didWeStartWork.get() ) {
        didWeStartWork.remove();
        unitOfWork.end();
      }
    }

    //or return result
    return result;
  }

  // TODO(dhanji): Cache this method's results.
  private Transactional readTransactionMetadata(final MethodInvocation methodInvocation) {
    Transactional transactional;
    Method method = methodInvocation.getMethod();
    Class<?> targetClass = methodInvocation.getThis().getClass();

    transactional = method.getAnnotation(Transactional.class);
    if (null == transactional) {
      // If none on method, try the class.
      transactional = targetClass.getAnnotation(Transactional.class);
    }
    if (null == transactional) {
      // If there is no transactional annotation present, use the default
      transactional = Internal.class.getAnnotation(Transactional.class);
    }

    return transactional;
  }

  /**
   * Returns True if rollback DID NOT HAPPEN (i.e. if commit should continue).
   *
   * @param transactional The metadata annotaiton of the method
   * @param e The exception to test for rollback
   * @param txn A JPA Transaction to issue rollbacks on
   */
  private boolean rollbackIfNecessary(final Transactional transactional, final Exception e,
      final DefaultConnectionProvider conn) {
    boolean commit = true;

    //check rollback clauses
    for (Class<? extends Exception> rollBackOn : transactional.rollbackOn()) {

      //if one matched, try to perform a rollback
      if (rollBackOn.isInstance(e)) {
        commit = false;

        //check ignore clauses (supercedes rollback clause)
        for (Class<? extends Exception> exceptOn : transactional.ignore()) {
          //An exception to the rollback clause was found, DON'T rollback
          // (i.e. commit and throw anyway)
          if (exceptOn.isInstance(e)) {
            commit = true;
            break;
          }
        }

        //rollback only if nothing matched the ignore check
        if (!commit) {
          logger.debug("Rolling back JDBC transaction for this thread");
          conn.rollback();
        }
        //otherwise continue to commit

        break;
      }
    }

    return commit;
  }
}
