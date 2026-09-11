package ewm.requestservice.request.service;

import ewm.requestservice.dto.EventInternalDto;
import ewm.requestservice.dto.EventRequestStatusUpdateRequestDto;
import ewm.requestservice.dto.EventRequestStatusUpdateResultDto;
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
import ewm.stats.client.CollectorClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final CollectorClient collectorClient;

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
        collectorClient.sendRegistration(userId, eventId);
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

    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(long userId, long eventId) {
        log.info("Getting requests for userId: {} and eventId: {}", userId, eventId);

        EventInternalDto event;
        try {
            event = eventClient.getEvent(eventId);
        } catch (FeignException.NotFound exception) {
            return Collections.emptyList();
        }

        if (!event.getInitiatorId().equals(userId)) {
            return Collections.emptyList();
        }

        return requestRepository.findAllByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResultDto setRequestsStatus(
            long userId,
            long eventId,
            EventRequestStatusUpdateRequestDto dto) {
        log.info("Setting request status for userId: {}, eventId: {}, dto: {}", userId, eventId, dto);

        RequestStatus statusToUpdate = RequestStatus.parse(dto.getStatus());

        if (statusToUpdate != RequestStatus.CONFIRMED && statusToUpdate != RequestStatus.REJECTED) {
            throw new ConflictException("Недопустимый статус для этой операции:" + statusToUpdate);
        }

        EventInternalDto event = findEventOfUserOrThrow(userId, eventId);
        List<Long> requestIds = dto.getRequestIds();

        if (requestIds.isEmpty()) {
            return new EventRequestStatusUpdateResultDto(Collections.emptyList(), Collections.emptyList());
        }

        List<Long> distinctIds = requestIds.stream().distinct().toList();

        if (statusToUpdate == RequestStatus.CONFIRMED) {
            return confirmRequests(event, distinctIds);
        }

        return rejectRequests(event, distinctIds);
    }

    private EventRequestStatusUpdateResultDto confirmRequests(EventInternalDto event, List<Long> requestIds) {
        int participantsLimit = event.getParticipantLimit();

        if (!event.isRequestModeration() || participantsLimit == 0) {
            throw new ConflictException("Подтверждение заявок не требуется");
        }

        long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long available = participantsLimit - confirmedRequests;

        if (requestIds.size() > available) {
            throw new ConflictException("Превышен лимит участников");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdInAndEventId(requestIds, event.getId());
        if (requests.size() < requestIds.size()) {
            throw new NotFoundException("Найдены не все заявки");
        }

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            RequestStatus currentStatus = request.getStatus();
            if (currentStatus != RequestStatus.PENDING) {
                throw new ConflictException("Нельзя изменить статус заявки с id: "
                        + request.getId() + ", она в статусе:" + currentStatus);
            }

            request.setStatus(RequestStatus.CONFIRMED);
            confirmed.add(request);
        }

        if (available != 0 && available == requestIds.size()) {
            List<ParticipationRequest> requestsToReject = requestRepository.findAllByEventIdAndStatus(
                    event.getId(), RequestStatus.PENDING);

            for (ParticipationRequest request : requestsToReject) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(request);
            }
        }

        EventRequestStatusUpdateResultDto result = new EventRequestStatusUpdateResultDto();
        result.setConfirmedRequests(confirmed.stream().map(ParticipationRequestMapper::toDto).toList());
        result.setRejectedRequests(rejected.stream().map(ParticipationRequestMapper::toDto).toList());

        return result;
    }

    private EventRequestStatusUpdateResultDto rejectRequests(EventInternalDto event, List<Long> requestIds) {
        List<ParticipationRequest> requests = requestRepository.findAllByIdInAndEventId(requestIds, event.getId());
        if (requests.size() < requestIds.size()) {
            throw new NotFoundException("Найдены не все заявки");
        }

        List<ParticipationRequest> rejected = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            RequestStatus currentStatus = request.getStatus();
            if (currentStatus != RequestStatus.PENDING) {
                throw new ConflictException("Нельзя изменить статус заявки с id: "
                        + request.getId() + ", она в статусе:" + currentStatus);
            }

            request.setStatus(RequestStatus.REJECTED);
            rejected.add(request);
        }

        EventRequestStatusUpdateResultDto result = new EventRequestStatusUpdateResultDto();
        result.setConfirmedRequests(Collections.emptyList());
        result.setRejectedRequests(rejected.stream().map(ParticipationRequestMapper::toDto).toList());

        return result;
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

    private EventInternalDto findEventOfUserOrThrow(long userId, long eventId) {
        EventInternalDto event = findEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException(
                    String.format("У пользователя с id: %d нет события с id: %d", userId, eventId));
        }

        return event;
    }
}
