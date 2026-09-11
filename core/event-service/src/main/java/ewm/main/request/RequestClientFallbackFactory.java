package ewm.main.request;

import ewm.main.client.ClientFallbackExceptionMapper;
import ewm.main.dto.ConfirmedRequestsCountDto;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequestClientFallbackFactory implements FallbackFactory<RequestClient> {

    @Override
    public RequestClient create(Throwable cause) {
        return new RequestClient() {
            @Override
            public List<ConfirmedRequestsCountDto> getConfirmedRequestsCounts(List<Long> eventIds) {
                throw ClientFallbackExceptionMapper.map("request-service", cause);
            }

            @Override
            public boolean hasConfirmedParticipation(long userId, long eventId) {
                throw ClientFallbackExceptionMapper.map("request-service", cause);
            }
        };
    }
}
