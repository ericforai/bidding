package com.xiyu.bid.resources.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.resources.dto.ExpenseApproveRequest;
import com.xiyu.bid.resources.dto.ExpenseReturnActionRequest;
import com.xiyu.bid.resources.entity.Expense;
import com.xiyu.bid.resources.entity.ExpenseApprovalRecord;
import com.xiyu.bid.resources.repository.ExpenseApprovalRecordRepository;
import com.xiyu.bid.resources.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class ExpenseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseApprovalRecordRepository approvalRecordRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private Expense guaranteeExpense;
    private Expense normalExpense;
    private Expense paidExpense;
    private Project projectNorth;
    private Project projectSouth;

    @TestConfiguration
    static class TestBeans {
        @Bean(name = "passwordEncryptionUtil")
        @Primary
        PasswordEncryptionUtil passwordEncryptionUtil() {
            return new PasswordEncryptionUtil() {
                @Override
                public void initialize() {
                }

                @Override
                public String encrypt(String plainPassword) {
                    return plainPassword;
                }

                @Override
                public String decrypt(String encryptedPassword) {
                    return encryptedPassword;
                }

                @Override
                public boolean isKeyValid() {
                    return true;
                }
            };
        }
    }

    @BeforeEach
    void setUp() {
        approvalRecordRepository.deleteAll();
        expenseRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        User northManager = userRepository.save(User.builder()
                .username("north-manager")
                .password("pwd")
                .email("north@example.com")
                .fullName("North Manager")
                .role(User.Role.MANAGER)
                .departmentCode("D-NORTH")
                .departmentName("华北事业部")
                .build());

        User southManager = userRepository.save(User.builder()
                .username("south-manager")
                .password("pwd")
                .email("south@example.com")
                .fullName("South Manager")
                .role(User.Role.MANAGER)
                .departmentCode("D-SOUTH")
                .departmentName("华南事业部")
                .build());

        projectNorth = projectRepository.save(Project.builder()
                .name("华北电网项目")
                .tenderId(9001L)
                .managerId(northManager.getId())
                .status(Project.Status.BIDDING)
                .build());

        projectSouth = projectRepository.save(Project.builder()
                .name("华南轨交项目")
                .tenderId(9002L)
                .managerId(southManager.getId())
                .status(Project.Status.PREPARING)
                .build());

        guaranteeExpense = expenseRepository.save(Expense.builder()
                .projectId(projectNorth.getId())
                .category(Expense.ExpenseCategory.OTHER)
                .expenseType("保证金")
                .amount(new BigDecimal("120000.00"))
                .date(LocalDate.now().minusDays(2))
                .description("投标保证金")
                .createdBy("creator")
                .status(Expense.ExpenseStatus.PENDING_APPROVAL)
                .build());

        normalExpense = expenseRepository.save(Expense.builder()
                .projectId(projectSouth.getId())
                .category(Expense.ExpenseCategory.MATERIAL)
                .expenseType("材料费")
                .amount(new BigDecimal("3000.00"))
                .date(LocalDate.now().minusDays(1))
                .description("材料采购")
                .createdBy("creator")
                .status(Expense.ExpenseStatus.PENDING_APPROVAL)
                .build());

        expenseRepository.save(Expense.builder()
                .projectId(projectNorth.getId())
                .category(Expense.ExpenseCategory.TRANSPORTATION)
                .expenseType("差旅费")
                .amount(new BigDecimal("5600.00"))
                .date(LocalDate.now().minusDays(10))
                .description("现场踏勘")
                .createdBy("creator")
                .status(Expense.ExpenseStatus.RETURNED)
                .build());

        paidExpense = expenseRepository.save(Expense.builder()
                .projectId(projectSouth.getId())
                .category(Expense.ExpenseCategory.LABOR)
                .expenseType("人工费")
                .amount(new BigDecimal("8800.00"))
                .date(LocalDate.now().minusDays(3))
                .description("驻场支持")
                .createdBy("creator")
                .status(Expense.ExpenseStatus.PAID)
                .build());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void approveExpense_ShouldPersistApprovedStateAndHistory() throws Exception {
        ExpenseApproveRequest request = new ExpenseApproveRequest();
        request.setResult(ExpenseApproveRequest.ApprovalResult.APPROVED);
        request.setApprover("manager");
        request.setComment("审批通过");

        mockMvc.perform(post("/api/resources/expenses/{id}/approve", guaranteeExpense.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedBy").value("manager"))
                .andExpect(jsonPath("$.data.approvalComment").value("审批通过"));

        Expense refreshed = expenseRepository.findById(guaranteeExpense.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(Expense.ExpenseStatus.APPROVED);
        assertThat(refreshed.getApprovedBy()).isEqualTo("manager");
        assertThat(refreshed.getApprovedAt()).isNotNull();

        List<ExpenseApprovalRecord> records =
                approvalRecordRepository.findByExpenseIdOrderByActedAtDesc(guaranteeExpense.getId());
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getResult()).isEqualTo(ExpenseApprovalRecord.ApprovalResult.APPROVED);
        assertThat(records.get(0).getApprover()).isEqualTo("manager");
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void guaranteeExpenseReturnFlow_ShouldTransitionToReturned() throws Exception {
        approveGuaranteeExpense();

        ExpenseReturnActionRequest requestReturn = new ExpenseReturnActionRequest();
        requestReturn.setActor("cashier");
        requestReturn.setComment("申请退还保证金");

        mockMvc.perform(post("/api/resources/expenses/{id}/return-request", guaranteeExpense.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReturn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RETURN_REQUESTED"))
                .andExpect(jsonPath("$.data.returnComment").value("申请退还保证金"));

        Expense afterRequest = expenseRepository.findById(guaranteeExpense.getId()).orElseThrow();
        assertThat(afterRequest.getStatus()).isEqualTo(Expense.ExpenseStatus.RETURN_REQUESTED);
        assertThat(afterRequest.getReturnRequestedAt()).isNotNull();

        ExpenseReturnActionRequest confirmReturn = new ExpenseReturnActionRequest();
        confirmReturn.setActor("manager");
        confirmReturn.setComment("确认已到账");

        mockMvc.perform(post("/api/resources/expenses/{id}/confirm-return", guaranteeExpense.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReturn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RETURNED"))
                .andExpect(jsonPath("$.data.returnComment").value("确认已到账"));

        Expense afterConfirm = expenseRepository.findById(guaranteeExpense.getId()).orElseThrow();
        assertThat(afterConfirm.getStatus()).isEqualTo(Expense.ExpenseStatus.RETURNED);
        assertThat(afterConfirm.getReturnConfirmedAt()).isNotNull();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void nonGuaranteeExpenseReturnRequest_ShouldFail() throws Exception {
        ExpenseReturnActionRequest request = new ExpenseReturnActionRequest();
        request.setActor("cashier");
        request.setComment("尝试退还普通费用");

        mockMvc.perform(post("/api/resources/expenses/{id}/return-request", normalExpense.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("Only deposit-like expenses can enter return flow"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void confirmReturnWithoutRequestState_ShouldFailWithConflict() throws Exception {
        ExpenseReturnActionRequest request = new ExpenseReturnActionRequest();
        request.setActor("manager");
        request.setComment("待审批状态直接确认退还");

        mockMvc.perform(post("/api/resources/expenses/{id}/confirm-return", guaranteeExpense.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("Expense is not awaiting return confirmation"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void missingExpense_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/resources/expenses/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getApprovalRecords_ShouldReturnProjectHistory() throws Exception {
        approveGuaranteeExpense();

        mockMvc.perform(get("/api/resources/expenses/approval-records")
                        .param("projectId", guaranteeExpense.getProjectId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].expenseId").value(guaranteeExpense.getId().intValue()))
                .andExpect(jsonPath("$.data[0].approver").value("manager"))
                .andExpect(jsonPath("$.data[0].result").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getExpenseLedger_ShouldFilterByDepartmentAndReturnSummary() throws Exception {
        mockMvc.perform(get("/api/resources/expenses/ledger")
                        .param("department", "华北"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.summary.recordCount").value(2))
                .andExpect(jsonPath("$.data.summary.depositCount").value(1))
                .andExpect(jsonPath("$.data.summary.byDepartment[0].label").value("华北事业部"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getExpenseLedger_ShouldFilterByProjectAndDateRange() throws Exception {
        mockMvc.perform(get("/api/resources/expenses/ledger")
                        .param("projectId", projectSouth.getId().toString())
                        .param("startDate", LocalDate.now().minusDays(3).toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].projectName").value("华南轨交项目"))
                .andExpect(jsonPath("$.data.summary.recordCount").value(2));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getExpenseLedger_ShouldExposeMultiDimensionalSummary() throws Exception {
        mockMvc.perform(get("/api/resources/expenses/ledger")
                        .param("projectId", projectNorth.getId().toString())
                        .param("projectKeyword", "电网")
                        .param("department", "D-NORTH")
                        .param("startDate", LocalDate.now().minusDays(15).toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].departmentCode").value("D-NORTH"))
                .andExpect(jsonPath("$.data.items[0].departmentName").value("华北事业部"))
                .andExpect(jsonPath("$.data.summary.recordCount").value(2))
                .andExpect(jsonPath("$.data.summary.totalAmount").value(125600.00))
                .andExpect(jsonPath("$.data.summary.pendingApprovalAmount").value(120000.00))
                .andExpect(jsonPath("$.data.summary.paidAmount").value(0))
                .andExpect(jsonPath("$.data.summary.returnRequestedAmount").value(0))
                .andExpect(jsonPath("$.data.summary.returnedAmount").value(5600.00))
                .andExpect(jsonPath("$.data.summary.byDepartment", hasSize(1)))
                .andExpect(jsonPath("$.data.summary.byDepartment[0].key").value("D-NORTH"))
                .andExpect(jsonPath("$.data.summary.byProject", hasSize(1)))
                .andExpect(jsonPath("$.data.summary.byProject[0].key").value(String.valueOf(projectNorth.getId())))
                .andExpect(jsonPath("$.data.summary.byExpenseType", hasSize(2)))
                .andExpect(jsonPath("$.data.summary.byExpenseType[0].key").value("保证金"))
                .andExpect(jsonPath("$.data.summary.byStatus", hasSize(2)))
                .andExpect(jsonPath("$.data.summary.byStatus[0].count", is(1)));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getExpenseLedger_ShouldFilterByExpenseTypeAndStatus() throws Exception {
        mockMvc.perform(get("/api/resources/expenses/ledger")
                        .param("expenseType", "人工费")
                        .param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id").value(paidExpense.getId().intValue()))
                .andExpect(jsonPath("$.data.summary.recordCount").value(1))
                .andExpect(jsonPath("$.data.summary.paidAmount").value(8800.00))
                .andExpect(jsonPath("$.data.summary.byExpenseType[0].label").value("人工费"))
                .andExpect(jsonPath("$.data.summary.byStatus[0].key").value("PAID"));
    }

    private void approveGuaranteeExpense() throws Exception {
        ExpenseApproveRequest request = new ExpenseApproveRequest();
        request.setResult(ExpenseApproveRequest.ApprovalResult.APPROVED);
        request.setApprover("manager");
        request.setComment("进入退还流程前先审批");

        mockMvc.perform(post("/api/resources/expenses/{id}/approve", guaranteeExpense.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
