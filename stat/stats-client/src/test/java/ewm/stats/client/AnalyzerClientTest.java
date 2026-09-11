package ewm.stats.client;

import ewm.stats.client.model.RecommendedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyzerClientTest {
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub stub;
    private AnalyzerClient client;

    @BeforeEach
    void setUp() {
        stub = mock(RecommendationsControllerGrpc.RecommendationsControllerBlockingStub.class);
        client = new AnalyzerClient(stub);
    }

    @Test
    void getRecommendationsForUserShouldSendRequestAndCollectResponses() {
        when(stub.getRecommendationsForUser(any())).thenReturn(responses());

        List<RecommendedEvent> result = client.getRecommendationsForUser(11L, 5);

        ArgumentCaptor<UserPredictionsRequestProto> requestCaptor =
                ArgumentCaptor.forClass(UserPredictionsRequestProto.class);
        verify(stub).getRecommendationsForUser(requestCaptor.capture());
        assertEquals(11L, requestCaptor.getValue().getUserId());
        assertEquals(5, requestCaptor.getValue().getMaxResults());
        assertEquals(expectedRecommendations(), result);
    }

    @Test
    void getSimilarEventsShouldSendRequestAndCollectResponses() {
        when(stub.getSimilarEvents(any())).thenReturn(responses());

        List<RecommendedEvent> result = client.getSimilarEvents(22L, 11L, 3);

        ArgumentCaptor<SimilarEventsRequestProto> requestCaptor =
                ArgumentCaptor.forClass(SimilarEventsRequestProto.class);
        verify(stub).getSimilarEvents(requestCaptor.capture());
        assertEquals(22L, requestCaptor.getValue().getEventId());
        assertEquals(11L, requestCaptor.getValue().getUserId());
        assertEquals(3, requestCaptor.getValue().getMaxResults());
        assertEquals(expectedRecommendations(), result);
    }

    @Test
    void getInteractionsCountShouldSendEventIdsAndCollectResponses() {
        when(stub.getInteractionsCount(any())).thenReturn(responses());

        List<RecommendedEvent> result = client.getInteractionsCount(List.of(22L, 33L));

        ArgumentCaptor<InteractionsCountRequestProto> requestCaptor =
                ArgumentCaptor.forClass(InteractionsCountRequestProto.class);
        verify(stub).getInteractionsCount(requestCaptor.capture());
        assertEquals(List.of(22L, 33L), requestCaptor.getValue().getEventIdList());
        assertEquals(expectedRecommendations(), result);
    }

    private Iterator<RecommendedEventProto> responses() {
        return List.of(
                RecommendedEventProto.newBuilder().setEventId(22L).setScore(0.8).build(),
                RecommendedEventProto.newBuilder().setEventId(33L).setScore(0.6).build()
        ).iterator();
    }

    private List<RecommendedEvent> expectedRecommendations() {
        return List.of(new RecommendedEvent(22L, 0.8), new RecommendedEvent(33L, 0.6));
    }
}
