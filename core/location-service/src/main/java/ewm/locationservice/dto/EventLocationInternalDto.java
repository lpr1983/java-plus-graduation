package ewm.locationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLocationInternalDto {
    private Long eventId;
    private Double lat;
    private Double lon;
    private PlaceInternalDto place;
}
