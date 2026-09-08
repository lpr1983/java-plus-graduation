package ewm.stats.aggregator.service;

import ewm.stats.aggregator.model.EventPair;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class SimilarityCalculator {
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    // w(u, A): максимальный вес взаимодействия каждого пользователя u с каждым мероприятием A.
    private final Map<Long, Map<Long, Double>> userEventWeights = new HashMap<>();

    // S(A) = sum(w(u, A)): сумма максимальных весов всех пользователей для каждого мероприятия A.
    private final Map<Long, Double> eventWeightSums = new HashMap<>();

    // S_min(A, B) = sum(min(w(u, A), w(u, B))): сумма общих вкладов пользователей для каждой пары мероприятий.
    private final Map<EventPair, Double> minWeightSums = new HashMap<>();

    public List<EventSimilarityAvro> update(UserActionAvro action) {
        long userId = action.getUserId();
        long updatedEventId = action.getEventId();
        double actionWeight = getActionWeight(action.getActionType());

        // Все w(u, eventId) для текущего пользователя u.
        Map<Long, Double> weightsForUser = userEventWeights.computeIfAbsent(userId, ignored -> new TreeMap<>());
        double oldEventWeight = weightsForUser.getOrDefault(updatedEventId, 0.0);

        // w_new(u, A) = max(w_old(u, A), actionWeight). Более слабое повторное действие состояние не изменяет.
        double newEventWeight = Math.max(oldEventWeight, actionWeight);
        if (Double.compare(newEventWeight, oldEventWeight) == 0) {
            return List.of();
        }

        // S_new(A) = S_old(A) + w_new(u, A) - w_old(u, A).
        double newEventWeightSum = eventWeightSums.getOrDefault(updatedEventId, 0.0)
                + newEventWeight - oldEventWeight;
        eventWeightSums.put(updatedEventId, newEventWeightSum);
        weightsForUser.put(updatedEventId, newEventWeight);

        List<EventSimilarityAvro> similarities = new ArrayList<>();
        Instant calculationTime = Instant.now();
        // Пересчитываем только пары с мероприятиями B, с которыми взаимодействовал тот же пользователь u.
        // Если w(u, B) = 0, вклад пользователя в S_min(A, B) не меняется, поэтому сообщение публиковать не нужно.
        for (long otherEventId : weightsForUser.keySet()) {
            if (otherEventId == updatedEventId) {
                continue;
            }

            EventPair pair = EventPair.of(updatedEventId, otherEventId);
            double otherEventWeight = weightsForUser.getOrDefault(otherEventId, 0.0);

            // Delta S_min = min(w_new(u, A), w(u, B)) - min(w_old(u, A), w(u, B)).
            double oldMinWeight = Math.min(oldEventWeight, otherEventWeight);
            double newMinWeight = Math.min(newEventWeight, otherEventWeight);
            double newMinWeightSum = minWeightSums.getOrDefault(pair, 0.0) + newMinWeight - oldMinWeight;
            minWeightSums.put(pair, newMinWeightSum);

            // similarity(A, B) = S_min(A, B) / sqrt(S(A) * S(B)).
            double otherEventWeightSum = eventWeightSums.get(otherEventId);
            double similarity = newMinWeightSum / Math.sqrt(newEventWeightSum * otherEventWeightSum);

            similarities.add(EventSimilarityAvro.newBuilder()
                    .setEventA(pair.eventA())
                    .setEventB(pair.eventB())
                    .setScore(similarity)
                    .setTimestamp(calculationTime)
                    .build());
        }

        return similarities;
    }

    private double getActionWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }
}
