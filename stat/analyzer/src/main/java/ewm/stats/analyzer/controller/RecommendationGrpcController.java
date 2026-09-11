package ewm.stats.analyzer.controller;

import ewm.stats.analyzer.mapper.RecommendedEventMapper;
import ewm.stats.analyzer.model.RecommendedEvent;
import ewm.stats.analyzer.service.RecommendationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@GrpcService
public class RecommendationGrpcController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;
    private final RecommendedEventMapper recommendedEventMapper;

    public RecommendationGrpcController(RecommendationService recommendationService,
                                        RecommendedEventMapper recommendedEventMapper) {
        this.recommendationService = recommendationService;
        this.recommendedEventMapper = recommendedEventMapper;
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        execute("user recommendations", responseObserver, () -> {
            validateId(request.getUserId(), "userId");
            validateMaxResults(request.getMaxResults());
            log.debug("Getting recommendations: userId={}, maxResults={}",
                    request.getUserId(), request.getMaxResults());
            return recommendationService.getRecommendationsForUser(request.getUserId(), request.getMaxResults());
        });
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        execute("similar events", responseObserver, () -> {
            validateId(request.getEventId(), "eventId");
            validateId(request.getUserId(), "userId");
            validateMaxResults(request.getMaxResults());
            log.debug("Getting similar events: eventId={}, userId={}, maxResults={}",
                    request.getEventId(), request.getUserId(), request.getMaxResults());
            return recommendationService.getSimilarEvents(
                    request.getEventId(), request.getUserId(), request.getMaxResults());
        });
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        execute("interaction counts", responseObserver, () -> {
            request.getEventIdList().forEach(eventId -> validateId(eventId, "eventId"));
            log.debug("Getting interaction counts for {} events", request.getEventIdCount());
            return recommendationService.getInteractionsCount(request.getEventIdList());
        });
    }

    private void execute(String operation, StreamObserver<RecommendedEventProto> responseObserver,
                         Supplier<List<RecommendedEvent>> action) {
        try {
            List<RecommendedEvent> recommendations = action.get();
            recommendations.stream().map(recommendedEventMapper::toProto).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
            log.debug("Completed {} request: results={}", operation, recommendations.size());
        } catch (IllegalArgumentException exception) {
            log.warn("Rejected {} request: {}", operation, exception.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .withCause(exception)
                    .asRuntimeException());
        } catch (Exception exception) {
            log.error("Failed to get {}", operation, exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get " + operation)
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    private void validateId(long id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    private void validateMaxResults(int maxResults) {
        if (maxResults <= 0) {
            throw new IllegalArgumentException("maxResults must be positive");
        }
    }
}
