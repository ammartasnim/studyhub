package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.CommentReqDto;
import com.dsi.studyhub.dtos.CommentResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {
    CommentResDto createComment(CommentReqDto request);
    CommentResDto editComment(Long commentId, CommentReqDto request);
    void deleteComment(Long commentId);
    Page<CommentResDto> getCommentsByPost(Long postId, Pageable pageable);
    Page<CommentResDto> getMyComments(Pageable pageable);
}
