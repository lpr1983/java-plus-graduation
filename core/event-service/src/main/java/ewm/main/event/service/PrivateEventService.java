package ewm.main.event.service;

import ewm.main.dto.EventFullDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.NewEventDto;
import ewm.main.dto.UpdateEventUserRequestDto;
import ewm.main.dto.search.PageParam;

import java.util.List;

public interface PrivateEventService {

    EventFullDto createEvent(long userId, NewEventDto dto);

    List<EventShortDto> getAllByUserId(long userId, PageParam pageParam);

    EventFullDto getEventOfUserById(long userId, long eventId);

    EventFullDto updateEventOfUser(long userId, long eventId, UpdateEventUserRequestDto dto);

    EventFullDto setPlace(long userId, long eventId, long placeId);

    void removePlace(long userId, long eventId);
}
