// Input: Tender entity and evaluator User
// Output: TODO task creation for biddingPerson and projectManager
// Pos: Service/标讯评估通知外壳
// 维护声明: REQ-BC-010 通知逻辑统一在此，TenderEvaluationSubmissionService.submit() 委托本类执行.

package com.xiyu.bid.tender.service;

import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.task.dto.TaskDTO;
import com.xiyu.bid.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 标讯评估提交后通知服务（REQ-BC-010）。
 * <p>职责：为投标负责人和项目负责人创建"评估待审"待办。
 * <p>待办创建失败时记录错误日志，不阻塞主流程。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenderEvaluationNotificationService {

    private static final String TASK_TITLE_PREFIX = "【评估待审】";
    private static final String TASK_DESCRIPTION_TEMPLATE = "标讯「%s」已完成评估提交，请及时审核并决定是否投标。";
    private static final int TASK_DUE_DAYS = 3;

    private final TaskService taskService;

    /**
     * 评估提交后为投标负责人和项目负责人创建待办。
     * <p>创建失败时记录错误日志，不抛出异常，确保主流程不受影响。
     */
    public void createEvaluationNotificationTodos(Tender tender) {
        String title = TASK_TITLE_PREFIX + tender.getTitle();
        String description = String.format(TASK_DESCRIPTION_TEMPLATE, tender.getTitle());
        LocalDateTime dueDate = LocalDateTime.now().plusDays(TASK_DUE_DAYS);

        if (tender.getBiddingPersonId() != null) {
            createTodoSafely(tender, title, description, dueDate,
                    tender.getBiddingPersonId(), "biddingPerson");
        }

        if (tender.getProjectManagerId() != null
                && !tender.getProjectManagerId().equals(tender.getBiddingPersonId())) {
            createTodoSafely(tender, title, description, dueDate,
                    tender.getProjectManagerId(), "projectManager");
        }
    }

    private void createTodoSafely(Tender tender, String title, String description,
                                  LocalDateTime dueDate, Long assigneeId, String role) {
        try {
            taskService.createTask(TaskDTO.builder()
                    .projectId(tender.getId())
                    .title(title)
                    .description(description)
                    .status(Task.Status.TODO)
                    .priority(Task.Priority.HIGH)
                    .assigneeId(assigneeId)
                    .dueDate(dueDate)
                    .build());
        } catch (RuntimeException e) {
            log.error("Failed to create evaluation todo for {} (tenderId={}, assigneeId={}): {}",
                    role, tender.getId(), assigneeId, e.getMessage());
        }
    }
}
