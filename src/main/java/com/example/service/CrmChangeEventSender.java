package com.example.service;

import com.example.model.queue.CustomerChangeEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class CrmChangeEventSender {

    private final RabbitTemplate rabbitTemplate;

    @Value("${crm.update.exchange.name}")
    private String crmUpdateExchangeName;

    @Value("${crm.update.routingKey.prefix}")
    private String crmUpdatePrefix;

    public void send(CustomerChangeEventDto event, String routingKeySuffix) {
        rabbitTemplate.convertAndSend(
            crmUpdateExchangeName,
            createRoutingKey(routingKeySuffix),
            event
        );
    }

    private String createRoutingKey(String suffix) {
        return crmUpdatePrefix + suffix;
    }

}