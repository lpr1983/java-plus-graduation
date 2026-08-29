package ewm.requestservice.request.service;

import ewm.requestservice.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    List<ParticipationRequestDto> getRequests(long userId);

    ParticipationRequestDto addRequest(long userId, long eventId);

    ParticipationRequestDto cancelRequest(long userId, long requestId);
}
