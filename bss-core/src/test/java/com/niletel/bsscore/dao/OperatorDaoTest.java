package com.niletel.bsscore.dao;

import com.niletel.bsscore.model.OperatorRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperatorDaoTest {
    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement statement = mock(PreparedStatement.class);
    private final ResultSet resultSet = mock(ResultSet.class);
    private final OperatorDao dao = new OperatorDao();

    @BeforeEach
    void setUp() throws Exception {
        DaoTestSupport.injectDataSource(dao, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void findByUsernameMapsOperatorAndClosesResources() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("username")).thenReturn("operator1");
        when(resultSet.getString("password_hash")).thenReturn("b".repeat(64));
        when(resultSet.getString("full_name")).thenReturn("Hany Samir");
        when(resultSet.getString("role_code")).thenReturn("OPR");
        when(resultSet.getBoolean("active")).thenReturn(true);

        assertEquals(Optional.of(new OperatorRecord(
                "operator1", "b".repeat(64), "Hany Samir", "OPR", true
        )), dao.findByUsername("operator1"));

        verify(connection).prepareStatement(org.mockito.ArgumentMatchers.contains("WHERE username = ?"));
        verify(statement).setString(1, "operator1");
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void findByUsernameReturnsEmptyWhenNoOperatorMatches() throws Exception {
        when(resultSet.next()).thenReturn(false);

        assertTrue(dao.findByUsername("missing").isEmpty());

        verify(statement).setString(1, "missing");
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }
}
