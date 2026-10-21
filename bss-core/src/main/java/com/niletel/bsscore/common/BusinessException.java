package com.niletel.bsscore.common;

import jakarta.ejb.ApplicationException;

/**
 * A business error with a contract code. Being an application exception, EJBs do not wrap it in
 * EJBException and the container transaction rolls back.
 */
@ApplicationException(rollback = true)
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.code() + ": " + errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
