# IP Validator

Given a proxy service like [BrightData](https://brightdata.com/), this service creates instances of Playwright and calls https://ipinfo.io/json to identify the IP address that was resolved when making the request. 

## Goal

- Validating IP rotation quality of the proxy
- Checking for IP leakage

## Inputs

Configured using `application.properties`

- Number of requests
- Proxy URL and credentials (set in `application-local.properties`, which will be in `.gitignore`)


## Execution

- Requests are made **sequentially** (one Playwright instance at a time)
- No delay between requests
- On request failure (timeout, proxy error): log the error, skip, and continue to the next request
- Failed requests are excluded from IP analysis but included in the report summary

## Report / Output

An HTML report in `./reports/report-<timestamp>.html` that contains:

**Summary**
- Total requests attempted, succeeded, and failed
- Total unique IPs observed
- Run duration

**IP Distribution**
- A table of each IP with raw count and percentage of total successful requests
- Sorted by frequency (descending)

**IP Rotation Quality**
- Number of times the same IP was used for consecutive (immediately back-to-back) requests
- Longest streak of the same IP in a row

**IP Leakage Detection**
- The machine's real public IP is resolved (without proxy) at the start of the run
- Any proxied request that returns the real IP is flagged as a leak
- Report shows: leak count and which request numbers leaked

## Tech Stack

- Maven
- Java 21
- Spring-Boot 4.x
- [Playwright Java](http://playwright.dev/java/docs/api/class-playwright)
