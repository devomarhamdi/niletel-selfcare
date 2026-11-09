package com.niletel.bsscore.service;

import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.DataAccessException;
import com.niletel.bsscore.common.ErrorCode;
import com.niletel.bsscore.common.InputValidation;
import com.niletel.bsscore.common.PasswordHasher;
import com.niletel.bsscore.dao.SubscriberDao;
import com.niletel.bsscore.model.AuthenticatedSubscriber;
import com.niletel.bsscore.model.SubscriberRecord;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import com.niletel.bsscore.dao.OperatorDao;
import com.niletel.bsscore.model.OperatorRecord;
import java.sql.SQLException;

@Stateless
public class AuthService {

    @Inject
    private SubscriberDao subscriberDao;
    @Inject
    private OperatorDao operatorDao;

    public AuthenticatedSubscriber loginSubscriber(String msisdn, String pin) {
        if (!InputValidation.isMsisdn(msisdn) || !InputValidation.isPin(pin)) {
            throw new BusinessException(ErrorCode.VAL001);
        }

        final SubscriberRecord subscriber;

        try {
            subscriber = subscriberDao.findByMsisdn(msisdn)
                    .orElseThrow(() -> new BusinessException(ErrorCode.AUTH001));
        } catch (SQLException e) {
            throw new DataAccessException("Database operation failed", e);
        }

        String enteredPinHash = PasswordHasher.sha256Hex(pin);

        if (!PasswordHasher.isEqualHex(subscriber.pinHash(), enteredPinHash)) {
            throw new BusinessException(ErrorCode.AUTH001);
        }

        if ("S".equals(subscriber.status())) {
            throw new BusinessException(ErrorCode.AUTH002);
        }

        return new AuthenticatedSubscriber(subscriber.msisdn(), subscriber.fullName());
    }
    public OperatorRecord loginOperator(String username, String password) {
        if (!InputValidation.isUsername(username) || !InputValidation.isPassword(password)) {
            throw new BusinessException(ErrorCode.VAL001);
        }

        return verify(username, password);
    }


    public OperatorRecord verify(String username, String password) {
        final OperatorRecord operator;
        try {
            operator = operatorDao.findByUsername(username)
                    .orElseThrow(() -> new BusinessException(ErrorCode.AUTH003));
        } catch (SQLException e) {
            throw new DataAccessException("Database operation failed", e);
        }

        String enteredHash = PasswordHasher.sha256Hex(password);
        if (!PasswordHasher.isEqualHex(operator.passwordHash(), enteredHash)) {
            throw new BusinessException(ErrorCode.AUTH003);
        }

        if (!operator.active()) {
            throw new BusinessException(ErrorCode.AUTH003);
        }

        return operator;
    }
}
