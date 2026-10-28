package com.ikaansh.dhananjay.TaskModule.service.Implementation;

import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.MessageResponseDTO;
import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.PageResponseDTO;
import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.TaskRequestDTO;
import com.ikaansh.dhananjay.TaskModule.dto.TaskDTO.TaskResponseDTO;
import com.ikaansh.dhananjay.TaskModule.entity.Task;
import com.ikaansh.dhananjay.TaskModule.entity.TaskStatus;
import com.ikaansh.dhananjay.TaskModule.entity.User;
import com.ikaansh.dhananjay.TaskModule.entity.UserType;
import com.ikaansh.dhananjay.TaskModule.event.TaskEvent;
import com.ikaansh.dhananjay.TaskModule.event.TaskEventType;
import com.ikaansh.dhananjay.TaskModule.exception.DuplicateRequestException;
import com.ikaansh.dhananjay.TaskModule.exception.ForbiddenOperationException;
import com.ikaansh.dhananjay.TaskModule.exception.InvalidTaskException;
import com.ikaansh.dhananjay.TaskModule.exception.TaskNotFoundException;
import com.ikaansh.dhananjay.TaskModule.exception.UserNotFoundException;
import com.ikaansh.dhananjay.TaskModule.mapper.TaskMapper;
import com.ikaansh.dhananjay.TaskModule.repository.TaskRepository;
import com.ikaansh.dhananjay.TaskModule.repository.UserRepository;
import com.ikaansh.dhananjay.TaskModule.service.Interface.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class TaskServiceImpl implements TaskService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "title", "status", "dueDate", "createdAt", "updatedAt");

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final long duplicateWindowSeconds;

    public TaskServiceImpl(TaskRepository taskRepository,
                           UserRepository userRepository,
                           ApplicationEventPublisher eventPublisher,
                           @Value("${app.task.duplicate-window-seconds:5}") long duplicateWindowSeconds) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.duplicateWindowSeconds = duplicateWindowSeconds;
    }

// <<------------------------------------ Create ----------------------------------->>

    @Override
    @Transactional
    public TaskResponseDTO create(TaskRequestDTO request, User currentUser) {
        requireUser(currentUser);
        String title = normalizeTitle(request.getTitle());
        String description = normalizeDescription(request.getDescription());
        LocalDate dueDate = requireDueDate(request.getDueDate());

        if (dueDate.isBefore(LocalDate.now())) {
            throw new InvalidTaskException("dueDate cannot be in the past");
        }

        // Pessimistic lock on the owner row: concurrent creates of the same user run one by one,
        // so the duplicate check below cannot be bypassed by a double click.
        User owner = userRepository.findByIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + currentUser.getId()));

        Instant windowStart = Instant.now().minusSeconds(duplicateWindowSeconds);
        if (taskRepository.existsByOwnerIdAndTitleAndDueDateAndCreatedAtAfter(
                owner.getId(), title, dueDate, windowStart)) {
            throw new DuplicateRequestException(
                    "Duplicate request: an identical task was just created. Please wait before retrying.");
        }

        Task task = Task.builder()
                .title(title)
                .description(description)
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO)
                .dueDate(dueDate)
                .owner(owner)
                .build();

        Task saved = taskRepository.saveAndFlush(task);
        publish(TaskEventType.CREATED, saved, currentUser);
        log.info("Task {} created by user {}", saved.getId(), currentUser.getId());
        return TaskMapper.toResponse(saved);
    }

// <<------------------------------------Get by Id Task------------------------------------->>

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDTO getById(Long id, User currentUser) {
        requireUser(currentUser);
        Task task = taskRepository.findWithOwnerById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        assertOwnerOrAdmin(task, currentUser, "view");
        return TaskMapper.toResponse(task);
    }

//<<----------------------------------Get All task--------------------------------------->>

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TaskResponseDTO> getMyTasks(User currentUser, TaskStatus status,
                                                       int page, int size, String sortBy, String direction) {
        requireUser(currentUser);
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        Page<Task> result = status == null
                ? taskRepository.findByOwnerId(currentUser.getId(), pageable)
                : taskRepository.findByOwnerIdAndStatus(currentUser.getId(), status, pageable);
        return PageResponseDTO.from(result.map(TaskMapper::toResponse));
    }

