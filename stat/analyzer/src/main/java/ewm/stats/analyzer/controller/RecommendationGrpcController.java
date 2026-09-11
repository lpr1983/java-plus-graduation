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
        try {
            validateMaxResults(request.getMaxResults());

            log.debug("Getting recommendations: userId={}, maxResults={}",
                    request.getUserId(), request.getMaxResults());

            List<RecommendedEvent> recommendations = recommendationService.getRecommendationsForUser(
                    request.getUserId(), request.getMaxResults());

            for (RecommendedEvent recommendation : recommendations) {
                responseObserver.onNext(recommendedEventMapper.toProto(recommendation));
            }

            responseObserver.onCompleted();
            log.debug("Completed user recommendations request: results={}", recommendations.size());
        } catch (IllegalArgumentException exception) {
            log.warn("Rejected user recommendations request: {}", exception.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .withCause(exception)
                    .asRuntimeException());
        } catch (Exception exception) {
            log.error("Failed to get user recommendations", exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get user recommendations")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            validateMaxResults(request.getMaxResults());

            log.debug("Getting similar events: eventId={}, userId={}, maxResults={}",
                    request.getEventId(), request.getUserId(), request.getMaxResults());

            List<RecommendedEvent> recommendations = recommendationService.getSimilarEvents(
                    request.getEventId(), request.getUserId(), request.getMaxResults());

            for (RecommendedEvent recommendation : recommendations) {
                responseObserver.onNext(recommendedEventMapper.toProto(recommendation));
            }

            responseObserver.onCompleted();
            log.debug("Completed similar events request: results={}", recommendations.size());
        } catch (IllegalArgumentException exception) {
            log.warn("Rejected similar events request: {}", exception.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .withCause(exception)
                    .asRuntimeException());
        } catch (Exception exception) {
            log.error("Failed to get similar events", exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get similar events")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.debug("Getting interaction counts for {} events", request.getEventIdCount());

            List<RecommendedEvent> recommendations = recommendationService.getInteractions(request.getEventIdList());

            for (RecommendedEvent recommendation : recommendations) {
                responseObserver.onNext(recommendedEventMapper.toProto(recommendation));
            }

            responseObserver.onCompleted();
            log.debug("Completed interaction counts request: results={}", recommendations.size());
        } catch (IllegalArgumentException exception) {
            log.warn("Rejected interaction counts request: {}", exception.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .withCause(exception)
                    .asRuntimeException());
        } catch (Exception exception) {
            log.error("Failed to get interaction counts", exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get interaction counts")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    private void validateMaxResults(int maxResults) {
        if (maxResults <= 0) {
            throw new IllegalArgumentException("maxResults must be positive");
        }
    }
}
