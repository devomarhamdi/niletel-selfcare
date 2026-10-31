package com.niletel.bsscore.service;

import com.niletel.bsscore.dao.TariffDao;
import com.niletel.bsscore.model.TariffRecord;
import com.niletel.bsscore.common.DataAccessException;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Singleton
@Startup
@Lock(LockType.READ)
public class TariffCache {

    private static final Logger LOG = Logger.getLogger(TariffCache.class.getName());

    @Inject
    private TariffDao tariffDao;

    private List<TariffRecord> cachedTariffs;

    @PostConstruct
    public void load() {
        try {
            cachedTariffs = Collections.unmodifiableList(tariffDao.findAll());
            LOG.info("loaded " + cachedTariffs.size() + " tariff plans");
        } catch (SQLException e) {
            LOG.severe("Failed to load tariff plans on startup: " + e.getMessage());
            throw new DataAccessException("Database unavailable at startup", e);
        }
    }

    public List<TariffRecord> getAll() {
        return cachedTariffs;
    }
}