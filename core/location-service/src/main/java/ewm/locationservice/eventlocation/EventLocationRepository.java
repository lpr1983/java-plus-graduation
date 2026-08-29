package ewm.locationservice.eventlocation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventLocationRepository extends JpaRepository<EventLocation, Long> {

    @Override
    @EntityGraph(attributePaths = "place")
    Optional<EventLocation> findById(Long eventId);

    @EntityGraph(attributePaths = "place")
    List<EventLocation> findAllByEventIdIn(List<Long> eventIds);

    @Query("select location.eventId from EventLocation location where location.place.id = :placeId")
    List<Long> findEventIdsByPlaceId(@Param("placeId") long placeId);

    @Query(value = """
            SELECT event_id
            FROM event_locations
            WHERE 6371 * ACOS(GREATEST(-1, LEAST(1,
                COS(RADIANS(:lat)) * COS(RADIANS(lat))
                * COS(RADIANS(lon) - RADIANS(:lon))
                + SIN(RADIANS(:lat)) * SIN(RADIANS(lat))))) <= :radius
            """, nativeQuery = true)
    List<Long> findEventIdsInRadius(@Param("lat") double lat,
                                    @Param("lon") double lon,
                                    @Param("radius") double radius);
}
