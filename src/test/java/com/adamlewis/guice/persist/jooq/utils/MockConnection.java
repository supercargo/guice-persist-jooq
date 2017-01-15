package com.adamlewis.guice.persist.jooq.utils;

import java.sql.SQLException;

public class MockConnection extends org.jooq.tools.jdbc.MockConnection {
  private boolean autoCommit;

  public MockConnection() {
    super(null);
  }

  @Override
  public boolean getAutoCommit() throws SQLException {
    return autoCommit;
  }

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    this.autoCommit = autoCommit;
  }
}
