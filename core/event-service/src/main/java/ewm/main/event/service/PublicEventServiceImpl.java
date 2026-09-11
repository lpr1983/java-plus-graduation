package ewm.main.event.service;

import ewm.main.dto.EventFullDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.PlaceInternalDto;
import ewm.main.event.model.Event;
import ewm.main.event.model.EventSort;
import ewm.main.event.model.EventState;
import ewm.main.dto.search.PageParam;
import ewm.main.dto.search.PublicEventSearchParam;
import ewm.main.event.repository.EventRepository;
import ewm.main.event.repository.EventSpecifications;
import ewm.main.exception.NotFoundException;
import ewm.main.exception.ValidationException;
import ewm.main.location.LocationClient;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class PublicEventServiceImpl implements PublicEventService {
    private final EventRepository eventRepository;
    private final EventDtoAssembler eventDtoAssembler;
    private final LocationClient locationClient;

    @Override
    public List<EventShortDto> getEvents(PublicEventSearchParam searchParam, PageParam pageParam) {
        log.info("Поиск событий с параметрами: {}, {}", searchParam, pageParam);

        LocalDateTime rangeStart = searchParam.getRangeStart();
        LocalDateTime rangeEnd = searchParam.getRangeEnd();

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("rangeStart должен быть раньше rangeEnd");
        }

        Specification<Event> specification = Specification
                .where(EventSpecifications.stateEqual(EventState.PUBLISHED))
                .and(EventSpecifications.searchByTextInAnnotationAndDescription(searchParam.getText()));

        if (rangeStart == null && rangeEnd == null) {
            specification = specification.and(EventSpecifications.eventDateAfter(LocalDateTime.now()));
        } else {
            specification = specification
                    .and(EventSpecifications.eventDateAfter(rangeStart))
                    .and(EventSpecifications.eventDateBefore(rangeEnd));
        }

        specification = specification
                .and(EventSpecifications.paid(searchParam.getPaid()))
                .and(EventSpecifications.categoryIdIn(searchParam.getCategories()));

        Long placeId = searchParam.getPlaceId();
        PlaceInternalDto place = placeId == null ? null : findPlaceOrThrow(placeId);
        specification = specification.and(EventSpecifications.placeSearch(place, searchParam.getRadius()));

        EventSort.parse(searchParam.getSort());
        return getEventsSortedByEventDate(specification, pageParam);
    }

    private List<EventShortDto> getEventsSortedByEventDate(Specification<Event> specification,
                                                           PageParam pageParam) {
        Pageable pageable = PageRequest.of(
                pageParam.getFrom() / pageParam.getSize(),
                pageParam.getSize(),
                Sort.by(Sort.Direction.ASC, "eventDate")
        );

        List<Event> events = eventRepository.findAll(specification, pageable).getContent();

        log.info("Найдено {} событий, соответствующих критериям.", events.size());

        return eventDtoAssembler.toShortDtoListForRead(events);
    }

    private PlaceInternalDto findPlaceOrThrow(long placeId) {
        try {
            return locationClient.getPlace(placeId);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("Не найдено место с id: " + placeId);
        }
    }

    @Override
    public EventFullDto getEventById(Long id) {

        Event event = eventRepository.findOneByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие с id: " + id + " не найдено или недоступно"));

        return eventDtoAssembler.toFullDtoForRead(event);
    }
}
