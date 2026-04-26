package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.CommentReqDto;
import com.dsi.studyhub.dtos.CommentResDto;

import java.util.List;

public interface CommentService {
    CommentResDto createComment(CommentReqDto request);
    CommentResDto editComment(Long commentId, String newContent);
    void deleteComment(Long commentId);
    List<CommentResDto> getCommentsByPost(Long postId);
}
