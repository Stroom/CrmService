package com.example.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.time.LocalDate;

@EqualsAndHashCode
@Data
public class IdDocumentDto {

    private Long id;
    private Long customerId;
    private String documentTypeCode;
    private String documentNumber;
    private LocalDate documentValidDate;
    private LocalDate documentExpiryDate;
    private String documentCountryCode;
    private Instant modifiedDTime;
    private String status;
    private boolean fromRegistryResponse;

}
