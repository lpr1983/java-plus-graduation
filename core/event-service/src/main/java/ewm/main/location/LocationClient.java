package ewm.main.location;

import ewm.main.dto.EventLocationInternalDto;
import ewm.main.dto.LocationDto;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "location-service", path = "/internal/event-locations")
public interface LocationClient {

    @PutMapping("/{eventId}")
    EventLocationInternalDto saveLocation(@PathVariable("eventId") long eventId,
                                          @RequestBody LocationDto locationDto);

    @GetMapping("/{eventId}")
    EventLocationInternalDto getLocation(@PathVariable("eventId") long eventId);

    @GetMapping
    List<EventLocationInternalDto> getLocations(@RequestParam("eventIds") List<Long> eventIds);

    @GetMapping("/search")
    List<Long> findEventIds(@RequestParam("placeId") long placeId,
                            @RequestParam(value = "radius", required = false) Double radius);

    @PutMapping("/{eventId}/place/{placeId}")
    EventLocationInternalDto setPlace(@PathVariable("eventId") long eventId,
                                      @PathVariable("placeId") long placeId);

    @DeleteMapping("/{eventId}/place")
    void removePlace(@PathVariable("eventId") long eventId);
}
