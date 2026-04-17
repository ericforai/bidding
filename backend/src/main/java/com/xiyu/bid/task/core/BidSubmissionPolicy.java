package com.xiyu.bid.task.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure core policy for bid-document submission validation.
 * No state, no dependencies, no side effects.
 *
 * <p>Validation rules (applied in order, accumulating gaps):
 * <ol start="0">
 *   <li>Rule 0: The project must have at least one task.</li>
 *   <li>Rule 1: All tasks must be completed (no incomplete tasks).</li>
 *   <li>Rule 2: At least one task must have an associated deliverable
 *       (only enforced when the project has tasks).</li>
 * </ol>
 */
public final class BidSubmissionPolicy {

    /** Default minimum deliverables per task. */
    private static final int DEFAULT_MIN_PER_TASK = 1;

    private BidSubmissionPolicy() {
    }

    /**
     * Validate whether a project can be submitted to bid document process.
     *
     * @param totalTasks           total number of tasks
     * @param completedTasks       count of COMPLETED tasks
     * @param tasksWithDeliverables count of tasks with >=1 deliverable
     * @param minPerTask           minimum deliverables per task
     * @return submission validation result with gap details
     */
    public static SubmissionValidationResult validateSubmission(
            final int totalTasks,
            final int completedTasks,
            final int tasksWithDeliverables,
            final int minPerTask) {

        List<TaskGap> gaps = new ArrayList<>();
        int effectiveMin = minPerTask > 0
                ? minPerTask : DEFAULT_MIN_PER_TASK;

        // Rule 0: Must have at least one task
        if (totalTasks <= 0) {
            gaps.add(new TaskGap(null, null, "项目没有任何任务，无法提交"));
        }

        // Rule 1: All tasks must be completed
        int incomplete = totalTasks - completedTasks;
        if (incomplete > 0) {
            gaps.add(new TaskGap(null, null,
                    "有 " + incomplete + " 个任务未完成"));
        }

        // Rule 2: At least one task must have deliverables
        if (tasksWithDeliverables == 0 && totalTasks > 0) {
            gaps.add(new TaskGap(null, null,
                    "没有任何任务关联交付物"));
        }

        if (!gaps.isEmpty()) {
            return new SubmissionValidationResult(false,
                    "提交失败: " + gaps.size() + " 项校验未通过",
                    gaps);
        }

        return new SubmissionValidationResult(true, "", List.of());
    }

    /**
     * Result of a submission validation.
     *
     * @param submittable whether project can be submitted
     * @param reason      human-readable summary
     * @param gaps        list of individual gaps if any
     */
    public record SubmissionValidationResult(
            boolean submittable,
            String reason,
            List<TaskGap> gaps) {
    }

    /**
     * Describes a single gap preventing submission.
     *
     * @param taskId      task id (null for aggregate gaps)
     * @param taskName    task display name
     * @param description human-readable gap description
     */
    public record TaskGap(
            Long taskId, String taskName, String description) {
    }
}
