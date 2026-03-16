package com.personal.ipvalidator.runner;

import com.personal.ipvalidator.model.RequestResult;
import com.personal.ipvalidator.service.IpValidatorService;
import com.personal.ipvalidator.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ValidationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ValidationRunner.class);

    private final IpValidatorService ipValidatorService;
    private final ReportService reportService;

    public ValidationRunner(IpValidatorService ipValidatorService, ReportService reportService) {
        this.ipValidatorService = ipValidatorService;
        this.reportService = reportService;
    }

    @Override
    public void run(String... args) throws Exception {
        String realIp = ipValidatorService.resolveRealIp();

        Instant start = Instant.now();
        List<RequestResult> results = ipValidatorService.runValidation();
        Duration runDuration = Duration.between(start, Instant.now());

        var reportPath = reportService.generateReport(realIp, results, runDuration);

        long succeeded = results.stream().filter(RequestResult::success).count();
        long failed = results.size() - succeeded;
        log.info("Validation complete: {} succeeded, {} failed. Report: {}",
                succeeded, failed, reportPath.toAbsolutePath());
    }
}
