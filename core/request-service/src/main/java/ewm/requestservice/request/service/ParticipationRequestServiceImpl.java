package ewm.requestservice.request.service;

import ewm.requestservice.dto.EventInternalDto;
import ewm.requestservice.dto.ParticipationRequestDto;
import ewm.requestservice.dto.UserShortDto;
import ewm.requestservice.event.EventClient;
import ewm.requestservice.exception.ConflictException;
import ewm.requestservice.exception.NotFoundException;
import ewm.requestservice.request.mapper.ParticipationRequestMapper;
import ewm.requestservice.request.model.ParticipationRequest;
import ewm.requestservice.request.model.RequestStatus;
import ewm.requestservice.request.repository.ParticipationRequestRepository;
import ewm.requestservice.user.UserClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    public List<ParticipationRequestDto> getRequests(long userId) {
        log.info("Getting requests for userId: {}", userId);
        findUserOrThrow(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(long userId, long eventId) {
        log.info("Adding request from userId: {} to eventId: {}", userId, eventId);

        UserShortDto requester = findUserOrThrow(userId);
        EventInternalDto event = findEventOrThrow(eventId);

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Нельзя подать заявку на участие в своём событии");
        }

        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Заявка на участие уже существует");
        }

        int limit = event.getParticipantLimit();
        if (limit > 0) {
            long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmed >= limit) {
                throw new ConflictException("Достигнут лимит участников события");
            }
        }

        RequestStatus status = RequestStatus.PENDING;
        if (!event.isRequestModeration() || limit == 0) {
            status = RequestStatus.CONFIRMED;
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .eventId(event.getId())
                .requesterId(requester.getId())
                .status(status)
                .created(LocalDateTime.now())
                .build();

        ParticipationRequestDto result = ParticipationRequestMapper.toDto(requestRepository.save(request));
        log.info("Request created with id: {}", result.getId());
        return result;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(long userId, long requestId) {
        log.info("Cancelling requestId: {} by userId: {}", requestId, userId);

        findUserOrThrow(userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка с id=" + requestId + " не найдена"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("Нельзя отменить чужую заявку");
        }

        request.setStatus(RequestStatus.CANCELED);
        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    private UserShortDto findUserOrThrow(long userId) {
        try {
            return userClient.getUser(userId);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private EventInternalDto findEventOrThrow(long eventId) {
        try {
            return eventClient.getEvent(eventId);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
    }
}
