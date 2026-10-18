package com.niletel.bsscore.dao;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ApplicationScoped
public class HealthDao {

    @Resource(lookup = "java:jboss/datasources/BssCoreDS")
    private DataSource dataSource;

    /** True when the database answers SELECT 1. */
    public boolean canRunSelectOne() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet result = statement.executeQuery()) {
            return result.next() && result.getInt(1) == 1;
        } catch (SQLException e) {
            return false;
        }
    }
}
