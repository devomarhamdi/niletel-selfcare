package com.niletel.bsscore.dao;

import com.niletel.bsscore.model.OperatorRecord;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@ApplicationScoped
public class OperatorDao {

    @Resource(lookup = "java:jboss/datasources/BssCoreDS")
    private DataSource dataSource;

    public Optional<OperatorRecord> findByUsername(String username)
            throws SQLException {

        String query = """
            SELECT username, password_hash, full_name, role_code, active
            FROM operator
            WHERE username = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    private OperatorRecord mapRow(ResultSet rs) throws SQLException {
        return new OperatorRecord(
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("full_name"),
                rs.getString("role_code"),
                rs.getBoolean("active")
        );
    }
}