//<<----------------------------------Get All Admin task-------------------------------->>

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TaskResponseDTO> getAllTasks(TaskStatus status,
                                                        int page, int size, String sortBy, String direction) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        Page<Task> result = status == null
                ? taskRepository.findAll(pageable)
                : taskRepository.findByStatus(status, pageable);
        return PageResponseDTO.from(result.map(TaskMapper::toResponse));
    }

// <<------------------------------------Update Task----------------------------------->>

    @Override
    @Transactional
    public TaskResponseDTO update(Long id, TaskRequestDTO request, User currentUser) {
        requireUser(currentUser);
        String title = normalizeTitle(request.getTitle());
        String description = normalizeDescription(request.getDescription());
        LocalDate dueDate = requireDueDate(request.getDueDate());

        // SELECT ... FOR UPDATE - a double click / parallel update waits here until the first commits
        Task task = taskRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        assertOwnerOrAdmin(task, currentUser, "update");

        if (!dueDate.equals(task.getDueDate()) && dueDate.isBefore(LocalDate.now())) {
            throw new InvalidTaskException("dueDate cannot be moved to a past date");
        }
        TaskStatus status = request.getStatus() != null ? request.getStatus() : task.getStatus();
        boolean changed = !Objects.equals(title, task.getTitle())
                || !Objects.equals(description, task.getDescription())
                || status != task.getStatus()
                || !dueDate.equals(task.getDueDate());

        if (!changed) {
            // Second click of the same update: nothing to write, no extra event
            log.debug("Task {} update skipped - no changes", id);
            return TaskMapper.toResponse(task);
        }

        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setDueDate(dueDate);

        Task saved = taskRepository.saveAndFlush(task);
        publish(TaskEventType.UPDATED, saved, currentUser);
        log.info("Task {} updated by user {}", id, currentUser.getId());
        return TaskMapper.toResponse(saved);
    }

// <<------------------------------------Delete Task----------------------------------->>

    @Override
    @Transactional
    public MessageResponseDTO delete(Long id, User currentUser) {
        requireUser(currentUser);
        Task task = taskRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        assertOwnerOrAdmin(task, currentUser, "delete");

        // Build the event before the row disappears
        TaskEvent event = new TaskEvent(this, TaskEventType.DELETED, task.getId(),
                task.getOwner().getId(), currentUser.getId(), task.getTitle(), task.getStatus());
        taskRepository.delete(task);
        taskRepository.flush();
        eventPublisher.publishEvent(event);
        log.info("Task {} deleted by user {}", id, currentUser.getId());
        return new MessageResponseDTO("Task deleted successfully");
    }

// <<------------------------------------Helpers Method---------------------------------->>

    private void requireUser(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new ForbiddenOperationException("Authenticated user is required");
        }
    }

    private boolean isAdmin(User user) {
        return user.getUserType() == UserType.ADMIN;
    }

    private void assertOwnerOrAdmin(Task task, User currentUser, String action) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (!Objects.equals(task.getOwner().getId(), currentUser.getId())) {
            throw new ForbiddenOperationException("You are not allowed to " + action + " this task");
        }
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskException("title is required");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 150) {
            throw new InvalidTaskException("title must not exceed 150 characters");
        }
        return trimmed;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.length() > 2000) {
            throw new InvalidTaskException("description must not exceed 2000 characters");
        }
        return trimmed;
    }

    private LocalDate requireDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new InvalidTaskException("dueDate is required");
        }
        return dueDate;
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new InvalidTaskException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new InvalidTaskException("size must be between 1 and 100");
        }
        String field = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim();
        if (!SORTABLE_FIELDS.contains(field)) {
            throw new InvalidTaskException("Invalid sortBy '" + field + "'. Allowed: " + SORTABLE_FIELDS);
        }
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction == null ? "desc" : direction.trim())
                .orElseThrow(() -> new InvalidTaskException("Invalid direction '" + direction + "'. Allowed: asc, desc"));

        Sort sort = Sort.by(dir, field);
        if (!"id".equals(field)) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        return PageRequest.of(page, size, sort);
    }

    private void publish(TaskEventType type, Task task, User actor) {
        eventPublisher.publishEvent(new TaskEvent(this, type, task.getId(),
                task.getOwner().getId(), actor.getId(), task.getTitle(), task.getStatus()));
    }
}
