package ewm.stat.client.config;

import ewm.stat.client.StatClient;
import ewm.stat.client.StatClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class StatClientConfig {

    @Bean
    public StatClient statClient(@Value("${spring.application.name}") String app,
                                 @Value("${stat-client.timeout-ms:0}") int timeoutMs,
                                 @Value("${stat-service.name}") String statsServiceName,
                                 DiscoveryClient discoveryClient
                                 ) {

        return new StatClientImpl(app, timeoutMs, statsServiceName, discoveryClient);
    }
}
