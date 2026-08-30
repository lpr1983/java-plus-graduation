package ewm.main.event.service;

import ewm.main.dto.ConfirmedRequestsCountDto;
import ewm.main.dto.EventFullDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.PlaceInternalDto;
import ewm.main.dto.ShortPlaceDto;
import ewm.main.dto.UserShortDto;
import ewm.main.event.mapper.EventMapper;
import ewm.main.event.model.Event;
import ewm.main.exception.NotFoundException;
import ewm.main.location.LocationClient;
import ewm.main.location.PlaceMapper;
import ewm.main.request.RequestClient;
import ewm.main.stat.StatService;
import ewm.main.user.UserClient;
import ewm.stat.client.model.GetStatsParams;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class EventDtoAssembler {
    private static final boolean UNIQUE_VIEWS = true;

    private final StatService statService;
    private final RequestClient requestClient;
    private final UserClient userClient;
    private final LocationClient locationClient;

    public EventDtoAssembler(StatService statService,
                             RequestClient requestClient,
                             UserClient userClient,
                             LocationClient locationClient) {
        this.statService = statService;
        this.requestClient = requestClient;
        this.userClient = userClient;
        this.locationClient = locationClient;
    }

    public EventShortDto toShortDto(Event event) {
        EventShortDto dto = EventMapper.toShortDto(event, getInitiator(event));

        dto.setViews(getViews(event));
        dto.setConfirmedRequests(getConfirmedRequests(event));

        return dto;
    }

    public EventFullDto toFullDto(Event event) {
        EventFullDto dto = EventMapper.toFullDto(event, getInitiator(event), getPlace(event));

        dto.setViews(getViews(event));
        dto.setConfirmedRequests(getConfirmedRequests(event));

        return dto;
    }

    public List<EventShortDto> toShortDtoList(List<Event> events) {
        Map<Long, Long> viewsByEventId = getViewsByEventId(events);
        Map<Long, Long> confirmedRequestsByEventId = getConfirmedRequestsByEventId(events);
        Map<Long, UserShortDto> initiatorsById = getInitiatorsById(events);

        List<EventShortDto> result = new ArrayList<>();

        for (Event event : events) {
            EventShortDto dto = EventMapper.toShortDto(event, getInitiatorFromCache(event, initiatorsById));
            dto.setViews(getViewsForEvent(event, viewsByEventId));
            dto.setConfirmedRequests(getConfirmedRequestsForEvent(event, confirmedRequestsByEventId));
            result.add(dto);
        }

        return result;
    }

    public List<EventFullDto> toFullDtoList(List<Event> events) {
        Map<Long, Long> viewsByEventId = getViewsByEventId(events);
        Map<Long, Long> confirmedRequestsByEventId = getConfirmedRequestsByEventId(events);
        Map<Long, UserShortDto> initiatorsById = getInitiatorsById(events);
        Map<Long, ShortPlaceDto> placesById = getPlacesById(events);

        List<EventFullDto> result = new ArrayList<>();

        for (Event event : events) {
            EventFullDto dto = EventMapper.toFullDto(
                    event,
                    getInitiatorFromCache(event, initiatorsById),
                    getPlaceFromCache(event, placesById)
            );
            dto.setViews(getViewsForEvent(event, viewsByEventId));
            dto.setConfirmedRequests(getConfirmedRequestsForEvent(event, confirmedRequestsByEventId));
            result.add(dto);
        }

        return result;
    }

    private ShortPlaceDto getPlace(Event event) {
        if (event.getPlaceId() == null) {
            return null;
        }

        try {
            return PlaceMapper.toShortDto(locationClient.getPlace(event.getPlaceId()));
        } catch (FeignException.NotFound exception) {
            return null;
        }
    }

    private ShortPlaceDto getPlaceFromCache(Event event, Map<Long, ShortPlaceDto> placesById) {
        if (event.getPlaceId() == null) {
            return null;
        }

        return placesById.get(event.getPlaceId());
    }

    private Map<Long, ShortPlaceDto> getPlacesById(List<Event> events) {
        Set<Long> placeIds = new LinkedHashSet<>();

        for (Event event : events) {
            if (event.getPlaceId() != null) {
                placeIds.add(event.getPlaceId());
            }
        }

        if (placeIds.isEmpty()) {
            return Map.of();
        }

        List<PlaceInternalDto> places = locationClient.getPlaces(new ArrayList<>(placeIds));
        Map<Long, ShortPlaceDto> result = new HashMap<>();

        for (PlaceInternalDto place : places) {
            result.put(place.getId(), PlaceMapper.toShortDto(place));
        }

        return result;
    }

    private UserShortDto getInitiator(Event event) {
        try {
            return userClient.getUser(event.getInitiatorId());
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("Не найден пользователь с id: " + event.getInitiatorId());
        }
    }

    private UserShortDto getInitiatorFromCache(Event event, Map<Long, UserShortDto> initiatorsById) {
        UserShortDto initiator = initiatorsById.get(event.getInitiatorId());

        if (initiator == null) {
            throw new NotFoundException("Не найден пользователь с id: " + event.getInitiatorId());
        }

        return initiator;
    }

    private Map<Long, UserShortDto> getInitiatorsById(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        Set<Long> initiatorIds = new LinkedHashSet<>();

        for (Event event : events) {
            initiatorIds.add(event.getInitiatorId());
        }

        List<UserShortDto> initiators = userClient.getUsers(new ArrayList<>(initiatorIds));
        Map<Long, UserShortDto> result = new HashMap<>();

        for (UserShortDto initiator : initiators) {
            result.put(initiator.getId(), initiator);
        }

        return result;
    }

    private Long getConfirmedRequests(Event event) {
        Map<Long, Long> confirmedRequestsByEventId = getConfirmedRequestsByEventId(List.of(event));
        return getConfirmedRequestsForEvent(event, confirmedRequestsByEventId);
    }

    private Map<Long, Long> getConfirmedRequestsByEventId(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = getEventIds(events);

        List<ConfirmedRequestsCountDto> counts = requestClient.getConfirmedRequestsCounts(eventIds);

        Map<Long, Long> result = new HashMap<>();

        for (ConfirmedRequestsCountDto count : counts) {
            result.put(count.getEventId(), count.getConfirmedRequests());
        }

        return result;
    }

    private Long getConfirmedRequestsForEvent(Event event, Map<Long, Long> confirmedRequestsByEventId) {
        return confirmedRequestsByEventId.getOrDefault(event.getId(), 0L);
    }

    private Long getViews(Event event) {
        if (event.getPublishedOn() == null) {
            return null;
        }

        String uri = getEventUri(event);

        GetStatsParams params = GetStatsParams.builder()
                .start(event.getPublishedOn())
                .end(LocalDateTime.now())
                .uris(List.of(uri))
                .unique(UNIQUE_VIEWS)
                .build();

        Map<String, Long> viewsByUri = statService.getViews(params);

        if (viewsByUri == null) {
            return null;
        }

        return viewsByUri.getOrDefault(uri, 0L);
    }

    private Map<Long, Long> getViewsByEventId(List<Event> events) {
        List<Event> eventsWithPublishedOn = getEventsWithPublishedOn(events);

        if (eventsWithPublishedOn.isEmpty()) {
            return Map.of();
        }

        GetStatsParams params = GetStatsParams.builder()
                .start(getMinPublishedOn(eventsWithPublishedOn))
                .end(LocalDateTime.now())
                .uris(getEventUris(eventsWithPublishedOn))
                .unique(UNIQUE_VIEWS)
                .build();

        Map<String, Long> viewsByUri = statService.getViews(params);

        if (viewsByUri == null) {
            return null;
        }

        Map<Long, Long> viewsByEventId = new HashMap<>();

        for (Event event : eventsWithPublishedOn) {
            String uri = getEventUri(event);
            Long views = viewsByUri.getOrDefault(uri, 0L);

            viewsByEventId.put(event.getId(), views);
        }

        return viewsByEventId;
    }

    private Long getViewsForEvent(Event event, Map<Long, Long> viewsByEventId) {
        if (viewsByEventId == null) {
            return null;
        }

        return viewsByEventId.get(event.getId());
    }

    private List<Event> getEventsWithPublishedOn(List<Event> events) {
        List<Event> result = new ArrayList<>();

        for (Event event : events) {
            if (event.getPublishedOn() != null) {
                result.add(event);
            }
        }

        return result;
    }

    private List<Long> getEventIds(List<Event> events) {
        Set<Long> uniqueIds = new LinkedHashSet<>();

        for (Event event : events) {
            uniqueIds.add(event.getId());
        }

        return new ArrayList<>(uniqueIds);
    }

    private List<String> getEventUris(List<Event> events) {
        Set<String> uniqueUris = new LinkedHashSet<>();

        for (Event event : events) {
            uniqueUris.add(getEventUri(event));
        }

        return new ArrayList<>(uniqueUris);
    }

    private LocalDateTime getMinPublishedOn(List<Event> events) {
        LocalDateTime minPublishedOn = events.get(0).getPublishedOn();

        for (Event event : events) {
            if (event.getPublishedOn().isBefore(minPublishedOn)) {
                minPublishedOn = event.getPublishedOn();
            }
        }

        return minPublishedOn;
    }

    private String getEventUri(Event event) {
        return "/events/" + event.getId();
    }
}
