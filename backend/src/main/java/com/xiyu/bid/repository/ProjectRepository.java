package com.xiyu.bid.repository;

import com.xiyu.bid.entity.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 项目数据访问接口
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * 根据状态查询项目
     */
    List<Project> findByStatus(Project.Status status);

    /**
     * 根据项目经理ID查询项目
     */
    List<Project> findByManagerId(Long managerId);

    /**
     * 根据标讯ID查询项目
     */
    List<Project> findByTenderId(Long tenderId);

    /**
     * 统计指定状态的项目数量
     */
    Long countByStatus(Project.Status status);

    /**
     * 查询所有活跃项目（非归档状态）
     */
    @Query("SELECT p FROM Project p WHERE p.status <> 'ARCHIVED'")
    List<Project> findActiveProjects();

    /**
     * 获取项目写锁，用于串行化对同一项目的并发写入（例如投标提交）。
     * 必须在 @Transactional 上下文内调用。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdForUpdate(Long id);

    @Query("""
            SELECT DISTINCT p
            FROM Project p
            LEFT JOIN FETCH p.teamMembers tm
            WHERE p.tenderId IN :tenderIds
            """)
    List<Project> findAllWithTeamMembersByTenderIdIn(Collection<Long> tenderIds);

    @Query("""
            SELECT DISTINCT p.id
            FROM Project p
            LEFT JOIN p.teamMembers tm
            WHERE p.managerId = :userId OR tm = :userId
            """)
    List<Long> findAccessibleProjectIdsByUserId(Long userId);

    @Query("SELECT p.id FROM Project p")
    List<Long> findAllProjectIds();

    @Query(value = """
            SELECT DISTINCT p.id
            FROM projects p
            LEFT JOIN users manager_user ON manager_user.id = p.manager_id
            LEFT JOIN project_team_members ptm ON ptm.project_id = p.id
            LEFT JOIN users member_user ON member_user.id = ptm.member_id
            WHERE COALESCE(manager_user.department_code, 'UNASSIGNED') IN (:departmentCodes)
               OR COALESCE(member_user.department_code, 'UNASSIGNED') IN (:departmentCodes)
            """, nativeQuery = true)
    List<Long> findAccessibleProjectIdsByDepartmentCodes(Collection<String> departmentCodes);

    @Query("""
            SELECT COUNT(DISTINCT p.id)
            FROM Project p
            LEFT JOIN p.teamMembers tm
            WHERE p.id = :projectId AND (p.managerId = :userId OR tm = :userId)
            """)
    long countAccessibleProjectByIdAndUserId(Long projectId, Long userId);

    /**
     * 根据项目名称模糊查询
     */
    List<Project> findByNameContainingIgnoreCase(String name);

    /**
     * 查询指定时间范围内开始的项目
     */
    @Query("SELECT p FROM Project p WHERE p.startDate BETWEEN :startDate AND :endDate")
    List<Project> findByStartDateBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
}
