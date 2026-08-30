package ewm.main.location;

import ewm.main.client.ClientFallbackExceptionMapper;
import ewm.main.dto.PlaceInternalDto;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LocationClientFallbackFactory implements FallbackFactory<LocationClient> {

    @Override
    public LocationClient create(Throwable cause) {
        return new LocationClient() {
            @Override
            public PlaceInternalDto getPlace(long placeId) {
                throw ClientFallbackExceptionMapper.map("location-service", cause);
            }

            @Override
            public List<PlaceInternalDto> getPlaces(List<Long> ids) {
                throw ClientFallbackExceptionMapper.map("location-service", cause);
            }
        };
    }
}
