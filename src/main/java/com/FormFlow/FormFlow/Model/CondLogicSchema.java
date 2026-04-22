package com.FormFlow.FormFlow.Model;

import com.FormFlow.FormFlow.enums.ConditionalOperator;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CondLogicSchema {

    // if false, ignore this condition entirely — field/section always active
    private boolean enabled;

    // the field this condition depends on
    private String sourceFieldId;

    // handles "NOT EQUAL" from frontend and NOT_EQUAL from our enum

    private ConditionalOperator operator;

    // any value — string, number, boolean
    private Object value;

    // "SHOW" or "HIDE"
    private String action;
}
