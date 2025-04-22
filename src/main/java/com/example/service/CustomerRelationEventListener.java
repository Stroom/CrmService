package com.example.service;

import com.example.model.CustomerRelationDto;
import com.example.model.queue.CustomerRelationChangeEventDto;
import com.example.model.queue.DebeziumChangeEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.example.model.queue.CustomerChangeEventType.DELETE;
import static com.example.model.queue.CustomerChangeEventType.INSERT;
import static com.example.model.queue.CustomerChangeEventType.UPDATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerRelationEventListener {

    private static final String VERSION = "v1";
    private static final Set<String> FIELDS = Set.of("id", "customerId", "relatedCustomerId", "typeCode", "validFrom", "validTo");

    private static final String INPUT_TABLE = "customer_relation";
    private static final String OUTPUT_TABLE = "customer_relation";
    private static final String ROUTING_KEY_SUFFIX = "relation";

    private final CrmChangeEventSender crmChangeEventSender;

    @RabbitListener(queues = "${queue.change.customerRelation.queueName}")
    @Transactional
    public void handleEvent(@Payload DebeziumChangeEvent event, Message message) {
        log.debug("Received Debezium event: {}", event);

        if (event == null || event.getOperation() == null || event.getSource() == null
            || !"crm_db".equals(event.getSource().getDb()) || !"crm".equals(event.getSource().getSchema())
            || !INPUT_TABLE.equals(event.getSource().getTable())) {
            log.error("Invalid event received: {}", event);
            return;
        }

        var objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        CustomerRelationDto before = objectMapper.convertValue(event.getBefore(), CustomerRelationDto.class);
        CustomerRelationDto after = objectMapper.convertValue(event.getAfter(), CustomerRelationDto.class);
        switch (event.getOperation()) {
            case "r" -> sendInitialize(after);
            case "c" -> sendCreate(after);
            case "u" -> sendUpdateIfChangedTrackedValue(before, after);
            case "d" -> sendDelete(before);
            default -> log.warn("Unknown operation: {}", event.getOperation());
        }
    }

    private void sendInitialize(CustomerRelationDto after) {
//        log.warn("CustomerRelation initialization command, currently ignored: {}", after.getId());
        crmChangeEventSender.send(toCreateEvent(after), ROUTING_KEY_SUFFIX + ".init_registry." + VERSION);
    }

    private void sendCreate(CustomerRelationDto after) {
        crmChangeEventSender.send(toCreateEvent(after), ROUTING_KEY_SUFFIX + "." + VERSION);
    }

    private CustomerRelationChangeEventDto toCreateEvent(CustomerRelationDto after) {
        return CustomerRelationChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(INSERT.name())
            .id(after.getId())
            .customerId(after.getCustomerId())
            .relatedCustomerId(after.getRelatedCustomerId())
            .before(Map.of())
            .after(fullMap(after))
            .changedFields(FIELDS)
            .build();
    }

    private void sendUpdateIfChangedTrackedValue(CustomerRelationDto before, CustomerRelationDto after) {
        var event = toUpdateEvent(before, after);
        if (event.getChangedFields().isEmpty()) {
            return;
        }
        crmChangeEventSender.send(event, ROUTING_KEY_SUFFIX + "." + VERSION);
    }

    private CustomerRelationChangeEventDto toUpdateEvent(CustomerRelationDto before, CustomerRelationDto after) {
        var changedFields = new HashSet<String>();
        if (!Objects.equals(before.getCustomerId(), after.getCustomerId())) {
            changedFields.add("customerId");
        }
        if (!Objects.equals(before.getRelatedCustomerId(), after.getRelatedCustomerId())) {
            changedFields.add("relatedCustomerId");
        }
        if (!Objects.equals(before.getRelationTypeCode(), after.getRelationTypeCode())) {
            changedFields.add("typeCode");
        }
        if (!Objects.equals(before.getValidFrom(), after.getValidFrom())) {
            changedFields.add("validFrom");
        }
        if (!Objects.equals(before.getValidTo(), after.getValidTo())) {
            changedFields.add("validTo");
        }

        return CustomerRelationChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(UPDATE.name())
            .id(after.getId())
            .customerId(before.getCustomerId())
            .relatedCustomerId(before.getRelatedCustomerId())
            .before(fullMap(before))
            .after(fullMap(after))
            .changedFields(changedFields)
            .build();
    }

    private void sendDelete(CustomerRelationDto before) {
        crmChangeEventSender.send(toDeleteEvent(before), ROUTING_KEY_SUFFIX + "." + VERSION);
    }

    private CustomerRelationChangeEventDto toDeleteEvent(CustomerRelationDto before) {
        return CustomerRelationChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(DELETE.name())
            .id(before.getId())
            .customerId(before.getCustomerId())
            .relatedCustomerId(before.getRelatedCustomerId())
            .before(fullMap(before))
            .after(Map.of())
            .changedFields(FIELDS)
            .build();
    }

    private Map<String, Object> fullMap(CustomerRelationDto relation) {
        var map = new HashMap<String, Object>();
        map.put("id", relation.getId());
        map.put("customerId", relation.getCustomerId());
        map.put("relatedCustomerId", relation.getRelatedCustomerId());
        map.put("typeCode", relation.getRelationTypeCode());
        map.put("validFrom", relation.getValidFrom());
        map.put("validTo", relation.getValidTo());
        return map;
    }

} 