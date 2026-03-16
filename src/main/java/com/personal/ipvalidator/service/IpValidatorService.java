package com.personal.ipvalidator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Proxy;
import com.personal.ipvalidator.config.AppProperties;
import com.personal.ipvalidator.model.RequestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IpValidatorService {

    private static final Logger log = LoggerFactory.getLogger(IpValidatorService.class);
    private static final String IP_CHECK_URL = "https://ipinfo.io/json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AppProperties properties;

    public IpValidatorService(AppProperties properties) {
        this.properties = properties;
    }

    public String resolveRealIp() {
        log.info("Resolving real public IP (without proxy)...");
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.navigate(IP_CHECK_URL);
            String realIp = extractIp(page.textContent("body"));
            browser.close();
            log.info("Real public IP: {}", realIp);
            return realIp;
        }
    }

    public List<RequestResult> runValidation() {
        List<RequestResult> results = new ArrayList<>();
        log.info("Starting IP validation with {} requests through proxy", properties.numberOfRequests());

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );

            for (int i = 1; i <= properties.numberOfRequests(); i++) {
                results.add(makeRequest(browser, i));
            }

            browser.close();
        }

        return results;
    }

    private RequestResult makeRequest(Browser browser, int requestNumber) {
        try {
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions().setProxy(
                            new Proxy(properties.proxyUrl())
                                    .setUsername(properties.proxyUsername())
                                    .setPassword(properties.proxyPassword())
                    )
            );
            Page page = context.newPage();
            page.navigate(IP_CHECK_URL);
            String ip = extractIp(page.textContent("body"));
            context.close();
            log.info("Request {}: IP = {}", requestNumber, ip);
            return RequestResult.success(requestNumber, ip);
        } catch (Exception e) {
            log.error("Request {} failed: {}", requestNumber, e.getMessage());
            return RequestResult.failure(requestNumber, e.getMessage());
        }
    }

    private String extractIp(String json) {
        try {
            JsonNode node = MAPPER.readTree(json.trim());
            return node.get("ip").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse IP from response: " + json, e);
        }
    }
}
