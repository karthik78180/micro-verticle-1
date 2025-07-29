package com.example;

import com.example.protobuf.GreetMessages;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ProtobufTestWriter {
    public static void main(String[] args) throws Exception {
        // Build the Protobuf message
        GreetMessages.GreetRequest request = GreetMessages.GreetRequest.newBuilder()
                .setName("Bob")
                .build();

        // Output location (ensure the directory exists)
        String outputPath = "src/test/resources/greet-request.bin";
        Files.createDirectories(Paths.get("src/test/resources"));

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            request.writeTo(fos);
        }

        System.out.println("✅ Protobuf payload written to: " + outputPath);
    }
}
