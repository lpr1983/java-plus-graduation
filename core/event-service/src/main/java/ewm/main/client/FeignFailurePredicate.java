package ewm.main.client;

import feign.FeignException;
import feign.RetryableException;

import java.util.function.Predicate;

public class FeignFailurePredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof RetryableException) {
            return true;
        }

        return throwable instanceof FeignException exception && exception.status() >= 500;
    }
}
