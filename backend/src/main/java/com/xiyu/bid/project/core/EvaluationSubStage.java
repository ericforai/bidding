// Input: evaluation sub-stage code
// Output: 3-step linear sub-FSM inside EVALUATING (PRD §3.3)
// Pos: project/core/ - pure enum
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.core;

/**
 * 评标阶段子状态（仅在 ProjectStage.EVALUATING 内部线性切换）。
 */
public enum EvaluationSubStage {
    IN_PROGRESS,     // 评标进行中
    AWAITING_BOARD,  // 等待评标结果公示
    ANNOUNCED        // 已公示
}
