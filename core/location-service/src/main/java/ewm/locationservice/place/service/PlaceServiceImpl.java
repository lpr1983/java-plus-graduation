package ewm.locationservice.place.service;

import ewm.locationservice.dto.PageParam;
import ewm.locationservice.exception.NotFoundException;
import ewm.locationservice.place.Place;
import ewm.locationservice.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {
    private final PlaceRepository placeRepository;

    @Override
    public Place create(Place place) {
        place.setId(null);
        return placeRepository.save(place);
    }

    @Override
    public Place update(long placeId, Place place) {
        Place existing = getById(placeId);

        existing.setName(place.getName());
        existing.setLat(place.getLat());
        existing.setLon(place.getLon());

        return placeRepository.save(existing);
    }

    @Override
    public void delete(long placeId) {
        Place place = getById(placeId);
        placeRepository.delete(place);
    }

    @Override
    public List<Place> getAll(PageParam pageParam) {
        PageRequest pageRequest = PageRequest.of(
                pageParam.getFrom() / pageParam.getSize(),
                pageParam.getSize()
        );

        return placeRepository.findAll(pageRequest).getContent();
    }

    @Override
    public Place getById(long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new NotFoundException("Локация не найдена: " + placeId));
    }

    @Override
    public List<Place> getByIds(List<Long> ids) {
        return placeRepository.findAllById(ids);
    }
}
