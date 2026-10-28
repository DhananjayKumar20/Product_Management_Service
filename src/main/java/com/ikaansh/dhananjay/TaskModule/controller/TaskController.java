package com.ikaansh.dhananjay.TaskModule.controller;

import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.MessageResponseDTO;
import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.PageResponseDTO;
import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.TaskRequestDTO;
import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.TaskResponseDTO;
import com.ikaansh.dhananjay.TaskModule.entity.TaskStatus;
import com.ikaansh.dhananjay.TaskModule.entity.User;
import com.ikaansh.dhananjay.TaskModule.service.Interface.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/tasks")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
@Tag(name = "Tasks", description = "Task CRUD - USER manages own tasks, ADMIN can manage any task")
public class TaskController {

    private final TaskService taskService;

//<<------------------------------------Create Task----------------------------------------->>

    @Operation(summary = "Create a task (owner = logged-in user)")
    @PostMapping
    public ResponseEntity<TaskResponseDTO> create(@Valid @RequestBody TaskRequestDTO request,
                                                  @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request, currentUser));
    }

//<<------------------------------------Get All task----------------------------------------->>

    @Operation(summary = "List my tasks (paginated, sortable, optional status filter)")
    @GetMapping
    public ResponseEntity<PageResponseDTO<TaskResponseDTO>> getMyTasks(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "must be >= 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "must be >= 1")
            @Max(value = 100, message = "must be <= 100") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) TaskStatus status,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.getMyTasks(currentUser, status, page, size, sortBy, direction));
    }

//<<------------------------------------Get Task by Id----------------------------------------->>

    @Operation(summary = "Get a task by id (owner or ADMIN)")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> getById(@PathVariable Long id,
                                                   @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.getById(id, currentUser));
    }

//<<------------------------------------Update Task----------------------------------------->>

    @Operation(summary = "Update a task (owner or ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> update(@PathVariable Long id,
                                                  @Valid @RequestBody TaskRequestDTO request,
                                                  @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.update(id, request, currentUser));
    }

//<<------------------------------------Delete Task----------------------------------------->>

    @Operation(summary = "Delete a task (owner or ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponseDTO> delete(@PathVariable Long id,
                                                     @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.delete(id, currentUser));
    }
}
