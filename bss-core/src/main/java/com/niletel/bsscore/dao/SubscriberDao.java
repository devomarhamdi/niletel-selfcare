package com.niletel.bsscore.dao;

import com.niletel.bsscore.model.SubscriberRecord;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SubscriberDao {
    @Resource(lookup = "java:jboss/datasources/BssCoreDS")
    private DataSource dataSource;

    public Optional<SubscriberRecord> findByMsisdn(String msisdn) throws SQLException {

        String query = """
            SELECT s.msisdn, s.full_name, s.pin_hash, s.status,
                s.balance_piasters, s.activation_dt, s.tariff_code,
                t.tariff_name
            FROM subscriber s
            JOIN tariff_plan t ON t.tariff_code = s.tariff_code
            WHERE s.msisdn = ?
        """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, msisdn);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    private SubscriberRecord mapRow(ResultSet resultSet) throws SQLException {
        return new SubscriberRecord(
                resultSet.getString("msisdn"),
                resultSet.getString("full_name"),
                resultSet.getString("pin_hash"),
                resultSet.getString("status"),
                resultSet.getInt("balance_piasters"),
                resultSet.getObject(
                        "activation_dt",
                        java.time.LocalDate.class
                ),
                resultSet.getString("tariff_code"),
                resultSet.getString("tariff_name")
        );
    }
}
