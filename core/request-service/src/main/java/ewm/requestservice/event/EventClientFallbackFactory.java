package ewm.requestservice.event;

import ewm.requestservice.client.ClientFallbackExceptionMapper;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class EventClientFallbackFactory implements FallbackFactory<EventClient> {

    @Override
    public EventClient create(Throwable cause) {
        return eventId -> {
            throw ClientFallbackExceptionMapper.map("event-service", cause);
        };
    }
}
