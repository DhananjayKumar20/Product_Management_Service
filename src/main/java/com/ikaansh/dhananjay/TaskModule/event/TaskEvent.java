package com.ikaansh.dhananjay.TaskModule.event;

import com.ikaansh.dhananjay.TaskModule.entity.TaskStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** Plain-value event (no entities) so it is safe to handle on another thread after commit. */
@Getter
public class TaskEvent extends ApplicationEvent {

    private final TaskEventType type;
    private final Long taskId;
    private final Long ownerId;
    private final Long actorId;
    private final String title;
    private final TaskStatus status;

    public TaskEvent(Object source, TaskEventType type, Long taskId, Long ownerId,
                     Long actorId, String title, TaskStatus status) {
        super(source);
        this.type = type;
        this.taskId = taskId;
        this.ownerId = ownerId;
        this.actorId = actorId;
        this.title = title;
        this.status = status;
    }
}
