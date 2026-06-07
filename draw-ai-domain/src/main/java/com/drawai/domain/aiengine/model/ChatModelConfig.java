package com.drawai.domain.aiengine.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 29287
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatModelConfig {
        private String roleName;
        private String baseurl;
        private String apiKey;
        private String modelName;
        private Double temperature;
        private Integer readTimeoutMs;
        private String instruction;
    }