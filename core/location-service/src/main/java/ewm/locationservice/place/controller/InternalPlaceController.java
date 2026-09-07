package ewm.locationservice.place.controller;

import ewm.locationservice.dto.PlaceInternalDto;
import ewm.locationservice.place.mapper.PlaceMapper;
import ewm.locationservice.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/places")
@RequiredArgsConstructor
public class InternalPlaceController {

    private final PlaceService placeService;

    @GetMapping("/{placeId}")
    public PlaceInternalDto getById(@PathVariable long placeId) {
        return PlaceMapper.toInternalDto(placeService.getById(placeId));
    }

    @GetMapping
    public List<PlaceInternalDto> getByIds(@RequestParam("ids") List<Long> ids) {
        return placeService.getByIds(ids).stream()
                .map(PlaceMapper::toInternalDto)
                .toList();
    }
}
