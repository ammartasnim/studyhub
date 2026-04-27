package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommentReqDto;
import com.dsi.studyhub.dtos.CommentResDto;
import com.dsi.studyhub.entities.Comment;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.UserRole;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.gamification.GamificationService;
import com.dsi.studyhub.gamification.XpConfig;
import com.dsi.studyhub.mappers.CommentMapper;
import com.dsi.studyhub.repositories.CommentRepository;
import com.dsi.studyhub.repositories.PostRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.CommentService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private PostRepository postRepository;
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
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setContent(request.content());
        comment.setUser(user);
        comment.setPost(post);

        Comment saved = commentRepository.save(comment);
        gamificationService.awardXp(user.getId(), XpConfig.COMMENT_CREATED);
        return commentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CommentResDto editComment(Long commentId, CommentReqDto request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        User user = authenticatedUserService.getAuthenticatedUser();
        if (!comment.getUser().getId().equals(user.getId()) && user.getRole()!= UserRole.Admin) {
            throw new ForbiddenException("You don't own this comment!");
        }
        commentMapper.partialUpdate(request, comment);

        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        User user = authenticatedUserService.getAuthenticatedUser();
        if (!comment.getUser().getId().equals(user.getId()) && user.getRole()!= UserRole.Admin) {
            throw new ForbiddenException("You don't own this comment!");
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public Page<CommentResDto> getCommentsByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostId(postId, pageable)
                .map(commentMapper::toDto);
    }

    @Override
    public Page<CommentResDto> getMyComments(Pageable pageable) {
        User user = authenticatedUserService.getAuthenticatedUser();

        return commentRepository.findByUserId(user.getId(), pageable)
                .map(commentMapper::toDto);
    }
}
