package com.mosedotten.json.migrator.engine.java;

import com.mosedotten.json.migrator.engine.dsl.ExecutionStrategy;
import com.mosedotten.json.migrator.engine.exception.MigrationExecutionException;
import com.mosedotten.json.migrator.engine.operation.JsonType;
import com.mosedotten.json.migrator.engine.operation.ValueJoinerStrategy;
import com.mosedotten.json.migrator.engine.operation.ValueSplitterStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests exercised through the same Java surface a consumer would use.
 * If a change makes the facade Java-hostile (e.g. leaks a Kotlin-only type), this file stops compiling.
 */
@DisplayName("When migrating from Java through the facade")
class JsonMigratorTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private ObjectNode obj(String json) { return (ObjectNode) mapper.readTree(json); }

    @Test
    void chainsMultipleMigrationsAndBumpsTheVersion() {
        ObjectNode result = JsonMigrator.migrate(
                obj("{\"schemaVersion\":1,\"firstName\":\"John\",\"lastName\":\"Doe\",\"deprecated\":true}"))
            .migration(1, 2, m -> m
                .merge("/fullName", "/firstName", "/lastName")
                .remove("/deprecated")
                .add("/enabled", BooleanNode.TRUE))
            .migration(2, 3, m -> m
                .move("/fullName", "/name"))
            .run();
        assertEquals(obj("{\"schemaVersion\":3,\"name\":\"John Doe\",\"enabled\":true}"), result);
    }

    @Test
    void appliesOperationsToEachArrayElementWithForEach() {
        ObjectNode result = JsonMigrator.migrate(
                obj("{\"schemaVersion\":1,\"users\":[{\"name\":\"John\"},{\"name\":\"Jane\"}]}"))
            .migration(1, 2, m -> m.forEach("/users", u -> u.move("/name", "/fullName")))
            .run();
        assertEquals(
            obj("{\"schemaVersion\":2,\"users\":[{\"fullName\":\"John\"},{\"fullName\":\"Jane\"}]}"),
            result);
    }

    @Test
    void transformsAValueWithAJavaLambda() {
        ObjectNode result = JsonMigrator.migrate(obj("{\"schemaVersion\":1,\"age\":30}"))
            .migration(1, 2, m -> m.transform("/age", v -> IntNode.valueOf(v.asInt() + 1)))
            .run();
        assertEquals(obj("{\"schemaVersion\":2,\"age\":31}"), result);
    }

    @Test
    void runsAnArbitraryCustomBlock() {
        ObjectNode result = JsonMigrator.migrate(obj("{\"schemaVersion\":1,\"name\":\"John\"}"))
            .migration(1, 2, m -> m.custom(node -> node.set("enabled", BooleanNode.TRUE)))
            .run();
        assertEquals(obj("{\"schemaVersion\":2,\"name\":\"John\",\"enabled\":true}"), result);
    }

    @Test
    void acceptsCustomJoinerAndSplitterStrategies() {
        ValueJoinerStrategy commaJoiner = values ->
            values.stream().map(JsonNode::asString).collect(Collectors.joining(","));
        ValueSplitterStrategy commaSplitter = value -> java.util.List.of(value.asString().split(","));

        ObjectNode merged = JsonMigrator.migrate(
                obj("{\"schemaVersion\":1,\"firstName\":\"John\",\"lastName\":\"Doe\"}"))
            .migration(1, 2, m -> m.merge("/fullName", commaJoiner, "/firstName", "/lastName"))
            .run();
        assertEquals(obj("{\"schemaVersion\":2,\"fullName\":\"John,Doe\"}"), merged);

        ObjectNode split = JsonMigrator.migrate(obj("{\"schemaVersion\":1,\"fullName\":\"John,Doe\"}"))
            .migration(1, 2, m -> m.split("/fullName", commaSplitter, "/firstName", "/lastName"))
            .run();
        assertEquals(obj("{\"schemaVersion\":2,\"firstName\":\"John\",\"lastName\":\"Doe\"}"), split);
    }

    @Test
    void honorsACustomVersionField() {
        ObjectNode result = JsonMigrator.migrate(obj("{\"version\":1,\"name\":\"John\"}"))
            .versionField("version")
            .migration(1, 2, m -> m.add("/enabled", BooleanNode.TRUE))
            .run();
        assertEquals(obj("{\"version\":2,\"name\":\"John\",\"enabled\":true}"), result);
    }

    @Test
    void writesTheVersionFieldWhenMissingIsAllowed() {
        ObjectNode result = JsonMigrator.migrate(obj("{\"name\":\"John\"}"))
            .allowMissingVersionField()
            .migration(1, 2, m -> m.add("/enabled", BooleanNode.TRUE))
            .run();
        assertEquals(obj("{\"name\":\"John\",\"enabled\":true,\"schemaVersion\":2}"), result);
    }

    @Test
    void passesRequireGuardsWhenSatisfied() {
        ObjectNode result = JsonMigrator.migrate(obj("{\"schemaVersion\":1,\"id\":\"123\",\"age\":30}"))
            .migration(1, 2, m -> m
                .requireExists("/id")
                .requireType("/age", JsonType.NUMBER))
            .run();
        assertEquals(obj("{\"schemaVersion\":2,\"id\":\"123\",\"age\":30}"), result);
    }

    @Test
    void rollsBackToTheOriginalWhenAMigrationFailsAtomically() {
        ObjectNode root = obj("{\"schemaVersion\":1,\"fullName\":\"John Doe\"}");

        assertThrows(MigrationExecutionException.class, () -> JsonMigrator.migrate(root)
            .migration(1, 2, m -> m.split("/fullName", "/name", "/name"))
            .run());
        assertEquals(obj("{\"schemaVersion\":1,\"fullName\":\"John Doe\"}"), root);
    }

    @Test
    void leavesPartialMutationsWhenAtomicityIsDisabled() {
        ObjectNode root = obj("{\"schemaVersion\":1,\"fullName\":\"John Doe\"}");

        assertThrows(MigrationExecutionException.class, () -> JsonMigrator.migrate(root)
            .nonAtomic()
            .migration(1, 2, m -> m.split("/fullName", "/name", "/name"))
            .run());
        assertEquals(obj("{\"schemaVersion\":1,\"fullName\":\"John Doe\",\"name\":\"John\"}"), root);
    }

    @Test
    void skipsAMigrationWhoseFromIsBehindTheDocumentVersion() {
        ObjectNode result = JsonMigrator.migrate(obj("{\"schemaVersion\":5,\"name\":\"John\"}"))
            .migration(1, 2, m -> m.add("/enabled", BooleanNode.TRUE))
            .run();
        assertEquals(obj("{\"schemaVersion\":5,\"name\":\"John\"}"), result);
    }

    @Test
    void setCreatesOrOverwritesAField() {
        ObjectNode result = JsonMigrator.migrate(obj("{\"schemaVersion\":1,\"enabled\":false}"))
            .migration(1, 2, m -> m
                .set("/enabled", BooleanNode.TRUE)
                .set("/count", IntNode.valueOf(3)))
            .run();
        assertEquals(obj("{\"schemaVersion\":2,\"enabled\":true,\"count\":3}"), result);
    }

    @Test
    void copyDuplicatesAField() {
        ObjectNode result = JsonMigrator.migrate(obj("{\"schemaVersion\":1,\"id\":\"123\"}"))
            .migration(1, 2, m -> m.copy("/id", "/legacyId"))
            .run();
        assertEquals(obj("{\"schemaVersion\":2,\"id\":\"123\",\"legacyId\":\"123\"}"), result);
    }

    @Test
    void createObjectEnsuresAnObjectExists() {
        ObjectNode result = JsonMigrator.migrate(obj("{\"schemaVersion\":1,\"name\":\"John\"}"))
            .migration(1, 2, m -> m.createObject("/address"))
            .run();
        assertEquals(obj("{\"schemaVersion\":2,\"name\":\"John\",\"address\":{}}"), result);
    }

    @Test
    void removeIfEmptyDropsEmptyContainersWithAndWithoutCascade() {
        ObjectNode withoutCascade = JsonMigrator.migrate(
                obj("{\"schemaVersion\":1,\"name\":\"John\",\"address\":{}}"))
            .migration(1, 2, m -> m.removeIfEmpty("/address"))
            .run();
        assertEquals(obj("{\"schemaVersion\":2,\"name\":\"John\"}"), withoutCascade);

        ObjectNode withCascade = JsonMigrator.migrate(obj("{\"schemaVersion\":1,\"profile\":{\"contact\":{}}}"))
            .migration(1, 2, m -> m.removeIfEmpty("/profile/contact", true))
            .run();
        assertEquals(obj("{\"schemaVersion\":2}"), withCascade);
    }

    @Test
    void transformLenientIsANoOpWhenTheFieldIsMissing() {
        ObjectNode result = JsonMigrator.migrate(obj("{\"schemaVersion\":1,\"name\":\"John\"}"))
            .migration(1, 2, m -> m.transformLenient("/age", v -> IntNode.valueOf(v.asInt() + 1)))
            .run();
        assertEquals(obj("{\"schemaVersion\":2,\"name\":\"John\"}"), result);
    }

    @Test
    void atomicRollbackIsExplicitlySelectable() {
        ObjectNode root = obj("{\"schemaVersion\":1,\"fullName\":\"John Doe\"}");

        assertThrows(MigrationExecutionException.class, () -> JsonMigrator.migrate(root)
            .atomic()
            .migration(1, 2, m -> m.split("/fullName", "/name", "/name"))
            .run());
        assertEquals(obj("{\"schemaVersion\":1,\"fullName\":\"John Doe\"}"), root);
    }

    @Test
    void usesACustomExecutionStrategy() {
        ExecutionStrategy passthrough = (_, block) -> block.invoke();
        ObjectNode root = obj("{\"schemaVersion\":1,\"fullName\":\"John Doe\"}");

        assertThrows(MigrationExecutionException.class, () -> JsonMigrator.migrate(root)
            .execution(passthrough)
            .migration(1, 2, m -> m.split("/fullName", "/name", "/name"))
            .run());
        // A passthrough strategy performs no rollback, so the partial mutation remains.
        assertEquals(obj("{\"schemaVersion\":1,\"fullName\":\"John Doe\",\"name\":\"John\"}"), root);
    }
}
