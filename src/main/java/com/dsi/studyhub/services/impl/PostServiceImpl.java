package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.PostRequestDTO;
import com.dsi.studyhub.dtos.PostResponseDTO;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.repositories.CommunityRepository;
import com.dsi.studyhub.repositories.PostRepository;
import com.dsi.studyhub.services.PostService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final CommunityRepository communityRepository;

    public PostServiceImpl(PostRepository postRepository, CommunityRepository communityRepository) {
        this.postRepository = postRepository;
        this.communityRepository = communityRepository;
    }

    @Override
    public PostResponseDTO createPost(PostRequestDTO postRequestDTO) {
        return null;
    }

    @Override
    public List<PostResponseDTO> getAllPosts() {
        List<Post> posts= postRepository.findAll();
        return null;
    }

}
