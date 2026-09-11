package ewm.stats.analyzer.mapper;

import ewm.stats.analyzer.model.RecommendedEvent;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendedEventMapperTest {
    private final RecommendedEventMapper mapper = new RecommendedEventMapper();

    @Test
    void shouldMapRecommendationToProto() {
        RecommendedEventProto result = mapper.toProto(new RecommendedEvent(10, 0.75));

        assertThat(result.getEventId()).isEqualTo(10);
        assertThat(result.getScore()).isEqualTo(0.75);
    }
}
