package com.example.service;

import com.example.model.CustomerDto;
import com.example.model.queue.CustomerChangeEventDto;
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
public class CustomerEventListener {

    private static final String VERSION = "v1";
    private static final Set<String> FIELDS = Set.of("id", "fullName", "firstName", "middleName", "lastName", "dateOfBirth", "orgCode");

    private static final String INPUT_TABLE = "customer";
    private static final String OUTPUT_TABLE = "customer";
    private static final String ROUTING_KEY_SUFFIX = "customer";

    private final CrmChangeEventSender crmChangeEventSender;

    @RabbitListener(queues = "${queue.change.customer.queueName}")
    @Transactional
    public void handleEvent(@Payload DebeziumChangeEvent event, Message message) {
        log.debug("Received Debezium event: {}", event);

        if (event == null || event.getOperation() == null || event.getSource() == null
            || !"crm_db".equals(event.getSource().getDb()) || !"crm".equals(event.getSource().getSchema())
            || !INPUT_TABLE.equals(event.getSource().getTable())) {
            log.error("Invalid event received: {}", event);
            return;
        }

        // TODO common debezium objectMapper config
        var objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        CustomerDto before = objectMapper.convertValue(event.getBefore(), CustomerDto.class);
        CustomerDto after = objectMapper.convertValue(event.getAfter(), CustomerDto.class);
        switch (event.getOperation()) {
            case "r" -> sendInitialize(after);
            case "c" -> sendCreate(after);
            case "u" -> sendUpdateIfChangedTrackedValue(before, after);
            case "d" -> sendDelete(before);
            default -> log.warn("Unknown operation: {}", event.getOperation());
        }
    }

    private void sendInitialize(CustomerDto after) {
//        log.warn("Customer initialization command, currently ignored: {}", after.getId());
        crmChangeEventSender.send(toCreateEvent(after), ROUTING_KEY_SUFFIX + ".init_registry." + after.getSourceOrgCode() + "." + VERSION);
    }

    private void sendCreate(CustomerDto after) {
        crmChangeEventSender.send(toCreateEvent(after), ROUTING_KEY_SUFFIX + "." + after.getSourceOrgCode() + "." + VERSION);
    }

    private CustomerChangeEventDto toCreateEvent(CustomerDto after) {
        return CustomerChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(INSERT.name())
            .id(after.getId())
            .before(Map.of())
            .after(fullMap(after))
            .changedFields(FIELDS)
            .build();
    }

    private void sendUpdateIfChangedTrackedValue(CustomerDto before, CustomerDto after) {
        var event = toUpdateEvent(before, after);
        if (event.getChangedFields().isEmpty()) {
            return;
        }
        crmChangeEventSender.send(event, ROUTING_KEY_SUFFIX + "." + before.getSourceOrgCode() + "." + VERSION);
    }

    private CustomerChangeEventDto toUpdateEvent(CustomerDto before, CustomerDto after) {
        var changedFields = new HashSet<String>();
        if (!Objects.equals(before.getFullName(), after.getFullName())) {
            changedFields.add("fullName");
        }
        if (!Objects.equals(before.getFirstName(), after.getFirstName())) {
            changedFields.add("firstName");
        }
        if (!Objects.equals(before.getMiddleName(), after.getMiddleName())) {
            changedFields.add("middleName");
        }
        if (!Objects.equals(before.getLastName(), after.getLastName())) {
            changedFields.add("lastName");
        }
        if (!Objects.equals(before.getDateOfBirth(), after.getDateOfBirth())) {
            changedFields.add("dateOfBirth");
        }
        if (!Objects.equals(before.getSourceOrgCode(), after.getSourceOrgCode())) {
            changedFields.add("orgCode");
        }

        return CustomerChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(UPDATE.name())
            .id(after.getId())
            .before(fullMap(before))
            .after(fullMap(after))
            .changedFields(changedFields)
            .build();
    }

    private void sendDelete(CustomerDto before) {
        crmChangeEventSender.send(toDeleteEvent(before), ROUTING_KEY_SUFFIX + "." + before.getSourceOrgCode() + "." + VERSION);
    }

    private CustomerChangeEventDto toDeleteEvent(CustomerDto before) {
        return CustomerChangeEventDto.builder()
            .table(OUTPUT_TABLE)
            .type(DELETE.name())
            .id(before.getId())
            .before(fullMap(before))
            .after(Map.of())
            .changedFields(FIELDS)
            .build();
    }

    private Map<String, Object> fullMap(CustomerDto customer) {
        var map = new HashMap<String, Object>();
        if (customer == null) {
            return map;
        }
        map.put("id", customer.getId());
        map.put("fullName", customer.getFullName());
        map.put("firstName", customer.getFirstName());
        map.put("middleName", customer.getMiddleName());
        map.put("lastName", customer.getLastName());
        map.put("dateOfBirth", customer.getDateOfBirth());
        map.put("orgCode", customer.getSourceOrgCode());
        return map;
    }

} 