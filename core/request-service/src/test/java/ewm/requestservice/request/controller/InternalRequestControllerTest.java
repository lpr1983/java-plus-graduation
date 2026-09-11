package ewm.requestservice.request.controller;

import ewm.requestservice.request.model.RequestStatus;
import ewm.requestservice.request.repository.ParticipationRequestRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalRequestControllerTest {
    @Test
    void shouldCheckConfirmedParticipation() {
        ParticipationRequestRepository repository = mock(ParticipationRequestRepository.class);
        when(repository.existsByRequesterIdAndEventIdAndStatus(10L, 20L, RequestStatus.CONFIRMED))
                .thenReturn(true);
        InternalRequestController controller = new InternalRequestController(repository);

        boolean result = controller.hasConfirmedParticipation(10L, 20L);

        assertTrue(result);
        verify(repository).existsByRequesterIdAndEventIdAndStatus(10L, 20L, RequestStatus.CONFIRMED);
    }
}
