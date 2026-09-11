package ewm.main.event.controller;

import ewm.main.dto.EventFullDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.search.PageParam;
import ewm.main.dto.search.PublicEventSearchParam;
import ewm.main.event.service.PublicEventService;
import ewm.stats.client.CollectorClient;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@Slf4j
@AllArgsConstructor
public class PublicEventController {
    private final PublicEventService publicEventService;
    private final CollectorClient collectorClient;

    @GetMapping
    public List<EventShortDto> getEvents(@Valid @ModelAttribute PublicEventSearchParam searchParam,
                                         @Valid @ModelAttribute PageParam pageParam) {
        return publicEventService.getEvents(searchParam, pageParam);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") long userId,
                                                   @RequestParam(defaultValue = "10") int maxResults) {
        return publicEventService.getRecommendations(userId, maxResults);
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(@PathVariable Long id,
                                     @RequestHeader("X-EWM-USER-ID") long userId) {
        EventFullDto event = publicEventService.getEventById(id);
        collectorClient.sendView(userId, id);
        return event;
    }
}
