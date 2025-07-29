package com.example.verticles;

import com.example.api.VerticleLifecycle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class SampleVerticle implements VerticleLifecycle<JsonObject> {

    @Override
    public void start(Vertx vertx, JsonObject config) {
        System.out.println("SampleVerticle started with config: " + config);
    }

    @Override
    public void handle(RoutingContext context) {
        JsonObject requestBody = context.body().asJsonObject();
        String name = requestBody.getString("name", "World");
        context.response()
                .putHeader("Content-Type", "text/plain")
                .end("Hello world " + name);
    }

    @Override
    public void stop() {
        System.out.println("SampleVerticle stopped");
    }
}
