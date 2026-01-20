# Micro Verticle 1

This project demonstrates a simple Vert.x verticle that can be dynamically deployed by the main Vert.x server. It includes examples of both JSON and Protobuf request handling.

## Features

- JSON request handling with `ReadHelloWorld` verticle
- Protocol Buffers request handling with `ProtoVerticle`
- Gradle build with protobuf compilation

## Building the Project

```bash
./gradlew clean build shadowJar
```

This will:
1. Generate the protobuf classes
2. Compile the Java code
3. Create a fat JAR with all dependencies

## Protobuf updates (new)

The protobuf definitions were expanded to include richer message types and simple service definitions.

- `src/main/proto/GreetRequest.proto` now contains:
  - fields: `name`, `age`, `tags` (repeated), nested `Address`, `attrs` map, and `style` enum
  - `GreetResponse` message
  - `service Greeter { rpc SayHello (GreetRequest) returns (GreetResponse); }`

- `src/main/proto/GreetRequestNew.proto` now contains:
  - fields: `newName`, `email`, `scores` (repeated), nested `Metadata`, and `priority` enum
  - `GreetResponseNew` message
  - `service GreeterNew { rpc SayHelloNew (GreetRequestNew) returns (GreetResponseNew); }`

To regenerate Java classes from the updated protos manually:
```bash
./gradlew generateProto
```

Generated classes will be in `build/generated/sources/proto/main/java`.

If you want gRPC stubs (server/client code), add the gRPC plugin and enable `grpc` generation in the protobuf Gradle block (I can add this if desired).

## Testing the Endpoints

### 1. Deploy the Verticle
```bash
curl --location 'http://localhost:8080/deploy' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{bearerToken}}' \
--data '{ "repo": "micro-verticle-1" }'
```

### 2. Test JSON Endpoint
```bash
curl --location 'http://localhost:8080/ReadHelloWorld.v1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{bearerToken}}' \
--data '{
    "name": "Sri"
}'
```

### 3. Test Protobuf Endpoint
You can produce test payloads locally using the included test writer utility. Run the test writer to create binary protobuf payloads for the old and new protos:
```bash
./gradlew -q run -PmainClass=com.example.ProtobufTestWriter
```

This writes two files to `src/test/resources`:
- `greet-request.bin` (old proto)
- `greet-request-new.bin` (new proto)

Then use `curl` to send either file to the Protobuf endpoint. Example:
```bash
curl --location 'http://localhost:8080/GreetProto.v1' \
--header 'Content-Type: application/octet-stream' \
--data-binary @src/test/resources/greet-request.bin
```

### 4. Undeploy the Verticle
```bash
curl --location 'http://localhost:8080/undeploy' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{bearerToken}}' \
--data '{ "repo": "micro-verticle-1" }'
```

## Guice Dependency Injection

This verticle demonstrates Guice DI integration with the vertx-on-demand server. Each verticle can have its own initialization and cleanup logic.

### Verticle Lifecycle

1. **Instantiation**: Guice creates the verticle and injects dependencies
2. **start()**: Called when deployed, receive configuration
3. **init()**: Called once before first request - initialize resources here
4. **handle()**: Called for each request
5. **shutdown()**: Called during undeployment - cleanup resources
6. **stop()**: Called when undeployed

### Example: SampleVerticle

The included `SampleVerticle` demonstrates the full DI pattern:

```java
public class SampleVerticle implements VerticleLifecycle<JsonObject>, VerticleInitializer {
    private final InitializationService initService;  // Injected by Guice
    private String greetingPrefix;                    // Initialized in init()

    public SampleVerticle(InitializationService initService) {
        this.initService = initService;
    }

    @Override
    public void init() throws Exception {
        // Initialize resources once before first request
        greetingPrefix = initService.getGlobalConfig()
            .getString("greeting_prefix", "Hello");
    }

    @Override
    public void handle(RoutingContext context) {
        // Use initialized resource
        String name = context.body().asJsonObject().getString("name");
        context.response().end(greetingPrefix + " " + name);
    }

    @Override
    public void shutdown() throws Exception {
        // Cleanup resources
        greetingPrefix = null;
    }
}
```

### Creating Your Own Verticle with DI

1. Implement both `VerticleLifecycle<T>` and `VerticleInitializer`
2. Add a constructor that accepts the dependencies you need (Guice will inject them)
3. Initialize resources in `init()` method
4. Use initialized resources in `handle()` method
5. Cleanup in `shutdown()` method

Example with database connection:

```java
public class DatabaseVerticle implements VerticleLifecycle<JsonObject>, VerticleInitializer {
    private final InitializationService initService;
    private DatabasePool dbPool;

    public DatabaseVerticle(InitializationService initService) {
        this.initService = initService;
    }

    @Override
    public void init() throws Exception {
        String dbUrl = initService.getGlobalConfig().getString("db_url");
        dbPool = new DatabasePool(dbUrl);
        dbPool.connect();
    }

    @Override
    public void handle(RoutingContext context) {
        dbPool.query("SELECT * FROM users")
            .onSuccess(result -> context.response().end(result.toJson()))
            .onFailure(err -> context.response().setStatusCode(500).end(err.getMessage()));
    }

    @Override
    public void shutdown() throws Exception {
        if (dbPool != null) {
            dbPool.disconnect();
        }
    }
}
```

## Project Structure

- `src/main/proto/`: Contains protobuf definition files
- `src/main/java/com/example/verticles/`: Contains the verticle implementations
  - `SampleVerticle.java`: Example verticle with Guice DI (demonstrating init/shutdown)
  - `ProtoVerticle.java`: Handles protobuf requests
  - `ProtoVerticleNew.java`: Handles new protobuf format
- `config/`: Contains verticle configuration files
  - `ReadHelloWorld.v1.json`: Configuration for SampleVerticle
  - `GreetProto.v1.json`: Configuration for Protobuf handling verticle

## Working with Protocol Buffers

### Proto File
The protobuf definition is in `src/main/proto/GreetRequest.proto`:
```protobuf
syntax = "proto3";
package com.example.protobuf;
option java_outer_classname = "GreetMessages";

message GreetRequest {
  string name = 1;
}
```

### Creating Test Data
To create a test protobuf binary file:
1. Use the generated classes to create a message
2. Serialize it to a file
3. Use this file with the curl command

Example test data is provided in `src/test/resources/greet-request.bin`
