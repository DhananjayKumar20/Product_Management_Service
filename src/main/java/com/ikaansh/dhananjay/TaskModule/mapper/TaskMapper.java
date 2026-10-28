package com.ikaansh.dhananjay.TaskModule.mapper;

import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.TaskResponseDTO;
import com.ikaansh.dhananjay.TaskModule.entity.Task;
import com.ikaansh.dhananjay.TaskModule.entity.User;

public final class TaskMapper {

    private TaskMapper() {
    }

    public static TaskResponseDTO toResponse(Task task) {
        User owner = task.getOwner();
        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .ownerId(owner.getId())
                .ownerName(owner.getName())
                .ownerEmail(owner.getEmail())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
