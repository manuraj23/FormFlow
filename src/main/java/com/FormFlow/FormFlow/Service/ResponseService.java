package com.FormFlow.FormFlow.Service;

import com.FormFlow.FormFlow.DTO.Response.FormResponseDTO;
import com.FormFlow.FormFlow.Entity.FormResponse;
import com.FormFlow.FormFlow.Repository.FormResponseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResponseService {

    private final FormResponseRepository repository;

    public ResponseService(FormResponseRepository repository) {
        this.repository = repository;
    }

    public FormResponseDTO saveResponse(FormResponseDTO dto) {
        FormResponse entity = mapToEntity(dto);
        entity.setSubmittedAt(LocalDateTime.now());

        FormResponse saved = repository.save(entity);
        return mapToDTO(saved);
    }

    public List<FormResponseDTO> getResponses(Long formId) {
        return repository.findByFormId(formId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<FormResponseDTO> getByEmail(String email) {
        return repository.findByEmail(email)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private FormResponseDTO mapToDTO(FormResponse entity) {
        FormResponseDTO dto = new FormResponseDTO();
        dto.setResponseId(entity.getResponseId());
        dto.setFormId(entity.getFormId());
        dto.setResponse(entity.getResponse());
        dto.setSubmittedAt(entity.getSubmittedAt());
        return dto;
    }

    private FormResponse mapToEntity(FormResponseDTO dto) {
        FormResponse entity = new FormResponse();
        entity.setResponseId(dto.getResponseId());
        entity.setFormId(dto.getFormId());
        entity.setResponse(dto.getResponse());
        entity.setSubmittedAt(dto.getSubmittedAt());
        return entity;
    }
}
