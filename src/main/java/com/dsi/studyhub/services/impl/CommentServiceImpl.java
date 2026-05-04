package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommentReqDto;
import com.dsi.studyhub.dtos.CommentResDto;
import com.dsi.studyhub.entities.Comment;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.UserRole;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.gamification.GamificationService;
import com.dsi.studyhub.gamification.XpConfig;
import com.dsi.studyhub.mappers.CommentMapper;
import com.dsi.studyhub.repositories.CommentRepository;
import com.dsi.studyhub.repositories.PostRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.CommentService;
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

        Comment saved = commentRepository.save(comment);
        Comment fresh = commentRepository.findById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Comment not found after save"));

        // No XP when commenting on your own post
        boolean isOwnPost = post.getUser().getId().equals(user.getId());
        if (!isOwnPost) gamificationService.awardXp(user.getId(), XpConfig.COMMENT_CREATED);

        return commentMapper.toDto(fresh);
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
    @Override
    @Transactional
    public void toggleLike(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        User user = authenticatedUserService.getAuthenticatedUser();
        Long commentOwnerId = comment.getUser().getId();

        // No XP when liking your own comment
        boolean isOwnComment = commentOwnerId.equals(user.getId());

        boolean alreadyLiked = user.getLikedComments().stream()
                .anyMatch(c -> c.getId().equals(commentId));

        if (alreadyLiked) {
            user.getLikedComments().removeIf(c -> c.getId().equals(commentId));
            if (!isOwnComment) gamificationService.awardXp(commentOwnerId, XpConfig.LIKE_REMOVED);
        } else {
            user.getLikedComments().add(comment);
            if (!isOwnComment) gamificationService.awardXp(commentOwnerId, XpConfig.LIKE_RECEIVED);
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
        User user = authenticatedUserService.getAuthenticatedUser();

        if (!comment.getUser().getId().equals(user.getId()) && user.getRole() != UserRole.Admin) {
            throw new ForbiddenException("You don't own this comment!");
        }

        for (User u : new HashSet<>(comment.getLikedByUsers())) {
            u.getLikedComments().remove(comment);
        }
        comment.getLikedByUsers().clear();
        commentRepository.save(comment);
        commentRepository.deleteById(commentId);
        gamificationService.awardXp(user.getId(), XpConfig.COMMENT_DELETED);
    }

    @Override
    public Page<CommentResDto> getCommentsByPost(Long postId, Pageable pageable) {
        User user = authenticatedUserService.getAuthenticatedUser();
        return commentRepository.findByPostIdAndParentCommentIsNull(postId, pageable)
                .map(c -> commentMapper.toDto(c));
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
        return Map.of("total", commentRepository.count());
    }

    @Override
    @Transactional
    public CommentResDto createReply(Long parentCommentId, CommentReqDto request) {
        User user = authenticatedUserService.getAuthenticatedUser();
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("Parent comment not found"));

        Comment reply = new Comment();
        reply.setContent(request.content());
        reply.setUser(user);
        reply.setPost(parent.getPost());
        reply.setParentComment(parent);

        Comment saved = commentRepository.save(reply);
        Comment fresh = commentRepository.findById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Reply not found after save"));

        // No XP when replying on your own post or to your own comment
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
}
