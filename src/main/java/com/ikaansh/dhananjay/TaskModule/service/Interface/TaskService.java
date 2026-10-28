package com.ikaansh.dhananjay.TaskModule.service.Interface;

import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.MessageResponseDTO;
import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.PageResponseDTO;
import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.TaskRequestDTO;
import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.TaskResponseDTO;
import com.ikaansh.dhananjay.TaskModule.entity.TaskStatus;
import com.ikaansh.dhananjay.TaskModule.entity.User;

public interface TaskService {

    TaskResponseDTO create(TaskRequestDTO request, User currentUser);

    TaskResponseDTO getById(Long id, User currentUser);

    /** Tasks owned by the current user (USER and ADMIN). */
    PageResponseDTO<TaskResponseDTO> getMyTasks(User currentUser, TaskStatus status,
                                                int page, int size, String sortBy, String direction);

    /** All tasks of all users (ADMIN only - enforced in controller + security config). */
    PageResponseDTO<TaskResponseDTO> getAllTasks(TaskStatus status,
                                                 int page, int size, String sortBy, String direction);

    TaskResponseDTO update(Long id, TaskRequestDTO request, User currentUser);

    MessageResponseDTO delete(Long id, User currentUser);
}
