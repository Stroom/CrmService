package com.example.model.queue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DebeziumChangeEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = -2324138403049087623L;

    private Source source;
    @JsonProperty("op")
    private String operation;
    private Object before;
    private Object after;

    // Only useful values at the moment
    @Data
    public static class Source {
        private String version;
        @JsonProperty("ts_ms")
        private Long timestamp;
        private String db;
        private String schema;
        private String table;
    }
} 