package com.trcm.qa.tests.ui;

import com.trcm.qa.base.BaseTest;
import com.trcm.qa.config.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Module: Environment (TC-047, TC-048). */
public class EnvironmentTest extends BaseTest {

    @Test(description = "TC-047 App loads correctly on the configured URL", groups = {"smoke", "regression", "environment"})
    public void appLoads() {
        Assert.assertTrue(page.isLoaded(), "Upload area and chat panel should render");
        Assert.assertTrue(page.getEnvironmentBadge().length() > 0);
    }

    @Test(description = "TC-048 Test run does not point to production", groups = {"smoke", "regression", "environment"})
    public void notPointingToProduction() {
        for (String key : new String[]{"app.url", "apim.base.url", "service.base.url"}) {
            Assert.assertFalse(ConfigReader.get(key).toLowerCase().contains("prod"),
                    key + " points to production: " + ConfigReader.get(key));
        }
    }
}
