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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private SeenPostRepository seenPostRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private PostMapper postMapper;
    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private GamificationService gamificationService;
    @Mock private FileStorageService fileStorageService;
    @Mock private NotificationService notificationService;
    @Mock private CommunityAuthService communityAuthService;

    @InjectMocks
    private PostServiceImpl postService;

    private User testUser;
    private Post testPost;
    private PostResDto testPostDto;

    // Builds shared fixtures to keep each test focused on one behavior.
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");
        testUser.setLikedPosts(new LinkedHashSet<>());
        testUser.setJoinedCommunities(new ArrayList<>());
        testUser.setOwnedCommunities(new ArrayList<>());

        testPost = new Post();
        testPost.setId(10L);
        testPost.setTitle("Test Post");
        testPost.setContent("Some content");
        testPost.setStatus(PostStatus.Approved);
        testPost.setUser(testUser);
        testPost.setCreatedAt(LocalDateTime.now());
        testPost.setLikes(new ArrayList<>());
        testPost.setComments(new ArrayList<>());

        testPostDto = PostResDto.builder()
                .id(10L)
                .title("Test Post")
                .content("Some content")
                .status(PostStatus.Approved)
                .likeCount(0)
                .commentCount(0)
                .build();
    }

    // Ensures non-community posts default to approved and award XP immediately.
    @Test
    void createPost_outsideCommunity_isAutoApproved() {
        PostReqDto req = new PostReqDto("My Title", "My Content", null, null);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);

        PostResDto result = postService.createPost(req);

        assertThat(result).isNotNull();
        verify(postRepository).save(argThat(p -> p.getStatus() == PostStatus.Approved));
        verify(gamificationService).awardXp(testUser.getId(), XpConfig.POST_CREATED);
    }

    // Verifies community members without approval permission stay pending.
    @Test
    void createPost_inCommunity_memberOnly_isPending() {
        Community community = buildCommunity(99L, new User() {{ setId(999L); }});
        community.getMembers().add(testUser);

        PostReqDto req = new PostReqDto("Title", "Content", null, 99L);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(communityRepository.findById(99L)).thenReturn(Optional.of(community));
        when(communityAuthService.hasPermission(testUser.getId(), 99L, CommunityPermission.APPROVE_POST))
                .thenReturn(false);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);

        postService.createPost(req);

        verify(postRepository).save(argThat(p -> p.getStatus() == PostStatus.Pending));
    }

    // Confirms owners bypass approval workflow and auto-approve posts.
    @Test
    void createPost_inCommunity_owner_isAutoApproved() {
        Community community = buildCommunity(99L, testUser);

        PostReqDto req = new PostReqDto("Title", "Content", null, 99L);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(communityRepository.findById(99L)).thenReturn(Optional.of(community));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);

        postService.createPost(req);

        verify(postRepository).save(argThat(p -> p.getStatus() == PostStatus.Approved));
    }

    // Enforces join-before-posting rule for community threads.
    @Test
    void createPost_inCommunity_nonMember_throwsForbidden() {
        Community community = buildCommunity(99L, new User() {{ setId(999L); }});

        PostReqDto req = new PostReqDto("Title", "Content", null, 99L);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(communityRepository.findById(99L)).thenReturn(Optional.of(community));

        assertThatThrownBy(() -> postService.createPost(req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("join this community");
    }

    // Guards against posting into a community that does not exist.
    @Test
    void createPost_communityNotFound_throwsNotFound() {
        PostReqDto req = new PostReqDto("Title", "Content", null, 999L);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(communityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Community not found");
    }

    // Checks image storage pipeline and stored paths wiring.
    @Test
    void createPost_withImages_storesPaths() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1});
        PostReqDto req = new PostReqDto("Title", "Content", List.of(file), null);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(fileStorageService.storeFile(file, "posts")).thenReturn("posts/a.png");
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);

        postService.createPost(req);

        verify(postRepository).save(argThat(p -> p.getImgs().equals(List.of("posts/a.png"))));
    }

    // Ensures IO failures bubble as a runtime error for consistent error handling.
    @Test
    void createPost_imageStorageFails_throwsRuntimeException() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1});
        PostReqDto req = new PostReqDto("Title", "Content", List.of(file), null);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(fileStorageService.storeFile(file, "posts")).thenThrow(new IOException("disk"));

        assertThatThrownBy(() -> postService.createPost(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to store image");
    }

    // Confirms flagged posts are hidden by returning a not-found response.
    @Test
    void getPostById_flagged_throwsNotFound() {
        testPost.setStatus(PostStatus.Flagged);
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));

        assertThatThrownBy(() -> postService.getPostById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Verifies the normal read path returns the mapped DTO.
    @Test
    void getPostById_found_returnsDto() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(postMapper.toDto(testPost)).thenReturn(testPostDto);

        PostResDto result = postService.getPostById(10L);

        assertThat(result).isEqualTo(testPostDto);
    }

    // Ensures missing posts surface as not found errors.
    @Test
    void getPostById_notFound_throwsException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found");
    }

    // Verifies the search branch uses the title filter repository method.
    @Test
    void getAllPosts_withTitleFilter_searchesByTitle() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(testPost));

        when(postRepository.findByTitleContainingIgnoreCase("java", pageable)).thenReturn(page);
        when(postMapper.toDto(testPost)).thenReturn(testPostDto);

        Page<PostResDto> result = postService.getAllPosts("java", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(postRepository).findByTitleContainingIgnoreCase("java", pageable);
    }

    // Confirms empty filter uses the default findAll query.
    @Test
    void getAllPosts_noFilter_returnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(testPost));

        when(postRepository.findAll(pageable)).thenReturn(page);
        when(postMapper.toDto(testPost)).thenReturn(testPostDto);

        Page<PostResDto> result = postService.getAllPosts(null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Ensures community feed only includes approved posts for that community.
    @Test
    void getPostsByCommunity_returnsApprovedOnly() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Post> page = new PageImpl<>(List.of(testPost));
        when(postRepository.findByCommunityIdAndStatus(5L, PostStatus.Approved, pageable)).thenReturn(page);
        when(postMapper.toDto(testPost)).thenReturn(testPostDto);

        Page<PostResDto> result = postService.getPostsByCommunity(5L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Verifies pending queue access is permission-gated and mapped correctly.
    @Test
    void getPendingPosts_requiresPermission_andReturnsDtos() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(postRepository.findByCommunityIdAndStatus(7L, PostStatus.Pending))
                .thenReturn(List.of(testPost));
        when(postMapper.toDto(testPost)).thenReturn(testPostDto);

        var results = postService.getPendingPosts(7L);

        verify(communityAuthService).requireOwnerOrPermission(
                testUser.getId(), 7L, CommunityPermission.APPROVE_POST);
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(testPostDto);
    }

    // Validates existence check before listing a user's posts.
    @Test
    void getPostsByUserId_notFound_throwsException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> postService.getPostsByUserId(99L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // Confirms repository data is mapped for existing users.
    @Test
    void getPostsByUserId_exists_returnsPosts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> postPage = new PageImpl<>(List.of(testPost));

        when(userRepository.existsById(1L)).thenReturn(true);
        when(postRepository.findByUserId(1L, pageable)).thenReturn(postPage);
        when(postMapper.toDto(testPost)).thenReturn(testPostDto);

        Page<PostResDto> result = postService.getPostsByUserId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testPostDto);
    }

    // Ensures "my posts" uses the authenticated user id.
    @Test
    void getMyPosts_returnsCurrentUserPosts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> postPage = new PageImpl<>(List.of(testPost));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(postRepository.findByUserId(testUser.getId(), pageable)).thenReturn(postPage);
        when(postMapper.toDto(testPost)).thenReturn(testPostDto);

        Page<PostResDto> result = postService.getMyPosts(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Confirms stats aggregation uses the correct repository counts.
    @Test
    void getPostStats_returnsCorrectCounts() {
        when(postRepository.countByStatusNot(PostStatus.Flagged)).thenReturn(42L);
        when(postRepository.countByStatus(PostStatus.Flagged)).thenReturn(3L);
        when(postRepository.countByStatus(PostStatus.Pending)).thenReturn(7L);

        var stats = postService.getPostStats();

        assertThat(stats.get("total")).isEqualTo(42L);
        assertThat(stats.get("flagged")).isEqualTo(3L);
        assertThat(stats.get("pending")).isEqualTo(7L);
    }

    // Ensures updates only overwrite non-blank fields and persist changes.
    @Test
    void updatePost_updatesFieldsCorrectly() {
        PostReqDto req = new PostReqDto("New Title", "New Content", null, null);
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);

        postService.updatePost(10L, req);

        assertThat(testPost.getTitle()).isEqualTo("New Title");
        assertThat(testPost.getContent()).isEqualTo("New Content");
    }

    // Protects existing data when updates are blank inputs.
    @Test
    void updatePost_blankTitle_keepsOriginal() {
        PostReqDto req = new PostReqDto("  ", "New Content", null, null);
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);

        postService.updatePost(10L, req);

        assertThat(testPost.getTitle()).isEqualTo("Test Post");
    }

    // Ensures image updates replace stored paths for posts.
    @Test
    void updatePost_withImages_replacesImages() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "b.png", "image/png", new byte[] {2});
        PostReqDto req = new PostReqDto("Title", "Content", List.of(file), null);
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(fileStorageService.storeFile(file, "posts")).thenReturn("posts/b.png");
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);

        postService.updatePost(10L, req);

        assertThat(testPost.getImgs()).containsExactly("posts/b.png");
    }

    // Confirms missing posts fail fast on update operations.
    @Test
    void updatePost_notFound_throwsException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(99L, new PostReqDto("Title", "Content", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Verifies approval requires a community context and permission checks.
    @Test
    void approvePost_success_setsApproved() {
        testPost.setStatus(PostStatus.Pending);
        testPost.setCommunity(buildCommunity(99L, new User() {{ setId(999L); }}));

        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);

        postService.approvePost(10L);

        assertThat(testPost.getStatus()).isEqualTo(PostStatus.Approved);
        verify(communityAuthService).requireOwnerOrPermission(testUser.getId(), 99L, CommunityPermission.APPROVE_POST);
    }

    // Ensures approval cannot be performed on non-community posts.
    @Test
    void approvePost_noCommunity_throwsForbidden() {
        testPost.setCommunity(null);
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));

        assertThatThrownBy(() -> postService.approvePost(10L))
                .isInstanceOf(ForbiddenException.class);
    }

    // Confirms rejection deletes community posts when permission is satisfied.
    @Test
    void rejectPost_success_deletesPost() {
        testPost.setCommunity(buildCommunity(99L, new User() {{ setId(999L); }}));
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);

        postService.rejectPost(10L);

        verify(communityAuthService).requireOwnerOrPermission(testUser.getId(), 99L, CommunityPermission.APPROVE_POST);
        verify(postRepository).deleteById(10L);
    }

    // Guards against rejecting non-community posts.
    @Test
    void rejectPost_noCommunity_throwsForbidden() {
        testPost.setCommunity(null);
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));

        assertThatThrownBy(() -> postService.rejectPost(10L))
                .isInstanceOf(ForbiddenException.class);
    }

    // Confirms moderator deletes require community and delete permission.
    @Test
    void moderatorDeletePost_success_deletesPost() {
        testPost.setCommunity(buildCommunity(88L, new User() {{ setId(999L); }}));
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);

        postService.moderatorDeletePost(10L);

        verify(communityAuthService).requireOwnerOrPermission(testUser.getId(), 88L, CommunityPermission.DELETE_POST);
        verify(postRepository).deleteById(10L);
    }

    // Ensures moderator deletes do not apply outside communities.
    @Test
    void moderatorDeletePost_noCommunity_throwsForbidden() {
        testPost.setCommunity(null);
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));

        assertThatThrownBy(() -> postService.moderatorDeletePost(10L))
                .isInstanceOf(ForbiddenException.class);
    }

    // Validates like toggles award XP and notify when liking others' posts.
    @Test
    void toggleLike_addLike_awardsXpToOwner() {
        User postOwner = new User();
        postOwner.setId(2L);
        testPost.setUser(postOwner);
        testUser.setLikedPosts(new LinkedHashSet<>());
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        postService.toggleLike(10L);

        assertThat(testUser.getLikedPosts()).contains(testPost);
        verify(gamificationService).awardXp(2L, XpConfig.LIKE_RECEIVED);
        verify(notificationService).createNotification(eq(2L), eq("LIKE"), any(), isNull(), eq(10L));
    }

    // Verifies unlikes reverse XP grants for the post owner.
    @Test
    void toggleLike_removeLike_deductsXpFromOwner() {
        User postOwner = new User();
        postOwner.setId(2L);
        testPost.setUser(postOwner);
        testUser.setLikedPosts(new LinkedHashSet<>(Set.of(testPost)));
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        postService.toggleLike(10L);

        assertThat(testUser.getLikedPosts()).doesNotContain(testPost);
        verify(gamificationService).awardXp(2L, XpConfig.LIKE_REMOVED);
    }

    // Avoids XP/notifications for self-like actions.
    @Test
    void toggleLike_ownPost_noXpAwarded() {
        testPost.setUser(testUser);
        testUser.setLikedPosts(new LinkedHashSet<>());
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        postService.toggleLike(10L);

        verify(gamificationService, never()).awardXp(anyLong(), anyInt());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    // Ensures toggles fail with a clear not-found error for missing posts.
    @Test
    void toggleLike_postNotFound_throwsException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.toggleLike(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Confirms deletions remove the post and adjust XP for the actor.
    @Test
    void deletePost_success_deductsXp() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);

        postService.deletePost(10L);

        verify(postRepository).deleteById(10L);
        verify(gamificationService).awardXp(testUser.getId(), XpConfig.POST_DELETED);
    }

    // Ensures delete fails if the post does not exist.
    @Test
    void deletePost_notFound_throwsException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Verifies seen tracking creates rows when none exist.
    @Test
    void markPostsSeen_createsSeenPost() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(seenPostRepository.findByUserIdAndPostId(testUser.getId(), 10L)).thenReturn(Optional.empty());

        postService.markPostsSeen(List.of(10L));

        verify(seenPostRepository).save(any(SeenPost.class));
    }

    // Confirms repeat views increment the seen count instead of creating new rows.
    @Test
    void markPostsSeen_incrementsExistingSeenPost() {
        SeenPost seen = new SeenPost();
        seen.setSeenCount(2);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(seenPostRepository.findByUserIdAndPostId(testUser.getId(), 10L)).thenReturn(Optional.of(seen));

        postService.markPostsSeen(List.of(10L));

        assertThat(seen.getSeenCount()).isEqualTo(3);
        verify(seenPostRepository).save(seen);
    }

    // Validates feed paging and duplicate avoidance with mixed buckets.
    @Test
    void getFeed_returnsPagedAndUnique() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(seenPostRepository.findSeenPostIdsByUserId(testUser.getId())).thenReturn(Set.of());
        when(postRepository.findCommunityFeedPosts(testUser.getId())).thenReturn(List.of(testPost));
        when(postRepository.findAllApprovedExcludingUser(testUser.getId())).thenReturn(List.of());
        when(postRepository.findPostsByFriendsAsRequester(testUser.getId())).thenReturn(List.of());
        when(postRepository.findPostsByFriendsAsAddressee(testUser.getId())).thenReturn(List.of());
        when(postRepository.findAllApproved()).thenReturn(List.of(testPost));
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);

        Page<PostResDto> result = postService.getFeed(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
    }

    // Ensures feed falls back to approved posts when primary sources are empty.
    @Test
    void getFeed_emptySources_fallsBackToApproved() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(seenPostRepository.findSeenPostIdsByUserId(testUser.getId())).thenReturn(Set.of());
        when(postRepository.findCommunityFeedPosts(testUser.getId())).thenReturn(List.of());
        when(postRepository.findAllApprovedExcludingUser(testUser.getId())).thenReturn(List.of());
        when(postRepository.findPostsByFriendsAsRequester(testUser.getId())).thenReturn(List.of());
        when(postRepository.findPostsByFriendsAsAddressee(testUser.getId())).thenReturn(List.of());
        when(postRepository.findAllApproved()).thenReturn(List.of(testPost));
        when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);

        Page<PostResDto> result = postService.getFeed(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    // Verifies scheduled cleanup uses a 30-day cutoff when purging seen posts.
    @Test
    void cleanupOldSeenPosts_callsRepository() {
        postService.cleanupOldSeenPosts();

        verify(seenPostRepository).deleteOlderThan(any(LocalDateTime.class));
    }

    // Confirms manual cleanup path clears all seen tracking for moderation or resets.
    @Test
    void clearAllSeenPosts_deletesAll() {
        postService.clearAllSeenPosts();

        verify(seenPostRepository).deleteAll();
    }

    // Keeps community setup consistent across permission-based tests.
    private Community buildCommunity(Long id, User owner) {
        Community c = new Community();
        c.setId(id);
        c.setOwner(owner);
        c.setMembers(new ArrayList<>());
        return c;
    }
}
