package ewm.locationservice.eventlocation;

import ewm.locationservice.place.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_locations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLocation {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;
}
