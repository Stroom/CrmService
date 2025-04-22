package com.example.model.queue;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class CustomerRelationChangeEventDto extends CustomerChangeEventDto {
    private Long customerId;
    private Long relatedCustomerId;
}