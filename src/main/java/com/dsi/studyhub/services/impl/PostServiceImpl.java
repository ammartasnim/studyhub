package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.PostReqDto;
import com.dsi.studyhub.dtos.PostResDto;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.entities.SeenPost;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.CommunityPermission;
import com.dsi.studyhub.enums.PostStatus;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.gamification.GamificationService;
import com.dsi.studyhub.gamification.XpConfig;
import com.dsi.studyhub.mappers.PostMapper;
import com.dsi.studyhub.repositories.CommunityRepository;
import com.dsi.studyhub.repositories.PostRepository;
import com.dsi.studyhub.repositories.SeenPostRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.CommunityAuthService;
import com.dsi.studyhub.services.FileStorageService;
import com.dsi.studyhub.services.PostService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    @Autowired private PostRepository postRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SeenPostRepository seenPostRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private PostMapper postMapper;
    @Autowired private AuthenticatedUserService authenticatedUserService;
    @Autowired private GamificationService gamificationService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private CommunityAuthService communityAuthService;

    @Override
    @Transactional
    public PostResDto createPost(PostReqDto request) {
        User user = authenticatedUserService.getAuthenticatedUser();
        List<String> imgPaths = new ArrayList<>();
        if (request.imgs() != null && !request.imgs().isEmpty()) {
            for (MultipartFile file : request.imgs()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String path = fileStorageService.storeFile(file, "posts");
                        imgPaths.add(path);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to store image: " + e.getMessage());
                    }
                }
            }
        }

        Post post = new Post();
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setImgs(imgPaths);
        post.setUser(user);

        if (request.communityId() != null) {
            Community community = communityRepository.findById(request.communityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

            boolean isOwner = community.getOwner().getId().equals(user.getId());
            boolean isMember = community.getMembers().stream()
                    .anyMatch(m -> m.getId().equals(user.getId()));

            if (!isOwner && !isMember) {
                throw new ForbiddenException("You must join this community before posting in it.");
            }

            post.setCommunity(community);

            if (isOwner) {
                post.setStatus(PostStatus.Approved);
            }
        } else {
            post.setCommunity(null);
        }

        Post savedPost = postRepository.save(post);
        gamificationService.awardXp(user.getId(), XpConfig.POST_CREATED);
        return postMapper.toDto(savedPost);
    }

    @Override
    @Transactional
    public PostResDto getPostById(Long id) {
        return postRepository.findById(id)
                .map(postMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    @Override
    @Transactional
    public Page<PostResDto> getAllPosts(String title, Pageable pageable) {
        Page<Post> posts = (title != null && !title.isEmpty())
                ? postRepository.findByTitleContainingIgnoreCase(title, pageable)
                : postRepository.findAll(pageable);
        return posts.map(postMapper::toDto);
    }

    @Override
    @Transactional
    public Page<PostResDto> getPostsByCommunity(Long communityId, Pageable pageable) {
        return postRepository.findByCommunityId(communityId, pageable).map(postMapper::toDto);
    }

    @Override
    @Transactional
    public Page<PostResDto> getPostsByUserId(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) throw new RuntimeException("User not found");
        return postRepository.findByUserId(userId, pageable).map(postMapper::toDto);
    }

    @Override
    @Transactional
    public Page<PostResDto> getMyPosts(Pageable pageable) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        return postRepository.findByUserId(currentUser.getId(), pageable).map(postMapper::toDto);
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
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User user = authenticatedUserService.getAuthenticatedUser();
        postRepository.deleteById(id);
        gamificationService.awardXp(user.getId(), XpConfig.POST_DELETED);
    }

    @Override
    @Transactional
    public void moderatorDeletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Long communityId = post.getCommunity() != null ? post.getCommunity().getId() : null;
        if (communityId == null) {
            throw new ForbiddenException("Post does not belong to a community.");
        }
        communityAuthService.requireOwnerOrPermission(currentUser.getId(), communityId, CommunityPermission.DELETE_POST);
        postRepository.deleteById(postId);
    }

    @Override
    @Transactional
    public void toggleLike(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        User user = authenticatedUserService.getAuthenticatedUser();
        Long postOwnerId = post.getUser().getId();
        boolean isOwnPost = postOwnerId.equals(user.getId());

        boolean alreadyLiked = user.getLikedPosts().stream()
                .anyMatch(p -> p.getId().equals(postId));

        if (alreadyLiked) {
            user.getLikedPosts().removeIf(p -> p.getId().equals(postId));
            if (!isOwnPost) gamificationService.awardXp(postOwnerId, XpConfig.LIKE_REMOVED);
        } else {
            user.getLikedPosts().add(post);
            if (!isOwnPost) gamificationService.awardXp(postOwnerId, XpConfig.LIKE_RECEIVED);
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public Page<PostResDto> getFeed(Pageable pageable) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Long userId = currentUser.getId();

        Set<Long> seenIds = seenPostRepository.findSeenPostIdsByUserId(userId);

        List<String> userCategories = currentUser.getJoinedCommunities().stream()
                .map(Community::getCategory)
                .filter(cat -> cat != null && !cat.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        currentUser.getOwnedCommunities().stream()
                .map(Community::getCategory)
                .filter(cat -> cat != null && !cat.trim().isEmpty())
                .forEach(cat -> { if (!userCategories.contains(cat)) userCategories.add(cat); });

        List<Post> communityPosts = postRepository.findCommunityFeedPosts(userId);
        List<Post> discoveryPosts = userCategories.isEmpty()
                ? postRepository.findAllApprovedExcludingUser(userId)
                : postRepository.findDiscoveryPostsByCategories(userId, userCategories);

        List<Post> unseenCommunity = communityPosts.stream().filter(p -> !seenIds.contains(p.getId())).collect(Collectors.toList());
        List<Post> seenCommunity   = communityPosts.stream().filter(p ->  seenIds.contains(p.getId())).collect(Collectors.toList());
        List<Post> unseenDiscovery = discoveryPosts.stream().filter(p -> !seenIds.contains(p.getId())).collect(Collectors.toList());
        List<Post> seenDiscovery   = discoveryPosts.stream().filter(p ->  seenIds.contains(p.getId())).collect(Collectors.toList());

        List<Post> sorted1 = sortByScore(unseenCommunity);
        List<Post> sorted2 = sortByScore(unseenDiscovery);
        List<Post> sorted3 = sortByScore(seenCommunity);
        List<Post> sorted4 = sortByScore(seenDiscovery);

        List<Post> merged = new ArrayList<>();
        Set<Long> addedIds = new LinkedHashSet<>();

        java.util.function.Consumer<List<Post>> addAll = list -> {
            for (Post p : list) {
                if (addedIds.add(p.getId())) merged.add(p);
            }
        };

        int ci = 0, di = 0;
        while (ci < sorted1.size() || di < sorted2.size()) {
            for (int i = 0; i < 7 && ci < sorted1.size(); i++, ci++) {
                if (addedIds.add(sorted1.get(ci).getId())) merged.add(sorted1.get(ci));
            }
            for (int i = 0; i < 3 && di < sorted2.size(); i++, di++) {
                if (addedIds.add(sorted2.get(di).getId())) merged.add(sorted2.get(di));
            }
        }

        addAll.accept(sorted3);
        addAll.accept(sorted4);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), merged.size());

        if (start >= merged.size()) {
            return new PageImpl<>(List.of(), pageable, merged.size());
        }

        List<PostResDto> pageDtos = merged.subList(start, end).stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(pageDtos, pageable, merged.size());
    }

    @Override
    @Transactional
    public void markPostsSeen(List<Long> postIds) {
        User user = authenticatedUserService.getAuthenticatedUser();
        for (Long postId : postIds) {
            postRepository.findById(postId).ifPresent(post -> {
                Optional<SeenPost> existing = seenPostRepository.findByUserIdAndPostId(user.getId(), postId);
                if (existing.isPresent()) {
                    SeenPost sp = existing.get();
                    sp.setSeenCount(sp.getSeenCount() + 1);
                    seenPostRepository.save(sp);
                } else {
                    SeenPost sp = new SeenPost();
                    sp.setUser(user);
                    sp.setPost(post);
                    seenPostRepository.save(sp);
                }
            });
        }
    }

    @Override
    @Transactional
    public PostResDto approvePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        if (post.getCommunity() == null) throw new ForbiddenException("Post does not belong to a community");
        if (!post.getCommunity().getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the community owner can approve posts");
        }
        post.setStatus(PostStatus.Approved);
        return postMapper.toDto(postRepository.save(post));
    }

    @Override
    @Transactional
    public PostResDto flagPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        if (post.getCommunity() == null) throw new ForbiddenException("Post does not belong to a community");
        if (!post.getCommunity().getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the community owner can flag posts");
        }
        post.setStatus(PostStatus.Flagged);
        return postMapper.toDto(postRepository.save(post));
    }

    @Override
    public Page<PostResDto> getPostsByStatus(String status, int page, int size) {
        PostStatus postStatus = PostStatus.valueOf(status);
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findByStatus(postStatus, pageable).map(postMapper::toDto);
    }

    @Override
    public Map<String, Long> getPostStats() {
        return Map.of(
                "total",   postRepository.count(),
                "flagged", postRepository.countByStatus(PostStatus.Flagged),
                "pending", postRepository.countByStatus(PostStatus.Pending)
        );
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldSeenPosts() {
        seenPostRepository.deleteOlderThan(LocalDateTime.now().minusDays(30));
    }

    private double scorePost(Post post) {
        int likes = post.getLikes() == null ? 0 : post.getLikes().size();
        int comments = post.getComments() == null ? 0 : post.getComments().size();
        long ageInHours = ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now());
        double recencyScore = Math.max(0, 100 - (ageInHours * 0.5));
        double score = (likes * 2.0) + (comments * 1.5) + recencyScore;
        score += score * (Math.random() * 0.1 - 0.05);
        return score;
    }

    private List<Post> sortByScore(List<Post> posts) {
        return posts.stream()
                .sorted(Comparator.comparingDouble(this::scorePost).reversed())
                .collect(Collectors.toList());
    }
}