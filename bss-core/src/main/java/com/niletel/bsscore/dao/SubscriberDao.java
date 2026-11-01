package com.niletel.bsscore.dao;

import com.niletel.bsscore.model.SubscriberRecord;
import com.niletel.bsscore.model.BalanceSnapshot;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public Optional<BalanceSnapshot> lockBalance(String msisdn) throws SQLException {
        String query = """
            SELECT status, balance_piasters
            FROM subscriber
            WHERE msisdn = ?
            FOR UPDATE
        """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, msisdn);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new BalanceSnapshot(
                        resultSet.getString("status"),
                        resultSet.getInt("balance_piasters")
                ));
            }
        }
    }

    public int deductBalance(String msisdn, int amountPiasters) throws SQLException {
        String query = """
            UPDATE subscriber
            SET balance_piasters = balance_piasters - ?
            WHERE msisdn = ?
            RETURNING balance_piasters
        """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, amountPiasters);
            statement.setString(2, msisdn);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Balance update returned no balance");
                }
                return resultSet.getInt("balance_piasters");
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
