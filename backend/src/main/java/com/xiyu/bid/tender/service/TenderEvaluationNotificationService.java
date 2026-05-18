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

@Component
@RequiredArgsConstructor
@Slf4j
public class TenderEvaluationNotificationService {

    private final TaskService taskService;

    /**
     * 评估提交后为投标负责人和项目负责人创建待办。
     */
    public void createEvaluationNotificationTodos(Tender tender) {
        String title = "【评估待审】" + tender.getTitle();
        String description = "标讯「" + tender.getTitle() + "」已完成评估提交，请及时审核并决定是否投标。";

        // 为投标负责人创建待办
        if (tender.getBiddingPersonId() != null) {
            try {
                taskService.createTask(TaskDTO.builder()
                        .projectId(tender.getId())
                        .title(title)
                        .description(description)
                        .status(Task.Status.TODO)
                        .priority(Task.Priority.HIGH)
                        .assigneeId(tender.getBiddingPersonId())
                        .dueDate(LocalDateTime.now().plusDays(3))
                        .build());
            } catch (RuntimeException e) {
                log.warn("Failed to create evaluation todo for biddingPerson {}: {}",
                        tender.getBiddingPersonId(), e.getMessage());
            }
        }

        // 为项目负责人创建待办（与投标负责人不重复时）
        if (tender.getProjectManagerId() != null
                && !tender.getProjectManagerId().equals(tender.getBiddingPersonId())) {
            try {
                taskService.createTask(TaskDTO.builder()
                        .projectId(tender.getId())
                        .title(title)
                        .description(description)
                        .status(Task.Status.TODO)
                        .priority(Task.Priority.HIGH)
                        .assigneeId(tender.getProjectManagerId())
                        .dueDate(LocalDateTime.now().plusDays(3))
                        .build());
            } catch (RuntimeException e) {
                log.warn("Failed to create evaluation todo for projectManager {}: {}",
                        tender.getProjectManagerId(), e.getMessage());
            }
        }
    }
}
