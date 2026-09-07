package ewm.main.request;

import ewm.main.client.ClientFallbackExceptionMapper;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RequestClientFallbackFactory implements FallbackFactory<RequestClient> {

    @Override
    public RequestClient create(Throwable cause) {
        return eventIds -> {
            throw ClientFallbackExceptionMapper.map("request-service", cause);
        };
    }
}
