package com.trcm.qa.tests.api;

import com.trcm.qa.api.ApiClient;
import com.trcm.qa.base.BaseApiTest;
import com.trcm.qa.config.ConfigReader;
import com.trcm.qa.reports.ReportLogger;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * APIM integration tests (APIM_Integration_Testing_1.xlsx, TC-01 to TC-06).
 *
 * Architecture under test (defense in depth):
 *   Client --Ocp-Apim-Subscription-Key--> APIM gateway --X-API-Key (Named Value)--> Coding Agent service
 *
 *   401 Unauthorized = "I don't know who you are"   (missing / invalid credentials)
 *   403 Forbidden    = "I know who you are, but no" (credentials recognised but not allowed)
 */
public class ApimAuthTest extends BaseApiTest {

    private static final String CHAT_BODY = "{\"message\": \"Which claims got rejected?\"}";

    @Test(description = "TC-01 Valid subscription key: request succeeds through APIM", groups = {"smoke", "regression", "api"})
    public void validSubscriptionKey() {
        Response response = given().spec(ApiClient.apim())
                .header(ApiClient.SUBSCRIPTION_HEADER, ConfigReader.get("apim.subscription.key"))
                .body(CHAT_BODY)
                .when().post(ApiClient.CHAT_PATH)
                .then()
                .statusCode(200)
                .body("reply", notNullValue())
                .time(lessThan(5000L)) // simple response-time check (ms)
                .extract().response();
        ReportLogger.json("Response", response.asPrettyString());
    }

    @Test(description = "TC-02 Missing subscription key is rejected by APIM with 401", groups = {"smoke", "regression", "api", "security"})
    public void missingSubscriptionKey() {
        given().spec(ApiClient.apim())
                .body(CHAT_BODY)
                .when().post(ApiClient.CHAT_PATH)
                .then()
                .statusCode(401)
                .body("message", containsString("missing subscription key"));
    }

    @Test(description = "TC-03 Invalid subscription key is rejected by APIM with 401", groups = {"regression", "api", "security"})
    public void invalidSubscriptionKey() {
        given().spec(ApiClient.apim())
                .header(ApiClient.SUBSCRIPTION_HEADER, "invalid-key-abc123")
                .body(CHAT_BODY)
                .when().post(ApiClient.CHAT_PATH)
                .then()
                .statusCode(401)
                .body("message", containsString("invalid subscription key"));
    }

    @Test(description = "TC-04 Direct backend call without X-API-Key is rejected with 401 (APIM bypass)",
            groups = {"regression", "api", "security"})
    public void directServiceCallWithoutKey() {
        given().spec(ApiClient.service())
                .body(CHAT_BODY)
                .when().post(ApiClient.CHAT_PATH)
                .then()
                .statusCode(401);
    }

    @Test(description = "TC-05 Health endpoint is public and returns healthy", groups = {"smoke", "regression", "api"})
    public void healthEndpointPublic() {
        given().spec(ApiClient.service())
                .when().get(ApiClient.HEALTH_PATH)
                .then()
                .statusCode(200)
                .body("status", equalTo("healthy"));
    }

    @Test(description = "TC-06a Wrong X-API-Key on the service returns 403 Invalid API key", groups = {"regression", "api", "security"})
    public void wrongServiceKeyReturns403() {
        given().spec(ApiClient.service())
                .header(ApiClient.SERVICE_KEY_HEADER, "wrong-service-key")
                .body(CHAT_BODY)
                .when().post(ApiClient.CHAT_PATH)
                .then()
                .statusCode(403)
                .body("detail", equalTo("Invalid API key"));
    }

    @Test(description = "TC-06b Misconfigured APIM Named Value: valid client key still gets 403 from the service",
            groups = {"regression", "api", "security"})
    public void misconfiguredNamedValueReturns403() {
        if (!ConfigReader.getBoolean("mock.server.enabled")) {
            throw new SkipException("Changing the APIM Named Value is only automated against the local mock");
        }
        try {
            ReportLogger.step("Simulate a wrong Named Value in APIM (like editing it in the Azure portal)");
            given().spec(ApiClient.admin()).queryParam("value", "wrong-named-value")
                    .when().post("/named-value").then().statusCode(200);

            given().spec(ApiClient.apim())
                    .header(ApiClient.SUBSCRIPTION_HEADER, ConfigReader.get("apim.subscription.key"))
                    .body(CHAT_BODY)
                    .when().post(ApiClient.CHAT_PATH)
                    .then()
                    .statusCode(403)
                    .body("detail", equalTo("Invalid API key"));
        } finally {
            // Always restore, even if the assertion fails, so other tests are not affected
            given().spec(ApiClient.admin()).when().post("/named-value");
            ReportLogger.step("Named Value restored");
        }
    }

    @Test(description = "TC-01b Valid service key directly on the backend returns 200", groups = {"regression", "api"})
    public void validServiceKeyDirect() {
        int status = given().spec(ApiClient.service())
                .header(ApiClient.SERVICE_KEY_HEADER, ConfigReader.get("service.api.key"))
                .body(CHAT_BODY)
                .when().post(ApiClient.CHAT_PATH)
                .then().extract().statusCode();
        Assert.assertEquals(status, 200);
    }
}
