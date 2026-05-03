package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.CommentResDto;
import com.dsi.studyhub.dtos.CommentReqDto;
import com.dsi.studyhub.services.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResDto> createComment(@RequestBody CommentReqDto request) {
        return new ResponseEntity<>(commentService.createComment(request), HttpStatus.CREATED);
    }
    @PostMapping("/post/{postId}")
    public ResponseEntity<CommentResDto> createCommentForPost(
            @PathVariable Long postId,
            @RequestBody CommentReqDto request) {
        CommentReqDto merged = new CommentReqDto(postId, request.content());
        return new ResponseEntity<>(commentService.createComment(merged), HttpStatus.CREATED);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResDto> editComment(
            @PathVariable Long commentId,
            @RequestBody CommentReqDto request) {
        return ResponseEntity.ok(commentService.editComment(commentId, request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<Page<CommentResDto>> getCommentsByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        return ResponseEntity.ok(commentService.getCommentsByPost(postId, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<CommentResDto>> getMyComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(commentService.getMyComments(pageable));
    }

    @GetMapping
    public ResponseEntity<Page<CommentResDto>> getAllComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(commentService.getAllComments(PageRequest.of(page, size)));
    }

    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getCommentStats() {
        return ResponseEntity.ok(commentService.getCommentStats());
    }
    @PostMapping("/{commentId}/like")
    public ResponseEntity<Void> toggleLike(@PathVariable Long commentId) {
        commentService.toggleLike(commentId);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{commentId}/reply")
    public ResponseEntity<CommentResDto> reply(
            @PathVariable Long commentId,
            @RequestBody CommentReqDto request
    ) {
        return ResponseEntity.ok(commentService.createReply(commentId, request));
    }
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<Page<CommentResDto>> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(commentService.getReplies(commentId, pageable));
    }

}
