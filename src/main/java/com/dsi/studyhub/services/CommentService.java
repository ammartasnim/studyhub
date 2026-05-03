package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.CommentReqDto;
import com.dsi.studyhub.dtos.CommentResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CommentService {
    CommentResDto createComment(CommentReqDto request);
    CommentResDto editComment(Long commentId, CommentReqDto request);
    CommentResDto createReply(Long parentCommentId, CommentReqDto request);
    void deleteComment(Long commentId);
    Page<CommentResDto> getCommentsByPost(Long postId, Pageable pageable);
    Page<CommentResDto> getMyComments(Pageable pageable);
    Page<CommentResDto> getAllComments(Pageable pageable);
    Map<String, Long> getCommentStats();
    void toggleLike(Long commentId);
    Page<CommentResDto> getReplies(Long commentId, Pageable pageable);
}
