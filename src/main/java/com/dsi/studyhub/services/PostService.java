package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.PostReqDto;
import com.dsi.studyhub.dtos.PostResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface PostService {
    PostResDto createPost(PostReqDto request);
    PostResDto getPostById(Long id);
    Page<PostResDto> getAllPosts(String title, Pageable pageable);
    Page<PostResDto> getPostsByCommunity(Long communityId, Pageable pageable);
    Page<PostResDto> getPostsByUserId(Long userId, Pageable pageable);
    Page<PostResDto> getMyPosts(Pageable pageable);
    PostResDto updatePost(Long id, PostReqDto request);
    void deletePost(Long id);
    void toggleLike(Long postId);
    Page<PostResDto> getFeed(Pageable pageable);
}
