package com.ikaansh.dhananjay.TaskModule.repository;

import com.ikaansh.dhananjay.TaskModule.entity.Task;
import com.ikaansh.dhananjay.TaskModule.entity.TaskStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Row lock for update / delete (SELECT ... FOR UPDATE) - owner stays lazy so only the task row is locked. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Task t where t.id = :id")
    Optional<Task> findByIdForUpdate(@Param("id") Long id);

    /** Single query with owner joined - no lazy load afterwards. */
    @EntityGraph(attributePaths = "owner")
    Optional<Task> findWithOwnerById(Long id);

    // ---- list queries: owner is fetched in the same SQL (no N+1) ----

    @EntityGraph(attributePaths = "owner")
    Page<Task> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Task> findByOwnerIdAndStatus(Long ownerId, TaskStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Task> findAll(Pageable pageable);

    /** Double click detection: same owner + title + due date created inside the window. */
    boolean existsByOwnerIdAndTitleAndDueDateAndCreatedAtAfter(
            Long ownerId, String title, LocalDate dueDate, Instant createdAfter);
}
