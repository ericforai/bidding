package com.xiyu.bid.exception;

import com.openai.core.http.Headers;
import com.openai.errors.UnauthorizedException;
import com.openai.models.ErrorObject;
import com.xiyu.bid.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleOpenAiUnauthorizedException_shouldReturnDeepSeekCredentialMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/projects/1/tender-breakdown");
        ErrorObject error = ErrorObject.builder()
                .code("invalid_api_key")
                .message("Authentication Fails, Your api key: ****2f99 is invalid")
                .param("api_key")
                .type("invalid_request_error")
                .build();
        UnauthorizedException exception = UnauthorizedException.builder()
                .headers(Headers.builder().build())
                .error(error)
                .build();

        ResponseEntity<ApiResponse<Void>> response = handler.handleOpenAiUnauthorizedException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(502);
        assertThat(response.getBody().getMessage()).contains("DeepSeek API Key 无效或已失效");
        assertThat(response.getBody().getMessage()).doesNotContain("2f99");
    }
}
