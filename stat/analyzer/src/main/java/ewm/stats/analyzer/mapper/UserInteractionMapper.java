package ewm.stats.analyzer.mapper;

import ewm.stats.analyzer.model.UserInteraction;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class UserInteractionMapper {
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    public UserInteraction toModel(UserActionAvro action) {
        return new UserInteraction(
                action.getUserId(), action.getEventId(), getActionWeight(action.getActionType()), action.getTimestamp());
    }

    private double getActionWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }
}
