package com.FormFlow.FormFlow.Service;

import com.FormFlow.FormFlow.Entity.FormResponse;
import com.FormFlow.FormFlow.Repository.FormResponseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResponseService {

    private final FormResponseRepository repository;

    public ResponseService(FormResponseRepository repository) {
        this.repository = repository;
    }

    public FormResponse saveResponse(FormResponse response) {
        return repository.save(response);
    }

    public List<FormResponse> getResponses(Long formId) {
        return repository.findByFormId(formId);
    }
}

