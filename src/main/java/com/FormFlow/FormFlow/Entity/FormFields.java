package com.FormFlow.FormFlow.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "form_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String label;
    private String fieldType;
    private boolean required;
    private int fieldOrder;
    @ManyToOne
    @JoinColumn(name = "section_id")
    private FormSection section;
}
