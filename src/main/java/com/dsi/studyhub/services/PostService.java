package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.PostRequestDTO;
import com.dsi.studyhub.dtos.PostResponseDTO;

import java.util.List;

public interface PostService {
    PostResponseDTO createPost(PostRequestDTO postRequestDTO);
    List<PostResponseDTO> getAllPosts();
}
