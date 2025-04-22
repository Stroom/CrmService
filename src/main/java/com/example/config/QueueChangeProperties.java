package com.example.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "queue.change")
@Configuration
public class QueueChangeProperties {

    private final QueueProperties customer = new QueueProperties();
    private final QueueProperties customerRelation = new QueueProperties();
    private final QueueProperties idDocument = new QueueProperties();
    private final QueueProperties idNumber = new QueueProperties();

    @Getter
    @Setter
    public static class QueueProperties {
        private String exchangeName;
        private String queueName;
        private String routingKey;
    }
}
