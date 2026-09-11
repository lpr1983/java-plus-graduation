package ewm.stats.analyzer.mapper;

import ewm.stats.analyzer.model.EventSimilarity;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Component
public class EventSimilarityMapper {

    public EventSimilarity toModel(EventSimilarityAvro similarity) {
        return EventSimilarity.of(
                similarity.getEventA(), similarity.getEventB(), similarity.getScore(), similarity.getTimestamp());
    }
}
