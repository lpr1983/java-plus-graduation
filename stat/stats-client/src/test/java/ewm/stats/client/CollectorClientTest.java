package ewm.stats.client;

import com.google.protobuf.Empty;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectorClientTest {
    @Test
    void sendViewShouldSendViewActionToCollector() {
        UserActionControllerGrpc.UserActionControllerBlockingStub stub =
                mock(UserActionControllerGrpc.UserActionControllerBlockingStub.class);
        when(stub.collectUserAction(any())).thenReturn(Empty.getDefaultInstance());
        CollectorClient client = new CollectorClient(stub);

        client.sendView(11L, 22L);

        ArgumentCaptor<UserActionProto> actionCaptor = ArgumentCaptor.forClass(UserActionProto.class);
        verify(stub).collectUserAction(actionCaptor.capture());
        UserActionProto action = actionCaptor.getValue();
        assertEquals(11L, action.getUserId());
        assertEquals(22L, action.getEventId());
        assertEquals(ActionTypeProto.ACTION_VIEW, action.getActionType());
        assertTrue(action.hasTimestamp());
    }
}
