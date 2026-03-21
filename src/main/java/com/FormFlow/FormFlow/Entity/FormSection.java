package com.FormFlow.FormFlow.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "form_sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sectionTitle;
    private int sectionOrder;

    @ManyToOne
    @JoinColumn(name = "form_id")
    private Form form;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL)
    private List<FormFields> fields;
}