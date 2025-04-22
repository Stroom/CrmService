package com.example.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.time.LocalDate;

@EqualsAndHashCode
@Data
public class IdNumberDto {

    private Long id;
    private Long customerId;
    private String identificationCode;
    private String identificationCountryCode;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Instant modifiedDTime;
    private String legalPrivateCode;

}
