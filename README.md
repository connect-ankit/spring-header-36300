# Spring MVC Content Negotiation Inconsistency

## 🔍 Overview

This project demonstrates a non-deterministic behavior in Spring Framework's `Accept` header handling. Spring enforces a hard-coded limit of **50 media type elements** to prevent CPU-based Denial of Service (DoS) attacks. However, this validation is inconsistent across different endpoint configurations, leading to unpredictable behavior.

## 📋 Project Information

- **Group ID**: `com.inn.accept`
- **Artifact ID**: `headers`
- **Version**: `1.0`
- **Spring Boot Version**: `3.5.9`
- **Java Version**: `17`
- **Server**: Jetty (port 8081)

## 🏗️ Project Structure

```
headers/
├── src/main/java/com/inn/accept/headers/
│   ├── HeadersApplication.java              # Main Spring Boot application
│   ├── TestController.java                  # Demo endpoints showcasing the issue
│   └── CustomSpringExceptionHandler.java    # Global exception handler
├── src/main/resources/
│   └── application.properties               # Configuration (port 8081, debug logging)
├── pom.xml                                  # Maven dependencies
└── README.md                                # This file
```

## 🔧 The Root Cause

The 50-element limit is enforced in:  
`org.springframework.util.MimeTypeUtils.sortBySpecificity(List<T> mimeTypes)`

```java
// Spring internal guardrail
if (mimeTypes.size() > 50) {
    throw new InvalidMimeTypeException(mimeTypes.toString(), "Too many elements");
}
```

## 🧪 Phase 1: Inconsistency by Return Type

The DoS protection is non-deterministic. The exact same "malformed" header is accepted by some endpoints and rejected by others based solely on the return type.

| Endpoint Setup | Return Type | Result with >50 Items | Technical Reason |
|----------------|-------------|----------------------|------------------|
| `/v1/error` | `String` | ❌ FAIL (406/500) | Spring MUST sort the header to find a String converter |
| `/v2/error` | `void` | ✅ SUCCESS (200 OK) | Spring skips negotiation/sorting as there is no body |
| `/v3/error` | `void + produces` | ❌ FAIL (406) | The `produces` contract forces a specificity check |

## 🧪 Phase 2: The "Fragility Zone" (47 vs 48 vs 51)

Validated against `/v1/error` endpoint. For endpoints that perform negotiation (like `String` returns), we observed three distinct failure states:

| Accept Header Element Count | Result Code | Internal State | Behavior |
|-----------------------------|-------------|----------------|----------|
| 0 – 47                      | ✅ 200 OK | Stable | Header parsed and sorted normally |
| 48 – 50                     | ⚠️ 500 Error | Unstable | Parsing Overflow: Internal crash before the 50-limit check (depends on media type specificity) |
| 51+                         | ❌ 406 Error | Guardrail | Hard Limit: MimeTypeUtils triggers the 50-element exception |

**Why 48-50 elements cause 500 errors**: Spring's internal sorting algorithm expands wildcards and calculates specificity. When certain media types like `*/*`, `application/*`, or `application/json` are present at positions 48-50, the sorting algorithm's internal expansion exceeds the 50-element limit before the explicit validation check runs, causing an internal crash.

**Example of Internal Crash**:
```
org.springframework.util.InvalidMimeTypeException: Invalid mime type "[a/1, a/2, ..., application/*json]": Too many elements
    at org.springframework.util.MimeTypeUtils.sortBySpecificity(MimeTypeUtils.java:xxx)
    at org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodProcessor.writeWithMessageConverters(...)
```

### Test Case 1: 47 Elements (Stable - 200 OK)

```bash
curl -vvv 'http://localhost:8081/v1/error' \
--header 'Accept: a/1,a/2,a/3,a/4,a/5,a/6,a/7,a/8,a/9,a/10,a/11,a/12,a/13,a/14,a/15,a/16,a/17,a/18,a/19,a/20,a/21,a/22,a/23,a/24,a/25,a/26,a/27,a/28,a/29,a/30,a/31,a/32,a/33,a/34,a/35,a/36,a/37,a/38,a/39,a/40,a/41,a/42,a/43,a/44,a/45,a/46,a/47'
```

**Expected**: `200 OK` with response body "success"

### Test Case 2.A: 48 Elements + application/json (Unstable - 500 Error)

