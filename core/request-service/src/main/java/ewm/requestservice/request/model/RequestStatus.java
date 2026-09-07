package ewm.requestservice.request.model;

import ewm.requestservice.exception.ValidationException;

public enum RequestStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELED;

    public static RequestStatus parse(String state) {
        try {
            return RequestStatus.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid RequestStatus: " + state);
        }
    }
}
