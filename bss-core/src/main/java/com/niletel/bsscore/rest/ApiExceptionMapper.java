package com.niletel.bsscore.rest;

import com.niletel.bsscore.common.ApiResponse;
import com.niletel.bsscore.common.BusinessException;
import com.niletel.bsscore.common.ErrorCode;
import jakarta.ejb.EJBException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Turns every exception into the contract envelope: BusinessException by its code, anything else SYS999 (500). */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Unknown paths and methods are outside the contract: keep the container's own answer.
        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse();
        }
        BusinessException business = findBusinessException(exception);
        if (business != null) {
            return ApiResponse.fail(business.getErrorCode());
        }
        LOG.log(Level.SEVERE, "Unexpected error", exception);
        return ApiResponse.fail(ErrorCode.SYS999);
    }

    private static BusinessException findBusinessException(Throwable exception) {
        Throwable current = exception;
        while (current instanceof EJBException && current.getCause() != null) {
            current = current.getCause();
        }
        return current instanceof BusinessException ? (BusinessException) current : null;
    }
}
