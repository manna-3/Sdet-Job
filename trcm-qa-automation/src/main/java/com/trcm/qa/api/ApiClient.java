package com.trcm.qa.api;

import com.trcm.qa.config.ConfigReader;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Builds reusable RestAssured request specifications.
 * Note: these specs add NO auth headers on purpose. Each test adds exactly the headers
 * it is testing (valid key, invalid key, no key), which keeps negative tests honest.
 */
public final class ApiClient {

    public static final String CHAT_PATH = "/coding-agent/chat";
    public static final String HEALTH_PATH = "/health";
    public static final String SUBSCRIPTION_HEADER = "Ocp-Apim-Subscription-Key";
    public static final String SERVICE_KEY_HEADER = "X-API-Key";

    private ApiClient() {
    }

    /** Requests that go through the APIM gateway. */
    public static RequestSpecification apim() {
        return base(ConfigReader.get("apim.base.url"));
    }

    /** Requests that hit the backend service directly, bypassing APIM. */
    public static RequestSpecification service() {
        return base(ConfigReader.get("service.base.url"));
    }

    /** Test-only hook on the mock that imitates changing an APIM Named Value. */
    public static RequestSpecification admin() {
        return base(ConfigReader.get("admin.base.url"));
    }

    private static RequestSpecification base(String baseUri) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.METHOD)
                .log(LogDetail.URI)
                .build();
    }
}
