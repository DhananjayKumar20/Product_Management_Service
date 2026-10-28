package com.ikaansh.dhananjay.TaskModule.controller;

import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.PageResponseDTO;
import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.TaskResponseDTO;
import com.ikaansh.dhananjay.TaskModule.entity.TaskStatus;
import com.ikaansh.dhananjay.TaskModule.service.Interface.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/admin/tasks")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Tasks", description = "ADMIN only endpoints")
public class AdminTaskController {

    private final TaskService taskService;

//<<------------------------------------Get All Admin task----------------------------------------->>

    @Operation(summary = "List tasks of ALL users (ADMIN only)")
    @GetMapping
    public ResponseEntity<PageResponseDTO<TaskResponseDTO>> getAllTasks(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "must be >= 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "must be >= 1")
            @Max(value = 100, message = "must be <= 100") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) TaskStatus status) {
        return ResponseEntity.ok(taskService.getAllTasks(status, page, size, sortBy, direction));
    }
}
