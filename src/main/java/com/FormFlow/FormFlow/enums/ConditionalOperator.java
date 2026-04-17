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
        String normalized = value.trim().toUpperCase().replace(" ", "_");
        if (normalized.equals("EQUALS")) return EQUAL;
        if (normalized.equals("NOT_EQUALS")) return NOT_EQUAL;
        return ConditionalOperator.valueOf(normalized);
    }

    }