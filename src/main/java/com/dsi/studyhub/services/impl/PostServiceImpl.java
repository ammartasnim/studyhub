package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.PostReqDto;
import com.dsi.studyhub.dtos.PostResDto;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.gamification.GamificationService;
import com.dsi.studyhub.gamification.XpConfig;
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
    @Autowired
    private GamificationService gamificationService;

    @Override
    @Transactional
    public PostResDto createPost(PostReqDto request) {
        User user = authenticatedUserService.getAuthenticatedUser();

        Post post = new Post();
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setImgs(request.imgs());
        post.setUser(user);

        if (request.communityId() != null) {
            Community community = communityRepository.findById(request.communityId())
                    .orElseThrow(() -> new RuntimeException("Community not found"));
            boolean isModerator = community.getModerator().getId().equals(user.getId());
            boolean isMember = community.getMembers().contains(user);
            if (!isModerator && !isMember) {
                throw new ForbiddenException("You must join this community before posting in it.");
            }
            post.setCommunity(community);
        } else {
            post.setCommunity(null);
        }

        Post savedPost = postRepository.save(post);
        gamificationService.awardXp(user.getId(), XpConfig.POST_CREATED);

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
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
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
                .orElseThrow(() -> new  ResourceNotFoundException("Post not found"));
        Long postOwnerId = post.getUser().getId();
        User user = authenticatedUserService.getAuthenticatedUser();

        if (post.getLikes().contains(user)) {
            post.getLikes().remove(user);
            gamificationService.awardXp(postOwnerId, XpConfig.LIKE_REMOVED);
        } else {
            post.getLikes().add(user);
//            post.getUser().setXpPts(post.getUser().getXpPts() + 5);
            gamificationService.awardXp(postOwnerId, XpConfig.LIKE_RECEIVED);
        }
        postRepository.save(post);
    }
    @Override
    @Transactional
    public Page<PostResDto> getFeed(Pageable pageable) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        return postRepository.findFeedForUser(currentUser.getId(), pageable)
                .map(postMapper::toDto);
    }


}
