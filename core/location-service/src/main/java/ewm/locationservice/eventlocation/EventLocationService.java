package ewm.locationservice.eventlocation;

import ewm.locationservice.dto.LocationDto;
import ewm.locationservice.exception.NotFoundException;
import ewm.locationservice.place.Place;
import ewm.locationservice.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventLocationService {
    private final EventLocationRepository eventLocationRepository;
    private final PlaceRepository placeRepository;

    public EventLocation save(long eventId, LocationDto locationDto) {
        EventLocation eventLocation = eventLocationRepository.findById(eventId)
                .orElseGet(() -> EventLocation.builder().eventId(eventId).build());

        eventLocation.setLat(locationDto.getLat());
        eventLocation.setLon(locationDto.getLon());

        return eventLocationRepository.save(eventLocation);
    }

    public EventLocation getByEventId(long eventId) {
        return eventLocationRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Не найдены координаты события: " + eventId));
    }

    public List<EventLocation> getByEventIds(List<Long> eventIds) {
        return eventLocationRepository.findAllByEventIdIn(eventIds);
    }

    public List<Long> findEventIds(long placeId, Double radius) {
        Place place = findPlace(placeId);

        if (radius == null) {
            return eventLocationRepository.findEventIdsByPlaceId(placeId);
        }

        return eventLocationRepository.findEventIdsInRadius(place.getLat(), place.getLon(), radius);
    }

    public EventLocation setPlace(long eventId, long placeId) {
        EventLocation eventLocation = getByEventId(eventId);
        eventLocation.setPlace(findPlace(placeId));
        return eventLocationRepository.save(eventLocation);
    }

    public void removePlace(long eventId) {
        EventLocation eventLocation = getByEventId(eventId);
        eventLocation.setPlace(null);
        eventLocationRepository.save(eventLocation);
    }

    private Place findPlace(long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new NotFoundException("Локация не найдена: " + placeId));
    }
}
