package com.example.verticles;

import com.example.api.VerticleLifecycle;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import com.example.protobuf.GreetMessagesNew;

public class ProtoVerticleNew implements VerticleLifecycle<GreetMessagesNew.GreetRequestNew> {

    @Override
    public void start(Vertx vertx, JsonObject config) {
        System.out.println("ProtoVerticle started with config: " + config);
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            byte[] bytes = context.body().buffer().getBytes();
            GreetMessagesNew.GreetRequestNew req = GreetMessagesNew.GreetRequestNew.parseFrom(bytes);
            String name = req.getNewName();
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
