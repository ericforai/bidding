package com.xiyu.bid.security;

import com.xiyu.bid.security.service.ProjectMemberService;
import com.xiyu.bid.service.ProjectAccessScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectMemberService projectMemberService;

    @MockBean
    private ProjectAccessScopeService projectAccessScopeService;

    @Test
    @WithMockUser(roles = "MANAGER")
    void addMember_ShouldReturnSuccess() throws Exception {
        when(projectAccessScopeService.currentUserHasAdminAccess()).thenReturn(true);

        mockMvc.perform(post("/api/projects/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 201,
                                  "memberRole": "TECHNICAL_EXPERT",
                                  "permissionLevel": "EDITOR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getMembers_ShouldReturnList() throws Exception {
        when(projectAccessScopeService.currentUserHasAdminAccess()).thenReturn(true);

        mockMvc.perform(get("/api/projects/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void removeMember_ShouldReturnSuccess() throws Exception {
        when(projectAccessScopeService.currentUserHasAdminAccess()).thenReturn(true);

        mockMvc.perform(delete("/api/projects/1/members/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
