package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommentReqDto;
import com.dsi.studyhub.dtos.CommentResDto;
import com.dsi.studyhub.entities.Comment;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.mappers.CommentMapper;
import com.dsi.studyhub.repositories.CommentRepository;
import com.dsi.studyhub.repositories.PostRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.CommentService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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
    private UserRepository userRepository;
    @Autowired
    private CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentResDto createComment(CommentReqDto request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setContent(request.content());
        comment.setUser(user);
        comment.setPost(post);

        Comment saved = commentRepository.save(comment);
        return commentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CommentResDto editComment(Long commentId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        comment.setContent(newContent);
        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new RuntimeException("Comment not found");
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentResDto> getCommentsByPost(Long postId) {
        List<Comment>  comments = commentRepository.findByPostId(postId);
        List<CommentResDto> dtos = new ArrayList<>();
        for (Comment comment : comments) {
            dtos.add(commentMapper.toDto(comment));
        }
        return dtos;
    }
}
