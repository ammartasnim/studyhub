package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.PostReqDto;
import com.dsi.studyhub.entities.Post;

import java.util.List;

public interface PostService {
    Post createPost(PostReqDto postRequestDTO);
    List<Post> getAllPosts(String title);
}
