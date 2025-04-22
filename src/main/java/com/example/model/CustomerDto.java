package com.example.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@EqualsAndHashCode
@Data
public class CustomerDto implements Serializable {

    private Long id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String fullNameLatin;
    private LocalDate dateOfBirth;
    private String placeOfBirthCountryCode;
    private String genderCode;
    private String titleCode;
    private String residencyCountryCode;
    private String taxResidencyCountryCode;
    private String taxPayerCode;
    private String vatNumber;
    private String legalEntityIdentifier;
    private String citizenCountryCode;
    private String dualCitizenCountryCode;
    private String statusCode;
    private String languageCode;
    private LocalDate dateOfDeath;
    private LocalDate dateOfBankruptcy;
    private String mainOfficeCode;
    private String marketingConsent;
    private Instant modifiedDTime;
    private Instant accuityVerificationTime;
    private boolean sensitive;
    private Integer numberOfEmployees;
    private BigDecimal balanceSheetTotal;
    private BigDecimal annualTurnover;
    private String legalForm;
    private Instant customerUpdateTime;
    private String secretPhrase;
    private String relationshipManager;
    private LocalDate dateToReview;
    private String nameOnCard;
    private String nameOnTrack1;
    private String createdBy;
    private boolean smallToMediumSizedEnterprise;
    private String sourceOrgCode;
    private Instant customerSheetUpdatedDtime;

}
