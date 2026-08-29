package ewm.locationservice.eventlocation;

import ewm.locationservice.dto.EventLocationInternalDto;
import ewm.locationservice.dto.LocationDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/internal/event-locations")
@RequiredArgsConstructor
public class InternalEventLocationController {
    private final EventLocationService eventLocationService;

    @PutMapping("/{eventId}")
    public EventLocationInternalDto save(@PathVariable long eventId,
                                         @Valid @RequestBody LocationDto locationDto) {
        return EventLocationMapper.toInternalDto(eventLocationService.save(eventId, locationDto));
    }

    @GetMapping("/{eventId}")
    public EventLocationInternalDto getByEventId(@PathVariable long eventId) {
        return EventLocationMapper.toInternalDto(eventLocationService.getByEventId(eventId));
    }

    @GetMapping
    public List<EventLocationInternalDto> getByEventIds(@RequestParam("eventIds") List<Long> eventIds) {
        return eventLocationService.getByEventIds(eventIds).stream()
                .map(EventLocationMapper::toInternalDto)
                .toList();
    }

    @GetMapping("/search")
    public List<Long> findEventIds(@RequestParam long placeId,
                                   @RequestParam(required = false) @PositiveOrZero Double radius) {
        return eventLocationService.findEventIds(placeId, radius);
    }

    @PutMapping("/{eventId}/place/{placeId}")
    public EventLocationInternalDto setPlace(@PathVariable long eventId, @PathVariable long placeId) {
        return EventLocationMapper.toInternalDto(eventLocationService.setPlace(eventId, placeId));
    }

    @DeleteMapping("/{eventId}/place")
    public void removePlace(@PathVariable long eventId) {
        eventLocationService.removePlace(eventId);
    }
}
