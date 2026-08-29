package ewm.requestservice.event;

import ewm.requestservice.dto.EventInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service", path = "/internal/events")
public interface EventClient {

    @GetMapping("/{eventId}")
    EventInternalDto getEvent(@PathVariable("eventId") long eventId);
}
