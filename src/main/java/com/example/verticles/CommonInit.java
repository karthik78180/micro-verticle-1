package com.example.verticles;

import com.example.api.VerticleInitializable;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example CommonInit class demonstrating centralized initialization for all verticles.
 *
 * This class is instantiated ONCE per repository deployment (before any verticles are created).
 * All verticles in this repository can use the shared resources initialized here.
 *
 * Configuration in ReadHelloWorld.v1.json:
 * {
 *   "verticleClass": "com.example.verticles.SampleVerticle",
 *   "initClass": "com.example.verticles.CommonInit",
 *   "address": "ReadHelloWorld.v1"
 * }
 */
public class CommonInit implements VerticleInitializable {

    private static final Logger logger = LoggerFactory.getLogger(CommonInit.class);

    // Shared resources initialized once
    private static String applicationName;
    private static long initializationTime;

    @Override
    public void initialize(Vertx vertx, JsonObject config) throws Exception {
        logger.info("🚀 CommonInit.initialize() - Setting up shared resources for repository");

        // Initialize shared resources
        applicationName = config.getString("app_name", "MicroVerticle-1");
        initializationTime = System.currentTimeMillis();

        logger.info("✅ Shared initialization complete");
        logger.info("  - Application: {}", applicationName);
        logger.info("  - Init time: {}", initializationTime);

        // Example: Initialize database connection pool
        // DatabasePool.initialize(config.getString("db_url"));

        // Example: Load configuration from external files
        // ConfigManager.load(config.getString("config_path"));

        // Example: Initialize cache
        // CacheManager.init();
    }

    @Override
    public void shutdown() throws Exception {
        logger.info("🛑 CommonInit.shutdown() - Cleaning up shared resources");

        // Cleanup shared resources
        applicationName = null;

        logger.info("✅ Shared resources cleanup complete");

        // Example: Close database connections
        // DatabasePool.shutdown();

        // Example: Clear cache
        // CacheManager.clear();
    }

    // Provide static access to shared resources for verticles
    public static String getApplicationName() {
        return applicationName;
    }

    public static long getInitializationTime() {
        return initializationTime;
    }

    public static long getUptimeMs() {
        if (initializationTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - initializationTime;
    }
}
