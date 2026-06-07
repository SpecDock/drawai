package com.drawai.domain.aiengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stores the data needed to recreate one maxGraph edge cell.
 *
 * @author specdock
 * @Date 2026/6/6
 * @Time 16:46
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GraphEdge {

    /**
     * maxGraph Cell.id. If absent, maxGraph can generate it when the edge is added.
     */
    private String id;

    /**
     * Parent Cell.id. A null value means the graph default parent should be used.
     */
    private String parentId;

    /**
     * Source node id of this edge.
     */
    private String sourceNodeId;

    /**
     * Target node id of this edge.
     */
    private String targetNodeId;

    /**
     * User object displayed or interpreted by maxGraph, commonly the edge label.
     */
    private Object value;

    /**
     * Controlled style fields for AI streamed edge generation.
     */
    private GraphEdgeStyle style;

    /**
     * Whether this edge can be connected by other edges.
     */
    private Boolean connectable;

    /**
     * Whether this edge is visible in the graph.
     */
    private Boolean visible;

    /**
     * Controlled style fields for basic maxGraph flowchart edges.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GraphEdgeStyle {

        /**
         * maxGraph edge style name, such as orthogonalEdgeStyle or elbowEdgeStyle.
         */
        private String edgeStyle;

        /**
         * Edge stroke color.
         */
        private String strokeColor;

        /**
         * Edge stroke width in pixels.
         */
        private Integer strokeWidth;

        /**
         * Start arrow marker name.
         */
        private String startArrow;

        /**
         * End arrow marker name.
         */
        private String endArrow;

        /**
         * Whether the edge should use rounded corners.
         */
        private Boolean rounded;

        /**
         * Whether the edge should be rendered as a dashed line.
         */
        private Boolean dashed;

        /**
         * Label font size in pixels.
         */
        private Integer fontSize;
    }
}
