package com.cf.analysis.db;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface Database {
    Connection getConnection() throws SQLException;
}
