package com.niletel.bsscore.service;

import com.niletel.bsscore.dao.HealthDao;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class HealthService {

    @Inject
    private HealthDao healthDao;

    public boolean isDatabaseUp() {
        return healthDao.canRunSelectOne();
    }
}
