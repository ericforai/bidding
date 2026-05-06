-- Input: migration/V11__create_approval_tables.sql
-- Output: rollback script for postgres environments; review data-loss comments before production use.
-- Pos: Flyway historical down migration coverage for 西域数智化投标管理平台.
-- 维护声明: source migration changes must update this rollback script in the same branch.

-- Manual rollback required for statement: COMMENT ON COLUMN approval_actions.action_type IS '操作类型: SUBMIT, APPROVE, REJECT, CANCEL'
-- Manual rollback required for statement: COMMENT ON COLUMN approval_actions.approval_request_id IS '关联的审批请求ID'
-- Manual rollback required for statement: COMMENT ON TABLE approval_actions IS '审批操作记录表'
-- Manual rollback required for statement: COMMENT ON COLUMN approval_requests.priority IS '优先级: 0-普通, 1-紧急, 2-非常紧急'
-- Manual rollback required for statement: COMMENT ON COLUMN approval_requests.status IS '审批状态: PENDING, APPROVED, REJECTED, CANCELLED'
-- Manual rollback required for statement: COMMENT ON COLUMN approval_requests.approval_type IS '审批类型'
-- Manual rollback required for statement: COMMENT ON COLUMN approval_requests.project_id IS '关联的项目ID'
-- Manual rollback required for statement: COMMENT ON COLUMN approval_requests.id IS '唯一标识'
-- Manual rollback required for statement: COMMENT ON TABLE approval_requests IS '审批请求表'
DROP INDEX IF EXISTS idx_aa_actor_id;
DROP INDEX IF EXISTS idx_aa_action_time;
DROP INDEX IF EXISTS idx_aa_approval_request_id;
DROP INDEX IF EXISTS idx_ar_current_approver;
DROP INDEX IF EXISTS idx_ar_approval_type;
DROP INDEX IF EXISTS idx_ar_created_at;
DROP INDEX IF EXISTS idx_ar_requester_id;
DROP INDEX IF EXISTS idx_ar_status;
DROP INDEX IF EXISTS idx_ar_project_id;
DROP TABLE IF EXISTS approval_actions;
DROP TABLE IF EXISTS approval_requests;
