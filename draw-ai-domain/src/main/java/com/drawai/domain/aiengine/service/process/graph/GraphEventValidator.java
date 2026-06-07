package com.drawai.domain.aiengine.service.process.graph;

import com.drawai.domain.aiengine.model.GraphEdge;
import com.drawai.domain.aiengine.model.GraphNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Validates and normalizes AI-generated graph events before streaming them to clients.
 */
@Service
public class GraphEventValidator {

    private static final String TYPE_NODE = "node";
    private static final String TYPE_EDGE = "edge";
    private static final Set<String> ALLOWED_NODE_SHAPES = Set.of("rectangle", "ellipse", "rhombus");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    public GraphEvent parse(String eventJson) {
        JsonNode root = readObject(eventJson);
        requireEnvelope(root);

        String type = text(root.get("type"), "type");
        JsonNode data = root.get("data");
        if (!data.isObject()) {
            throw new IllegalArgumentException("Graph event data must be an object");
        }

        if (TYPE_NODE.equals(type)) {
            GraphNode node = convert(data, GraphNode.class);
            validateNode(node);
            return GraphEvent.node(node);
        }
        if (TYPE_EDGE.equals(type)) {
            GraphEdge edge = convert(data, GraphEdge.class);
            validateEdge(edge);
            return GraphEvent.edge(edge);
        }

        throw new IllegalArgumentException("Graph event type must be node or edge");
    }

    public GraphEvent parseSelected(String selectedJson) {
        JsonNode root = readObject(selectedJson);
        if (root.has("type") && root.has("data")) {
            return parse(selectedJson);
        }

        if (root.has("sourceNodeId") || root.has("targetNodeId")) {
            GraphEdge edge = convert(root, GraphEdge.class);
            validateEdge(edge);
            return GraphEvent.edge(edge);
        }

        GraphNode node = convert(root, GraphNode.class);
        validateNode(node);
        return GraphEvent.node(node);
    }

    public String normalize(String eventJson) {
        return toJson(parse(eventJson));
    }

    public String toJson(GraphEvent event) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", event.type());
        root.set("data", objectMapper.valueToTree(event.data()));
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize graph event", e);
        }
    }

    public GraphEvent lockId(GraphEvent event, String lockedId) {
        requireText(lockedId, "lockedId");
        if (event.isNode()) {
            event.node().setId(lockedId);
            validateNode(event.node());
            return event;
        }

        event.edge().setId(lockedId);
        validateEdge(event.edge());
        return event;
    }

    private JsonNode readObject(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw new IllegalArgumentException("Graph event must be a JSON object");
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Graph event must be valid JSON", e);
        }
    }

    private void requireEnvelope(JsonNode root) {
        Set<String> fields = new HashSet<>();
        Iterator<String> names = root.fieldNames();
        while (names.hasNext()) {
            fields.add(names.next());
        }
        if (!fields.equals(Set.of("type", "data"))) {
            throw new IllegalArgumentException("Graph event must contain only type and data");
        }
    }

    private <T> T convert(JsonNode data, Class<T> type) {
        try {
            return objectMapper.treeToValue(data, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Graph event data does not match " + type.getSimpleName(), e);
        }
    }

    private void validateNode(GraphNode node) {
        requireText(node.getId(), "node.id");
        if (!(node.getValue() instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException("node.value must be a non-blank string");
        }
        requireFinite(node.getX(), "node.x");
        requireFinite(node.getY(), "node.y");
        requirePositive(node.getWidth(), "node.width");
        requirePositive(node.getHeight(), "node.height");
        if (node.getRelative() == null || node.getConnectable() == null
                || node.getVisible() == null || node.getCollapsed() == null) {
            throw new IllegalArgumentException("node boolean fields are required");
        }
        validateNodeStyle(node.getStyle());
    }

    private void validateNodeStyle(GraphNode.GraphNodeStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("node.style is required");
        }
        requireText(style.getShape(), "node.style.shape");
        if (!ALLOWED_NODE_SHAPES.contains(style.getShape())) {
            throw new IllegalArgumentException("node.style.shape is not allowed");
        }
        requireText(style.getFillColor(), "node.style.fillColor");
        requireText(style.getStrokeColor(), "node.style.strokeColor");
        if (style.getRounded() == null) {
            throw new IllegalArgumentException("node.style.rounded is required");
        }
        requirePositive(style.getFontSize(), "node.style.fontSize");
    }

    private void validateEdge(GraphEdge edge) {
        requireText(edge.getId(), "edge.id");
        requireText(edge.getSourceNodeId(), "edge.sourceNodeId");
        requireText(edge.getTargetNodeId(), "edge.targetNodeId");
        if (edge.getValue() != null && !(edge.getValue() instanceof String)) {
            throw new IllegalArgumentException("edge.value must be a string or null");
        }
        if (edge.getConnectable() == null || edge.getVisible() == null) {
            throw new IllegalArgumentException("edge boolean fields are required");
        }
        validateEdgeStyle(edge.getStyle());
    }

    private void validateEdgeStyle(GraphEdge.GraphEdgeStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("edge.style is required");
        }
        requireText(style.getEdgeStyle(), "edge.style.edgeStyle");
        requireText(style.getStrokeColor(), "edge.style.strokeColor");
        requirePositive(style.getStrokeWidth(), "edge.style.strokeWidth");
        if (style.getEndArrow() != null && style.getEndArrow().isBlank()) {
            throw new IllegalArgumentException("edge.style.endArrow must not be blank");
        }
        if (style.getStartArrow() != null && style.getStartArrow().isBlank()) {
            throw new IllegalArgumentException("edge.style.startArrow must not be blank");
        }
        if (style.getRounded() == null || style.getDashed() == null) {
            throw new IllegalArgumentException("edge.style rounded and dashed are required");
        }
        requirePositive(style.getFontSize(), "edge.style.fontSize");
    }

    private String text(JsonNode value, String field) {
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.asText();
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private void requireFinite(Double value, String field) {
        if (value == null || !Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be a finite number");
        }
    }

    private void requirePositive(Double value, String field) {
        requireFinite(value, field);
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private void requirePositive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    public record GraphEvent(String type, GraphNode node, GraphEdge edge) {

        public static GraphEvent node(GraphNode node) {
            return new GraphEvent(TYPE_NODE, node, null);
        }

        public static GraphEvent edge(GraphEdge edge) {
            return new GraphEvent(TYPE_EDGE, null, edge);
        }

        public boolean isNode() {
            return TYPE_NODE.equals(type);
        }

        public Object data() {
            return isNode() ? node : edge;
        }

        public String id() {
            return isNode() ? node.getId() : edge.getId();
        }
    }
}
