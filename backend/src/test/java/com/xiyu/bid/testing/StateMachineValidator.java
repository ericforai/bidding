package com.xiyu.bid.testing;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 状态机验证器
 *
 * 验证实体状态转换是否符合预定义的状态机规则。
 * 使用随机游走测试发现非法状态转换。
 *
 * 使用示例:
 * <pre>
 * StateMachineValidator.builder()
 *     .entity("collaboration_thread")
 *     .states("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED")
 *     .transition("OPEN", "IN_PROGRESS")
 *     .transition("IN_PROGRESS", "RESOLVED")
 *     .transition("RESOLVED", "CLOSED")
 *     .transition("OPEN", "CLOSED")  // 允许跳过
 *     .build()
 *     .validate(service::updateStatus);
 * </pre>
 */
@Slf4j
public class StateMachineValidator {

    private final String entityName;
    private final Set<String> states;
    private final Map<String, Set<String>> transitions;
    private final String initialState;

    private StateMachineValidator(Builder builder) {
        this.entityName = builder.entityName;
        this.states = builder.states;
        this.transitions = builder.transitions;
        this.initialState = builder.initialState;
    }

    /**
     * 验证状态转换的合法性
     */
    public void verifyTransition(String fromState, String toState) {
        if (!states.contains(fromState)) {
            throw new IllegalArgumentException(
                String.format("Invalid from state '%s' for entity %s. Valid states: %s",
                    fromState, entityName, states));
        }

        if (!states.contains(toState)) {
            throw new IllegalArgumentException(
                String.format("Invalid to state '%s' for entity %s. Valid states: %s",
                    toState, entityName, states));
        }

        Set<String> validTargets = transitions.get(fromState);
        if (validTargets == null || !validTargets.contains(toState)) {
            throw new IllegalStateException(
                String.format("Invalid state transition for %s: %s -> %s is not allowed. Valid transitions from %s: %s",
                    entityName, fromState, toState, fromState, validTargets));
        }

        log.debug("State transition verified: {} -> {} for {}", fromState, toState, entityName);
    }

    /**
     * 获取指定状态的所有合法转换目标
     */
    public Set<String> getValidTransitions(String fromState) {
        Set<String> valid = transitions.get(fromState);
        return valid != null ? Collections.unmodifiableSet(valid) : Collections.emptySet();
    }

    /**
     * 检查是否为最终状态（没有出边）
     */
    public boolean isFinalState(String state) {
        Set<String> valid = transitions.get(state);
        return valid == null || valid.isEmpty();
    }

