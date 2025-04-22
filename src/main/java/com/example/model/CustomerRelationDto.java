package com.example.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@ToString
public class CustomerRelationDto {

    private Long id;
    private Long customerId;
    private Long relatedCustomerId;
    private String relatedCustomerName;
    private Long relatedRelationId;
    private String relatedCustomerIdentificationCode;
    private String relatedCustomerCountryCode;
    private String relatedCustomerLegalPrivateCode;
    private String relationTypeCode;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String sourceName;
    private Long sourceRef;
    private String fileId;
    private String basisOfProcurationNumber;
    private Instant modifiedDTime;
    private Instant createdDTime;

}
