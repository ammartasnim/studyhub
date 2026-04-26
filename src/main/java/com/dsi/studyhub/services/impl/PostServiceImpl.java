package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.PostReqDto;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.repositories.CommunityRepository;
import com.dsi.studyhub.repositories.PostRepository;
import com.dsi.studyhub.services.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final CommunityRepository communityRepository;

    public PostServiceImpl(PostRepository postRepository, CommunityRepository communityRepository) {
        this.postRepository = postRepository;
        this.communityRepository = communityRepository;
    }

    @Override
    public Post createPost(PostReqDto postRequestDTO) {
        Community community = communityRepository.findById(postRequestDTO.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found with id: " + postRequestDTO.communityId()));

        Post post = new Post();
        post.setTitle(postRequestDTO.title());
        post.setContent(postRequestDTO.content());
        post.setImgs(postRequestDTO.imgs());
        post.setCommunity(community);

        return postRepository.save(post);
    }

    @Override
    public Page<Post> getAllPosts(String title, Pageable pageable) {
        if (title != null && !title.isEmpty()) {
            return postRepository.findByTitle(title, pageable);
        }
        return postRepository.findAll(pageable);
    }

}