```bash
curl -vvv 'http://localhost:8081/v1/error' \
--header 'Accept: a/1,a/2,a/3,a/4,a/5,a/6,a/7,a/8,a/9,a/10,a/11,a/12,a/13,a/14,a/15,a/16,a/17,a/18,a/19,a/20,a/21,a/22,a/23,a/24,a/25,a/26,a/27,a/28,a/29,a/30,a/31,a/32,a/33,a/34,a/35,a/36,a/37,a/38,a/39,a/40,a/41,a/42,a/43,a/44,a/45,a/46,a/47,a/48,application/json'
```

**Expected**: `500 Internal Server Error` (sorting algorithm expansion triggers overflow)

### Test Case 2.B: 49 Elements (Stable - 200 OK)

```bash
curl -vvv 'http://localhost:8081/v1/error' \
--header 'Accept: a/1,a/2,a/3,a/4,a/5,a/6,a/7,a/8,a/9,a/10,a/11,a/12,a/13,a/14,a/15,a/16,a/17,a/18,a/19,a/20,a/21,a/22,a/23,a/24,a/25,a/26,a/27,a/28,a/29,a/30,a/31,a/32,a/33,a/34,a/35,a/36,a/37,a/38,a/39,a/40,a/41,a/42,a/43,a/44,a/45,a/46,a/47,a/48,application/text'
```

