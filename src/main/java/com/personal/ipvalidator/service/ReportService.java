package com.personal.ipvalidator.service;

import com.personal.ipvalidator.model.RequestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    public Path generateReport(String realIp, List<RequestResult> results, Duration runDuration) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path reportDir = Path.of("reports");
        Files.createDirectories(reportDir);
        Path reportPath = reportDir.resolve("report-" + timestamp + ".html");

        List<RequestResult> successful = results.stream().filter(RequestResult::success).toList();
        List<RequestResult> failed = results.stream().filter(r -> !r.success()).toList();

        Map<String, Long> ipCounts = successful.stream()
                .collect(Collectors.groupingBy(RequestResult::ip, Collectors.counting()));

        // Sort by frequency descending
        Map<String, Long> sortedIpCounts = ipCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        int consecutiveCount = countConsecutiveRepeats(successful);
        int longestStreak = longestConsecutiveStreak(successful);

        List<RequestResult> leaks = successful.stream()
                .filter(r -> r.ip().equals(realIp))
                .toList();

        String html = buildHtml(realIp, results.size(), successful.size(), failed.size(),
                sortedIpCounts, consecutiveCount, longestStreak, leaks, runDuration, failed);

        Files.writeString(reportPath, html);
        log.info("Report written to {}", reportPath.toAbsolutePath());
        return reportPath;
    }

    private int countConsecutiveRepeats(List<RequestResult> successful) {
        int count = 0;
        for (int i = 1; i < successful.size(); i++) {
            if (successful.get(i).ip().equals(successful.get(i - 1).ip())) {
                count++;
            }
        }
        return count;
    }

    private int longestConsecutiveStreak(List<RequestResult> successful) {
        if (successful.isEmpty()) return 0;
        int maxStreak = 1;
        int currentStreak = 1;
        for (int i = 1; i < successful.size(); i++) {
            if (successful.get(i).ip().equals(successful.get(i - 1).ip())) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        return maxStreak;
    }

    private String buildHtml(String realIp, int total, int succeeded, int failedCount,
                             Map<String, Long> ipCounts, int consecutiveCount, int longestStreak,
                             List<RequestResult> leaks, Duration runDuration,
                             List<RequestResult> failed) {
        var sb = new StringBuilder();
        sb.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>IP Validator Report</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; margin: 40px; background: #f5f5f5; color: #333; }
                        h1 { color: #1a1a2e; }
                        h2 { color: #16213e; margin-top: 30px; }
                        table { border-collapse: collapse; width: 100%%; max-width: 600px; margin: 10px 0; }
                        th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
                        th { background: #16213e; color: white; }
                        tr:nth-child(even) { background: #f2f2f2; }
                        .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 15px; max-width: 800px; }
                        .summary-card { background: white; border-radius: 8px; padding: 15px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                        .summary-card .label { font-size: 0.85em; color: #666; }
                        .summary-card .value { font-size: 1.5em; font-weight: bold; margin-top: 5px; }
                        .leak { color: #e74c3c; font-weight: bold; }
                        .no-leak { color: #27ae60; font-weight: bold; }
                    </style>
                </head>
                <body>
                <h1>IP Validator Report</h1>
                """);

        // Summary
        sb.append("<h2>Summary</h2>\n<div class=\"summary-grid\">\n");
        appendCard(sb, "Total Requests", String.valueOf(total));
        appendCard(sb, "Succeeded", String.valueOf(succeeded));
        appendCard(sb, "Failed", String.valueOf(failedCount));
        appendCard(sb, "Unique IPs", String.valueOf(ipCounts.size()));
        appendCard(sb, "Run Duration", formatDuration(runDuration));
        appendCard(sb, "Real IP", realIp);
        sb.append("</div>\n");

        // IP Distribution
        sb.append("<h2>IP Distribution</h2>\n<table>\n<tr><th>IP Address</th><th>Count</th><th>Percentage</th></tr>\n");
        for (var entry : ipCounts.entrySet()) {
            double pct = (entry.getValue() * 100.0) / succeeded;
            sb.append("<tr><td>").append(entry.getKey())
                    .append("</td><td>").append(entry.getValue())
                    .append("</td><td>").append(String.format("%.1f%%", pct))
                    .append("</td></tr>\n");
        }
        sb.append("</table>\n");

        // IP Rotation Quality
        sb.append("<h2>IP Rotation Quality</h2>\n<div class=\"summary-grid\">\n");
        appendCard(sb, "Consecutive Repeats", String.valueOf(consecutiveCount));
        appendCard(sb, "Longest Streak", String.valueOf(longestStreak));
        sb.append("</div>\n");

        // IP Leakage Detection
        sb.append("<h2>IP Leakage Detection</h2>\n");
        if (leaks.isEmpty()) {
            sb.append("<p class=\"no-leak\">No IP leaks detected.</p>\n");
        } else {
            sb.append("<p class=\"leak\">").append(leaks.size()).append(" leak(s) detected!</p>\n");
            sb.append("<p>Leaked on request(s): ");
            sb.append(leaks.stream()
                    .map(r -> String.valueOf(r.requestNumber()))
                    .collect(Collectors.joining(", ")));
            sb.append("</p>\n");
        }

        // Failed requests
        if (!failed.isEmpty()) {
            sb.append("<h2>Failed Requests</h2>\n<table>\n<tr><th>Request #</th><th>Error</th></tr>\n");
            for (var f : failed) {
                sb.append("<tr><td>").append(f.requestNumber())
                        .append("</td><td>").append(escapeHtml(f.error()))
                        .append("</td></tr>\n");
            }
            sb.append("</table>\n");
        }

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    private void appendCard(StringBuilder sb, String label, String value) {
        sb.append("<div class=\"summary-card\"><div class=\"label\">").append(label)
                .append("</div><div class=\"value\">").append(escapeHtml(value))
                .append("</div></div>\n");
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) return seconds + "s";
        return "%dm %ds".formatted(seconds / 60, seconds % 60);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
