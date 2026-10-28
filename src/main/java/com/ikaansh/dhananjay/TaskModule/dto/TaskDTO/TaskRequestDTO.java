package com.ikaansh.dhananjay.TaskModule.dto.TaskDTO;

import com.ikaansh.dhananjay.TaskModule.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDTO {

    @NotBlank(message = "title is required")
    @Size(max = 150, message = "title must not exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "description must not exceed 2000 characters")
    private String description;

    /** Optional. Defaults to TODO on create, unchanged on update when omitted. */
    private TaskStatus status;

    @NotNull(message = "dueDate is required (yyyy-MM-dd)")
    private LocalDate dueDate;
}
