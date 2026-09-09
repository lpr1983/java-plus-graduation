package ewm.stats.aggregator.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class AggregationProcessor {
    private final Consumer<Long, UserActionAvro> consumer;
    private final Producer<String, SpecificRecordBase> producer;
    private final SimilarityCalculator similarityCalculator;
    private final String userActionsTopic;
    private final String eventsSimilarityTopic;
    private final Duration pollTimeout;
    private final long sendTimeoutMs;
    private volatile boolean running = true;

    public AggregationProcessor(Consumer<Long, UserActionAvro> consumer,
                                Producer<String, SpecificRecordBase> producer,
                                SimilarityCalculator similarityCalculator,
                                @Value("${kafka.topic.user-actions}") String userActionsTopic,
                                @Value("${kafka.topic.events-similarity}") String eventsSimilarityTopic,
                                @Value("${kafka.consumer.poll-timeout-ms}") long pollTimeoutMs,
                                @Value("${kafka.producer.send-timeout-ms}") long sendTimeoutMs) {
        this.consumer = consumer;
        this.producer = producer;
        this.similarityCalculator = similarityCalculator;
        this.userActionsTopic = userActionsTopic;
        this.eventsSimilarityTopic = eventsSimilarityTopic;
        this.pollTimeout = Duration.ofMillis(pollTimeoutMs);
        this.sendTimeoutMs = sendTimeoutMs;
    }

    public void run() {
        log.info("Starting aggregation: inputTopic={}, outputTopic={}", userActionsTopic, eventsSimilarityTopic);
        consumer.subscribe(List.of(userActionsTopic));
        try {
            while (running) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(pollTimeout);
                if (!records.isEmpty()) {
                    log.debug("Polled {} user actions from topic {}", records.count(), userActionsTopic);
                }
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    List<EventSimilarityAvro> similarities = similarityCalculator.update(record.value());
                    publishAndAwaitCompletion(similarities);
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                    log.debug("Committed offsets after processing {} user actions", records.count());
                }
            }
        } catch (WakeupException exception) {
            if (running) {
                log.error("Aggregation consumer was unexpectedly woken up", exception);
                throw exception;
            }
        } finally {
            log.info("Aggregation processor stopped");
        }
    }

    private void publishAndAwaitCompletion(List<EventSimilarityAvro> similarities) {
        List<Future<RecordMetadata>> sendResults = new ArrayList<>(similarities.size());
        for (EventSimilarityAvro similarity : similarities) {
            String key = similarity.getEventA() + ":" + similarity.getEventB();
            ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(eventsSimilarityTopic, key, similarity);

            Future<RecordMetadata> sendResult = producer.send(record);

            sendResults.add(sendResult);
        }

        // Отправка всех событий инициируется до ожидания, чтобы не блокироваться после каждого вызова producer.send().
        // Подтверждение каждого события проверяется до фиксации входного offset.
        for (Future<RecordMetadata> sendResult : sendResults) {
            try {
                sendResult.get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while publishing event similarities to topic {}", eventsSimilarityTopic, exception);
                throw new IllegalStateException("Event similarity publishing was interrupted", exception);
            } catch (ExecutionException | TimeoutException exception) {
                log.error("Failed to publish event similarities to topic {}", eventsSimilarityTopic, exception);
                throw new IllegalStateException("Failed to publish event similarity", exception);
            }
        }
        if (!similarities.isEmpty()) {
            log.debug("Published {} event similarities to topic {}", similarities.size(), eventsSimilarityTopic);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping aggregation processor");
        running = false;
        consumer.wakeup();
    }
}
