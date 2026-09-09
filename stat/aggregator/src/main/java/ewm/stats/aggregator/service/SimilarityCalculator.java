package ewm.stats.aggregator.service;

import ewm.stats.aggregator.model.EventPair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Реализует формулу сходства мероприятий similarity(A, B) = S_min(A, B) / (sqrt(S(A)) * sqrt(S(B))).
 * Промежуточные суммы, необходимые для последовательного пересчёта результата, хранит в памяти.
 * w(u, A) — вес действия пользователя u для мероприятия A:
 * VIEW = 0.4, REGISTER = 0.8, LIKE = 1.0.
 * S(A) = sum(w(u, A)) — сумма весов мероприятия A по всем пользователям.
 * S(B) = sum(w(u, B)) — сумма весов мероприятия B по всем пользователям.
 * S_min(A, B) = sum(min(w(u, A), w(u, B))). Для пользователя u определяется вес взаимодействия с A и вес с B.
 * Из этих двух весов оставляется минимальный. Затем минимальные веса складываются по всем пользователям.
 * Если пользователь u не взаимодействовал с одним из мероприятий, его вклад в S_min(A, B) равен нулю.
 */
@Slf4j
@Component
public class SimilarityCalculator {
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    // w(u, A) — веса мероприятий для пользователей.
    // Внешний ключ — userId, внутренний ключ — eventId, значение — вес w(u, A).
    private final Map<Long, Map<Long, Double>> userEventWeights = new HashMap<>();

    // S(A) — по мероприятиями.
    // Ключ — eventId, значение — S(A).
    private final Map<Long, Double> eventWeightSums = new HashMap<>();

    // S_min(A, B) — по парам мероприятий.
    // Ключ — пара мероприятий, значение — S_min(A, B).
    private final Map<EventPair, Double> minWeightSums = new HashMap<>();

    public List<EventSimilarityAvro> update(UserActionAvro action) {
        long userId = action.getUserId();
        long updatedEventId = action.getEventId();
        double actionWeight = getActionWeight(action.getActionType());

        // w(u, A) — веса мероприятий для текущего пользователя userId.
        // Ключ — eventId, значение — вес мероприятия для текущего пользователя.
        Map<Long, Double> weightsForUser = userEventWeights.computeIfAbsent(userId, i -> new HashMap<>());
        double oldEventWeight = weightsForUser.getOrDefault(updatedEventId, 0.0);

        // Если вес нового действия не превышает сохранённый w(u, A),
        // состояние и коэффициенты сходства не изменяются.
        double newEventWeight = Math.max(oldEventWeight, actionWeight);
        if (Double.compare(newEventWeight, oldEventWeight) == 0) {
            log.debug("Ignored user action that does not increase weight: userId={}, eventId={}, actionType={}, weight={}",
                    userId, updatedEventId, action.getActionType(), oldEventWeight);
            return List.of();
        }

        // S_new(A) = S_old(A) + w_new(u, A) - w_old(u, A).
        double newEventWeightSum = eventWeightSums.getOrDefault(updatedEventId, 0.0)
                + newEventWeight - oldEventWeight;
        eventWeightSums.put(updatedEventId, newEventWeightSum);
        weightsForUser.put(updatedEventId, newEventWeight);

        List<EventSimilarityAvro> similarities = new ArrayList<>();
        Instant calculationTime = Instant.now();

        // weightsForUser содержит только мероприятия, с которыми взаимодействовал текущий пользователь.
        // Поэтому мероприятия B с w(u, B) = 0 не входят в цикл и сообщение для такой пары не формируется.
        for (long otherEventId : weightsForUser.keySet()) {
            if (otherEventId == updatedEventId) {
                continue;
            }

            EventPair pair = EventPair.of(updatedEventId, otherEventId);
            double otherEventWeight = weightsForUser.get(otherEventId);

            // Delta S_min = min(w_new(u, A), w(u, B)) - min(w_old(u, A), w(u, B)).
            double oldMinWeight = Math.min(oldEventWeight, otherEventWeight);
            double newMinWeight = Math.min(newEventWeight, otherEventWeight);
            double newMinWeightSum = minWeightSums.getOrDefault(pair, 0.0) + newMinWeight - oldMinWeight;
            minWeightSums.put(pair, newMinWeightSum);

            // similarity(A, B) = S_min(A, B) / (sqrt(S(A)) * sqrt(S(B))).
            double otherEventWeightSum = eventWeightSums.get(otherEventId);
            if (newEventWeightSum <= 0 || otherEventWeightSum <= 0) {
                throw new IllegalStateException("Event weight sums must be positive: eventId=" + updatedEventId
                        + ", sum=" + newEventWeightSum + ", eventId=" + otherEventId + ", sum=" + otherEventWeightSum);
            }
            double similarity = newMinWeightSum / (Math.sqrt(newEventWeightSum) * Math.sqrt(otherEventWeightSum));
            log.trace("Calculated similarity: eventA={}, eventB={}, minWeightSum={}, eventWeightSum={}, "
                            + "otherEventWeightSum={}, similarity={}",
                    pair.getEventA(), pair.getEventB(), newMinWeightSum, newEventWeightSum, otherEventWeightSum, similarity);

            similarities.add(EventSimilarityAvro.newBuilder()
                    .setEventA(pair.getEventA())
                    .setEventB(pair.getEventB())
                    .setScore(similarity)
                    .setTimestamp(calculationTime)
                    .build());
        }

        log.debug("Updated event weight: userId={}, eventId={}, oldWeight={}, newWeight={}, similarities={}",
                userId, updatedEventId, oldEventWeight, newEventWeight, similarities.size());
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
