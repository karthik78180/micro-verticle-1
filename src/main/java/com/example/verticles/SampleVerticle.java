package com.example.verticles;

import com.example.api.VerticleLifecycle;
import com.example.api.VerticleInitializer;
import com.example.service.InitializationService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example SampleVerticle demonstrating Guice DI with initialization and shutdown.
 *
 * This verticle:
 * 1. Gets InitializationService injected by Guice
 * 2. Initializes resources in init() before first request
 * 3. Handles requests using initialized resources
 * 4. Cleans up resources in shutdown() during undeployment
 */
public class SampleVerticle implements VerticleLifecycle<JsonObject>, VerticleInitializer {

    private static final Logger logger = LoggerFactory.getLogger(SampleVerticle.class);

    // Dependencies injected by Guice
    private final InitializationService initService;

    // Verticle-specific resources initialized in init()
    private String greetingPrefix;
    private long verticalStartTime;

    /**
     * Constructor with dependency injection.
     * Guice will automatically inject InitializationService.
     *
     * @param initService Shared initialization service (injected by Guice)
     */
    public SampleVerticle(InitializationService initService) {
        this.initService = initService;
        logger.info("✅ SampleVerticle instantiated with injected InitializationService");
    }

    /**
     * Called when verticle is deployed.
     * This is called by the server before init().
     */
    @Override
    public void start(Vertx vertx, JsonObject config) {
        logger.info("🚀 SampleVerticle.start() called");
        logger.info("Config: {}", config);
    }

    /**
     * Initialize verticle-specific resources.
     * Called once after start() and before first request.
     * This is where you initialize things specific to this verticle.
     */
    @Override
    public void init() throws Exception {
        logger.info("🔧 SampleVerticle.init() - Initializing verticle resources");

        // Initialize verticle-specific resources
        this.verticalStartTime = System.currentTimeMillis();
        this.greetingPrefix = initService.getGlobalConfig().getString("greeting_prefix", "Hello");

        logger.info("✅ SampleVerticle initialization complete");
        logger.info("  - Greeting prefix: {}", greetingPrefix);
        logger.info("  - Startup time: {}", verticalStartTime);
    }

    /**
     * Handle incoming HTTP requests.
     * This uses resources initialized in init().
     */
    @Override
    public void handle(RoutingContext context) {
        try {
            JsonObject requestBody = context.body().asJsonObject();
            String name = requestBody.getString("name", "World");

            // Use initialized resource
            long uptime = System.currentTimeMillis() - verticalStartTime;

            JsonObject response = new JsonObject()
                    .put("message", greetingPrefix + " " + name)
                    .put("verticle", "SampleVerticle")
                    .put("uptime_ms", uptime);

            context.response()
                    .putHeader("Content-Type", "application/json")
                    .end(response.encodePrettily());

            logger.debug("Request handled for: {}", name);
        } catch (Exception e) {
            logger.error("Error handling request", e);
            context.response()
                    .setStatusCode(400)
                    .end("Error: " + e.getMessage());
        }
    }

    /**
     * Called when verticle is stopped.
     */
    @Override
    public void stop() {
        logger.info("🛑 SampleVerticle.stop() called");
    }

    /**
     * Cleanup resources initialized in init().
     * Called during undeployment after stop().
     */
    @Override
    public void shutdown() throws Exception {
        logger.info("🧹 SampleVerticle.shutdown() - Cleaning up resources");

        // Cleanup verticle-specific resources
        greetingPrefix = null;

        logger.info("✅ SampleVerticle shutdown complete");
    }
}
