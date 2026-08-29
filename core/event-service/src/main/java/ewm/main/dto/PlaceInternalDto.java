package ewm.main.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceInternalDto {
    private Long id;
    private String name;
    private Double lat;
    private Double lon;
}
