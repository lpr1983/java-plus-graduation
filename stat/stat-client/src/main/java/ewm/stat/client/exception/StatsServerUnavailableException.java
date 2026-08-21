package ewm.stat.client.exception;

public class StatsServerUnavailableException extends RuntimeException {
    public StatsServerUnavailableException(String message) {
        super(message);
    }

    public StatsServerUnavailableException(String message, Exception exception) {
        super(message, exception);
    }
}
