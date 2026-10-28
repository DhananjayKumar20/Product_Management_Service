package com.ikaansh.dhananjay.TaskModule.dto.TaskDTO;

import com.ikaansh.dhananjay.TaskModule.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDTO {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDate dueDate;
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
