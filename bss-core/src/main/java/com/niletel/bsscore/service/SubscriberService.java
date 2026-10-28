package com.niletel.bsscore.service;

import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.DataAccessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.dao.SubscriberDao;
import com.niletel.bsscore.model.SubscriberRecord;
import com.niletel.bsscore.model.SubscriberResponse;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.sql.SQLException;

@Stateless
public class SubscriberService {
    @Inject
    private SubscriberDao subscriberDao;

    public SubscriberResponse getProfile(String msisdn) {
        
        try {
            SubscriberRecord subscriber = subscriberDao.findByMsisdn(msisdn)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SYS999));

            return new SubscriberResponse(
                    subscriber.msisdn(),
                    subscriber.fullName(),
                    subscriber.status(),
                    subscriber.balancePiasters(),
                    subscriber.activationDate(),
                    subscriber.tariffCode(),
                    subscriber.tariffName()
            );
        } catch (SQLException e) {
            throw new DataAccessException("Database operation failed", e);
        }
    }
}
