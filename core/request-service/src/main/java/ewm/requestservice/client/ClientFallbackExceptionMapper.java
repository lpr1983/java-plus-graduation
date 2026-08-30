package ewm.requestservice.client;

import ewm.requestservice.exception.ServiceUnavailableException;
import feign.FeignException;

public final class ClientFallbackExceptionMapper {

    private ClientFallbackExceptionMapper() {
    }

    public static RuntimeException map(String serviceName, Throwable cause) {
        if (cause instanceof FeignException exception
                && exception.status() >= 400
                && exception.status() < 500) {
            return exception;
        }

        return new ServiceUnavailableException(
                "Service '" + serviceName + "' is temporarily unavailable.",
                cause
        );
    }
}
