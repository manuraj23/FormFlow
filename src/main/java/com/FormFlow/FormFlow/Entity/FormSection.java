package com.FormFlow.FormFlow.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "form_sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String sectionTitle;
    private int sectionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id")
    @JsonIgnore
    private Form form;

    @Column(precision = 5, scale = 2)
    private BigDecimal positiveMarks;

    @Column(precision = 5, scale = 2)
    private BigDecimal negativeMarks;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("fieldOrder ASC")
    @JsonIgnoreProperties("section")
    private List<FormFields> fields;
}