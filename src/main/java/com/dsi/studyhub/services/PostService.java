package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.PostReqDto;
import com.dsi.studyhub.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {
    Post createPost(PostReqDto postRequestDTO);
    Page<Post> getAllPosts(String title, Pageable pageable);
}
