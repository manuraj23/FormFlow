package com.FormFlow.FormFlow.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "form_timer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormTimer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    private Integer duration;

    private LocalDateTime startTime;
}