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

## Generating Protobuf Classes

The project uses the `protobuf-gradle-plugin` to automatically generate Java classes from `.proto` files. The proto files are located in `src/main/proto`.

To manually generate protobuf classes:
```bash
./gradlew generateProto
```

Generated classes will be in `build/generated/sources/proto/main/java`.

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
```bash
curl --location 'http://localhost:8080/GreetProto.v1' \
--header 'Content-Type: application/octet-stream' \
--header 'Authorization: Bearer {{bearerToken}}' \
--data-binary '@/C:/Users/KAVUR/one-data/micro-verticle-1/src/test/resources/greet-request.bin'
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
