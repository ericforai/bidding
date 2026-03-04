package com.xiyu.bid.repository;

import com.xiyu.bid.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

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
    @Query("SELECT p FROM Project p WHERE p.status != com.xiyu.bid.entity.Project.Status.ARCHIVED")
    List<Project> findActiveProjects();

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
