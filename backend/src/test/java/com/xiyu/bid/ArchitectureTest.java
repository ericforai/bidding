// Input: ArchUnit framework
// Output: Architecture validation rules
// Pos: Test/架构测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture Tests for XiYu Bid Platform
 *
 * Enforces layered architecture and prevents dependency violations.
 * Run with: mvn test -Dtest=ArchitectureTest
 *
 * Violations will block the build - this is intentional (J4: Reflex).
 *
 * ============= 分阶段实施策略 (Phase C) =============
 * 新模块 (2026-03-04起): 严格遵守所有架构规则
 *   - calendar, collaboration, competitionintel, scoreanalysis
 *   - roi, versionhistory, documenteditor, documents
 *
 * 老模块 (POC阶段): 暂时豁免，后续逐步重构
 *   - auth, tender, project, task, qualification, case, template
 *   - fee, platform, compliance, dashboard, alerts, resources
 *
 * 测试类: 通过@AnalyzeClasses的importOption排除
 * =============
 */
@AnalyzeClasses(
    packages = "com.xiyu.bid",
    importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    /**
     * RULE 1: Controller层不能直接依赖Repository层
     * 必须通过Service层进行数据访问
     * 只检查新模块 (calendar, collaboration, competitionintel, scoreanalysis, roi, versionhistory, documenteditor, documents)
     */
    @ArchTest
    public static final ArchRule new_module_controller_should_not_depend_on_repository =
        noClasses()
            .that().resideInAPackage("com.xiyu.bid.calendar.controller..")
            .or().resideInAPackage("com.xiyu.bid.collaboration.controller..")
            .or().resideInAPackage("com.xiyu.bid.competitionintel.controller..")
            .or().resideInAPackage("com.xiyu.bid.scoreanalysis.controller..")
            .or().resideInAPackage("com.xiyu.bid.roi.controller..")
            .or().resideInAPackage("com.xiyu.bid.versionhistory.controller..")
            .or().resideInAPackage("com.xiyu.bid.documenteditor.controller..")
            .or().resideInAPackage("com.xiyu.bid.documents.controller..")
            .should().dependOnClassesThat()
            .resideInAPackage("..repository..")
            .because("新模块Controller必须通过Service层访问数据");

    /**
     * RULE 1.1: Auth/Tender 控制器不得直接依赖 Repository
     * 作为分阶段整治的首批老模块约束
     */
    @ArchTest
    public static final ArchRule auth_tender_controller_should_not_depend_on_repository =
        noClasses()
            .that().resideInAPackage("com.xiyu.bid.controller..")
            .or().resideInAPackage("com.xiyu.bid.tender.controller..")
            .or().resideInAPackage("com.xiyu.bid.batch.controller..")
            .or().resideInAPackage("com.xiyu.bid.export.controller..")
            .or().resideInAPackage("com.xiyu.bid.bidresult.controller..")
            .should().dependOnClassesThat()
            .resideInAPackage("..repository..")
            .because("首批整治模块控制器必须通过Service层访问数据");

    /**
     * RULE 2: Service层不应依赖上层
     * Service层可以依赖Repository和其他Service的DTO
     * 只检查新模块
     */
    @ArchTest
    public static final ArchRule new_module_service_should_not_depend_on_controller =
        noClasses()
            .that().resideInAPackage("com.xiyu.bid.calendar.service..")
            .or().resideInAPackage("com.xiyu.bid.collaboration.service..")
            .or().resideInAPackage("com.xiyu.bid.competitionintel.service..")
            .or().resideInAPackage("com.xiyu.bid.scoreanalysis.service..")
            .or().resideInAPackage("com.xiyu.bid.roi.service..")
            .or().resideInAPackage("com.xiyu.bid.versionhistory.service..")
            .or().resideInAPackage("com.xiyu.bid.documenteditor.service..")
            .or().resideInAPackage("com.xiyu.bid.documents.service..")
            .should().dependOnClassesThat()
            .resideInAPackage("..controller..")
            .because("新模块Service不应依赖Controller");

    /**
     * RULE 3: Entity不能依赖Service/Controller
     * Entity必须是纯粹的领域模型
     * 所有模块都遵守
     */
    @ArchTest
    public static final ArchRule entities_should_be_independent =
        noClasses()
            .that().resideInAPackage("..entity..")
            .should().dependOnClassesThat()
            .resideInAPackage("..service..")
            .orShould().dependOnClassesThat()
            .resideInAPackage("..controller..")
            .because("Entity必须是纯粹的领域模型");

    /**
     * RULE 4: Controller不能依赖Entity
     * 只检查新模块
     */
    @ArchTest
    public static final ArchRule new_module_controller_should_not_depend_on_entity =
        noClasses()
            .that().resideInAPackage("com.xiyu.bid.calendar.controller..")
            .or().resideInAPackage("com.xiyu.bid.collaboration.controller..")
            .or().resideInAPackage("com.xiyu.bid.competitionintel.controller..")
            .or().resideInAPackage("com.xiyu.bid.scoreanalysis.controller..")
            .or().resideInAPackage("com.xiyu.bid.roi.controller..")
            .or().resideInAPackage("com.xiyu.bid.versionhistory.controller..")
            .or().resideInAPackage("com.xiyu.bid.documenteditor.controller..")
            .or().resideInAPackage("com.xiyu.bid.documents.controller..")
            .should().dependOnClassesThat()
            .resideInAPackage("..entity..")
            .because("新模块Controller应通过DTO返回数据");

    /**
     * RULE 5: DTO不能依赖Service
     * 只检查新模块
     */
    @ArchTest
    public static final ArchRule new_module_dto_should_not_depend_on_service =
        noClasses()
            .that().resideInAPackage("com.xiyu.bid.calendar.dto..")
            .or().resideInAPackage("com.xiyu.bid.collaboration.dto..")
            .or().resideInAPackage("com.xiyu.bid.competitionintel.dto..")
            .or().resideInAPackage("com.xiyu.bid.scoreanalysis.dto..")
            .or().resideInAPackage("com.xiyu.bid.roi.dto..")
            .or().resideInAPackage("com.xiyu.bid.versionhistory.dto..")
            .or().resideInAPackage("com.xiyu.bid.documenteditor.dto..")
            .or().resideInAPackage("com.xiyu.bid.documents.dto..")
            .should().dependOnClassesThat()
            .resideInAPackage("..service..")
            .because("新模块DTO不应依赖Service");

    /**
     * RULE 6: 禁止循环依赖
     * 所有模块都遵守
     */
    @ArchTest
    public static final void no_circular_dependencies(JavaClasses classes) {
        slices().matching("com.xiyu.bid.(*)..")
                .should().beFreeOfCycles()
                .check(classes);
    }

    /**
     * RULE 7: 新模块之间应相互独立
     * 只检查新模块
     */
    @ArchTest
    public static final void new_modules_should_be_independent(JavaClasses classes) {
        slices().matching("com.xiyu.bid.(calendar|collaboration|competitionintel|scoreanalysis|roi|versionhistory|documenteditor|documents)..")
                .should().notDependOnEachOther()
                .check(classes);
    }

    /**
     * RULE 8: Util工具类不能依赖业务逻辑
     * 所有模块都遵守
     */
    @ArchTest
    public static final ArchRule utils_should_not_depend_on_business_logic =
        noClasses()
            .that().haveSimpleNameContaining("Util")
            .or().haveSimpleNameContaining("Helper")
            .should().dependOnClassesThat()
            .resideInAPackage("..service..")
            .orShould().dependOnClassesThat()
            .resideInAPackage("..repository..")
            .because("工具类应该是无状态的纯函数");

    /**
     * RULE 9: Config配置类不能依赖Service
     * 所有模块都遵守
     */
    @ArchTest
    public static final ArchRule config_should_not_depend_on_service =
        noClasses()
            .that().resideInAPackage("..config..")
            .should().dependOnClassesThat()
            .resideInAPackage("..service..")
            .because("配置类不应依赖业务逻辑层");

    /**
     * RULE 10: 禁止在Controller中直接使用JPA EntityManager
     * 只检查新模块
     */
    @ArchTest
    public static final ArchRule new_module_controller_should_not_use_entity_manager =
        noClasses()
            .that().resideInAPackage("com.xiyu.bid.calendar.controller..")
            .or().resideInAPackage("com.xiyu.bid.collaboration.controller..")
            .or().resideInAPackage("com.xiyu.bid.competitionintel.controller..")
            .or().resideInAPackage("com.xiyu.bid.scoreanalysis.controller..")
            .or().resideInAPackage("com.xiyu.bid.roi.controller..")
            .or().resideInAPackage("com.xiyu.bid.versionhistory.controller..")
            .or().resideInAPackage("com.xiyu.bid.documenteditor.controller..")
            .or().resideInAPackage("com.xiyu.bid.documents.controller..")
            .should().dependOnClassesThat()
            .haveSimpleNameContaining("EntityManager")
            .orShould().dependOnClassesThat()
            .haveSimpleNameContaining("SessionFactory")
            .because("新模块Controller必须通过Repository访问数据库");
}
