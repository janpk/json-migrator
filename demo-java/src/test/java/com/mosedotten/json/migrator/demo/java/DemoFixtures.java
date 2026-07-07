package com.mosedotten.json.migrator.demo.java;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

abstract class DemoFixtures {

    // A real application is lenient about fields it does not model, so it tolerates newer producers.
    protected final JsonMapper mapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    /** A credit-application document as it might sit in the database (or arrive on Kafka) at {@code version}. */
    protected ObjectNode storedDocument(int version) {
        return (ObjectNode) mapper.readTree(resource("credit-application-v" + version + ".json"));
    }

    private String resource(String name) {
        try (InputStream in = getClass().getResourceAsStream("/" + name)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
