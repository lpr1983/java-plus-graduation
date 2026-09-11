package ewm.stats.analyzer.service;

import ewm.stats.analyzer.model.EventSimilarity;
import ewm.stats.analyzer.model.RecommendedEvent;
import ewm.stats.analyzer.model.UserInteraction;
import ewm.stats.analyzer.repository.EventSimilarityRepository;
import ewm.stats.analyzer.repository.UserInteractionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {
    private static final int NEAREST_NEIGHBORS_LIMIT = 20;

    private final UserInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    public RecommendationService(UserInteractionRepository userInteractionRepository,
                                 EventSimilarityRepository eventSimilarityRepository) {
        this.userInteractionRepository = userInteractionRepository;
        this.eventSimilarityRepository = eventSimilarityRepository;
    }

    public List<RecommendedEvent> getRecommendationsForUser(long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        List<UserInteraction> recentInteractions = userInteractionRepository.findRecentByUserId(userId, maxResults);

        if (recentInteractions.isEmpty()) {
            return List.of();
        }

        List<Long> recentEventIds = recentInteractions.stream()
                .map(UserInteraction::getEventId)
                .toList();

        List<Long> candidateIds = eventSimilarityRepository.findRecommendationCandidateIds(
                recentEventIds, userId, maxResults);

        List<RecommendedEvent> recommendations = new ArrayList<>();
        for (long candidateId : candidateIds) {
            double predictedScore = predictScore(candidateId, userId);
            RecommendedEvent recommendation = new RecommendedEvent(candidateId, predictedScore);
            recommendations.add(recommendation);
        }

        recommendations.sort((first, second) -> {
            int byScore = Double.compare(second.getScore(), first.getScore());
            return byScore != 0 ? byScore : Long.compare(first.getEventId(), second.getEventId());
        });

        return recommendations;
    }

    public List<RecommendedEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        List<EventSimilarity> similarities = eventSimilarityRepository.findNotInteractedByEventId(
                eventId, userId, maxResults);

        List<RecommendedEvent> recommendations = new ArrayList<>();
        for (EventSimilarity similarity : similarities) {
            long similarEventId = similarity.getOtherEventId(eventId);
            RecommendedEvent recommendation = new RecommendedEvent(similarEventId, similarity.getScore());
            recommendations.add(recommendation);
        }

        return recommendations;
    }

    public List<RecommendedEvent> getInteractionsCount(Collection<Long> eventIds) {
        Map<Long, Double> weightSums = userInteractionRepository.sumWeightsByEventIds(eventIds);

        return eventIds.stream()
                .distinct()
                .filter(weightSums::containsKey)
                .map(eventId -> new RecommendedEvent(eventId, weightSums.get(eventId)))
                .toList();
    }

    /**
     * Рассчитывает прогноз для мероприятия A по формуле
     * R(u, A) = sum(similarity(A, B) * w(u, B)) / sum(similarity(A, B)).
     * В расчёт входят K ближайших мероприятий B, с которыми взаимодействовал пользователь u.
     */
    private double predictScore(long eventId, long userId) {
        List<EventSimilarity> similarities = eventSimilarityRepository.findInteractedByEventId(
                eventId, userId, NEAREST_NEIGHBORS_LIMIT);

        List<Long> interactedEventIds = new ArrayList<>();
        for (EventSimilarity similarity : similarities) {
            interactedEventIds.add(similarity.getOtherEventId(eventId));
        }

        List<UserInteraction> interactions = userInteractionRepository.findByUserIdAndEventIds(
                userId, interactedEventIds);
        Map<Long, Double> weights = new HashMap<>();
        for (UserInteraction interaction : interactions) {
            weights.put(interaction.getEventId(), interaction.getWeight());
        }

        double weightedScoreSum = 0;
        double similaritySum = 0;

        for (EventSimilarity similarity : similarities) {
            long interactedEventId = similarity.getOtherEventId(eventId);
            Double weight = weights.get(interactedEventId);

            if (weight == null) {
                throw new IllegalStateException("Interaction used for prediction was not found: eventId="
                        + interactedEventId + ", userId=" + userId);
            }
            // Числитель: sum(similarity(A, B) * w(u, B)).
            weightedScoreSum += similarity.getScore() * weight;
            // Знаменатель: sum(similarity(A, B)).
            similaritySum += similarity.getScore();
        }

        if (similaritySum == 0) {
            throw new IllegalStateException("Cannot predict event " + eventId + ": sum of similarities is zero");
        }

        return weightedScoreSum / similaritySum;
    }
}
