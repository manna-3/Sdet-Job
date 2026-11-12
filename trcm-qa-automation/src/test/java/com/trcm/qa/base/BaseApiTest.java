package com.trcm.qa.base;

import io.restassured.RestAssured;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/** Parent class for API tests. No browser is opened, so these run fast. */
public abstract class BaseApiTest {

    @BeforeSuite(alwaysRun = true)
    public void startEnvironment() {
        MockServerHook.startIfEnabled();
        // When an assertion fails, RestAssured prints the full request and response to help debugging.
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterSuite(alwaysRun = true)
    public void stopEnvironment() {
        MockServerHook.stop();
    }
}
