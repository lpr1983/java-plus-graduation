package ewm.stats.analyzer.mapper;

import ewm.stats.analyzer.model.RecommendedEvent;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

@Component
public class RecommendedEventMapper {

    public RecommendedEventProto toProto(RecommendedEvent recommendation) {
        return RecommendedEventProto.newBuilder()
                .setEventId(recommendation.getEventId())
                .setScore(recommendation.getScore())
                .build();
    }
}
