package com.xiyu.bid.collaboration;

import com.xiyu.bid.collaboration.dto.CommentCreateRequest;
import com.xiyu.bid.collaboration.dto.CommentDTO;
import com.xiyu.bid.collaboration.dto.CommentUpdateRequest;
import com.xiyu.bid.collaboration.dto.ThreadCreateRequest;
import com.xiyu.bid.collaboration.dto.CollaborationThreadDTO;
import com.xiyu.bid.collaboration.dto.ThreadStatus;
import com.xiyu.bid.collaboration.entity.Comment;
import com.xiyu.bid.collaboration.entity.CollaborationThread;
import com.xiyu.bid.collaboration.repository.CommentRepository;
import com.xiyu.bid.collaboration.repository.CollaborationThreadRepository;
import com.xiyu.bid.collaboration.service.CollaborationService;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.audit.service.IAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CollaborationService单元测试
 * 测试协作模块的核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    @Mock
    private CollaborationThreadRepository threadRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private IAuditLogService auditLogService;

    private CollaborationService collaborationService;

    private CollaborationThread testThread;
    private Comment testComment;
    private ThreadCreateRequest threadCreateRequest;
    private CommentCreateRequest commentCreateRequest;
    private CommentUpdateRequest commentUpdateRequest;

    @BeforeEach
    void setUp() {
        collaborationService = new CollaborationService(
                threadRepository,
                commentRepository,
                auditLogService
        );

        testThread = CollaborationThread.builder()
                .id(1L)
                .projectId(100L)
                .title("Discussion about bid strategy")
                .status(CollaborationThread.ThreadStatus.OPEN)
                .createdBy(10L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testComment = Comment.builder()
                .id(1L)
                .threadId(1L)
                .userId(10L)
                .content("This is a test comment")
                .mentions(null)
                .parentId(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        threadCreateRequest = ThreadCreateRequest.builder()
                .projectId(100L)
                .title("Discussion about bid strategy")
                .createdBy(10L)
                .build();

        commentCreateRequest = CommentCreateRequest.builder()
                .threadId(1L)
                .userId(10L)
                .content("This is a test comment")
                .mentions(null)
                .parentId(null)
                .build();

        commentUpdateRequest = CommentUpdateRequest.builder()
                .content("Updated comment content")
                .build();
    }

    // ========== createThread Tests ==========

    @Test
    void createThread_WithValidRequest_ShouldReturnSavedThread() {
        // Given
        when(threadRepository.save(any(CollaborationThread.class))).thenReturn(testThread);

        // When
        CollaborationThreadDTO result = collaborationService.createThread(threadCreateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProjectId()).isEqualTo(100L);
        assertThat(result.getTitle()).isEqualTo("Discussion about bid strategy");
        assertThat(result.getStatus()).isEqualTo(ThreadStatus.OPEN);
        assertThat(result.getCreatedBy()).isEqualTo(10L);

        verify(threadRepository).save(any(CollaborationThread.class));
    }

    @Test
    void createThread_WithNullProjectId_ShouldThrowException() {
        // Given
        ThreadCreateRequest invalidRequest = ThreadCreateRequest.builder()
                .projectId(null)
                .title("Test thread")
                .createdBy(10L)
                .build();

        // When & Then
        assertThatThrownBy(() -> collaborationService.createThread(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID");

        verify(threadRepository, never()).save(any());
    }

    @Test
    void createThread_WithNullTitle_ShouldThrowException() {
        // Given
        ThreadCreateRequest invalidRequest = ThreadCreateRequest.builder()
                .projectId(100L)
                .title(null)
                .createdBy(10L)
                .build();

        // When & Then
        assertThatThrownBy(() -> collaborationService.createThread(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title");

        verify(threadRepository, never()).save(any());
    }

    @Test
    void createThread_WithEmptyTitle_ShouldThrowException() {
        // Given
        ThreadCreateRequest invalidRequest = ThreadCreateRequest.builder()
                .projectId(100L)
                .title("   ")
                .createdBy(10L)
                .build();

        // When & Then
        assertThatThrownBy(() -> collaborationService.createThread(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title");

        verify(threadRepository, never()).save(any());
    }

    // ========== addComment Tests ==========

    @Test
    void addComment_WithValidRequest_ShouldReturnSavedComment() {
        // Given
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // When
        CommentDTO result = collaborationService.addComment(1L, commentCreateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getThreadId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(result.getContent()).isEqualTo("This is a test comment");

        verify(threadRepository).findById(1L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_WithNonExistentThread_ShouldThrowException() {
        // Given
        when(threadRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> collaborationService.addComment(999L, commentCreateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Thread not found");

        verify(threadRepository).findById(999L);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_WithNullContent_ShouldThrowException() {
        // Given
        CommentCreateRequest invalidRequest = CommentCreateRequest.builder()
                .threadId(1L)
                .userId(10L)
                .content(null)
                .build();

        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));

        // When & Then
        assertThatThrownBy(() -> collaborationService.addComment(1L, invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_WithEmptyContent_ShouldThrowException() {
        // Given
        CommentCreateRequest invalidRequest = CommentCreateRequest.builder()
                .threadId(1L)
                .userId(10L)
                .content("   ")
                .build();

        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));

        // When & Then
        assertThatThrownBy(() -> collaborationService.addComment(1L, invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_WithNestedComment_ShouldReturnSavedComment() {
        // Given
        CommentCreateRequest nestedRequest = CommentCreateRequest.builder()
                .threadId(1L)
                .userId(10L)
                .content("Nested reply")
                .parentId(5L)
                .build();

        Comment nestedComment = Comment.builder()
                .id(2L)
                .threadId(1L)
                .userId(10L)
                .content("Nested reply")
                .parentId(5L)
                .build();

        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(commentRepository.save(any(Comment.class))).thenReturn(nestedComment);

        // When
        CommentDTO result = collaborationService.addComment(1L, nestedRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isEqualTo(5L);

        verify(commentRepository).save(any(Comment.class));
    }

    // ========== updateComment Tests ==========

    @Test
    void updateComment_WithValidData_ShouldReturnUpdatedComment() {
        // Given
        Comment updatedComment = Comment.builder()
                .id(1L)
                .threadId(1L)
                .userId(10L)
                .content("Updated comment content")
                .isDeleted(false)
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(updatedComment);

        // When
        CommentDTO result = collaborationService.updateComment(1L, commentUpdateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Updated comment content");

        verify(commentRepository).findById(1L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void updateComment_WithNonExistentComment_ShouldThrowException() {
        // Given
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> collaborationService.updateComment(999L, commentUpdateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment not found");

        verify(commentRepository).findById(999L);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_WithDeletedComment_ShouldThrowException() {
        // Given
        Comment deletedComment = Comment.builder()
                .id(1L)
                .content("Deleted content")
                .isDeleted(true)
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(deletedComment));

        // When & Then
        assertThatThrownBy(() -> collaborationService.updateComment(1L, commentUpdateRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot update deleted comment");

        verify(commentRepository).findById(1L);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_WithNullContent_ShouldThrowException() {
        // Given
        CommentUpdateRequest invalidRequest = CommentUpdateRequest.builder()
                .content(null)
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        // When & Then
        assertThatThrownBy(() -> collaborationService.updateComment(1L, invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content");

        verify(commentRepository, never()).save(any());
    }

    // ========== deleteComment Tests ==========

    @Test
    void deleteComment_WithValidId_ShouldSoftDelete() {
        // Given
        Comment deletedComment = Comment.builder()
                .id(1L)
                .content("Content to delete")
                .isDeleted(true)
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(deletedComment);

        // When
        collaborationService.deleteComment(1L);

        // Then
        verify(commentRepository).findById(1L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void deleteComment_WithNonExistentComment_ShouldThrowException() {
        // Given
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> collaborationService.deleteComment(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment not found");

        verify(commentRepository).findById(999L);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void deleteComment_WithAlreadyDeletedComment_ShouldThrowException() {
        // Given
        Comment alreadyDeletedComment = Comment.builder()
                .id(1L)
                .content("Content")
                .isDeleted(true)
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(alreadyDeletedComment));

        // When & Then
        assertThatThrownBy(() -> collaborationService.deleteComment(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Comment already deleted");

        verify(commentRepository).findById(1L);
        verify(commentRepository, never()).save(any());
    }

    // ========== getThreadsByProject Tests ==========

    @Test
    void getThreadsByProject_WithValidProjectId_ShouldReturnThreads() {
        // Given
        CollaborationThread thread2 = CollaborationThread.builder()
                .id(2L)
                .projectId(100L)
                .title("Second thread")
                .status(CollaborationThread.ThreadStatus.IN_PROGRESS)
                .build();

        when(threadRepository.findByProjectId(100L))
                .thenReturn(Arrays.asList(testThread, thread2));

        // When
        List<CollaborationThreadDTO> result = collaborationService.getThreadsByProject(100L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProjectId()).isEqualTo(100L);
        assertThat(result.get(1).getProjectId()).isEqualTo(100L);

        verify(threadRepository).findByProjectId(100L);
    }

    @Test
    void getThreadsByProject_WithNoThreads_ShouldReturnEmptyList() {
        // Given
        when(threadRepository.findByProjectId(999L)).thenReturn(List.of());

        // When
        List<CollaborationThreadDTO> result = collaborationService.getThreadsByProject(999L);

        // Then
        assertThat(result).isEmpty();

        verify(threadRepository).findByProjectId(999L);
    }

    // ========== getCommentsByThread Tests ==========

    @Test
    void getCommentsByThread_WithValidThreadId_ShouldReturnComments() {
        // Given
        Comment comment2 = Comment.builder()
                .id(2L)
                .threadId(1L)
                .userId(11L)
                .content("Second comment")
                .isDeleted(false)
                .build();

        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(commentRepository.findByThreadIdAndIsDeletedFalseOrderByCreatedAtAsc(1L))
                .thenReturn(Arrays.asList(testComment, comment2));

        // When
        List<CommentDTO> result = collaborationService.getCommentsByThread(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getThreadId()).isEqualTo(1L);
        assertThat(result.get(1).getThreadId()).isEqualTo(1L);

        verify(threadRepository).findById(1L);
        verify(commentRepository).findByThreadIdAndIsDeletedFalseOrderByCreatedAtAsc(1L);
    }

    @Test
    void getCommentsByThread_WithNoComments_ShouldReturnEmptyList() {
        // Given
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(commentRepository.findByThreadIdAndIsDeletedFalseOrderByCreatedAtAsc(1L))
                .thenReturn(List.of());

        // When
        List<CommentDTO> result = collaborationService.getCommentsByThread(1L);

        // Then
        assertThat(result).isEmpty();

        verify(threadRepository).findById(1L);
        verify(commentRepository).findByThreadIdAndIsDeletedFalseOrderByCreatedAtAsc(1L);
    }

    // ========== getMentionsForUser Tests ==========

    @Test
    void getMentionsForUser_WithValidUserId_ShouldReturnComments() {
        // Given
        Comment mentionedComment = Comment.builder()
                .id(2L)
                .threadId(1L)
                .userId(10L)
                .content("Mentioning user")
                .mentions("[10, 20, 30]")
                .isDeleted(false)
                .build();

        when(commentRepository.findByMentionsContainingAndIsDeletedFalse("[10]"))
                .thenReturn(Arrays.asList(testComment, mentionedComment));

        // When
        List<CommentDTO> result = collaborationService.getMentionsForUser(10L);

        // Then
        assertThat(result).hasSize(2);

        verify(commentRepository).findByMentionsContainingAndIsDeletedFalse("[10]");
    }

    @Test
    void getMentionsForUser_WithNoMentions_ShouldReturnEmptyList() {
        // Given
        when(commentRepository.findByMentionsContainingAndIsDeletedFalse("[999]"))
                .thenReturn(List.of());

        // When
        List<CommentDTO> result = collaborationService.getMentionsForUser(999L);

        // Then
        assertThat(result).isEmpty();

        verify(commentRepository).findByMentionsContainingAndIsDeletedFalse("[999]");
    }

    // ========== updateThreadStatus Tests ==========

    @Test
    void updateThreadStatus_WithValidData_ShouldReturnUpdatedThread() {
        // Given
        CollaborationThread updatedThread = CollaborationThread.builder()
                .id(1L)
                .projectId(100L)
                .title("Discussion about bid strategy")
                .status(CollaborationThread.ThreadStatus.IN_PROGRESS)
                .build();

        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(threadRepository.save(any(CollaborationThread.class))).thenReturn(updatedThread);

        // When
        CollaborationThreadDTO result = collaborationService.updateThreadStatus(
                1L,
                ThreadStatus.IN_PROGRESS
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ThreadStatus.IN_PROGRESS);

        verify(threadRepository).findById(1L);
        verify(threadRepository).save(any(CollaborationThread.class));
    }

    @Test
    void updateThreadStatus_WithNonExistentThread_ShouldThrowException() {
        // Given
        when(threadRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> collaborationService.updateThreadStatus(
                999L,
                ThreadStatus.CLOSED
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Thread not found");

        verify(threadRepository).findById(999L);
        verify(threadRepository, never()).save(any());
    }

    @Test
    void updateThreadStatus_ToAllStatuses_ShouldUpdateSuccessfully() {
        // Given
        when(threadRepository.findById(1L)).thenReturn(Optional.of(testThread));
        when(threadRepository.save(any(CollaborationThread.class))).thenReturn(testThread);

        // When & Then - IN_PROGRESS
        CollaborationThreadDTO result1 = collaborationService.updateThreadStatus(
                1L,
                ThreadStatus.IN_PROGRESS
        );
        assertThat(result1.getStatus()).isEqualTo(ThreadStatus.IN_PROGRESS);

        // When & Then - RESOLVED
        CollaborationThreadDTO result2 = collaborationService.updateThreadStatus(
                1L,
                ThreadStatus.RESOLVED
        );
        assertThat(result2.getStatus()).isEqualTo(ThreadStatus.RESOLVED);

        // When & Then - CLOSED
        CollaborationThreadDTO result3 = collaborationService.updateThreadStatus(
                1L,
                ThreadStatus.CLOSED
        );
        assertThat(result3.getStatus()).isEqualTo(ThreadStatus.CLOSED);

        verify(threadRepository, times(3)).save(any(CollaborationThread.class));
    }
}
