package com.xiyu.bid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeConfigResponse {

    @Builder.Default
    private List<UserDataScopeItem> userDataScope = new ArrayList<>();

    @Builder.Default
    private List<DepartmentDataScopeItem> deptDataScope = new ArrayList<>();

    @Builder.Default
    private List<DepartmentOptionItem> deptOptions = new ArrayList<>();

    @Builder.Default
    private List<DepartmentTreeItem> deptTree = new ArrayList<>();

    @Builder.Default
    private List<UserOptionItem> userOptions = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDataScopeItem {
        private Long userId;
        private String userName;
        private String deptCode;
        private String dept;
        private String role;
        private String dataScope;
        @Builder.Default
        private List<Long> allowedProjects = new ArrayList<>();
        @Builder.Default
        private List<String> allowedDepts = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentDataScopeItem {
        private String deptCode;
        private String deptName;
        private String dataScope;
        private boolean canViewOtherDepts;
        @Builder.Default
        private List<String> allowedDepts = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentOptionItem {
        private String code;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentTreeItem {
        private String deptCode;
        private String deptName;
        private String parentDeptCode;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserOptionItem {
        private Long id;
        private String name;
        private String role;
        private String deptCode;
        private String dept;
    }
}
