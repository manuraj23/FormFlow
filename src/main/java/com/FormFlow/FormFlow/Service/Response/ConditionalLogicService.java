package com.FormFlow.FormFlow.Service.Response;

import com.FormFlow.FormFlow.Entity.FormFields;
import com.FormFlow.FormFlow.Model.CondLogicSchema;
import com.FormFlow.FormFlow.enums.ConditionalOperator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class ConditionalLogicService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // public entry point — called from ResponseService
    // fresh visited set per call so concurrent requests never share state
    public boolean isActive(Object logicObj,
                            Map<String, Object> submittedResponse,
                            Map<String, FormFields> fieldMap) {
        return isActiveInternal(logicObj, submittedResponse, fieldMap, new HashSet<>());
    }

    private boolean isActiveInternal(Object logicObj,
                                     Map<String, Object> submittedResponse,
                                     Map<String, FormFields> fieldMap,
                                     Set<String> visited) {

        // no condition means always active
        if (logicObj == null) return true;

        // convert raw map to CondLogicSchema
        CondLogicSchema logic = objectMapper.convertValue(logicObj, CondLogicSchema.class);

        // if condition exists but is disabled — always active
        if (!logic.isEnabled()) return true;

        String parentId = logic.getSourceFieldId();

        // circular reference guard — if we've already visited this field
        // in the current chain, treat as active to avoid stack overflow
        if (visited.contains(parentId)) return true;
        visited.add(parentId);

        // --- LAYER 3: walk up the chain before evaluating this condition ---
        // check if the parent field itself is active before trusting its value
        // if parent is hidden, this field is hidden too regardless of submitted value
        if (fieldMap != null && fieldMap.containsKey(parentId)) {
            FormFields parentField = fieldMap.get(parentId);
            boolean parentActive = isActiveInternal(
                    parentField.getFieldLogic(),
                    submittedResponse,
                    fieldMap,
                    visited
            );
            if (!parentActive) return false;
        }
        // --------------------------------------------------------------------

        // get the value submitted for the field this condition depends on
        Object dependsOnValue = submittedResponse.get(parentId);

        // evaluate the condition
        boolean conditionMet = evaluate(dependsOnValue, logic.getOperator(), logic.getValue());

        // SHOW — field is active when condition IS met
        // HIDE — field is active when condition is NOT met
        if ("SHOW".equalsIgnoreCase(logic.getAction())) {
            return conditionMet;
        } else if ("HIDE".equalsIgnoreCase(logic.getAction())) {
            return !conditionMet;
        }

        // default to active if action is unrecognized
        return true;
    }

    private boolean evaluate(Object submittedValue,
                             ConditionalOperator operator,
                             Object conditionValue) {

        // if the depends-on field has no value — condition is not met
        if (submittedValue == null) return false;

        String submitted = submittedValue.toString().trim();
        String condition = conditionValue != null ? conditionValue.toString().trim() : "";

        switch (operator) {

            case EQUAL:
                return submitted.equalsIgnoreCase(condition);

            case NOT_EQUAL:
                return !submitted.equalsIgnoreCase(condition);

            case CONTAINS:
                return submitted.toLowerCase().contains(condition.toLowerCase());

            case GREATER_THAN:
                try {
                    return Double.parseDouble(submitted) > Double.parseDouble(condition);
                } catch (NumberFormatException e) {
                    return false;
                }

            case LESS_THAN:
                try {
                    return Double.parseDouble(submitted) < Double.parseDouble(condition);
                } catch (NumberFormatException e) {
                    return false;
                }

            default:
                return true;
        }
    }
}