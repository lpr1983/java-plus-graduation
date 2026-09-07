package ewm.stats.collector.controller;

import com.google.protobuf.Empty;
import ewm.stats.collector.service.UserActionService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@GrpcService
public class UserActionGrpcController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionService userActionService;

    public UserActionGrpcController(UserActionService userActionService) {
        this.userActionService = userActionService;
    }

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            userActionService.collect(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .withCause(exception)
                    .asRuntimeException());
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to collect user action")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }
}
