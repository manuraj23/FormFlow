package com.FormFlow.FormFlow.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ConditionalOperator {
    EQUAL,
    NOT_EQUAL,
    CONTAINS,
    GREATER_THAN,
    LESS_THAN;


    @JsonCreator
    public static ConditionalOperator from(String value) {
        return ConditionalOperator.valueOf(
                value.trim().toUpperCase().replace(" ", "_")
        );

    }
    }