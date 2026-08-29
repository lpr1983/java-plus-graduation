package ewm.main.event.service;

import ewm.main.dto.EventShortDto;
import ewm.main.dto.UpdateEventUserRequestDto;
import ewm.main.dto.UserShortDto;
import ewm.main.event.mapper.EventMapper;
import ewm.main.event.model.Event;
import ewm.main.event.model.EventState;
import ewm.main.dto.search.PageParam;
import ewm.main.event.repository.EventRepository;
import ewm.main.exception.ConflictException;
import ewm.main.location.LocationClient;
import jakarta.validation.ValidationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ewm.main.category.Category;
import ewm.main.category.repository.CategoryRepository;
import ewm.main.dto.EventFullDto;
import ewm.main.dto.NewEventDto;
import ewm.main.exception.NotFoundException;
import ewm.main.user.UserClient;
import feign.FeignException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class PrivateEventServiceImpl implements PrivateEventService {
    private final UserClient userClient;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventDtoAssembler eventDtoAssembler;
    private final LocationClient locationClient;

    @Override
    public EventFullDto getEventOfUserById(long userId, long eventId) {
        log.info("Получение события для userId: {}, eventId: {}", userId, eventId);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        log.info("Событие успешно получено, eventId: {}", eventId);

        return eventDtoAssembler.toFullDto(event);
    }

    @Override
    public List<EventShortDto> getAllByUserId(long userId, PageParam pageParam) {
        log.info("Получение событий для userId: {}, с: {}, размер: {}", userId, pageParam);

        Pageable pageable = PageRequest.of(pageParam.getFrom() / pageParam.getSize(), pageParam.getSize());

        List<Event> events = eventRepository.findByInitiatorIdOrderByEventDateAsc(userId, pageable);

        log.info("Количество событий: {}", events.size());

        return eventDtoAssembler.toShortDtoList(events);
    }

    @Override
    public EventFullDto createEvent(long userId, NewEventDto dto) {
        log.info("Создание события для userId: {}, детали события: {}", userId, dto);

        validateEventDate(dto.getEventDate());

        UserShortDto user = findUserByIdOrThrow(userId);

        Category category = findCategoryByIdOrThrow(dto.getCategory());

        Event event = EventMapper.toEntity(dto, category, user);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        Event savedEvent = eventRepository.save(event);
        locationClient.saveLocation(savedEvent.getId(), dto.getLocation());
        log.info("Событие успешно создано с id: {}", savedEvent.getId());

        return eventDtoAssembler.toFullDto(savedEvent);
    }

    @Override
    public EventFullDto updateEventOfUser(long userId, long eventId, UpdateEventUserRequestDto dto) {
        log.info("Обновление события для userId: {}, eventId: {}, детали обновления: {}", userId, eventId, dto);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        checkEventIsEditable(event);

        validateEventDate(event.getEventDate());

        Category category = dto.getCategory() != null ? findCategoryByIdOrThrow(dto.getCategory()) : null;

        EventMapper.updateEntity(event, dto, category);

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case "SEND_TO_REVIEW" -> event.setState(EventState.PENDING);
                case "CANCEL_REVIEW" -> event.setState(EventState.CANCELED);
                default -> throw new ValidationException("Недопустимое действие: " + dto.getStateAction());
            }
        }

        Event updatedEvent = eventRepository.save(event);
        if (dto.getLocation() != null) {
            locationClient.saveLocation(updatedEvent.getId(), dto.getLocation());
        }
        log.info("Событие успешно обновлено с id: {}", updatedEvent.getId());

        return eventDtoAssembler.toFullDto(updatedEvent);
    }

    @Override
    public EventFullDto setPlace(long userId, long eventId, long placeId) {
        log.info("Привязка события с id: {} к месту: {}", eventId, placeId);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        checkEventIsEditable(event);

        setPlaceOrThrow(eventId, placeId);

        return eventDtoAssembler.toFullDto(event);
    }

    @Override
    public void removePlace(long userId, long eventId) {
        log.info("Отвязка места от события с id: {}", eventId);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        checkEventIsEditable(event);

        locationClient.removePlace(eventId);
    }

    private UserShortDto findUserByIdOrThrow(long userId) {
        try {
            return userClient.getUser(userId);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("Не найден пользователь с id: " + userId);
        }
    }

    private Category findCategoryByIdOrThrow(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Не найдена категория с id: " + categoryId));
    }

    private void setPlaceOrThrow(long eventId, long placeId) {
        try {
            locationClient.setPlace(eventId, placeId);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("Не найдено место: " + placeId);
        }
    }

    private Event findEventByUserIdAndEventIdOrThrow(long userId, long eventId) {
        return eventRepository.findOneByInitiatorIdAndId(userId, eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("У пользователя с id: %d нет события с id: %d", userId, eventId)));
    }

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }
    }

    private void checkEventIsEditable(Event event) {
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Можно изменять события только в статусах PENDING и CANCELED");
        }
    }

}
