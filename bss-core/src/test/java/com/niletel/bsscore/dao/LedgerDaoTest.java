package com.niletel.bsscore.dao;

import com.niletel.bsscore.model.LedgerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LedgerDaoTest {
    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement statement = mock(PreparedStatement.class);
    private final ResultSet resultSet = mock(ResultSet.class);
    private final LedgerDao dao = new LedgerDao();

    @BeforeEach
    void setUp() throws Exception {
        DaoTestSupport.injectDataSource(dao, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void insertBindsValuesReturnsGeneratedIdAndClosesResources() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("txn_id")).thenReturn(50231L);

        assertEquals(50231L, dao.insert(
                "01012345678", "D", 500, 4500, "REF-001", "CALL CHARGE"
        ));

        verify(connection).prepareStatement(org.mockito.ArgumentMatchers.contains("RETURNING txn_id"));
        verify(statement).setString(1, "01012345678");
        verify(statement).setString(2, "D");
        verify(statement).setInt(3, 500);
        verify(statement).setInt(4, 4500);
        verify(statement).setString(5, "REF-001");
        verify(statement).setString(6, "CALL CHARGE");
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void insertThrowsWhenNoGeneratedIdIsReturned() throws Exception {
        when(resultSet.next()).thenReturn(false);

        assertThrows(SQLException.class, () -> dao.insert(
                "01012345678", "D", 500, 4500, "REF-001", "CALL CHARGE"
        ));

        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void insertDebitUsesConflictGuardAndReturnsGeneratedId() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("txn_id")).thenReturn(50231L);

        assertEquals(OptionalLong.of(50231L), dao.insertDebit(
                "01012345678", 500, 4500, "REF-001", "CALL CHARGE"
        ));

        verify(connection).prepareStatement(org.mockito.ArgumentMatchers.contains(
                "ON CONFLICT (ref_no) WHERE txn_type = 'D' DO NOTHING"
        ));
        verify(statement).setString(1, "01012345678");
        verify(statement).setInt(2, 500);
        verify(statement).setInt(3, 4500);
        verify(statement).setString(4, "REF-001");
        verify(statement).setString(5, "CALL CHARGE");
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void insertDebitReturnsEmptyWhenReferenceConflicts() throws Exception {
        when(resultSet.next()).thenReturn(false);

        assertEquals(OptionalLong.empty(), dao.insertDebit(
                "01012345678", 500, 4500, "REF-001", "CALL CHARGE"
        ));

        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void findByIdMapsLedgerRowAndClosesResources() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("txn_id")).thenReturn(50231L);
        when(resultSet.getString("msisdn")).thenReturn("01012345678");
        when(resultSet.getObject("txn_ts", LocalDateTime.class))
                .thenReturn(LocalDateTime.of(2025, 3, 1, 10, 15, 30));
        when(resultSet.getString("txn_type")).thenReturn("R");
        when(resultSet.getInt("amount_piasters")).thenReturn(5000);
        when(resultSet.getInt("balance_after")).thenReturn(5000);
        when(resultSet.getString("ref_no")).thenReturn("");
        when(resultSet.getString("reason")).thenReturn("VOUCHER RECHARGE");

        assertEquals(Optional.of(new LedgerRecord(
                50231L, "01012345678", LocalDateTime.of(2025, 3, 1, 10, 15, 30),
                "R", 5000, 5000, "", "VOUCHER RECHARGE"
        )), dao.findById(50231L));

        verify(connection).prepareStatement(org.mockito.ArgumentMatchers.contains("WHERE txn_id = ?"));
        verify(statement).setLong(1, 50231L);
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void findByIdReturnsEmptyWhenNoTransactionMatches() throws Exception {
        when(resultSet.next()).thenReturn(false);

        assertTrue(dao.findById(99999L).isEmpty());

        verify(statement).setLong(1, 99999L);
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }
}
