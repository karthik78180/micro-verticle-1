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

## Project Structure

- `src/main/proto/`: Contains protobuf definition files
- `src/main/java/com/example/verticles/`: Contains the verticle implementations
- `config/`: Contains verticle configuration files
  - `ReadHelloWorld.v1.json`: Configuration for JSON handling verticle
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
