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
import com.dsi.studyhub.services.NotificationService;
import com.dsi.studyhub.services.PostService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
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
    @Autowired private NotificationService notificationService;
    @Autowired private CommunityAuthService communityAuthService;

    // Post creation
    @Override
    @Transactional
    public PostResDto createPost(PostReqDto request) {
        User user = authenticatedUserService.getAuthenticatedUser();

        List<String> imgPaths = new ArrayList<>();
        if (request.imgs() != null) {
            for (MultipartFile file : request.imgs()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        imgPaths.add(fileStorageService.storeFile(file, "posts"));
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

            boolean canApprove = isOwner || communityAuthService.hasPermission(
                    user.getId(), community.getId(), CommunityPermission.APPROVE_POST);
            if (canApprove) {
                post.setStatus(PostStatus.Approved);
            }
        } else {
            post.setStatus(PostStatus.Approved);
        }

        Post savedPost = postRepository.save(post);
        gamificationService.awardXp(user.getId(), XpConfig.POST_CREATED);
        return postMapper.toDto(savedPost);
    }

    // Post reads
    @Override
    @Transactional
    public PostResDto getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        if (post.getStatus() == PostStatus.Flagged) {
            throw new ResourceNotFoundException("Post not found");
        }
        return postMapper.toDto(post);
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
        return postRepository
                .findByCommunityIdAndStatus(communityId, PostStatus.Approved, pageable)
                .map(postMapper::toDto);
    }

    @Override
    @Transactional
    public List<PostResDto> getPendingPosts(Long communityId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(
                currentUser.getId(), communityId, CommunityPermission.APPROVE_POST);

        return postRepository
                .findByCommunityIdAndStatus(communityId, PostStatus.Pending)
                .stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Page<PostResDto> getPostsByUserId(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) throw new ResourceNotFoundException("User not found");
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
    public Page<PostResDto> getPostsByStatus(String status, int page, int size) {
        PostStatus postStatus = PostStatus.valueOf(status);
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findByStatus(postStatus, pageable).map(postMapper::toDto);
    }

    @Override
    public Map<String, Long> getPostStats() {
        return Map.of(
                "total",   postRepository.countByStatusNot(PostStatus.Flagged),
                "flagged", postRepository.countByStatus(PostStatus.Flagged),
                "pending", postRepository.countByStatus(PostStatus.Pending)
        );
    }

    // Post updates
    @Override
    @Transactional
    public PostResDto updatePost(Long id, PostReqDto request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (request.title() != null && !request.title().isBlank()) {
            post.setTitle(request.title());
        }
        if (request.content() != null && !request.content().isBlank()) {
            post.setContent(request.content());
        }
        if (request.imgs() != null && !request.imgs().isEmpty()) {
            List<String> imgPaths = new ArrayList<>();
            for (MultipartFile file : request.imgs()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        imgPaths.add(fileStorageService.storeFile(file, "posts"));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to store image: " + e.getMessage());
                    }
                }
            }
            post.setImgs(imgPaths);
        }

        return postMapper.toDto(postRepository.save(post));
    }
    @Override
    @Transactional
    public PostResDto approvePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getCommunity() == null) {
            throw new ForbiddenException("Post does not belong to a community.");
        }

        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(
                currentUser.getId(), post.getCommunity().getId(), CommunityPermission.APPROVE_POST);

        post.setStatus(PostStatus.Approved);
        return postMapper.toDto(postRepository.save(post));
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
            if (!isOwnPost) {
                gamificationService.awardXp(postOwnerId, XpConfig.LIKE_RECEIVED);
                notificationService.createNotification(
                        postOwnerId,
                        "LIKE",
                        user.getUsername() + " liked your post",
                        null,
                        postId
                );
            }
        }
        userRepository.save(user);
    }

    // Post deletion and moderation
    @Override
    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        User user = authenticatedUserService.getAuthenticatedUser();
        postRepository.deleteById(id);
        gamificationService.awardXp(user.getId(), XpConfig.POST_DELETED);
    }

    @Override
    @Transactional
    public void rejectPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getCommunity() == null) {
            throw new ForbiddenException("Post does not belong to a community.");
        }

        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(
                currentUser.getId(), post.getCommunity().getId(), CommunityPermission.APPROVE_POST);

        postRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void moderatorDeletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        if (post.getCommunity() == null) {
            throw new ForbiddenException("Post does not belong to a community.");
        }

        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(
                currentUser.getId(), post.getCommunity().getId(), CommunityPermission.DELETE_POST);

        postRepository.deleteById(postId);
    }

    @Override
    @Transactional
    public void clearAllSeenPosts() {
        seenPostRepository.deleteAll();
    }

    // Feed composition
    @Override
    @Transactional
    public Page<PostResDto> getFeed(Pageable pageable) {
        // Builds a mixed feed from community, discovery, and friend posts with seen/unseen weighting.
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Long userId = currentUser.getId();

        Set<Long> seenIds = seenPostRepository.findSeenPostIdsByUserId(userId);

        List<String> userCategories = new ArrayList<>();
        currentUser.getJoinedCommunities().stream()
                .map(Community::getCategory)
                .filter(cat -> cat != null && !cat.trim().isEmpty())
                .forEach(cat -> { if (!userCategories.contains(cat)) userCategories.add(cat); });
        currentUser.getOwnedCommunities().stream()
                .map(Community::getCategory)
                .filter(cat -> cat != null && !cat.trim().isEmpty())
                .forEach(cat -> { if (!userCategories.contains(cat)) userCategories.add(cat); });

        List<Post> communityPosts = postRepository.findCommunityFeedPosts(userId);
        List<Post> discoveryPosts = userCategories.isEmpty()
                ? postRepository.findAllApprovedExcludingUser(userId)
                : postRepository.findDiscoveryPostsByCategories(userId, userCategories);

        Set<Long> friendPostIds = new LinkedHashSet<>();
        List<Post> friendPosts = new ArrayList<>();
        for (Post p : postRepository.findPostsByFriendsAsRequester(userId)) {
            if (friendPostIds.add(p.getId())) friendPosts.add(p);
        }
        for (Post p : postRepository.findPostsByFriendsAsAddressee(userId)) {
            if (friendPostIds.add(p.getId())) friendPosts.add(p);
        }

        if (communityPosts.isEmpty() && discoveryPosts.isEmpty()) {
            discoveryPosts = postRepository.findAllApprovedExcludingUser(userId);
        }

        List<Post> unseenCommunity = communityPosts.stream().filter(p -> !seenIds.contains(p.getId())).collect(Collectors.toList());
        List<Post> seenCommunity   = communityPosts.stream().filter(p ->  seenIds.contains(p.getId())).collect(Collectors.toList());
        List<Post> unseenDiscovery = discoveryPosts.stream().filter(p -> !seenIds.contains(p.getId())).collect(Collectors.toList());
        List<Post> seenDiscovery   = discoveryPosts.stream().filter(p ->  seenIds.contains(p.getId())).collect(Collectors.toList());
        List<Post> unseenFriends   = friendPosts.stream().filter(p -> !seenIds.contains(p.getId())).collect(Collectors.toList());
        List<Post> seenFriends     = friendPosts.stream().filter(p ->  seenIds.contains(p.getId())).collect(Collectors.toList());

        List<Post> bucketA = sortByScore(unseenCommunity);
        List<Post> bucketB = sortByScore(unseenDiscovery);
        List<Post> bucketC = sortByScore(seenCommunity);
        List<Post> bucketD = sortByScore(seenDiscovery);
        List<Post> bucketE = sortByScore(unseenFriends);
        List<Post> bucketF = sortByScore(seenFriends);

        List<Post> merged = new ArrayList<>();
        Set<Long> addedIds = new LinkedHashSet<>();

        int ci = 0, di = 0;
        while (ci < bucketA.size() || di < bucketB.size()) {
            for (int i = 0; i < 7 && ci < bucketA.size(); i++, ci++) {
                if (addedIds.add(bucketA.get(ci).getId())) merged.add(bucketA.get(ci));
            }
            for (int i = 0; i < 3 && di < bucketB.size(); i++, di++) {
                if (addedIds.add(bucketB.get(di).getId())) merged.add(bucketB.get(di));
            }
        }

        List<Post> interleaved = new ArrayList<>();
        int fi = 0;
        for (int i = 0; i < merged.size(); i++) {
            interleaved.add(merged.get(i));
            if ((i + 1) % 5 == 0 && fi < bucketE.size()) {
                Post fp = bucketE.get(fi++);
                if (addedIds.add(fp.getId())) interleaved.add(fp);
            }
        }
        while (fi < bucketE.size()) {
            Post fp = bucketE.get(fi++);
            if (addedIds.add(fp.getId())) interleaved.add(fp);
        }

        for (Post p : bucketC) { if (addedIds.add(p.getId())) interleaved.add(p); }
        for (Post p : bucketD) { if (addedIds.add(p.getId())) interleaved.add(p); }
        for (Post p : bucketF) { if (addedIds.add(p.getId())) interleaved.add(p); }
        
        if (interleaved.isEmpty()) {
            sortByScore(postRepository.findAllApprovedExcludingUser(userId))
                    .forEach(p -> { if (addedIds.add(p.getId())) interleaved.add(p); });
        }

        sortByScore(postRepository.findAllApprovedExcludingUser(userId))
                .forEach(p -> { if (addedIds.add(p.getId())) interleaved.add(p); });

        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), interleaved.size());

        if (start >= interleaved.size()) {
            return new PageImpl<>(List.of(), pageable, interleaved.size());
        }

        List<PostResDto> pageDtos = interleaved.subList(start, end).stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(pageDtos, pageable, interleaved.size());
    }

    // Seen posts
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

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldSeenPosts() {
        seenPostRepository.deleteOlderThan(LocalDateTime.now().minusDays(30));
    }

    // Scoring helpers
    private double scorePost(Post post) {
        int likes    = post.getLikes()    == null ? 0 : post.getLikes().size();
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
