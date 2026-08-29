package ewm.locationservice.eventlocation;

import ewm.locationservice.dto.EventLocationInternalDto;
import ewm.locationservice.place.mapper.PlaceMapper;

public final class EventLocationMapper {

    private EventLocationMapper() {
    }

    public static EventLocationInternalDto toInternalDto(EventLocation eventLocation) {
        return EventLocationInternalDto.builder()
                .eventId(eventLocation.getEventId())
                .lat(eventLocation.getLat())
                .lon(eventLocation.getLon())
                .place(eventLocation.getPlace() == null
                        ? null
                        : PlaceMapper.toInternalDto(eventLocation.getPlace()))
                .build();
    }
}
