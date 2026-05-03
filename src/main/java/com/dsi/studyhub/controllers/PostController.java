package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.CommentReqDto;
import com.dsi.studyhub.dtos.CommentResDto;
import com.dsi.studyhub.dtos.PostReqDto;
import com.dsi.studyhub.dtos.PostResDto;
import com.dsi.studyhub.services.CommentService;
import com.dsi.studyhub.services.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @Autowired
    private CommentService commentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResDto> createPost(
            @RequestPart("title")   String title,
            @RequestPart("content" )String content,
            @RequestPart(value = "imgs",        required = false) List<MultipartFile> imgs,
            @RequestPart(value = "communityId", required = false)          String communityId
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
            Pageable pageable) { // Updated to support pagination
        return ResponseEntity.ok(postService.getPostsByCommunity(communityId, pageable));
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

    @PutMapping("/{id}")
    public ResponseEntity<PostResDto> updatePost(@PathVariable Long id, @RequestBody PostReqDto request) {
        return ResponseEntity.ok(postService.updatePost(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> toggleLike(@PathVariable Long postId) {
        postService.toggleLike(postId);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/feed")
    public ResponseEntity<Page<PostResDto>> getFeed(Pageable pageable) {
        return ResponseEntity.ok(postService.getFeed(pageable));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResDto> createCommentForPost(
            @PathVariable Long postId,
            @RequestBody CommentReqDto request) {
        CommentReqDto req = new CommentReqDto(postId, request.content());
        return new ResponseEntity<>(commentService.createComment(req), HttpStatus.CREATED);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<Page<CommentResDto>> getCommentsForPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(commentService.getCommentsByPost(postId, pageable));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<PostResDto> approvePost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.approvePost(id));
    }

    @PatchMapping("/{id}/flag")
    public ResponseEntity<PostResDto> flagPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.flagPost(id));
    }
}
