package ewm.stats.collector.controller;

import com.google.protobuf.Empty;
import ewm.stats.collector.service.UserActionService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Slf4j
@GrpcService
public class UserActionGrpcController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionService userActionService;

    public UserActionGrpcController(UserActionService userActionService) {
        this.userActionService = userActionService;
    }

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.debug("Received user action: userId={}, eventId={}, actionType={}",
                request.getUserId(), request.getEventId(), request.getActionType());
        try {
            userActionService.collect(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.debug("User action collected: userId={}, eventId={}", request.getUserId(), request.getEventId());
        } catch (IllegalArgumentException exception) {
            log.warn("Rejected user action: userId={}, eventId={}, reason={}",
                    request.getUserId(), request.getEventId(), exception.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .withCause(exception)
                    .asRuntimeException());
        } catch (Exception exception) {
            log.error("Failed to collect user action: userId={}, eventId={}",
                    request.getUserId(), request.getEventId(), exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to collect user action")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }
}
