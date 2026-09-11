package ewm.main.event.service;

import ewm.main.dto.ConfirmedRequestsCountDto;
import ewm.main.dto.EventFullDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.PlaceInternalDto;
import ewm.main.dto.ShortPlaceDto;
import ewm.main.dto.UserShortDto;
import ewm.main.event.mapper.EventMapper;
import ewm.main.event.model.Event;
import ewm.main.exception.ServiceUnavailableException;
import ewm.main.location.LocationClient;
import ewm.main.location.PlaceMapper;
import ewm.main.request.RequestClient;
import ewm.main.user.UserClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class EventDtoAssembler {
    private static final String MISSING_USER_NAME = "Объект не найден";
    private static final String UNAVAILABLE_USER_NAME = "Данные временно недоступны";

    private final RequestClient requestClient;
    private final UserClient userClient;
    private final LocationClient locationClient;

    public EventDtoAssembler(RequestClient requestClient,
                             UserClient userClient,
                             LocationClient locationClient) {
        this.requestClient = requestClient;
        this.userClient = userClient;
        this.locationClient = locationClient;
    }

    public EventShortDto toShortDto(Event event) {
        EventShortDto dto = EventMapper.toShortDto(event, getInitiator(event, false));

        dto.setConfirmedRequests(getConfirmedRequests(event, false));

        return dto;
    }

    public EventFullDto toFullDto(Event event) {
        return toFullDto(event, false);
    }

    public EventFullDto toFullDtoForRead(Event event) {
        return toFullDto(event, true);
    }

    private EventFullDto toFullDto(Event event, boolean allowUnavailableServices) {
        EventFullDto dto = EventMapper.toFullDto(
                event,
                getInitiator(event, allowUnavailableServices),
                getPlace(event, allowUnavailableServices)
        );

        dto.setConfirmedRequests(getConfirmedRequests(event, allowUnavailableServices));

        return dto;
    }

    public List<EventShortDto> toShortDtoList(List<Event> events) {
        return toShortDtoList(events, false);
    }

    public List<EventShortDto> toShortDtoListForRead(List<Event> events) {
        return toShortDtoList(events, true);
    }

    private List<EventShortDto> toShortDtoList(List<Event> events, boolean allowUnavailableServices) {
        Map<Long, Long> confirmedRequestsByEventId =
                getConfirmedRequestsByEventId(events, allowUnavailableServices);
        Map<Long, UserShortDto> initiatorsById = getInitiatorsById(events, allowUnavailableServices);

        List<EventShortDto> result = new ArrayList<>();

        for (Event event : events) {
            EventShortDto dto = EventMapper.toShortDto(event, getInitiatorFromCache(event, initiatorsById));
            dto.setConfirmedRequests(getConfirmedRequestsForEvent(event, confirmedRequestsByEventId));
            result.add(dto);
        }

        return result;
    }

    public List<EventFullDto> toFullDtoList(List<Event> events) {
        return toFullDtoList(events, false);
    }

    public List<EventFullDto> toFullDtoListForRead(List<Event> events) {
        return toFullDtoList(events, true);
    }

    private List<EventFullDto> toFullDtoList(List<Event> events, boolean allowUnavailableServices) {
        Map<Long, Long> confirmedRequestsByEventId =
                getConfirmedRequestsByEventId(events, allowUnavailableServices);
        Map<Long, UserShortDto> initiatorsById = getInitiatorsById(events, allowUnavailableServices);
        Map<Long, ShortPlaceDto> placesById = getPlacesById(events, allowUnavailableServices);

        List<EventFullDto> result = new ArrayList<>();

        for (Event event : events) {
            EventFullDto dto = EventMapper.toFullDto(
                    event,
                    getInitiatorFromCache(event, initiatorsById),
                    getPlaceFromCache(event, placesById)
            );
            dto.setConfirmedRequests(getConfirmedRequestsForEvent(event, confirmedRequestsByEventId));
            result.add(dto);
        }

        return result;
    }

    private ShortPlaceDto getPlace(Event event, boolean allowUnavailableServices) {
        if (event.getPlaceId() == null) {
            return null;
        }

        try {
            return PlaceMapper.toShortDto(locationClient.getPlace(event.getPlaceId()));
        } catch (FeignException.NotFound exception) {
            return null;
        } catch (ServiceUnavailableException exception) {
            if (allowUnavailableServices) {
                log.warn("Не удалось получить место для события {}: {}", event.getId(), exception.getMessage());
                return null;
            }
            throw exception;
        }
    }

    private ShortPlaceDto getPlaceFromCache(Event event, Map<Long, ShortPlaceDto> placesById) {
        if (event.getPlaceId() == null) {
            return null;
        }

        return placesById.get(event.getPlaceId());
    }

    private Map<Long, ShortPlaceDto> getPlacesById(List<Event> events, boolean allowUnavailableServices) {
        Set<Long> placeIds = new LinkedHashSet<>();

        for (Event event : events) {
            if (event.getPlaceId() != null) {
                placeIds.add(event.getPlaceId());
            }
        }

        if (placeIds.isEmpty()) {
            return Map.of();
        }

        List<PlaceInternalDto> places;
        try {
            places = locationClient.getPlaces(new ArrayList<>(placeIds));
        } catch (ServiceUnavailableException exception) {
            if (allowUnavailableServices) {
                log.warn("Не удалось получить места для событий: {}", exception.getMessage());
                return Map.of();
            }
            throw exception;
        }
        Map<Long, ShortPlaceDto> result = new HashMap<>();

        for (PlaceInternalDto place : places) {
            result.put(place.getId(), PlaceMapper.toShortDto(place));
        }

        return result;
    }

    private UserShortDto getInitiator(Event event, boolean allowUnavailableServices) {
        try {
            return userClient.getUser(event.getInitiatorId());
        } catch (FeignException.NotFound exception) {
            return missingUser(event.getInitiatorId());
        } catch (ServiceUnavailableException exception) {
            if (allowUnavailableServices) {
                log.warn("Не удалось получить инициатора события {}: {}", event.getId(), exception.getMessage());
                return unavailableUser(event.getInitiatorId());
            }
            throw exception;
        }
    }

    private UserShortDto getInitiatorFromCache(Event event, Map<Long, UserShortDto> initiatorsById) {
        UserShortDto initiator = initiatorsById.get(event.getInitiatorId());

        if (initiator == null) {
            return missingUser(event.getInitiatorId());
        }

        return initiator;
    }

    private UserShortDto missingUser(long userId) {
        return userWithSubstituteName(userId, MISSING_USER_NAME);
    }

    private UserShortDto unavailableUser(long userId) {
        return userWithSubstituteName(userId, UNAVAILABLE_USER_NAME);
    }

    private UserShortDto userWithSubstituteName(long userId, String name) {
        return UserShortDto.builder()
                .id(userId)
                .name(name)
                .build();
    }

    private Map<Long, UserShortDto> getInitiatorsById(List<Event> events, boolean allowUnavailableServices) {
        if (events.isEmpty()) {
            return Map.of();
        }

        Set<Long> initiatorIds = new LinkedHashSet<>();

        for (Event event : events) {
            initiatorIds.add(event.getInitiatorId());
        }

        List<UserShortDto> initiators;
        try {
            initiators = userClient.getUsers(new ArrayList<>(initiatorIds));
        } catch (ServiceUnavailableException exception) {
            if (allowUnavailableServices) {
                log.warn("Не удалось получить инициаторов событий: {}", exception.getMessage());
                Map<Long, UserShortDto> unavailableInitiators = new HashMap<>();
                for (Long initiatorId : initiatorIds) {
                    unavailableInitiators.put(initiatorId, unavailableUser(initiatorId));
                }
                return unavailableInitiators;
            }
            throw exception;
        }
        Map<Long, UserShortDto> result = new HashMap<>();

        for (UserShortDto initiator : initiators) {
            result.put(initiator.getId(), initiator);
        }

        return result;
    }

    private Long getConfirmedRequests(Event event, boolean allowUnavailableServices) {
        Map<Long, Long> confirmedRequestsByEventId =
                getConfirmedRequestsByEventId(List.of(event), allowUnavailableServices);
        return getConfirmedRequestsForEvent(event, confirmedRequestsByEventId);
    }

    private Map<Long, Long> getConfirmedRequestsByEventId(List<Event> events, boolean allowUnavailableServices) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = getEventIds(events);

        List<ConfirmedRequestsCountDto> counts;
        try {
            counts = requestClient.getConfirmedRequestsCounts(eventIds);
        } catch (ServiceUnavailableException exception) {
            if (allowUnavailableServices) {
                log.warn("Не удалось получить число подтвержденных заявок: {}", exception.getMessage());
                return null;
            }
            throw exception;
        }

        Map<Long, Long> result = new HashMap<>();

        for (ConfirmedRequestsCountDto count : counts) {
            result.put(count.getEventId(), count.getConfirmedRequests());
        }

        return result;
    }

    private Long getConfirmedRequestsForEvent(Event event, Map<Long, Long> confirmedRequestsByEventId) {
        if (confirmedRequestsByEventId == null) {
            return null;
        }

        return confirmedRequestsByEventId.getOrDefault(event.getId(), 0L);
    }

    private List<Long> getEventIds(List<Event> events) {
        Set<Long> uniqueIds = new LinkedHashSet<>();

        for (Event event : events) {
            uniqueIds.add(event.getId());
        }

        return new ArrayList<>(uniqueIds);
    }

}