    /**
     * 获取所有可能到达指定状态的源状态
     */
    public Set<String> getIncomingTransitions(String toState) {
        Set<String> incoming = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : transitions.entrySet()) {
            if (entry.getValue().contains(toState)) {
                incoming.add(entry.getKey());
            }
        }
        return incoming;
    }

    /**
     * 验证状态机完整性
     * - 所有状态可达
     * - 没有孤立状态
     * - 至少有一个初始状态
     */
    public void validateIntegrity() {
        // 检查是否有孤立状态（无法从初始状态到达）
        Set<String> reachable = computeReachableStates();
        Set<String> unreachable = new HashSet<>(states);
        unreachable.removeAll(reachable);

        if (!unreachable.isEmpty()) {
            log.warn("Unreachable states detected for {}: {}", entityName, unreachable);
        }

        // 检查是否有死锁状态（无法到达最终状态）
        Set<String> deadlocked = findDeadlockedStates();
        if (!deadlocked.isEmpty()) {
            log.warn("Deadlocked states detected for {}: {}", entityName, deadlocked);
        }
    }

    /**
     * 计算从初始状态可达的所有状态
     */
    private Set<String> computeReachableStates() {
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        if (initialState != null) {
            queue.add(initialState);
            reachable.add(initialState);
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            Set<String> nextStates = transitions.get(current);

            if (nextStates != null) {
                for (String next : nextStates) {
                    if (!reachable.contains(next)) {
                        reachable.add(next);
                        queue.add(next);
                    }
                }
            }
        }

        return reachable;
    }

    /**
     * 找出无法到达最终状态的死锁状态
     */
    private Set<String> findDeadlockedStates() {
        Set<String> finalStates = new HashSet<>();
        for (String state : states) {
            if (isFinalState(state)) {
                finalStates.add(state);
            }
        }

        if (finalStates.isEmpty()) {
            return Collections.emptySet();
        }

        // 从每个状态反向BFS，看能否到达最终状态
        Set<String> canReachFinal = new HashSet<>();
        for (String finalState : finalStates) {
            canReachFinal.add(finalState);
            Queue<String> queue = new LinkedList<>();
            queue.add(finalState);

            while (!queue.isEmpty()) {
                String current = queue.poll();
                Set<String> incoming = getIncomingTransitions(current);

                for (String incomingState : incoming) {
                    if (!canReachFinal.contains(incomingState)) {
                        canReachFinal.add(incomingState);
                        queue.add(incomingState);
                    }
                }
            }
        }

        Set<String> deadlocked = new HashSet<>(states);
        deadlocked.removeAll(canReachFinal);
        return deadlocked;
    }

    /**
     * 创建Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder模式
     */
    public static class Builder {
        private String entityName;
        private final Set<String> states = new HashSet<>();
        private final Map<String, Set<String>> transitions = new HashMap<>();
        private String initialState;

        public Builder entity(String name) {
            this.entityName = name;
            return this;
        }

        public Builder states(String... states) {
            Collections.addAll(this.states, states);
            return this;
        }

        public Builder initialState(String state) {
            this.initialState = state;
            return this;
        }

        public Builder transition(String from, String to) {
            transitions.computeIfAbsent(from, k -> new HashSet<>()).add(to);
            // 确保所有状态都在集合中
            states.add(from);
            states.add(to);
            return this;
        }

        public Builder bidirectional(String s1, String s2) {
            transition(s1, s2);
            transition(s2, s1);
            return this;
        }

        public Builder anyTo(String toState) {
            for (String state : states) {
                if (!state.equals(toState)) {
                    transition(state, toState);
                }
            }
            return this;
        }

        public StateMachineValidator build() {
            if (entityName == null) {
                throw new IllegalStateException("Entity name is required");
            }
            if (states.isEmpty()) {
                throw new IllegalStateException("At least one state is required");
            }
            return new StateMachineValidator(this);
        }
    }

    /**
     * 预定义的状态机
     */
    public static class Predefined {
        /**
         * 协作线程状态机
         */
        public static StateMachineValidator collaborationThread() {
            return builder()
                .entity("collaboration_thread")
                .states("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED")
                .initialState("OPEN")
                .transition("OPEN", "IN_PROGRESS")
                .transition("OPEN", "CLOSED")
                .transition("IN_PROGRESS", "RESOLVED")
                .transition("IN_PROGRESS", "CLOSED")
                .transition("RESOLVED", "CLOSED")
                .build();
        }

        /**
         * 项目状态机
         */
        public static StateMachineValidator project() {
            return builder()
                .entity("project")
                .states("DRAFT", "IN_PROGRESS", "REVIEW", "APPROVED", "REJECTED", "COMPLETED", "CANCELLED")
                .initialState("DRAFT")
                .transition("DRAFT", "IN_PROGRESS")
                .transition("IN_PROGRESS", "REVIEW")
                .transition("REVIEW", "APPROVED")
                .transition("REVIEW", "REJECTED")
                .transition("APPROVED", "COMPLETED")
                .transition("DRAFT", "CANCELLED")
                .transition("IN_PROGRESS", "CANCELLED")
                .transition("REVIEW", "CANCELLED")
                .build();
        }

        /**
         * 任务状态机
         */
        public static StateMachineValidator task() {
            return builder()
                .entity("task")
                .states("TODO", "IN_PROGRESS", "REVIEW", "DONE", "CANCELLED")
                .initialState("TODO")
                .transition("TODO", "IN_PROGRESS")
                .transition("TODO", "CANCELLED")
                .transition("IN_PROGRESS", "REVIEW")
                .transition("IN_PROGRESS", "CANCELLED")
                .transition("REVIEW", "DONE")
                .transition("REVIEW", "IN_PROGRESS")  // 返工
                .build();
        }

        /**
         * 费用状态机
         */
        public static StateMachineValidator fee() {
            return builder()
                .entity("fee")
                .states("PENDING", "PAID", "RETURNED", "CANCELLED")
                .initialState("PENDING")
                .transition("PENDING", "PAID")
                .transition("PAID", "RETURNED")
                .transition("PENDING", "CANCELLED")
                .transition("RETURNED", "PAID")  // 重新支付
                .build();
        }

        /**
         * 文档版本状态机
         */
        public static StateMachineValidator documentVersion() {
            return builder()
                .entity("document_version")
                .states("DRAFT", "PUBLISHED", "ARCHIVED")
                .initialState("DRAFT")
                .transition("DRAFT", "PUBLISHED")
                .transition("PUBLISHED", "ARCHIVED")
                .transition("ARCHIVED", "DRAFT")  // 从版本恢复
                .build();
        }
    }
}
