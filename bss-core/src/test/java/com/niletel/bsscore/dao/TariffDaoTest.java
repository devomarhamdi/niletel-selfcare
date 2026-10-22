package com.niletel.bsscore.dao;

import com.niletel.bsscore.model.TariffRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TariffDaoTest {
    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement statement = mock(PreparedStatement.class);
    private final ResultSet resultSet = mock(ResultSet.class);
    private final TariffDao dao = new TariffDao();

    @BeforeEach
    void setUp() throws Exception {
        DaoTestSupport.injectDataSource(dao, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void findAllMapsTariffsAndClosesResources() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("tariff_code")).thenReturn("PRE01");
        when(resultSet.getString("tariff_name")).thenReturn("Nile Prepaid");
        when(resultSet.getInt("rate_per_min_piasters")).thenReturn(19);
        when(resultSet.getString("active_flag")).thenReturn("Y");

        assertEquals(List.of(new TariffRecord("PRE01", "Nile Prepaid", 19, "Y")),
                dao.findAll());

        verify(connection).prepareStatement(org.mockito.ArgumentMatchers.contains("ORDER BY tariff_code"));
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }
}
