package ewm.requestservice.request.controller;

import ewm.requestservice.dto.ConfirmedRequestsCountDto;
import ewm.requestservice.request.mapper.ConfirmedRequestsCountMapper;
import ewm.requestservice.request.model.RequestStatus;
import ewm.requestservice.request.repository.EventConfirmedRequestsCount;
import ewm.requestservice.request.repository.ParticipationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final ParticipationRequestRepository requestRepository;

    @GetMapping("/confirmed-counts")
    public List<ConfirmedRequestsCountDto> getConfirmedRequestsCounts(@RequestParam("eventIds") List<Long> eventIds) {
        List<EventConfirmedRequestsCount> confirmedRequestsCounts =
                requestRepository.countConfirmedRequestsByEventIds(eventIds, RequestStatus.CONFIRMED);

        return confirmedRequestsCounts.stream()
                .map(ConfirmedRequestsCountMapper::toDto)
                .toList();
    }
}
