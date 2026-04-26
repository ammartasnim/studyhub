package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.PostReqDto;
import com.dsi.studyhub.dtos.PostResDto;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.mappers.PostMapper;
import com.dsi.studyhub.repositories.CommunityRepository;
import com.dsi.studyhub.repositories.PostRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.PostService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class PostServiceImpl implements PostService {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommunityRepository communityRepository;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public PostResDto createPost(PostReqDto request) {
        User user = authenticatedUserService.getAuthenticatedUser();

        Community community = communityRepository.findById(request.communityId())
                .orElseThrow(() -> new RuntimeException("Community not found"));

        Post post = new Post();
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setImgs(request.imgs());
        post.setUser(user);
        post.setCommunity(community);

        user.setXpPts(user.getXpPts() + 20);
        userRepository.save(user);

        Post savedPost = postRepository.save(post);
        return postMapper.toDto(savedPost);
    }

    @Override
    public PostResDto getPostById(Long id) {
        return postRepository.findById(id)
                .map(postMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    @Override
    @Transactional
    public Page<PostResDto> getAllPosts(String title, Pageable pageable) {
        Page<Post> posts;

        if (title != null && !title.isEmpty()) {
            posts = postRepository.findByTitleContainingIgnoreCase(title, pageable);
        } else {
            posts = postRepository.findAll(pageable);
        }

        return posts.map(postMapper::toDto);
    }

    @Override
    @Transactional
    public Page<PostResDto> getPostsByCommunity(Long communityId, Pageable pageable) {
        Page<Post> entityPage = postRepository.findByCommunityId(communityId, pageable);

        return entityPage.map(postMapper::toDto);
    }

    @Override
    public Page<PostResDto> getPostsByUserId(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        return postRepository.findByUserId(userId, pageable)
                .map(postMapper::toDto);
    }

    @Override
    public Page<PostResDto> getMyPosts(Pageable pageable) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        return postRepository.findByUserId(currentUser.getId(), pageable)
                .map(postMapper::toDto);
    }

    @Override
    @Transactional
    public PostResDto updatePost(Long id, PostReqDto request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        postMapper.partialUpdate(request, post);
        return postMapper.toDto(postRepository.save(post));
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        if (!postRepository.existsById(id)) {
            throw new RuntimeException("Post not found");
        }
        postRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleLike(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User user = authenticatedUserService.getAuthenticatedUser();

        if (post.getLikes().contains(user)) {
            post.getLikes().remove(user);
        } else {
            post.getLikes().add(user);
            post.getUser().setXpPts(post.getUser().getXpPts() + 5);
        }
        postRepository.save(post);
    }


}
