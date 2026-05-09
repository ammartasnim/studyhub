package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.*;
import com.dsi.studyhub.services.CommentService;
import com.dsi.studyhub.services.PostService;
import com.dsi.studyhub.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final ReportService reportService;

    // Post creation and updates
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResDto> createPost(
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "imgs", required = false) List<MultipartFile> imgs,
            @RequestPart(value = "communityId", required = false) String communityId
    ) {
        PostReqDto dto = new PostReqDto(
                title,
                content,
                imgs,
                communityId != null ? Long.parseLong(communityId) : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResDto> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @GetMapping
    public ResponseEntity<Page<PostResDto>> getAllPosts(
            @RequestParam(required = false) String title,
            Pageable pageable) {
        return ResponseEntity.ok(postService.getAllPosts(title, pageable));
    }

    @GetMapping("/community/{communityId}")
    public ResponseEntity<Page<PostResDto>> getPostsByCommunity(
            @PathVariable Long communityId,
            Pageable pageable) {
        return ResponseEntity.ok(postService.getPostsByCommunity(communityId, pageable));
    }

    @GetMapping("/community/{communityId}/pending")
    public ResponseEntity<List<PostResDto>> getPendingPosts(@PathVariable Long communityId) {
        return ResponseEntity.ok(postService.getPendingPosts(communityId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PostResDto>> getPostsByUser(
            @PathVariable Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(postService.getPostsByUserId(userId, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<PostResDto>> getMyPosts(Pageable pageable) {
        return ResponseEntity.ok(postService.getMyPosts(pageable));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResDto> updatePost(
            @PathVariable Long id,
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "imgs", required = false) List<MultipartFile> imgs
    ) {
        PostReqDto dto = new PostReqDto(title, content, imgs, null);
        return ResponseEntity.ok(postService.updatePost(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    // Post interactions

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> toggleLike(@PathVariable Long postId) {
        postService.toggleLike(postId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<PostResDto>> getFeed(Pageable pageable) {
        return ResponseEntity.ok(postService.getFeed(pageable));
    }

    // Post comments

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResDto> createCommentForPost(
            @PathVariable Long postId,
            @RequestBody CommentReqDto request) {
        CommentReqDto req = new CommentReqDto(postId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(req));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<Page<CommentResDto>> getCommentsForPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(commentService.getCommentsByPost(postId, pageable));
    }

    // Post moderation

    @PatchMapping("/{id}/approve")
    public ResponseEntity<PostResDto> approvePost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.approvePost(id));
    }

    @DeleteMapping("/{id}/reject")
    public ResponseEntity<Void> rejectPost(@PathVariable Long id) {
        postService.rejectPost(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/seen")
    public ResponseEntity<Void> markPostsSeen(@RequestBody List<Long> postIds) {
        postService.markPostsSeen(postIds);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/seen/all")
    public ResponseEntity<Void> clearAllSeenPosts() {
        postService.clearAllSeenPosts();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}/moderate")
    public ResponseEntity<Void> moderatorDeletePost(@PathVariable Long postId) {
        postService.moderatorDeletePost(postId);
        return ResponseEntity.noContent().build();
    }

    // Admin stats and reports

    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Long>> getPostStats() {
        return ResponseEntity.ok(postService.getPostStats());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Page<PostResDto>> getByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getPostsByStatus(status, page, size));
    }
    @GetMapping("/posts/{postId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<ReportResDto>> getReportsForPost(@PathVariable Long postId) {
        return ResponseEntity.ok(reportService.getReportsForPost(postId));
    }
}
