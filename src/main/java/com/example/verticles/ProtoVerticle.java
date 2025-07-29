package com.example.verticles;

import com.example.api.VerticleLifecycle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import com.example.protobuf.GreetMessages;

public class ProtoVerticle implements VerticleLifecycle<GreetMessages.GreetRequest> {

    @Override
    public void start(Vertx vertx, JsonObject config) {
        System.out.println("ProtoVerticle started with config: " + config);
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            byte[] bytes = context.body().buffer().getBytes();
            GreetMessages.GreetRequest req = GreetMessages.GreetRequest.parseFrom(bytes);
            String name = req.getName();
            context.response()
                    .putHeader("Content-Type", "text/plain")
                    .end("Hello from Proto, " + name);
        } catch (Exception e) {
            context.response().setStatusCode(400).end("Invalid Protobuf payload");
        }
    }

    @Override
    public void stop() {
        System.out.println("ProtoVerticle stopped");
    }
}