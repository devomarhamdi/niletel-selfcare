package com.niletel.bsscore.dao;

import com.niletel.bsscore.model.SubscriberRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriberDaoTest {
    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement statement = mock(PreparedStatement.class);
    private final ResultSet resultSet = mock(ResultSet.class);
    private final SubscriberDao dao = new SubscriberDao();

    @BeforeEach
    void setUp() throws Exception {
        DaoTestSupport.injectDataSource(dao, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void findByMsisdnMapsSubscriberAndClosesResources() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("msisdn")).thenReturn("01012345678");
        when(resultSet.getString("full_name")).thenReturn("Mona Adel");
        when(resultSet.getString("pin_hash")).thenReturn("a".repeat(64));
        when(resultSet.getString("status")).thenReturn("A");
        when(resultSet.getInt("balance_piasters")).thenReturn(6750);
        when(resultSet.getObject("activation_dt", LocalDate.class))
                .thenReturn(LocalDate.of(2025, 3, 1));
        when(resultSet.getString("tariff_code")).thenReturn("PRE01");
        when(resultSet.getString("tariff_name")).thenReturn("Nile Prepaid");

        Optional<SubscriberRecord> result = dao.findByMsisdn("01012345678");

        assertEquals(Optional.of(new SubscriberRecord(
                "01012345678", "Mona Adel", "a".repeat(64), "A", 6750,
                LocalDate.of(2025, 3, 1), "PRE01", "Nile Prepaid"
        )), result);
        verify(connection).prepareStatement(org.mockito.ArgumentMatchers.contains("WHERE s.msisdn = ?"));
        verify(statement).setString(1, "01012345678");
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void findByMsisdnReturnsEmptyWhenNoSubscriberMatches() throws Exception {
        when(resultSet.next()).thenReturn(false);

        assertTrue(dao.findByMsisdn("01000000000").isEmpty());

        verify(statement).setString(1, "01000000000");
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }
}
