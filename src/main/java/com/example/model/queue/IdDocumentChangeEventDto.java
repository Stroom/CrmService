package com.example.model.queue;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class IdDocumentChangeEventDto extends CustomerChangeEventDto {
    private Long customerId;
}