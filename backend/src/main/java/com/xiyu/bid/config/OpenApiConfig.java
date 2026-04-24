package com.xiyu.bid.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${spring.application.name:xiyu-bid}")
    private String applicationName;

    @Value("${app.version:1.0.3}")
    private String applicationVersion;

    @Bean
    public OpenAPI xiyuBidOpenApi() {
        Info info = new Info()
                .title("西域数智化投标管理平台 API")
                .version(applicationVersion)
                .description("""
                        西域数智化投标管理平台对外标准 API 接口规范。

                        - 全部接口位于 `/api` 前缀下
                        - 统一响应体 `ApiResponse<T> { success, code, message, data }`
                        - 认证方式：Bearer JWT Token（在 Authorize 中输入登录返回的 token）
                        - 接口分类按业务域：标讯、项目、知识库、报价、AI、协作、看板、审计等
                        """)
                .contact(new Contact()
                        .name("西域投标平台研发组")
                        .email("dev@xiyu.example.com"))
                .license(new License()
                        .name("Proprietary - 西域集团")
                        .url("https://www.xiyu.example.com"));

        SecurityScheme bearerScheme = new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("登录后获取的 JWT，请求时通过 `Authorization: Bearer <token>` 携带");

        return new OpenAPI()
                .info(info)
                .servers(List.of(
                        new Server().url("http://127.0.0.1:18080").description("本地开发"),
                        new Server().url("/").description("当前部署环境")
                ))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
