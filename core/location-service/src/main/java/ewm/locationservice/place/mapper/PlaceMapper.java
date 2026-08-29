package ewm.locationservice.place.mapper;

import ewm.locationservice.dto.PlaceInternalDto;
import ewm.locationservice.place.Place;

public final class PlaceMapper {

    private PlaceMapper() {
    }

    public static PlaceInternalDto toInternalDto(Place place) {
        return PlaceInternalDto.builder()
                .id(place.getId())
                .name(place.getName())
                .lat(place.getLat())
                .lon(place.getLon())
                .build();
    }
}
