package com.niletel.bsscore.dao;

import javax.sql.DataSource;
import java.lang.reflect.Field;

final class DaoTestSupport {
    private DaoTestSupport() {}

    static void injectDataSource(Object dao, DataSource dataSource)
            throws ReflectiveOperationException {
        Field field = dao.getClass().getDeclaredField("dataSource");
        field.setAccessible(true);
        field.set(dao, dataSource);
    }
}
