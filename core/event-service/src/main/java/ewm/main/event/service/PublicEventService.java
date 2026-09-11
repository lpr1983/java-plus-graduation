package ewm.main.event.service;

import ewm.main.dto.EventFullDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.search.PageParam;
import ewm.main.dto.search.PublicEventSearchParam;

import java.util.List;

public interface PublicEventService {
    List<EventShortDto> getEvents(PublicEventSearchParam searchParam, PageParam pageParam);

    List<EventShortDto> getRecommendations(long userId, int maxResults);

    EventFullDto getEventById(Long id);
}