**Expected**: `200 OK` (non-matching media type doesn't trigger sorting expansion)

### Test Case 3: 51 Elements (Guardrail - 406 Error)

```bash
curl -vvv 'http://localhost:8081/v1/error' \
--header 'Accept: a/1,a/2,a/3,a/4,a/5,a/6,a/7,a/8,a/9,a/10,a/11,a/12,a/13,a/14,a/15,a/16,a/17,a/18,a/19,a/20,a/21,a/22,a/23,a/24,a/25,a/26,a/27,a/28,a/29,a/30,a/31,a/32,a/33,a/34,a/35,a/36,a/37,a/38,a/39,a/40,a/41,a/42,a/43,a/44,a/45,a/46,a/47,a/48,a/49,a/50,a/51,application/json'
```

**Expected**: `406 Not Acceptable` (hard limit enforced)

## 🧪 Phase 3: Contract Mismatch (406)

⚠️ **Important**: A `406 Not Acceptable` error can occur in two distinct scenarios, which can be confusing:

1. **Media Type Mismatch** (Phase 3): Single Accept header doesn't match the `produces` attribute
2. **Element Limit Exceeded** (Phase 2): Accept header contains 51+ elements

Both throw the same `406` status code but for completely different reasons.

### Scenario: Media Type Mismatch

**Endpoint**: `@RequestMapping(produces = "application/json")`  
**Request**: `Accept: application/xml`  
**Result**: `406 Not Acceptable`  
**Reason**: The client requests XML but the endpoint only produces JSON

**Observation**: The `produces` attribute acts as a strict contract. If the Accept header doesn't match any of the declared media types, Spring rejects the request. Removing the `produces` attribute allows Spring to use any available converter (usually JSON).

### How to Distinguish the Two 406 Errors:

| Scenario | Accept Header | Error Message | Root Cause |
|----------|---------------|---------------|------------|
| **Scenario 1** | 51+ elements | "Too many elements" | DoS protection limit exceeded |
| **Scenario 3** | Single mismatch | "Could not find acceptable representation" | Media type negotiation failed |

⚠️ **Note**: Examples 1 and 3 in the "Reproduction / Demo cURLs" section both return `406 Not Acceptable`, but they represent completely different failure scenarios.

## 🚨 Critical Issue: The 500 Error Runtime Risk (Test Case 2.A)

**⚠️ PRODUCTION IMPACT WARNING**: Test Case 2.A represents the most dangerous scenario in this issue.

### The Problem

When Accept headers contain **48-50 elements with specific media types** (like `application/json`, `*/*`, or `application/*`), Spring's internal sorting algorithm crashes **before** the 50-element validation check runs, resulting in an unhandled `500 Internal Server Error`.

**Why This Is Critical**:
- ❌ **Bypasses validation**: The 50-element guardrail never executes
- ❌ **Unhandled exception**: Standard exception handlers may not catch this internal crash
- ❌ **Non-deterministic**: Depends on which media types are present and their positions
- ❌ **Production outage risk**: Can cause service disruption for legitimate users
- ❌ **Hard to debug**: Error occurs deep in Spring's internal sorting logic

### Example Scenario


# This request causes a 500 error (internal crash) with 49 elements
```bash
curl -vvv 'http://localhost:8081/v1/error' \
--header 'Accept: a/1,a/2,a/3,a/4,a/5,a/6,a/7,a/8,a/9,a/10,a/11,a/12,a/13,a/14,a/15,a/16,a/17,a/18,a/19,a/20,a/21,a/22,a/23,a/24,a/25,a/26,a/27,a/28,a/29,a/30,a/31,a/32,a/33,a/34,a/35,a/36,a/37,a/38,a/39,a/40,a/41,a/42,a/43,a/44,a/45,a/46,a/47,a/48,application/json'
```

## 🚀 Running the Project

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Build and Run

```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The application will start on **http://localhost:8081**

## 🧪 Reproduction / Demo cURLs

### 1. The 51-Element Failure (406)

```bash
# Target: /v1/error (String Return)
curl -vvv 'http://localhost:8081/v1/error' \
--header 'Accept: a/1,a/2,a/3,a/4,a/5,a/6,a/7,a/8,a/9,a/10,a/11,a/12,a/13,a/14,a/15,a/16,a/17,a/18,a/19,a/20,a/21,a/22,a/23,a/24,a/25,a/26,a/27,a/28,a/29,a/30,a/31,a/32,a/33,a/34,a/35,a/36,a/37,a/38,a/39,a/40,a/41,a/42,a/43,a/44,a/45,a/46,a/47,a/48,a/49,a/50,a/51'
```

**Expected**: `406 Not Acceptable`

### 2. The Return Type Bypass (Success)

```bash
# Target: /v2/error (Void Return)
# Exact same header as above returns 200 OK
curl -vvv 'http://localhost:8081/v2/error' \
--header 'Accept: a/1,a/2,a/3,a/4,a/5,a/6,a/7,a/8,a/9,a/10,a/11,a/12,a/13,a/14,a/15,a/16,a/17,a/18,a/19,a/20,a/21,a/22,a/23,a/24,a/25,a/26,a/27,a/28,a/29,a/30,a/31,a/32,a/33,a/34,a/35,a/36,a/37,a/38,a/39,a/40,a/41,a/42,a/43,a/44,a/45,a/46,a/47,a/48,a/49,a/50,a/51'
```

**Expected**: `200 OK` (empty body) - Exception is logged but ignored

### 3. The Media Type Mismatch (406)

```bash
# Target: /v3/error (produces=application/json)
curl -vvv 'http://localhost:8081/v3/error' \
--header 'Accept: application/xml'
```

**Expected**: `406 Not Acceptable`

## 🛡️ Custom Exception Handler

The project includes `CustomSpringExceptionHandler` that:
- Catches `HttpMediaTypeNotAcceptableException`
- Returns a `400 Bad Request` with custom headers
- Logs the error with descriptive messages
- Provides a JSON error response

## 📊 Key Findings

1. **Non-deterministic behavior**: Same malformed header produces different results based on endpoint return type
2. **Silent failures**: `void` methods with no `produces` attribute silently accept invalid headers
3. **Fragility zone**: The 500 error at 48-50 elements is not just about count, but about the specific media types present and how Spring's sorting algorithm processes them
4. **Inconsistent enforcement**: DoS protection only applies when content negotiation is required

## 📝 Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <!-- Tomcat excluded, using Jetty instead -->
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jetty</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 🔍 Debug Logging

The project is configured with DEBUG logging for:
- `org.springframework.web`
- `com.inn.accept.headers`

Check the console output to see internal Spring behavior when processing malformed headers.

## 📚 References

- [Spring Framework Documentation - Content Negotiation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/content-negotiation.html)
- [CVE-2023-20861 - Spring Framework DoS via Accept Header](https://spring.io/security/cve-2023-20861)
- [MimeTypeUtils Source Code](https://github.com/spring-projects/spring-framework/blob/main/spring-core/src/main/java/org/springframework/util/MimeTypeUtils.java)

---

**Note**: This project is for demonstration and testing purposes only. It highlights a potential security and consistency issue in Spring Framework's content negotiation mechanism.
