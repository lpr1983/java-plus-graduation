package ewm.main.event.controller;

import ewm.main.dto.EventInternalDto;
import ewm.main.event.model.Event;
import ewm.main.event.repository.EventRepository;
import ewm.main.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;

    @GetMapping("/{eventId}")
    public EventInternalDto getEvent(@PathVariable long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        return EventInternalDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .state(event.getState().name())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.isRequestModeration())
                .build();
    }
}
