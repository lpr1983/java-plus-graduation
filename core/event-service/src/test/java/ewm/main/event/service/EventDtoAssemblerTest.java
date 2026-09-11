package ewm.main.event.service;

import ewm.main.category.Category;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.UserShortDto;
import ewm.main.event.model.Event;
import ewm.main.location.LocationClient;
import ewm.main.request.RequestClient;
import ewm.main.user.UserClient;
import ewm.stats.client.AnalyzerClient;
import ewm.stats.client.model.RecommendedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventDtoAssemblerTest {
    private RequestClient requestClient;
    private UserClient userClient;
    private AnalyzerClient analyzerClient;
    private EventDtoAssembler assembler;

    @BeforeEach
    void setUp() {
        requestClient = mock(RequestClient.class);
        userClient = mock(UserClient.class);
        analyzerClient = mock(AnalyzerClient.class);
        assembler = new EventDtoAssembler(requestClient, userClient, mock(LocationClient.class), analyzerClient);
    }

    @Test
    void shouldRequestRatingsForEventListInOneBatch() {
        Event first = event(1L);
        Event second = event(2L);
        when(requestClient.getConfirmedRequestsCounts(any())).thenReturn(List.of());
        when(userClient.getUsers(any())).thenReturn(List.of(UserShortDto.builder().id(10L).name("User").build()));
        when(analyzerClient.getInteractionsCount(any())).thenReturn(List.of(new RecommendedEvent(1L, 2.5)));

        List<EventShortDto> result = assembler.toShortDtoList(List.of(first, second));

        verify(analyzerClient).getInteractionsCount(List.of(1L, 2L));
        assertEquals(2.5, result.get(0).getRating());
        assertEquals(0.0, result.get(1).getRating());
    }

    private Event event(long id) {
        return Event.builder()
                .id(id)
                .title("Event " + id)
                .annotation("Annotation")
                .category(Category.builder().id(1L).name("Category").build())
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiatorId(10L)
                .build();
    }
}
