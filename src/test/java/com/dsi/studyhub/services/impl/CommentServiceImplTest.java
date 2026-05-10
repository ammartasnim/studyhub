package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommentReqDto;
import com.dsi.studyhub.dtos.CommentResDto;
import com.dsi.studyhub.entities.Comment;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.CommentStatus;
import com.dsi.studyhub.enums.CommunityPermission;
import com.dsi.studyhub.enums.UserRole;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.gamification.GamificationService;
import com.dsi.studyhub.gamification.XpConfig;
import com.dsi.studyhub.mappers.CommentMapper;
import com.dsi.studyhub.repositories.CommentRepository;
import com.dsi.studyhub.repositories.PostRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.AiService;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.CommunityAuthService;
import com.dsi.studyhub.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommentMapper commentMapper;
    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private GamificationService gamificationService;
    @Mock private CommunityAuthService communityAuthService;
    @Mock private NotificationService notificationService;
    @Mock private AiService aiService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User author;
    private User otherUser;
    private User adminUser;
    private User replyAuthor;
    private Post post;
    private Comment comment;
    private Comment reply;
    private CommentResDto commentDto;

    // Builds shared fixtures to keep each test focused on one behavior.
    @BeforeEach
    void setUp() {
        author = buildUser(1L, "author", UserRole.Client);
        otherUser = buildUser(2L, "other", UserRole.Client);
        adminUser = buildUser(3L, "admin", UserRole.Admin);
        replyAuthor = buildUser(4L, "reply", UserRole.Client);

        post = new Post();
        post.setId(10L);
        post.setUser(otherUser);

        comment = new Comment();
        comment.setId(100L);
        comment.setPost(post);
        comment.setUser(author);
        comment.setContent("hello");
        comment.setLikedByUsers(new HashSet<>());

        reply = new Comment();
        reply.setId(101L);
        reply.setPost(post);
        reply.setUser(otherUser);
        reply.setParentComment(comment);
        reply.setContent("reply");
        reply.setLikedByUsers(new HashSet<>());

        commentDto = CommentResDto.builder()
                .id(100L)
                .postId(10L)
                .userId(author.getId())
                .content("hello")
                .build();
    }

    // Allows safe content to be created, awarding XP and notifying the post owner.
    @Test
    void createComment_safeContent_awardsXpAndNotifiesOwner() {
        CommentReqDto req = new CommentReqDto(post.getId(), "Nice post");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(aiService.isContentSafe(req.content())).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(comment.getId());
            return saved;
        });
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentResDto result = commentService.createComment(req);

        assertThat(result).isEqualTo(commentDto);
        verify(gamificationService).awardXp(author.getId(), XpConfig.COMMENT_CREATED);
        verify(notificationService).createNotification(eq(otherUser.getId()), eq("COMMENT"), any(), isNull(), eq(post.getId()));
    }

    // Rejects unsafe content before any write operations.
    @Test
    void createComment_unsafeContent_throwsForbidden() {
        CommentReqDto req = new CommentReqDto(post.getId(), "bad");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(aiService.isContentSafe(req.content())).thenReturn(false);

        assertThatThrownBy(() -> commentService.createComment(req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("harmful");
    }

    // Bubbles up missing post errors for invalid requests.
    @Test
    void createComment_postNotFound_throwsRuntimeException() {
        CommentReqDto req = new CommentReqDto(999L, "Hi");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(author);
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Post not found");
    }

    // Sends mention notifications to referenced users except author and post owner.
    @Test
    void createComment_mentions_notifyMentionedUsers() {
        CommentReqDto req = new CommentReqDto(post.getId(), "hey @mark and @owner");
        User mentioned = buildUser(4L, "mark", UserRole.Client);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(aiService.isContentSafe(req.content())).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(comment.getId());
            return saved;
        });
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(commentMapper.toDto(comment)).thenReturn(commentDto);
        when(userRepository.findByUsername("mark")).thenReturn(Optional.of(mentioned));
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(otherUser));

        commentService.createComment(req);

        verify(notificationService).createNotification(eq(mentioned.getId()), eq("MENTION"), any(), isNull(), eq(post.getId()));
        verify(notificationService, never()).createNotification(eq(otherUser.getId()), eq("MENTION"), any(), any(), any());
    }

    // Creates a reply when safe and awards XP when not self-replying.
    @Test
    void createReply_safeContent_awardsXpWhenNotOwn() {
        CommentReqDto req = new CommentReqDto(post.getId(), "Reply");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(replyAuthor);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(aiService.isContentSafe(req.content())).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(reply.getId());
            return saved;
        });
        when(commentRepository.findById(reply.getId())).thenReturn(Optional.of(reply));
        when(commentMapper.toDto(reply)).thenReturn(commentDto);

        commentService.createReply(comment.getId(), req);

        verify(gamificationService).awardXp(replyAuthor.getId(), XpConfig.COMMENT_CREATED);
    }

    // Skips XP when replying to your own post or your own comment.
    @Test
    void createReply_ownPostOrOwnComment_noXp() {
        CommentReqDto req = new CommentReqDto(post.getId(), "Reply");
        Post ownPost = new Post();
        ownPost.setId(11L);
        ownPost.setUser(replyAuthor);
        Comment ownParent = new Comment();
        ownParent.setId(200L);
        ownParent.setUser(replyAuthor);
        ownParent.setPost(ownPost);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(replyAuthor);
        when(commentRepository.findById(ownParent.getId())).thenReturn(Optional.of(ownParent));
        when(aiService.isContentSafe(req.content())).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(201L);
            return saved;
        });
        when(commentRepository.findById(201L)).thenReturn(Optional.of(reply));
        when(commentMapper.toDto(reply)).thenReturn(commentDto);

        commentService.createReply(ownParent.getId(), req);

        verify(gamificationService, never()).awardXp(anyLong(), any());
    }

    // Blocks replies with unsafe content.
    @Test
    void createReply_unsafeContent_throwsForbidden() {
        CommentReqDto req = new CommentReqDto(post.getId(), "bad");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(replyAuthor);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(aiService.isContentSafe(req.content())).thenReturn(false);

        assertThatThrownBy(() -> commentService.createReply(comment.getId(), req))
                .isInstanceOf(ForbiddenException.class);
    }

    // Ensures missing parent comments fail with a clear error.
    @Test
    void createReply_parentNotFound_throwsRuntimeException() {
        CommentReqDto req = new CommentReqDto(post.getId(), "Reply");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(replyAuthor);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createReply(comment.getId(), req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Parent comment not found");
    }

    // Sends mention notifications in replies, excluding the author.
    @Test
    void createReply_mentions_notifyMentionedUsers() {
        CommentReqDto req = new CommentReqDto(post.getId(), "hi @mark");
        User mentioned = buildUser(4L, "mark", UserRole.Client);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(replyAuthor);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(aiService.isContentSafe(req.content())).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(reply.getId());
            return saved;
        });
        when(commentRepository.findById(reply.getId())).thenReturn(Optional.of(reply));
        when(commentMapper.toDto(reply)).thenReturn(commentDto);
        when(userRepository.findByUsername("mark")).thenReturn(Optional.of(mentioned));

        commentService.createReply(comment.getId(), req);

        verify(notificationService).createNotification(eq(mentioned.getId()), eq("MENTION"), any(), isNull(), eq(post.getId()));
    }

    // Allows owners to edit their own comments.
    @Test
    void editComment_owner_canEdit() {
        CommentReqDto req = new CommentReqDto(post.getId(), "Updated");
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(author);
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        commentService.editComment(comment.getId(), req);

        assertThat(comment.getContent()).isEqualTo("Updated");
    }

    // Allows admins to edit comments they do not own.
    @Test
    void editComment_admin_canEdit() {
        CommentReqDto req = new CommentReqDto(post.getId(), "Updated");
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(adminUser);
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        commentService.editComment(comment.getId(), req);

        assertThat(comment.getContent()).isEqualTo("Updated");
    }

    // Blocks edits from non-owners without admin role.
    @Test
    void editComment_nonOwner_throwsForbidden() {
        CommentReqDto req = new CommentReqDto(post.getId(), "Updated");
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(otherUser);

        assertThatThrownBy(() -> commentService.editComment(comment.getId(), req))
                .isInstanceOf(ForbiddenException.class);
    }

    // Ensures edits fail fast when comments are missing.
    @Test
    void editComment_notFound_throwsRuntimeException() {
        CommentReqDto req = new CommentReqDto(post.getId(), "Updated");
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.editComment(comment.getId(), req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Comment not found");
    }

    // Removes likes and awards XP when owners delete their comments.
    @Test
    void deleteComment_owner_cleansLikes_andAwardsXp() {
        User liker = buildUser(9L, "liker", UserRole.Client);
        liker.getLikedComments().add(comment);
        comment.getLikedByUsers().add(liker);
        comment.getReplies().add(reply);
        reply.getLikedByUsers().add(liker);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(author);

        commentService.deleteComment(comment.getId());

        verify(userRepository).save(liker);
        verify(commentRepository).save(reply);
        verify(commentRepository).save(comment);
        verify(commentRepository).deleteById(comment.getId());
        verify(gamificationService).awardXp(author.getId(), XpConfig.COMMENT_DELETED);
    }

    // Allows admins to delete comments they do not own.
    @Test
    void deleteComment_admin_canDelete() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(adminUser);

        commentService.deleteComment(comment.getId());

        verify(commentRepository).deleteById(comment.getId());
    }

    // Blocks deletes from non-owners without admin role.
    @Test
    void deleteComment_nonOwner_throwsForbidden() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(otherUser);

        assertThatThrownBy(() -> commentService.deleteComment(comment.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // Ensures delete fails fast when comments are missing.
    @Test
    void deleteComment_notFound_throwsRuntimeException() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(comment.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Comment not found");
    }

    // Enforces community-only deletes and required permissions for moderators.
    @Test
    void moderatorDeleteComment_requiresCommunity() {
        comment.setPost(new Post());
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(otherUser);

        assertThatThrownBy(() -> commentService.moderatorDeleteComment(comment.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("community");
    }

    // Performs moderator delete after permission checks and like cleanup.
    @Test
    void moderatorDeleteComment_success_cleansLikes() {
        User liker = buildUser(9L, "liker", UserRole.Client);
        liker.getLikedComments().add(comment);
        comment.getLikedByUsers().add(liker);
        Community community = new Community();
        community.setId(55L);
        post.setCommunity(community);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(otherUser);

        commentService.moderatorDeleteComment(comment.getId());

        verify(communityAuthService).requireOwnerOrPermission(otherUser.getId(), 55L, CommunityPermission.DELETE_COMMENT);
        verify(userRepository).save(liker);
        verify(commentRepository).save(comment);
        verify(commentRepository).deleteById(comment.getId());
    }

    // Adds likes, awards XP, and notifies owners on first like.
    @Test
    void toggleLike_addLike_awardsXpAndNotifies() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(otherUser);
        when(userRepository.save(any(User.class))).thenReturn(otherUser);

        commentService.toggleLike(comment.getId());

        verify(gamificationService).awardXp(author.getId(), XpConfig.LIKE_RECEIVED);
        verify(notificationService).createNotification(eq(author.getId()), eq("LIKE"), any(), isNull(), eq(comment.getId()));
    }

    // Removes likes and reverses XP when unliking.
    @Test
    void toggleLike_removeLike_awardsXpRemoved() {
        otherUser.getLikedComments().add(comment);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(otherUser);
        when(userRepository.save(any(User.class))).thenReturn(otherUser);

        commentService.toggleLike(comment.getId());

        verify(gamificationService).awardXp(author.getId(), XpConfig.LIKE_REMOVED);
    }

    // Skips XP/notification when liking your own comment.
    @Test
    void toggleLike_ownComment_noXp() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(author);
        when(userRepository.save(any(User.class))).thenReturn(author);

        commentService.toggleLike(comment.getId());

        verify(gamificationService, never()).awardXp(anyLong(), any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    // Surfaces missing comment errors for like toggles.
    @Test
    void toggleLike_notFound_throwsException() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.toggleLike(comment.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Returns paged comments for a post, excluding replies.
    @Test
    void getCommentsByPost_returnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        when(commentRepository.findByPostIdAndParentCommentIsNull(post.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(comment)));
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        Page<CommentResDto> result = commentService.getCommentsByPost(post.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Lists all comments for admin-style views.
    @Test
    void getAllComments_returnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        when(commentRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(comment)));
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        Page<CommentResDto> result = commentService.getAllComments(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Uses the authenticated user when listing personal comments.
    @Test
    void getMyComments_usesAuthenticatedUser() {
        Pageable pageable = PageRequest.of(0, 5);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(author);
        when(commentRepository.findByUserId(author.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(comment)));
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        Page<CommentResDto> result = commentService.getMyComments(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Returns replies for a parent comment.
    @Test
    void getReplies_returnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        when(commentRepository.findByParentCommentId(comment.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(reply)));
        when(commentMapper.toDto(reply)).thenReturn(commentDto);

        Page<CommentResDto> result = commentService.getReplies(comment.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Returns unflagged comment counts for admin stats.
    @Test
    void getCommentStats_returnsTotal() {
        when(commentRepository.countByStatusNot(CommentStatus.Flagged)).thenReturn(5L);

        var stats = commentService.getCommentStats();

        assertThat(stats.get("total")).isEqualTo(5L);
    }

    // Helper for consistent user setup across tests.
    private User buildUser(Long id, String username, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setLikedComments(new HashSet<>());
        user.setLikedPosts(new HashSet<>());
        user.setJoinedCommunities(new ArrayList<>());
        user.setOwnedCommunities(new ArrayList<>());
        return user;
    }
}
