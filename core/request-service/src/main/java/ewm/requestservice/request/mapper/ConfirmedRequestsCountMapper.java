package ewm.requestservice.request.mapper;

import ewm.requestservice.dto.ConfirmedRequestsCountDto;
import ewm.requestservice.request.repository.EventConfirmedRequestsCount;

public final class ConfirmedRequestsCountMapper {

    private ConfirmedRequestsCountMapper() {
    }

    public static ConfirmedRequestsCountDto toDto(EventConfirmedRequestsCount count) {
        return ConfirmedRequestsCountDto.builder()
                .eventId(count.getEventId())
                .confirmedRequests(count.getConfirmedRequests())
                .build();
    }
}
