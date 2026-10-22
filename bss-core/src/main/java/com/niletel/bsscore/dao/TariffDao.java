package com.niletel.bsscore.dao;

import com.niletel.bsscore.model.TariffRecord;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TariffDao {

    @Resource(lookup = "java:jboss/datasources/BssCoreDS")
    private DataSource dataSource;

    public List<TariffRecord> findAll() throws SQLException {

        List<TariffRecord> tariffs = new ArrayList<>();

        String query = """
            SELECT tariff_code, tariff_name,
                rate_per_min_piasters, active_flag
            FROM tariff_plan
            ORDER BY tariff_code
        """;

        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(query)) {

            try(ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tariffs.add(mapRow(rs));
                }

                return tariffs;
            }
        }

    }

    private TariffRecord mapRow(ResultSet rs) throws SQLException {
        return new TariffRecord(
                rs.getString("tariff_code"),
                rs.getString("tariff_name"),
                rs.getInt("rate_per_min_piasters"),
                rs.getString("active_flag")
        );
    }

}
