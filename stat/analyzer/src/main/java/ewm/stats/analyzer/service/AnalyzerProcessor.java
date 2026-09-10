package ewm.stats.analyzer.service;

import ewm.stats.analyzer.model.EventSimilarity;
import ewm.stats.analyzer.model.UserInteraction;
import ewm.stats.analyzer.repository.EventSimilarityRepository;
import ewm.stats.analyzer.repository.UserInteractionRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class AnalyzerProcessor {
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    private final Consumer<Long, UserActionAvro> userActionConsumer;
    private final Consumer<String, EventSimilarityAvro> eventSimilarityConsumer;
    private final UserInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;
    private final String userActionsTopic;
    private final String eventSimilaritiesTopic;
    private final Duration pollTimeout;
    private volatile boolean running = true;

    public AnalyzerProcessor(Consumer<Long, UserActionAvro> userActionConsumer,
                             Consumer<String, EventSimilarityAvro> eventSimilarityConsumer,
                             UserInteractionRepository userInteractionRepository,
                             EventSimilarityRepository eventSimilarityRepository,
                             @Value("${kafka.topic.user-actions}") String userActionsTopic,
                             @Value("${kafka.topic.events-similarity}") String eventSimilaritiesTopic,
                             @Value("${kafka.consumer.poll-timeout-ms}") long pollTimeoutMs) {
        this.userActionConsumer = userActionConsumer;
        this.eventSimilarityConsumer = eventSimilarityConsumer;
        this.userInteractionRepository = userInteractionRepository;
        this.eventSimilarityRepository = eventSimilarityRepository;
        this.userActionsTopic = userActionsTopic;
        this.eventSimilaritiesTopic = eventSimilaritiesTopic;
        this.pollTimeout = Duration.ofMillis(pollTimeoutMs);
    }

    public void run() {
        log.info("Starting analyzer: userActionsTopic={}, eventSimilaritiesTopic={}", userActionsTopic, eventSimilaritiesTopic);
        userActionConsumer.subscribe(List.of(userActionsTopic));
        eventSimilarityConsumer.subscribe(List.of(eventSimilaritiesTopic));
        try {
            while (running) {
                processUserActions();
                processEventSimilarities();
            }
        } catch (WakeupException exception) {
            if (running) {
                log.error("Analyzer consumer was unexpectedly woken up", exception);
                throw exception;
            }
        } finally {
            log.info("Analyzer processor stopped");
        }
    }

    private void processUserActions() {
        ConsumerRecords<Long, UserActionAvro> records = userActionConsumer.poll(pollTimeout);
        for (ConsumerRecord<Long, UserActionAvro> record : records) {
            UserActionAvro action = record.value();
            UserInteraction interaction = new UserInteraction(
                    action.getUserId(), action.getEventId(), getActionWeight(action.getActionType()), action.getTimestamp());
            userInteractionRepository.save(interaction);
        }
        if (!records.isEmpty()) {
            userActionConsumer.commitSync();
            log.debug("Saved and committed {} user actions", records.count());
        }
    }

    private void processEventSimilarities() {
        ConsumerRecords<String, EventSimilarityAvro> records = eventSimilarityConsumer.poll(pollTimeout);
        for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
            EventSimilarityAvro similarity = record.value();
            eventSimilarityRepository.save(EventSimilarity.of(similarity.getEventA(), similarity.getEventB(),
                    similarity.getScore(), similarity.getTimestamp()));
        }
        if (!records.isEmpty()) {
            eventSimilarityConsumer.commitSync();
            log.debug("Saved and committed {} event similarities", records.count());
        }
    }

    private double getActionWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping analyzer processor");
        running = false;
        userActionConsumer.wakeup();
        eventSimilarityConsumer.wakeup();
    }
}
