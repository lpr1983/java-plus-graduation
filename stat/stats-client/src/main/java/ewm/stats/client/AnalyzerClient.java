package ewm.stats.client;

import ewm.stats.client.model.RecommendedEvent;
import net.devh.boot.grpc.client.inject.GrpcClient;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class AnalyzerClient {
    private final RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public AnalyzerClient(
            @GrpcClient("analyzer") RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub) {
        this.analyzerStub = analyzerStub;
    }

    public List<RecommendedEvent> getRecommendationsForUser(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        return collect(analyzerStub.getRecommendationsForUser(request));
    }

    public List<RecommendedEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        return collect(analyzerStub.getSimilarEvents(request));
    }

    public List<RecommendedEvent> getInteractionsCount(Collection<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();
        return collect(analyzerStub.getInteractionsCount(request));
    }

    private List<RecommendedEvent> collect(Iterator<RecommendedEventProto> responses) {
        List<RecommendedEvent> result = new ArrayList<>();
        responses.forEachRemaining(response -> result.add(
                new RecommendedEvent(response.getEventId(), response.getScore())
        ));
        return result;
    }
}
