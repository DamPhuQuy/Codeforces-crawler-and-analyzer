package com.cf.analysis.dal;

import java.sql.SQLException;
import java.util.List;

public interface DataAccessInterface<T, K> {
    void insert(T entity) throws SQLException;
    T findById(K id) throws SQLException;
    void delete(K id) throws SQLException;
    List<T> findAll() throws SQLException;
}
