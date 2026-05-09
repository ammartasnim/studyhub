package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommentReqDto;
import com.dsi.studyhub.dtos.CommentResDto;
import com.dsi.studyhub.entities.Comment;
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
import com.dsi.studyhub.services.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private AuthenticatedUserService authenticatedUserService;
    @Autowired
    private GamificationService gamificationService;
    @Autowired
    private CommunityAuthService communityAuthService;
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AiService  aiService;
    @Override
    @Transactional
    public CommentResDto createComment(CommentReqDto request) {
        User user = authenticatedUserService.getAuthenticatedUser();
        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + request.postId()));

        Comment comment = new Comment();
        comment.setContent(request.content());
        comment.setUser(user);
        comment.setPost(post);
        Boolean isSafe = aiService.isContentSafe(request.content());

        if(isSafe) {
            Comment saved = commentRepository.save(comment);
            Comment fresh = commentRepository.findById(saved.getId())
                    .orElseThrow(() -> new RuntimeException("Comment not found after save"));

            boolean isOwnPost = post.getUser().getId().equals(user.getId());
            if (!isOwnPost) {
                gamificationService.awardXp(user.getId(), XpConfig.COMMENT_CREATED);

                notificationService.createNotification(
                        post.getUser().getId(),
                        "COMMENT",
                        user.getUsername() + " commented on your post",
                        null,
                        post.getId()
                );
            }

            return commentMapper.toDto(fresh);

        }
        else {
            throw new ForbiddenException("Your comment contains harmful content and cannot be posted.");
        }

    }

    @Override
    @Transactional
    public CommentResDto editComment(Long commentId, CommentReqDto request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
        User user = authenticatedUserService.getAuthenticatedUser();

        if (!comment.getUser().getId().equals(user.getId()) && user.getRole() != UserRole.Admin) {
            throw new ForbiddenException("You don't own this comment!");
        }

        comment.setContent(request.content());
        return commentMapper.toDto(commentRepository.save(comment));
    }
    // Comment interactions
    @Override
    @Transactional
    public void toggleLike(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        User user = authenticatedUserService.getAuthenticatedUser();
        Long commentOwnerId = comment.getUser().getId();

        boolean isOwnComment = commentOwnerId.equals(user.getId());

        boolean alreadyLiked = user.getLikedComments().stream()
                .anyMatch(c -> c.getId().equals(commentId));

        if (alreadyLiked) {
            user.getLikedComments().removeIf(c -> c.getId().equals(commentId));
            if (!isOwnComment) gamificationService.awardXp(commentOwnerId, XpConfig.LIKE_REMOVED);
        } else {
            user.getLikedComments().add(comment);
            if (!isOwnComment) {
                gamificationService.awardXp(commentOwnerId, XpConfig.LIKE_RECEIVED);
                notificationService.createNotification(
                        commentOwnerId,
                        "LIKE",
                        user.getUsername() + " liked your comment",
                        null,
                        commentId
                );
            }
        }

        userRepository.save(user);
    }

    // Comment deletion
    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        // Removes comment and associated likes for replies and the root comment.
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
        User user = authenticatedUserService.getAuthenticatedUser();

        if (!comment.getUser().getId().equals(user.getId()) && user.getRole() != UserRole.Admin) {
            throw new ForbiddenException("You don't own this comment!");
        }

        for (Comment reply : comment.getReplies()) {
            for (User u : new HashSet<>(reply.getLikedByUsers())) {
                u.getLikedComments().remove(reply);
                userRepository.save(u);
            }
            reply.getLikedByUsers().clear();
            commentRepository.save(reply);
        }

        for (User u : new HashSet<>(comment.getLikedByUsers())) {
            u.getLikedComments().remove(comment);
            userRepository.save(u);
        }
        comment.getLikedByUsers().clear();
        commentRepository.save(comment);

        commentRepository.deleteById(commentId);
        gamificationService.awardXp(user.getId(), XpConfig.COMMENT_DELETED);
    }

    // Comment queries
    @Override
    public Page<CommentResDto> getCommentsByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostIdAndParentCommentIsNull(postId, pageable)
                .map(commentMapper::toDto);
    }

    @Override
    public Page<CommentResDto> getAllComments(Pageable pageable) {
        return commentRepository.findAll(pageable)
                .map(commentMapper::toDto);
    }

    @Override
    public Page<CommentResDto> getMyComments(Pageable pageable) {
        User user = authenticatedUserService.getAuthenticatedUser();
        return commentRepository.findByUserId(user.getId(), pageable)
                .map(commentMapper::toDto);
    }

    @Override
    public Map<String, Long> getCommentStats() {
        return Map.of("total", commentRepository.countByStatusNot(CommentStatus.Flagged));
    }

    // Replies
    @Override
    @Transactional
    public CommentResDto createReply(Long parentCommentId, CommentReqDto request) {
        User user = authenticatedUserService.getAuthenticatedUser();
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("Parent comment not found"));
         Boolean Isvalid=aiService.isContentSafe(request.content());
        if(!Isvalid){
            throw new ForbiddenException("Your reply contains harmful content and cannot be posted.");
        }

        Comment reply = new Comment();
        reply.setContent(request.content());
        reply.setUser(user);
        reply.setPost(parent.getPost());
        reply.setParentComment(parent);

        Comment saved = commentRepository.save(reply);
        Comment fresh = commentRepository.findById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Reply not found after save"));

        boolean isOwnPost = parent.getPost().getUser().getId().equals(user.getId());
        boolean isOwnComment = parent.getUser().getId().equals(user.getId());
        if (!isOwnPost && !isOwnComment) {
            gamificationService.awardXp(user.getId(), XpConfig.COMMENT_CREATED);
        }
        return commentMapper.toDto(fresh);
    }
    @Override
    public Page<CommentResDto> getReplies(Long commentId, Pageable pageable) {
        return commentRepository.findByParentCommentId(commentId, pageable)
                .map(commentMapper::toDto);
    }
    // Moderation
    @Override
    @Transactional
    public void moderatorDeleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));
        User currentUser = authenticatedUserService.getAuthenticatedUser();

        Long communityId = comment.getPost() != null && comment.getPost().getCommunity() != null
                ? comment.getPost().getCommunity().getId()
                : null;

        if (communityId == null) {
            throw new ForbiddenException("Comment does not belong to a community post.");
        }

        communityAuthService.requireOwnerOrPermission(currentUser.getId(), communityId, CommunityPermission.DELETE_COMMENT);

        for (User u : new HashSet<>(comment.getLikedByUsers())) {
            u.getLikedComments().remove(comment);
            userRepository.save(u);
        }
        comment.getLikedByUsers().clear();
        commentRepository.save(comment);
        commentRepository.deleteById(commentId);
    }
}
