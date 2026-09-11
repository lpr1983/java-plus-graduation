package ewm.stats.analyzer.controller;

import ewm.stats.analyzer.mapper.RecommendedEventMapper;
import ewm.stats.analyzer.model.RecommendedEvent;
import ewm.stats.analyzer.service.RecommendationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationGrpcControllerTest {
    @Mock
    private RecommendationService recommendationService;
    @Mock
    private StreamObserver<RecommendedEventProto> responseObserver;

    private RecommendationGrpcController controller;

    @BeforeEach
    void setUp() {
        controller = new RecommendationGrpcController(recommendationService, new RecommendedEventMapper());
    }

    @Test
    void shouldStreamUserRecommendationsAndCompleteResponse() {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(1)
                .setMaxResults(2)
                .build();
        when(recommendationService.getRecommendationsForUser(1, 2)).thenReturn(List.of(
                new RecommendedEvent(10, 0.8),
                new RecommendedEvent(20, 0.6)
        ));

        controller.getRecommendationsForUser(request, responseObserver);

        InOrder order = inOrder(responseObserver);
        order.verify(responseObserver).onNext(recommendation(10, 0.8));
        order.verify(responseObserver).onNext(recommendation(20, 0.6));
        order.verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    @Test
    void shouldStreamSimilarEventsAndCompleteResponse() {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(10)
                .setUserId(1)
                .setMaxResults(1)
                .build();
        when(recommendationService.getSimilarEvents(10, 1, 1))
                .thenReturn(List.of(new RecommendedEvent(20, 0.7)));

        controller.getSimilarEvents(request, responseObserver);

        verify(responseObserver).onNext(recommendation(20, 0.7));
        verify(responseObserver).onCompleted();
    }

    @Test
    void shouldStreamInteractionCountsAndCompleteResponse() {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(List.of(10L, 20L))
                .build();
        when(recommendationService.getInteractionsCount(List.of(10L, 20L)))
                .thenReturn(List.of(new RecommendedEvent(10, 2.4)));

        controller.getInteractionsCount(request, responseObserver);

        verify(responseObserver).onNext(recommendation(10, 2.4));
        verify(responseObserver).onCompleted();
    }

    @Test
    void shouldRejectInvalidRequest() {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(0)
                .setMaxResults(10)
                .build();

        controller.getRecommendationsForUser(request, responseObserver);

        verify(responseObserver).onError(argThat(error ->
                Status.fromThrowable(error).getCode() == Status.Code.INVALID_ARGUMENT));
        verify(responseObserver, never()).onCompleted();
        verifyNoInteractions(recommendationService);
    }

    @Test
    void shouldReturnInternalErrorWhenServiceFails() {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(10)
                .setUserId(1)
                .setMaxResults(5)
                .build();
        when(recommendationService.getSimilarEvents(10, 1, 5))
                .thenThrow(new IllegalStateException("Calculation failed"));

        controller.getSimilarEvents(request, responseObserver);

        verify(responseObserver).onError(argThat(error ->
                Status.fromThrowable(error).getCode() == Status.Code.INTERNAL));
        verify(responseObserver, never()).onCompleted();
    }

    private RecommendedEventProto recommendation(long eventId, double score) {
        return RecommendedEventProto.newBuilder().setEventId(eventId).setScore(score).build();
    }
}
