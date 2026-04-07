package com.FormFlow.FormFlow.DTO.User;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class FormAccessDTO {

    private UUID formId;
    private String owner;

    private Access access;

    @Data
    public static class Access {
        private List<String> editor;
        private List<String> responder;
        private List<String> viewer;
        private List<String> responseViewer;
        private List<String> message;
    }
}