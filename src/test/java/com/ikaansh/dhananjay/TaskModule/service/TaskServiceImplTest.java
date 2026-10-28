package com.ikaansh.dhananjay.TaskModule.service;

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
import com.ikaansh.dhananjay.TaskModule.repository.TaskRepository;
import com.ikaansh.dhananjay.TaskModule.repository.UserRepository;
import com.ikaansh.dhananjay.TaskModule.service.Implementation.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TaskServiceImpl taskService;

    private User owner;
    private User otherUser;
    private User admin;

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl(taskRepository, userRepository, eventPublisher, 5);
        owner = user(1L, "owner@x.com", UserType.USER);
        otherUser = user(2L, "other@x.com", UserType.USER);
        admin = user(99L, "admin@x.com", UserType.ADMIN);
    }

    private User user(Long id, String email, UserType type) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setName("Name " + id);
        u.setUserType(type);
        return u;
    }

    private Task task(Long id, User taskOwner, String title, LocalDate due) {
        return Task.builder()
                .id(id)
                .title(title)
                .description("desc")
                .status(TaskStatus.TODO)
                .dueDate(due)
                .owner(taskOwner)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
    }

    private TaskRequestDTO request(String title, LocalDate due, TaskStatus status) {
        return TaskRequestDTO.builder().title(title).description("desc").dueDate(due).status(status).build();
    }

    private LocalDate future() {
        return LocalDate.now().plusDays(7);
    }

    // ---------------- create ----------------

    @Test
    void create_success_defaultsToTodo_setsOwner_publishesEvent() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(owner));
        when(taskRepository.existsByOwnerIdAndTitleAndDueDateAndCreatedAtAfter(
                eq(1L), eq("Write report"), eq(future()), any(Instant.class))).thenReturn(false);
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        TaskResponseDTO response = taskService.create(request("  Write report ", future(), null), owner);

        assertEquals(10L, response.getId());
        assertEquals("Write report", response.getTitle());
        assertEquals(TaskStatus.TODO, response.getStatus());
        assertEquals(1L, response.getOwnerId());

        ArgumentCaptor<TaskEvent> captor = ArgumentCaptor.forClass(TaskEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(TaskEventType.CREATED, captor.getValue().getType());
        assertEquals(10L, captor.getValue().getTaskId());
    }

    @Test
    void create_duplicateWithinWindow_throwsDuplicateRequest() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(owner));
        when(taskRepository.existsByOwnerIdAndTitleAndDueDateAndCreatedAtAfter(
                eq(1L), anyString(), any(LocalDate.class), any(Instant.class))).thenReturn(true);

        assertThrows(DuplicateRequestException.class,
                () -> taskService.create(request("Write report", future(), TaskStatus.TODO), owner));

        verify(taskRepository, never()).saveAndFlush(any(Task.class));
        verify(eventPublisher, never()).publishEvent(any(TaskEvent.class));
    }

    @Test
    void create_pastDueDate_throwsInvalidTask() {
        assertThrows(InvalidTaskException.class,
                () -> taskService.create(request("Old", LocalDate.now().minusDays(1), null), owner));

        verify(userRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void create_blankTitle_throwsInvalidTask() {
        assertThrows(InvalidTaskException.class,
                () -> taskService.create(request("   ", future(), null), owner));
    }

    @Test
    void create_missingDueDate_throwsInvalidTask() {
        assertThrows(InvalidTaskException.class,
                () -> taskService.create(request("Title", null, null), owner));
    }

    @Test
    void create_withoutAuthenticatedUser_throwsForbidden() {
        assertThrows(ForbiddenOperationException.class,
                () -> taskService.create(request("Title", future(), null), null));
    }

    // ---------------- read ----------------

    @Test
    void getById_owner_success() {
        when(taskRepository.findWithOwnerById(5L)).thenReturn(Optional.of(task(5L, owner, "T", future())));

        TaskResponseDTO response = taskService.getById(5L, owner);

        assertEquals(5L, response.getId());
        assertEquals("owner@x.com", response.getOwnerEmail());
    }

    @Test
    void getById_admin_canReadAnyTask() {
        when(taskRepository.findWithOwnerById(5L)).thenReturn(Optional.of(task(5L, owner, "T", future())));

        assertEquals(5L, taskService.getById(5L, admin).getId());
    }

    @Test
    void getById_otherUser_throwsForbidden() {
        when(taskRepository.findWithOwnerById(5L)).thenReturn(Optional.of(task(5L, owner, "T", future())));

        assertThrows(ForbiddenOperationException.class, () -> taskService.getById(5L, otherUser));
    }

    @Test
    void getById_missing_throwsNotFound() {
        when(taskRepository.findWithOwnerById(404L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.getById(404L, owner));
    }

    @Test
    void getMyTasks_withoutStatus_usesOwnerQuery() {
        Task t = task(1L, owner, "A", future());
        when(taskRepository.findByOwnerId(eq(1L), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(t), inv.<Pageable>getArgument(1), 1));

        PageResponseDTO<TaskResponseDTO> page = taskService.getMyTasks(owner, null, 0, 10, "title", "asc");

        assertEquals(1, page.getContent().size());
        assertEquals(1L, page.getTotalElements());
        assertTrue(page.isFirst());
        assertTrue(page.isLast());
    }

    @Test
    void getMyTasks_withStatus_usesOwnerAndStatusQuery() {
        when(taskRepository.findByOwnerIdAndStatus(eq(1L), eq(TaskStatus.DONE), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<Task>(List.of(), inv.<Pageable>getArgument(2), 0));

        PageResponseDTO<TaskResponseDTO> page = taskService.getMyTasks(owner, TaskStatus.DONE, 0, 10, "createdAt", "desc");

        assertTrue(page.getContent().isEmpty());
        verify(taskRepository, never()).findByOwnerId(anyLong(), any(Pageable.class));
    }

    @Test
    void getMyTasks_invalidSortField_throwsInvalidTask() {
        assertThrows(InvalidTaskException.class,
                () -> taskService.getMyTasks(owner, null, 0, 10, "password", "asc"));
    }

    @Test
    void getMyTasks_invalidDirection_throwsInvalidTask() {
        assertThrows(InvalidTaskException.class,
                () -> taskService.getMyTasks(owner, null, 0, 10, "title", "sideways"));
    }

    @Test
    void getMyTasks_invalidPageSize_throwsInvalidTask() {
        assertThrows(InvalidTaskException.class,
                () -> taskService.getMyTasks(owner, null, 0, 1000, "title", "asc"));
        assertThrows(InvalidTaskException.class,
                () -> taskService.getMyTasks(owner, null, -1, 10, "title", "asc"));
    }

    @Test
    void getAllTasks_noStatus_usesFindAll() {
        Task t = task(1L, owner, "A", future());
        when(taskRepository.findAll(any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(t), inv.<Pageable>getArgument(0), 1));

        PageResponseDTO<TaskResponseDTO> page = taskService.getAllTasks(null, 0, 5, "dueDate", "desc");

        assertEquals(1, page.getContent().size());
    }

    // ---------------- update ----------------

    @Test
    void update_owner_changesFields_publishesEvent() {
        Task existing = task(5L, owner, "Old", future());
        when(taskRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(existing));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponseDTO response = taskService.update(5L, request("New", future(), TaskStatus.DONE), owner);

        assertEquals("New", response.getTitle());
        assertEquals(TaskStatus.DONE, response.getStatus());
        ArgumentCaptor<TaskEvent> captor = ArgumentCaptor.forClass(TaskEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(TaskEventType.UPDATED, captor.getValue().getType());
    }

    @Test
    void update_noChanges_skipsWriteAndEvent_doubleClickSafe() {
        LocalDate due = future();
        Task existing = task(5L, owner, "Same", due);
        when(taskRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(existing));

        TaskResponseDTO response = taskService.update(5L, request("Same", due, TaskStatus.TODO), owner);

        assertEquals("Same", response.getTitle());
        verify(taskRepository, never()).saveAndFlush(any(Task.class));
        verify(eventPublisher, never()).publishEvent(any(TaskEvent.class));
    }

    @Test
    void update_statusOmitted_keepsExistingStatus() {
        Task existing = task(5L, owner, "Old", future());
        existing.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(existing));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponseDTO response = taskService.update(5L, request("Renamed", future(), null), owner);

        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
    }

    @Test
    void update_admin_canUpdateAnyTask() {
        Task existing = task(5L, owner, "Old", future());
        when(taskRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(existing));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponseDTO response = taskService.update(5L, request("By admin", future(), TaskStatus.DONE), admin);

        assertEquals("By admin", response.getTitle());
        assertEquals(1L, response.getOwnerId());
    }

    @Test
    void update_otherUser_throwsForbidden() {
        when(taskRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(task(5L, owner, "Old", future())));

        assertThrows(ForbiddenOperationException.class,
                () -> taskService.update(5L, request("Hack", future(), null), otherUser));

        verify(taskRepository, never()).saveAndFlush(any(Task.class));
    }

    @Test
    void update_missing_throwsNotFound() {
        when(taskRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,
                () -> taskService.update(404L, request("X", future(), null), owner));
    }

    @Test
    void update_movingDueDateToPast_throwsInvalidTask() {
        when(taskRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(task(5L, owner, "Old", future())));

        assertThrows(InvalidTaskException.class,
                () -> taskService.update(5L, request("Old", LocalDate.now().minusDays(2), null), owner));
    }

    @Test
    void update_keepingAlreadyPastDueDate_isAllowed() {
        LocalDate past = LocalDate.now().minusDays(3);
        Task existing = task(5L, owner, "Overdue", past);
        when(taskRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(existing));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponseDTO response = taskService.update(5L, request("Overdue", past, TaskStatus.DONE), owner);

        assertEquals(TaskStatus.DONE, response.getStatus());
    }

    // ---------------- delete ----------------

    @Test
    void delete_owner_deletesAndPublishesEvent() {
        Task existing = task(5L, owner, "Bye", future());
        when(taskRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(existing));

        MessageResponseDTO response = taskService.delete(5L, owner);

        assertEquals("Task deleted successfully", response.getMessage());
        verify(taskRepository).delete(existing);
        ArgumentCaptor<TaskEvent> captor = ArgumentCaptor.forClass(TaskEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(TaskEventType.DELETED, captor.getValue().getType());
        assertEquals(5L, captor.getValue().getTaskId());
    }

    @Test
    void delete_admin_canDeleteAnyTask() {
        Task existing = task(5L, owner, "Bye", future());
        when(taskRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(existing));

        taskService.delete(5L, admin);

        verify(taskRepository).delete(existing);
    }

    @Test
    void delete_otherUser_throwsForbidden() {
        when(taskRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(task(5L, owner, "Bye", future())));

        assertThrows(ForbiddenOperationException.class, () -> taskService.delete(5L, otherUser));

        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void delete_missing_throwsNotFound_secondClickIsHarmless() {
        when(taskRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.delete(5L, owner));

        verify(taskRepository, never()).delete(any(Task.class));
    }
}
