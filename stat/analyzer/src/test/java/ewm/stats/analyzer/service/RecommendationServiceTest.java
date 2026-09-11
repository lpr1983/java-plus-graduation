package ewm.stats.analyzer.service;

import ewm.stats.analyzer.model.EventSimilarity;
import ewm.stats.analyzer.model.RecommendedEvent;
import ewm.stats.analyzer.model.UserInteraction;
import ewm.stats.analyzer.repository.EventSimilarityRepository;
import ewm.stats.analyzer.repository.UserInteractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {
    private static final long USER_ID = 1;
    private static final Instant TIMESTAMP = Instant.parse("2026-09-11T08:00:00Z");

    @Mock
    private UserInteractionRepository userInteractionRepository;
    @Mock
    private EventSimilarityRepository eventSimilarityRepository;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(userInteractionRepository, eventSimilarityRepository);
    }

    @Test
    void shouldCalculateRecommendationUsingWeightedAverage() {
        when(userInteractionRepository.findRecentByUserId(USER_ID, 10)).thenReturn(List.of(interaction(10, 0.4)));
        when(eventSimilarityRepository.findRecommendationCandidateIds(
                argThat(eventIds -> eventIds.equals(Set.of(10L))), eq(USER_ID), eq(10))).thenReturn(List.of(30L));
        when(eventSimilarityRepository.findInteractedByEventId(30, USER_ID, 20)).thenReturn(List.of(
                similarity(10, 30, 0.9),
                similarity(20, 30, 0.3)
        ));
        when(userInteractionRepository.findByUserIdAndEventIds(
                eq(USER_ID), argThat(eventIds -> eventIds.containsAll(List.of(10L, 20L)))))
                .thenReturn(List.of(interaction(10, 0.4), interaction(20, 0.8)));

        List<RecommendedEvent> result = recommendationService.getRecommendationsForUser(USER_ID, 10);

        assertThat(result).singleElement().satisfies(recommendation -> {
            assertThat(recommendation.getEventId()).isEqualTo(30);
            assertThat(recommendation.getScore()).isCloseTo(0.5, within(0.000001));
        });
        verify(eventSimilarityRepository).findInteractedByEventId(30, USER_ID, 20);
    }

    @Test
    void shouldReturnSimilarEventsNotInteractedByUser() {
        when(eventSimilarityRepository.findNotInteractedByEventId(10, USER_ID, 2)).thenReturn(List.of(
                similarity(10, 30, 0.9),
                similarity(5, 10, 0.7)
        ));

        List<RecommendedEvent> result = recommendationService.getSimilarEvents(10, USER_ID, 2);

        assertThat(result).extracting(RecommendedEvent::getEventId, RecommendedEvent::getScore)
                .containsExactly(
                        tuple(30L, 0.9),
                        tuple(5L, 0.7)
                );
    }

    @Test
    void shouldReturnInteractionWeightSumsInRequestOrder() {
        when(userInteractionRepository.sumWeightsByEventIds(List.of(30L, 10L, 40L)))
                .thenReturn(Map.of(10L, 1.2, 30L, 2.4));

        List<RecommendedEvent> result = recommendationService.getInteractionsCount(List.of(30L, 10L, 40L));

        assertThat(result).extracting(RecommendedEvent::getEventId, RecommendedEvent::getScore)
                .containsExactly(
                        tuple(30L, 2.4),
                        tuple(10L, 1.2)
                );
    }

    @Test
    void shouldReturnNoRecommendationsWhenUserHasNoInteractions() {
        when(userInteractionRepository.findRecentByUserId(USER_ID, 10)).thenReturn(List.of());

        assertThat(recommendationService.getRecommendationsForUser(USER_ID, 10)).isEmpty();

        verifyNoInteractions(eventSimilarityRepository);
    }

    @Test
    void shouldFailWhenSimilaritySumIsZero() {
        when(userInteractionRepository.findRecentByUserId(USER_ID, 10)).thenReturn(List.of(interaction(10, 0.4)));
        when(eventSimilarityRepository.findRecommendationCandidateIds(
                argThat(eventIds -> eventIds.contains(10L)), eq(USER_ID), eq(10))).thenReturn(List.of(30L));
        when(eventSimilarityRepository.findInteractedByEventId(30, USER_ID, 20))
                .thenReturn(List.of(similarity(10, 30, 0)));
        when(userInteractionRepository.findByUserIdAndEventIds(
                eq(USER_ID), argThat(eventIds -> eventIds.contains(10L))))
                .thenReturn(List.of(interaction(10, 0.4)));

        assertThatThrownBy(() -> recommendationService.getRecommendationsForUser(USER_ID, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot predict event 30: sum of similarities is zero");
    }

    private UserInteraction interaction(long eventId, double weight) {
        return new UserInteraction(USER_ID, eventId, weight, TIMESTAMP);
    }

    private EventSimilarity similarity(long eventA, long eventB, double score) {
        return EventSimilarity.of(eventA, eventB, score, TIMESTAMP);
    }
}
