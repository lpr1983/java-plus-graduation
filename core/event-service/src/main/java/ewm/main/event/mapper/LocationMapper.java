package ewm.main.event.mapper;

import ewm.main.dto.LocationDto;
import ewm.main.event.model.Location;

public final class LocationMapper {

    private LocationMapper() {
    }

    public static Location toLocation(LocationDto locationDto) {
        return locationDto == null
                ? null
                : new Location(locationDto.getLat(), locationDto.getLon());
    }

    public static LocationDto toLocationDto(Location location) {
        return location == null
                ? null
                : new LocationDto(location.getLat(), location.getLon());
    }
}
