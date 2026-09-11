package ewm.requestservice.request.service;

import ewm.requestservice.dto.EventInternalDto;
import ewm.requestservice.dto.ParticipationRequestDto;
import ewm.requestservice.dto.UserShortDto;
import ewm.requestservice.event.EventClient;
import ewm.requestservice.request.model.ParticipationRequest;
import ewm.requestservice.request.repository.ParticipationRequestRepository;
import ewm.requestservice.user.UserClient;
import ewm.stats.client.CollectorClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParticipationRequestServiceImplTest {
    private ParticipationRequestRepository requestRepository;
    private UserClient userClient;
    private EventClient eventClient;
    private CollectorClient collectorClient;
    private ParticipationRequestService service;

    @BeforeEach
    void setUp() {
        requestRepository = mock(ParticipationRequestRepository.class);
        userClient = mock(UserClient.class);
        eventClient = mock(EventClient.class);
        collectorClient = mock(CollectorClient.class);
        service = new ParticipationRequestServiceImpl(requestRepository, userClient, eventClient, collectorClient);
    }

    @Test
    void addRequestShouldSendRegistrationAfterSavingRequest() {
        when(userClient.getUser(10L)).thenReturn(UserShortDto.builder().id(10L).name("User").build());
        when(eventClient.getEvent(20L)).thenReturn(EventInternalDto.builder()
                .id(20L)
                .initiatorId(30L)
                .state("PUBLISHED")
                .participantLimit(0)
                .build());
        when(requestRepository.save(any())).thenAnswer(invocation -> {
            ParticipationRequest request = invocation.getArgument(0);
            request.setId(1L);
            return request;
        });

        ParticipationRequestDto result = service.addRequest(10L, 20L);

        assertEquals(1L, result.getId());
        InOrder order = inOrder(requestRepository, collectorClient);
        order.verify(requestRepository).save(any(ParticipationRequest.class));
        order.verify(collectorClient).sendRegistration(10L, 20L);
    }
}
