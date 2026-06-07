package com.drawai.domain.aiengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stores the data needed to recreate one maxGraph vertex cell.
 *
 * @author specdock
 * @Date 2026/6/6
 * @Time 16:46
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GraphNode {

    /**
     * maxGraph Cell.id. If absent, maxGraph can generate it when the cell is added.
     */
    private String id;

    /**
     * Parent Cell.id. A null value means the graph default parent should be used.
     */
    private String parentId;

    /**
     * User object displayed or interpreted by maxGraph, commonly the node label.
     */
    private Object value;

    /**
     * Geometry x coordinate, or position[0] for graph.insertVertex(...).
     */
    private Double x;

    /**
     * Geometry y coordinate, or position[1] for graph.insertVertex(...).
     */
    private Double y;

    /**
     * Geometry width, or size[0] for graph.insertVertex(...).
     */
    private Double width;

    /**
     * Geometry height, or size[1] for graph.insertVertex(...).
     */
    private Double height;

    /**
     * Whether the geometry is relative to its parent cell.
     */
    private Boolean relative;

    /**
     * maxGraph CellStyle values, such as shape, fillColor, strokeColor or rounded.
     */
    private GraphNodeStyle style;

    /**
     * Whether this node can be connected by edges.
     */
    private Boolean connectable;

    /**
     * Whether this node is visible in the graph.
     */
    private Boolean visible;

    /**
     * Whether this node is currently collapsed.
     */
    private Boolean collapsed;

    /**
     * Controlled style fields for AI streamed node generation.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GraphNodeStyle {

        /**
         * maxGraph shape name, such as rectangle, ellipse or rhombus.
         */
        private String shape;

        /**
         * Node fill color.
         */
        private String fillColor;

        /**
         * Node border color.
         */
        private String strokeColor;

        /**
         * Whether the node should use rounded corners.
         */
        private Boolean rounded;

        /**
         * Label font size in pixels.
         */
        private Integer fontSize;
    }
}
