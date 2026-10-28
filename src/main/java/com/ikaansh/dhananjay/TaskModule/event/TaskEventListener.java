package com.ikaansh.dhananjay.TaskModule.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Async consumer. Runs on the "taskEventExecutor" pool only AFTER the DB transaction commits,
 * so a rolled-back create/update/delete never produces an event.
 */
@Slf4j
@Component
public class TaskEventListener {

    @Async("taskEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTaskEvent(TaskEvent event) {
        log.info("[TaskEvent] type={} taskId={} ownerId={} actorId={} title='{}' status={}",
                event.getType(), event.getTaskId(), event.getOwnerId(),
                event.getActorId(), event.getTitle(), event.getStatus());
    }
}
