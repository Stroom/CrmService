package com.example.service;

import com.example.model.IdDocumentDto;
import com.example.model.queue.DebeziumChangeEvent;
import com.example.model.queue.IdDocumentChangeEventDto;
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
public class IdDocumentEventListener {

    private static final String VERSION = "v1";
    private static final Set<String> FIELDS = Set.of("id", "customerId", "typeCode", "number", "countryCode", "validFrom", "validTo");

    private static final String INPUT_TABLE = "identification_document";
    private static final String OUTPUT_TABLE = "identification_document";
    private static final String ROUTING_KEY_SUFFIX = "idDocument";

    private final CrmChangeEventSender crmChangeEventSender;

    @RabbitListener(queues = "${queue.change.idDocument.queueName}")
    @Transactional
    public void handleEvent(@Payload DebeziumChangeEvent event, Message message) {
        log.debug("Received Debezium event: {}", event);

        if (event == null || event.getOperation() == null || event.getSource() == null
            || !"crm_db".equals(event.getSource().getDb()) || !"crm".equals(event.getSource().getSchema())
            || !INPUT_TABLE.equals(event.getSource().getTable())) {
            log.debug("Invalid event received: {}", event);
            return;
        }

        var objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        IdDocumentDto before = objectMapper.convertValue(event.getBefore(), IdDocumentDto.class);
        IdDocumentDto after = objectMapper.convertValue(event.getAfter(), IdDocumentDto.class);
        switch (event.getOperation()) {
            case "r" -> sendInitialize(after);
            case "c" -> sendCreate(after);
            case "u" -> sendUpdateIfChangedTrackedValue(before, after);
            case "d" -> sendDelete(before);
            default -> log.warn("Unknown operation: {}", event.getOperation());
        }
    }

    private void sendInitialize(IdDocumentDto after) {
//        log.warn("IdDocument initialization command, currently ignored: {}", after.getId());
        crmChangeEventSender.send(toCreateEvent(after), ROUTING_KEY_SUFFIX + ".init_registry." + VERSION);
    }

    private void sendCreate(IdDocumentDto after) {
        crmChangeEventSender.send(toCreateEvent(after), ROUTING_KEY_SUFFIX + "." + VERSION);
    }

    private IdDocumentChangeEventDto toCreateEvent(IdDocumentDto after) {
        return IdDocumentChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(INSERT.name())
            .id(after.getId())
            .customerId(after.getCustomerId())
            .before(Map.of())
            .after(fullMap(after))
            .changedFields(FIELDS)
            .build();
    }

    private void sendUpdateIfChangedTrackedValue(IdDocumentDto before, IdDocumentDto after) {
        var event = toUpdateEvent(before, after);
        if (event.getChangedFields().isEmpty()) {
            return;
        }
        crmChangeEventSender.send(event, ROUTING_KEY_SUFFIX + "." + VERSION);
    }

    private IdDocumentChangeEventDto toUpdateEvent(IdDocumentDto before, IdDocumentDto after) {
        var changedFields = new HashSet<String>();
        if (!Objects.equals(before.getCustomerId(), after.getCustomerId())) {
            changedFields.add("customerId");
        }
        if (!Objects.equals(before.getDocumentTypeCode(), after.getDocumentTypeCode())) {
            changedFields.add("typeCode");
        }
        if (!Objects.equals(before.getDocumentNumber(), after.getDocumentNumber())) {
            changedFields.add("number");
        }
        if (!Objects.equals(before.getDocumentCountryCode(), after.getDocumentCountryCode())) {
            changedFields.add("countryCode");
        }
        if (!Objects.equals(before.getDocumentValidDate(), after.getDocumentValidDate())) {
            changedFields.add("validFrom");
        }
        if (!Objects.equals(before.getDocumentExpiryDate(), after.getDocumentExpiryDate())) {
            changedFields.add("validTo");
        }

        return IdDocumentChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(UPDATE.name())
            .id(after.getId())
            .customerId(after.getCustomerId())
            .before(fullMap(before))
            .after(fullMap(after))
            .changedFields(changedFields)
            .build();
    }

    private void sendDelete(IdDocumentDto before) {
        crmChangeEventSender.send(toDeleteEvent(before), ROUTING_KEY_SUFFIX + "." + VERSION);
    }

    private IdDocumentChangeEventDto toDeleteEvent(IdDocumentDto before) {
        return IdDocumentChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(DELETE.name())
            .id(before.getId())
            .customerId(before.getCustomerId())
            .before(fullMap(before))
            .after(Map.of())
            .changedFields(FIELDS)
            .build();
    }

    private Map<String, Object> fullMap(IdDocumentDto idDocument) {
        var map = new HashMap<String, Object>();
        map.put("id", idDocument.getId());
        map.put("customerId", idDocument.getCustomerId());
        map.put("typeCode", idDocument.getDocumentTypeCode());
        map.put("number", idDocument.getDocumentNumber());
        map.put("countryCode", idDocument.getDocumentCountryCode());
        map.put("validFrom", idDocument.getDocumentValidDate());
        map.put("validTo", idDocument.getDocumentExpiryDate());
        return map;
    }

} 