package com.example.service;

import com.example.model.IdNumberDto;
import com.example.model.queue.DebeziumChangeEvent;
import com.example.model.queue.IdNumberChangeEventDto;
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
public class IdNumberEventListener {

    private static final String VERSION = "v1";
    private static final Set<String> FIELDS = Set.of("id", "customerId", "idCode", "idCountryCode", "validFrom", "validTo");

    private static final String INPUT_TABLE = "identification_number";
    private static final String OUTPUT_TABLE = "identification_number";
    private static final String ROUTING_KEY_SUFFIX = "idNumber";

    private final CrmChangeEventSender crmChangeEventSender;

    @RabbitListener(queues = "${queue.change.idNumber.queueName}")
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

        IdNumberDto before = objectMapper.convertValue(event.getBefore(), IdNumberDto.class);
        IdNumberDto after = objectMapper.convertValue(event.getAfter(), IdNumberDto.class);
        switch (event.getOperation()) {
            case "r" -> sendInitialize(after);
            case "c" -> sendCreate(after);
            case "u" -> sendUpdateIfChangedTrackedValue(before, after);
            case "d" -> sendDelete(before);
            default -> log.warn("Unknown operation: {}", event.getOperation());
        }
    }

    private void sendInitialize(IdNumberDto after) {
//        log.warn("IdNumber initialization command, currently ignored: {}", after.getId());
        crmChangeEventSender.send(toCreateEvent(after), ROUTING_KEY_SUFFIX + ".init_registry." + VERSION);
    }

    private void sendCreate(IdNumberDto after) {
        crmChangeEventSender.send(toCreateEvent(after), ROUTING_KEY_SUFFIX + "." + VERSION);
    }

    private IdNumberChangeEventDto toCreateEvent(IdNumberDto after) {
        return IdNumberChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(INSERT.name())
            .id(after.getId())
            .customerId(after.getCustomerId())
            .before(Map.of())
            .after(fullMap(after))
            .changedFields(FIELDS)
            .build();
    }

    private void sendUpdateIfChangedTrackedValue(IdNumberDto before, IdNumberDto after) {
        var event = toUpdateEvent(before, after);
        if (event.getChangedFields().isEmpty()) {
            return;
        }
        crmChangeEventSender.send(event, ROUTING_KEY_SUFFIX + "." + VERSION);
    }

    private IdNumberChangeEventDto toUpdateEvent(IdNumberDto before, IdNumberDto after) {
        var changedFields = new HashSet<String>();
        if (!Objects.equals(before.getCustomerId(), after.getCustomerId())) {
            changedFields.add("customerId");
        }
        if (!Objects.equals(before.getIdentificationCode(), after.getIdentificationCode())) {
            changedFields.add("idCode");
        }
        if (!Objects.equals(before.getIdentificationCountryCode(), after.getIdentificationCountryCode())) {
            changedFields.add("idCountryCode");
        }
        if (!Objects.equals(before.getValidFrom(), after.getValidFrom())) {
            changedFields.add("validFrom");
        }
        if (!Objects.equals(before.getValidTo(), after.getValidTo())) {
            changedFields.add("validTo");
        }

        return IdNumberChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(UPDATE.name())
            .id(after.getId())
            .customerId(after.getCustomerId())
            .before(fullMap(before))
            .after(fullMap(after))
            .changedFields(FIELDS)
            .build();
    }

    private void sendDelete(IdNumberDto before) {
        crmChangeEventSender.send(toDeleteEvent(before), ROUTING_KEY_SUFFIX + "." + VERSION);
    }

    private IdNumberChangeEventDto toDeleteEvent(IdNumberDto before) {
        return IdNumberChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(DELETE.name())
            .id(before.getId())
            .customerId(before.getCustomerId())
            .before(fullMap(before))
            .after(Map.of())
            .changedFields(FIELDS)
            .build();
    }

    private Map<String, Object> fullMap(IdNumberDto idNumber) {
        var map = new HashMap<String, Object>();
        map.put("id", idNumber.getId());
        map.put("customerId", idNumber.getCustomerId());
        map.put("idCode", idNumber.getIdentificationCode());
        map.put("idCountryCode", idNumber.getIdentificationCountryCode());
        map.put("validFrom", idNumber.getValidFrom());
        map.put("validTo", idNumber.getValidTo());
        return map;
    }

} 