package ewm.main.event.service;

import ewm.main.dto.EventShortDto;
import ewm.main.event.model.Event;
import ewm.main.event.model.EventState;
import ewm.main.event.repository.EventRepository;
import ewm.main.exception.ValidationException;
import ewm.main.location.LocationClient;
import ewm.main.request.RequestClient;
import ewm.stats.client.AnalyzerClient;
import ewm.stats.client.CollectorClient;
import ewm.stats.client.model.RecommendedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicEventServiceImplTest {
    private EventRepository eventRepository;
    private EventDtoAssembler eventDtoAssembler;
    private AnalyzerClient analyzerClient;
    private RequestClient requestClient;
    private CollectorClient collectorClient;
    private PublicEventService service;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        eventDtoAssembler = mock(EventDtoAssembler.class);
        analyzerClient = mock(AnalyzerClient.class);
        requestClient = mock(RequestClient.class);
        collectorClient = mock(CollectorClient.class);
        service = new PublicEventServiceImpl(
                eventRepository,
                eventDtoAssembler,
                mock(LocationClient.class),
                analyzerClient,
                requestClient,
                collectorClient
        );
    }

    @Test
    void getRecommendationsShouldPreserveAnalyzerOrderAndSkipUnavailableEvents() {
        Event first = Event.builder().id(1L).build();
        Event second = Event.builder().id(2L).build();
        List<EventShortDto> expected = List.of(
                EventShortDto.builder().id(2L).build(),
                EventShortDto.builder().id(1L).build()
        );
        when(analyzerClient.getRecommendationsForUser(10L, 3)).thenReturn(List.of(
                new RecommendedEvent(2L, 0.9),
                new RecommendedEvent(3L, 0.8),
                new RecommendedEvent(1L, 0.7)
        ));
        when(eventRepository.findAllByIdInAndState(List.of(2L, 3L, 1L), EventState.PUBLISHED))
                .thenReturn(List.of(first, second));
        when(eventDtoAssembler.toShortDtoListForRead(List.of(second, first))).thenReturn(expected);

        List<EventShortDto> result = service.getRecommendations(10L, 3);

        assertEquals(expected, result);
        verify(analyzerClient).getRecommendationsForUser(10L, 3);
        verify(eventRepository).findAllByIdInAndState(List.of(2L, 3L, 1L), EventState.PUBLISHED);
        verify(eventDtoAssembler).toShortDtoListForRead(List.of(second, first));
    }

    @Test
    void likeEventShouldSendLikeForConfirmedParticipant() {
        when(eventRepository.findOneByIdAndState(20L, EventState.PUBLISHED))
                .thenReturn(Optional.of(Event.builder().id(20L).build()));
        when(requestClient.hasConfirmedParticipation(10L, 20L)).thenReturn(true);

        service.likeEvent(10L, 20L);

        verify(collectorClient).sendLike(10L, 20L);
    }

    @Test
    void likeEventShouldRejectUserWithoutConfirmedParticipation() {
        when(eventRepository.findOneByIdAndState(20L, EventState.PUBLISHED))
                .thenReturn(Optional.of(Event.builder().id(20L).build()));
        when(requestClient.hasConfirmedParticipation(10L, 20L)).thenReturn(false);

        assertThrows(ValidationException.class, () -> service.likeEvent(10L, 20L));

        verifyNoInteractions(collectorClient);
    }
}
