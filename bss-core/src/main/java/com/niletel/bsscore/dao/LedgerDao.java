package com.niletel.bsscore.dao;

import com.niletel.bsscore.model.LedgerRecord;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@ApplicationScoped
public class LedgerDao {

    @Resource(lookup = "java:jboss/datasources/BssCoreDS")
    private DataSource dataSource;

    public long insert(String msisdn, String txnType, int amountPiasters,
                      int balanceAfter, String refNo, String reason) throws SQLException {

        String query = """
            INSERT INTO ledger (
                msisdn,
                txn_type,
                amount_piasters,
                balance_after,
                ref_no,
                reason
            )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING txn_id
        """;

        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, msisdn);
            ps.setString(2, txnType);
            ps.setInt(3, amountPiasters);
            ps.setInt(4, balanceAfter);
            ps.setString(5, refNo);
            ps.setString(6, reason);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Ledger insert returned no transaction ID");
                }

                return rs.getLong("txn_id");
            }
        }
    }


    public Optional<LedgerRecord> findById(long txnId)
            throws SQLException {

        String query = """
            SELECT txn_id, msisdn, txn_type, amount_piasters,
                    balance_after, ref_no, reason
            FROM ledger
            WHERE txn_id = ?
        """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setLong(1, txnId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }

                return Optional.empty();
            }
        }
    }

    private LedgerRecord mapRow(ResultSet rs) throws SQLException {
        return new LedgerRecord(
                rs.getLong("txn_id"),
                rs.getString("msisdn"),
                rs.getString("txn_type"),
                rs.getInt("amount_piasters"),
                rs.getInt("balance_after"),
                rs.getString("ref_no"),
                rs.getString("reason")
        );
    }
}
