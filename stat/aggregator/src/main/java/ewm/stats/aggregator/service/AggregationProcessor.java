package ewm.stats.aggregator.service;

import jakarta.annotation.PreDestroy;
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
        consumer.subscribe(List.of(userActionsTopic));
        try {
            while (running) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(pollTimeout);
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    List<EventSimilarityAvro> similarities = similarityCalculator.update(record.value());
                    publishAndAwaitCompletion(similarities);
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException exception) {
            if (running) {
                throw exception;
            }
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

        // Все события одной калькуляции сначала передаются producer, чтобы Kafka могла отправить их одной пачкой.
        // Затем синхронно проверяем результат всей пачки до фиксации входного offset.
        for (Future<RecordMetadata> sendResult : sendResults) {
            try {
                sendResult.get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Event similarity publishing was interrupted", exception);
            } catch (ExecutionException | TimeoutException exception) {
                throw new IllegalStateException("Failed to publish event similarity", exception);
            }
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        consumer.wakeup();
    }
}
