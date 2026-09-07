package ewm.main.location;

import ewm.main.dto.PlaceInternalDto;
import ewm.main.dto.ShortPlaceDto;

public final class PlaceMapper {

    private PlaceMapper() {
    }

    public static ShortPlaceDto toShortDto(PlaceInternalDto place) {
        if (place == null) {
            return null;
        }

        return ShortPlaceDto.builder()
                .id(place.getId())
                .name(place.getName())
                .build();
    }
}
