package com.inn.accept.headers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * TestController demonstrates Spring Framework's non-deterministic behavior in Accept header handling.
 * 
 * Spring enforces a 50-element limit on Accept headers to prevent DoS attacks, but this validation
 * is inconsistent across different endpoint configurations, leading to unpredictable behavior.
 * 
 * Root Cause: org.springframework.util.MimeTypeUtils.sortBySpecificity(List<T> mimeTypes)
 * throws InvalidMimeTypeException when mimeTypes.size() > 50
 */
@RestController
public class TestController {

    /**
     * Phase 1 & 2: String Return Type - Content Negotiation Required
     * 
     * This endpoint MUST perform content negotiation to find a suitable String converter.
     * Spring sorts the Accept header by specificity, triggering validation.
     * 
     * Observed Behavior (Fragility Zone):
     * - 0-47 elements   → 200 OK (stable)
     * - 48-50 elements  → 500 Error (unstable - depends on media type specificity)
     *                     algorithm's internal expansion exceeds 50 elements before validation
     * - 51+ elements    → 406 Error (hard limit enforced)
     * 
     * Test Commands:
     * # 47 elements (stable):
     * curl -vvv 'localhost:8081/v1/error' --header 'Accept: a/1,a/2,...,a/47'
     * 
     * # 48 elements + application/json (500 error):
     * curl -vvv 'localhost:8081/v1/error' --header 'Accept: a/1,a/2,...,a/48,application/json'
     * 
     * # 51+ elements (406 error):
     * curl -vvv 'localhost:8081/v1/error' --header 'Accept: a/1,a/2,...,a/51'
     */
    @RequestMapping(value = "/v1/error", method = {RequestMethod.GET})
    public String test() {
        return "success";
    }

    /**
     * Phase 1: Void Return Type - No Content Negotiation
     * 
     * This endpoint returns void with no 'produces' attribute.
     * Spring skips content negotiation since there's no response body.
     * 
     * Observed Behavior:
     * - Even with 51+ Accept header elements → 200 OK (empty body)
     * - Spring logs HttpMediaTypeNotAcceptableException internally but ignores it
     * - Exception is suppressed because no response body needs to be serialized
     *
     * 
     * Test Command:
     * curl -vvv 'localhost:8081/v2/error' --header 'Accept: a/1,a/2,...,a/51'
     * 
     * Expected: 200 OK (but exception logged in DEBUG mode)
     */
    @RequestMapping(value = "/v2/error", method = {RequestMethod.GET})
    public void test2() {
        System.out.println("test2");
    }

    /**
     * Phase 1 & 3: Void Return Type with 'produces' - Contract Enforcement
     * 
     * This endpoint returns void but declares 'produces = application/json'.
     * The 'produces' attribute forces Spring to validate the Accept header contract.
     * 
     * Observed Behavior:
     * - Accept header mismatch (e.g., application/xml) → 406 Error
     * - 51+ elements → 406 Error
     * 
     * Note: Both scenarios return 406 but for different reasons:
     * 1. Media type mismatch: "Could not find acceptable representation"
     * 2. Element limit exceeded: "Too many elements"
     * 
     * Test Commands:
     * # Media type mismatch:
     * curl -vvv 'localhost:8081/v3/error' --header 'Accept: application/xml'
     * 
     * # Element limit exceeded:
     * curl -vvv 'localhost:8081/v3/error' --header 'Accept: a/1,a/2,...,a/51'
     */
    @RequestMapping(value = "/v3/error", method = {RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_VALUE)
    public void test3() {
        System.out.println("test3");
    }
}
