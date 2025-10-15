package com.example;

import com.example.protobuf.GreetMessages;
import com.example.protobuf.GreetMessagesNew;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ProtobufTestWriter {
    public static void main(String[] args) throws Exception {
        // Ensure output directory exists
        Files.createDirectories(Paths.get("src/test/resources"));

        // Build the original Protobuf message with richer fields
        GreetMessages.GreetRequest request = GreetMessages.GreetRequest.newBuilder()
                .setName("Bob")
                .setAge(30)
                .addTags("tester")
                .putAttrs("role", "developer")
                .setStyle(GreetMessages.GreetRequest.GreetingStyle.FRIENDLY)
                .setAddress(GreetMessages.GreetRequest.Address.newBuilder()
                        .setStreet("123 Main St")
                        .setCity("Metropolis")
                        .setCountry("Freedonia")
                        .build())
                .build();

        String out1 = "src/test/resources/greet-request.bin";
        try (FileOutputStream fos = new FileOutputStream(out1)) {
            request.writeTo(fos);
        }
        System.out.println("✅ Wrote: " + out1);

        // Build the new Protobuf message
        GreetMessagesNew.GreetRequestNew requestNew = GreetMessagesNew.GreetRequestNew.newBuilder()
                .setNewName("Alice")
                .setEmail("alice@example.com")
                .addScores(95)
                .addScores(88)
                .setMeta(GreetMessagesNew.GreetRequestNew.Metadata.newBuilder()
                        .setSource("unit-test")
                        .setTimestamp(System.currentTimeMillis())
                        .build())
                .setPriority(GreetMessagesNew.GreetRequestNew.Priority.HIGH)
                .build();

        String out2 = "src/test/resources/greet-request-new.bin";
        try (FileOutputStream fos = new FileOutputStream(out2)) {
            requestNew.writeTo(fos);
        }
        System.out.println("✅ Wrote: " + out2);
    }
}
