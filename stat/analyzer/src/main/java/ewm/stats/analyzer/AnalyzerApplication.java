package ewm.stats.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ewm.stats.analyzer.service.AnalyzerProcessor;

@SpringBootApplication
public class AnalyzerApplication {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApplication.class, args)) {
            context.getBean(AnalyzerProcessor.class).run();
        }
    }
}
