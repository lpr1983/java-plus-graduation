package ewm.main.location;

import ewm.main.dto.PlaceInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "location-service",
        path = "/internal/places",
        fallbackFactory = LocationClientFallbackFactory.class
)
public interface LocationClient {

    @GetMapping("/{placeId}")
    PlaceInternalDto getPlace(@PathVariable("placeId") long placeId);

    @GetMapping
    List<PlaceInternalDto> getPlaces(@RequestParam("ids") List<Long> ids);
}
