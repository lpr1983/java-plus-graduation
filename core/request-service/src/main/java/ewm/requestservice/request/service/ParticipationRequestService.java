package ewm.requestservice.request.service;

import ewm.requestservice.dto.EventRequestStatusUpdateRequestDto;
import ewm.requestservice.dto.EventRequestStatusUpdateResultDto;
import ewm.requestservice.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    List<ParticipationRequestDto> getRequests(long userId);

    ParticipationRequestDto addRequest(long userId, long eventId);

    ParticipationRequestDto cancelRequest(long userId, long requestId);

    List<ParticipationRequestDto> getRequestsForEvent(long userId, long eventId);

    EventRequestStatusUpdateResultDto setRequestsStatus(long userId, long eventId,
                                                        EventRequestStatusUpdateRequestDto dto);
}
