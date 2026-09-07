package ewm.requestservice.request.mapper;

import ewm.requestservice.dto.ParticipationRequestDto;
import ewm.requestservice.request.model.ParticipationRequest;

public class ParticipationRequestMapper {

    private ParticipationRequestMapper() {
    }

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus().name())
                .created(request.getCreated())
                .build();
    }
}
