package ewm.stats.analyzer.service;

import ewm.stats.analyzer.mapper.EventSimilarityMapper;
import ewm.stats.analyzer.mapper.UserInteractionMapper;
import ewm.stats.analyzer.repository.EventSimilarityRepository;
import ewm.stats.analyzer.repository.UserInteractionRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyzerProcessorTest {
    private static final String USER_ACTIONS_TOPIC = "stats.user-actions.v1";
    private static final String EVENT_SIMILARITIES_TOPIC = "stats.events-similarity.v1";
    private static final Instant TIMESTAMP = Instant.parse("2026-09-10T12:00:00Z");

    @Mock
    private Consumer<Long, UserActionAvro> userActionConsumer;
    @Mock
    private Consumer<String, EventSimilarityAvro> eventSimilarityConsumer;
    @Mock
    private UserInteractionRepository userInteractionRepository;
    @Mock
    private EventSimilarityRepository eventSimilarityRepository;

    private AnalyzerProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new AnalyzerProcessor(
                userActionConsumer,
                eventSimilarityConsumer,
                userInteractionRepository,
                eventSimilarityRepository,
                new UserInteractionMapper(),
                new EventSimilarityMapper(),
                USER_ACTIONS_TOPIC,
                EVENT_SIMILARITIES_TOPIC,
                100
        );
    }

    @Test
    void shouldProcessAndCommitUserActionsBeforeEventSimilarities() {
        when(userActionConsumer.poll(any(Duration.class))).thenReturn(userActionRecords());
        when(eventSimilarityConsumer.poll(any(Duration.class))).thenAnswer(invocation -> {
            processor.stop();
            return eventSimilarityRecords();
        });

        processor.run();

        InOrder order = inOrder(userActionConsumer, userInteractionRepository,
                eventSimilarityConsumer, eventSimilarityRepository);
        order.verify(userActionConsumer).poll(any(Duration.class));
        order.verify(userInteractionRepository).save(any());
        order.verify(userActionConsumer).commitSync();
        order.verify(eventSimilarityConsumer).poll(any(Duration.class));
        order.verify(eventSimilarityRepository).save(any());
        order.verify(eventSimilarityConsumer).commitSync();
    }

    @Test
    void shouldNotCommitOffsetOrReadSimilaritiesWhenUserActionSavingFails() {
        when(userActionConsumer.poll(any(Duration.class))).thenReturn(userActionRecords());
        doThrow(new IllegalStateException("Database is unavailable")).when(userInteractionRepository).save(any());

        assertThatThrownBy(processor::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database is unavailable");

        verify(userActionConsumer, never()).commitSync();
        verify(eventSimilarityConsumer, never()).poll(any(Duration.class));
        verifyNoInteractions(eventSimilarityRepository);
    }

    @Test
    void shouldNotCommitSimilarityOffsetWhenSavingFails() {
        when(userActionConsumer.poll(any(Duration.class))).thenReturn(new ConsumerRecords<>(Map.of()));
        when(eventSimilarityConsumer.poll(any(Duration.class))).thenReturn(eventSimilarityRecords());
        doThrow(new IllegalStateException("Database is unavailable")).when(eventSimilarityRepository).save(any());

        assertThatThrownBy(processor::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database is unavailable");

        verify(eventSimilarityConsumer, never()).commitSync();
    }

    private ConsumerRecords<Long, UserActionAvro> userActionRecords() {
        UserActionAvro action = UserActionAvro.newBuilder()
                .setUserId(1)
                .setEventId(10)
                .setActionType(ActionTypeAvro.VIEW)
                .setTimestamp(TIMESTAMP)
                .build();
        TopicPartition partition = new TopicPartition(USER_ACTIONS_TOPIC, 0);
        return new ConsumerRecords<>(Map.of(partition, List.of(new ConsumerRecord<>(USER_ACTIONS_TOPIC, 0, 0, 1L, action))));
    }

    private ConsumerRecords<String, EventSimilarityAvro> eventSimilarityRecords() {
        EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                .setEventA(10)
                .setEventB(20)
                .setScore(0.5)
                .setTimestamp(TIMESTAMP)
                .build();
        TopicPartition partition = new TopicPartition(EVENT_SIMILARITIES_TOPIC, 0);
        return new ConsumerRecords<>(
                Map.of(partition, List.of(new ConsumerRecord<>(EVENT_SIMILARITIES_TOPIC, 0, 0, "10:20", similarity))));
    }
}
