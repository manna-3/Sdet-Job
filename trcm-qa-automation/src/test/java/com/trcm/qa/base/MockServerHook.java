package com.trcm.qa.base;

import com.trcm.mock.MockTrcmServer;
import com.trcm.qa.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/** Starts the local mock app only when the environment asks for it (env=local). */
final class MockServerHook {

    private static final Logger LOG = LogManager.getLogger(MockServerHook.class);

    private MockServerHook() {
    }

    static void startIfEnabled() {
        if (ConfigReader.getBoolean("mock.server.enabled")) {
            try {
                MockTrcmServer.start(ConfigReader.getInt("mock.server.port"));
            } catch (IOException e) {
                throw new IllegalStateException("Could not start mock server. Is port "
                        + ConfigReader.get("mock.server.port") + " already in use?", e);
            }
        } else {
            LOG.info("Mock server disabled for env '{}'. Tests will hit {}", ConfigReader.env(), ConfigReader.get("app.url"));
        }
    }

    static void stop() {
        MockTrcmServer.stop();
    }
}
