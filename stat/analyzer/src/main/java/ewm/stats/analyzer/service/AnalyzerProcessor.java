package ewm.stats.analyzer.service;

import ewm.stats.analyzer.mapper.EventSimilarityMapper;
import ewm.stats.analyzer.mapper.UserInteractionMapper;
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
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class AnalyzerProcessor {
    private final Consumer<Long, UserActionAvro> userActionConsumer;
    private final Consumer<String, EventSimilarityAvro> eventSimilarityConsumer;
    private final UserInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;
    private final UserInteractionMapper userInteractionMapper;
    private final EventSimilarityMapper eventSimilarityMapper;
    private final String userActionsTopic;
    private final String eventSimilaritiesTopic;
    private final Duration pollTimeout;
    private volatile boolean running = true;

    public AnalyzerProcessor(Consumer<Long, UserActionAvro> userActionConsumer,
                             Consumer<String, EventSimilarityAvro> eventSimilarityConsumer,
                             UserInteractionRepository userInteractionRepository,
                             EventSimilarityRepository eventSimilarityRepository,
                             UserInteractionMapper userInteractionMapper,
                             EventSimilarityMapper eventSimilarityMapper,
                             @Value("${kafka.topic.user-actions}") String userActionsTopic,
                             @Value("${kafka.topic.events-similarity}") String eventSimilaritiesTopic,
                             @Value("${kafka.consumer.poll-timeout-ms}") long pollTimeoutMs) {
        this.userActionConsumer = userActionConsumer;
        this.eventSimilarityConsumer = eventSimilarityConsumer;
        this.userInteractionRepository = userInteractionRepository;
        this.eventSimilarityRepository = eventSimilarityRepository;
        this.userInteractionMapper = userInteractionMapper;
        this.eventSimilarityMapper = eventSimilarityMapper;
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
        } catch (WakeupException ignored) {
        } finally {
            log.info("Analyzer processor stopped");
        }
    }

    private void processUserActions() {
        ConsumerRecords<Long, UserActionAvro> records = userActionConsumer.poll(pollTimeout);
        for (ConsumerRecord<Long, UserActionAvro> record : records) {
            userInteractionRepository.save(userInteractionMapper.toModel(record.value()));
        }
        if (!records.isEmpty()) {
            userActionConsumer.commitSync();
            log.debug("Saved and committed {} user actions", records.count());
        }
    }

    private void processEventSimilarities() {
        ConsumerRecords<String, EventSimilarityAvro> records = eventSimilarityConsumer.poll(pollTimeout);
        for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
            eventSimilarityRepository.save(eventSimilarityMapper.toModel(record.value()));
        }
        if (!records.isEmpty()) {
            eventSimilarityConsumer.commitSync();
            log.debug("Saved and committed {} event similarities", records.count());
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping analyzer processor");
        running = false;
        userActionConsumer.wakeup();
        eventSimilarityConsumer.wakeup();
    }
}
