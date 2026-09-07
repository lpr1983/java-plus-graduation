package ewm.requestservice.request.controller;

import ewm.requestservice.dto.EventRequestStatusUpdateRequestDto;
import ewm.requestservice.dto.EventRequestStatusUpdateResultDto;
import ewm.requestservice.dto.ParticipationRequestDto;
import ewm.requestservice.request.service.ParticipationRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class EventParticipationRequestController {

    private final ParticipationRequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getRequestsForEvent(@PathVariable long userId,
                                                             @PathVariable long eventId) {
        return requestService.getRequestsForEvent(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResultDto setRequestsStatus(
            @PathVariable long userId,
            @PathVariable long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequestDto dto) {
        return requestService.setRequestsStatus(userId, eventId, dto);
    }
}
