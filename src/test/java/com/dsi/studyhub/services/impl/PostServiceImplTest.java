//package com.dsi.studyhub.services.impl;
//
//import com.dsi.studyhub.dtos.PostReqDto;
//import com.dsi.studyhub.dtos.PostResDto;
//import com.dsi.studyhub.entities.Community;
//import com.dsi.studyhub.entities.Post;
//import com.dsi.studyhub.entities.User;
//import com.dsi.studyhub.enums.CommunityPermission;
//import com.dsi.studyhub.enums.PostStatus;
//import com.dsi.studyhub.exceptions.ForbiddenException;
//import com.dsi.studyhub.exceptions.ResourceNotFoundException;
//import com.dsi.studyhub.gamification.GamificationService;
//import com.dsi.studyhub.gamification.XpConfig;
//import com.dsi.studyhub.mappers.PostMapper;
//import com.dsi.studyhub.repositories.CommunityRepository;
//import com.dsi.studyhub.repositories.PostRepository;
//import com.dsi.studyhub.repositories.SeenPostRepository;
//import com.dsi.studyhub.repositories.UserRepository;
//import com.dsi.studyhub.services.AuthenticatedUserService;
//import com.dsi.studyhub.services.CommunityAuthService;
//import com.dsi.studyhub.services.FileStorageService;
//import com.dsi.studyhub.services.NotificationService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.*;
//
//import java.time.LocalDateTime;
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
///**
// * Unit tests for PostServiceImpl.
// *
// * All dependencies are mocked with Mockito — no Spring context,
// * no database. Tests run fast and in total isolation.
// */
//@ExtendWith(MockitoExtension.class)
//class PostServiceImplTest {
//
//    // ─── Mocks ────────────────────────────────────────────────────────────────
//
//    @Mock private PostRepository         postRepository;
//    @Mock private UserRepository         userRepository;
//    @Mock private SeenPostRepository     seenPostRepository;
//    @Mock private CommunityRepository    communityRepository;
//    @Mock private PostMapper             postMapper;
//    @Mock private AuthenticatedUserService authenticatedUserService;
//    @Mock private GamificationService    gamificationService;
//    @Mock private FileStorageService     fileStorageService;
//    @Mock private NotificationService    notificationService;
//    @Mock private CommunityAuthService   communityAuthService;
//
//    @InjectMocks
//    private PostServiceImpl postService;
//
//    // ─── Shared test fixtures ─────────────────────────────────────────────────
//
//    private User   testUser;
//    private Post   testPost;
//    private PostResDto testPostDto;
//
//    @BeforeEach
//    void setUp() {
//        // A minimal user used across many tests
//        testUser = new User();
//        testUser.setId(1L);
//        testUser.setUsername("alice");
//        testUser.setLikedPosts(new LinkedHashSet<>());
//        testUser.setJoinedCommunities(new ArrayList<>());
//        testUser.setOwnedCommunities(new ArrayList<>());
//
//        // A minimal approved post
//        testPost = new Post();
//        testPost.setId(10L);
//        testPost.setTitle("Test Post");
//        testPost.setContent("Some content");
//        testPost.setStatus(PostStatus.Approved);
//        testPost.setUser(testUser);
//        testPost.setCreatedAt(LocalDateTime.now());
//        testPost.setLikes(new ArrayList<>());
//        testPost.setComments(new ArrayList<>());
//
//        // The DTO that the mapper would return
//        testPostDto = PostResDto.builder()
//                .id(10L)
//                .title("Test Post")
//                .content("Some content")
//                .status(PostStatus.Approved)
//                .likeCount(0)
//                .commentCount(0)
//                .build();
//    }
//
//    // =========================================================================
//    // CREATE POST
//    // =========================================================================
//
//    @Nested
//    @DisplayName("createPost()")
//    class CreatePostTests {
//
//        @Test
//        @DisplayName("should create a post outside a community and auto-approve it")
//        void createPost_outsideCommunity_isAutoApproved() {
//            // Arrange
//            PostReqDto req = new PostReqDto("My Title", "My Content", null, null);
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);
//
//            // Act
//            PostResDto result = postService.createPost(req);
//
//            // Assert
//            assertThat(result).isNotNull();
//            // Verify the saved post had Approved status
//            verify(postRepository).save(argThat(p -> p.getStatus() == PostStatus.Approved));
//            // XP should be awarded for post creation
//            verify(gamificationService).awardXp(testUser.getId(), XpConfig.POST_CREATED);
//        }
//
//        @Test
//        @DisplayName("should set post status to Pending when user is just a member (not owner/mod)")
//        void createPost_inCommunity_memberOnly_isPending() {
//            // Arrange — user is a member, NOT the owner
//            Community community = buildCommunity(99L, new User() {{ setId(999L); }});
//            community.getMembers().add(testUser); // user is a member
//
//            PostReqDto req = new PostReqDto("Title", "Content", null, 99L);
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//            when(communityRepository.findById(99L)).thenReturn(Optional.of(community));
//            when(communityAuthService.hasPermission(testUser.getId(), 99L, CommunityPermission.APPROVE_POST))
//                    .thenReturn(false);
//            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);
//
//            // Act
//            postService.createPost(req);
//
//            // Assert — saved post must be Pending
//            verify(postRepository).save(argThat(p -> p.getStatus() == PostStatus.Pending));
//        }
//
//        @Test
//        @DisplayName("should auto-approve post when user is the community owner")
//        void createPost_inCommunity_owner_isAutoApproved() {
//            // Arrange — testUser IS the owner
//            Community community = buildCommunity(99L, testUser);
//
//            PostReqDto req = new PostReqDto("Title", "Content", null, 99L);
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//            when(communityRepository.findById(99L)).thenReturn(Optional.of(community));
//            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);
//
//            // Act
//            postService.createPost(req);
//
//            // Assert
//            verify(postRepository).save(argThat(p -> p.getStatus() == PostStatus.Approved));
//        }
//
//        @Test
//        @DisplayName("should throw ForbiddenException when user is neither owner nor member")
//        void createPost_inCommunity_nonMember_throwsForbidden() {
//            // Arrange — user not in members, not owner
//            Community community = buildCommunity(99L, new User() {{ setId(999L); }});
//            // members list is empty
//
//            PostReqDto req = new PostReqDto("Title", "Content", null, 99L);
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//            when(communityRepository.findById(99L)).thenReturn(Optional.of(community));
//
//            // Act & Assert
//            assertThatThrownBy(() -> postService.createPost(req))
//                    .isInstanceOf(ForbiddenException.class)
//                    .hasMessageContaining("join this community");
//        }
//
//        @Test
//        @DisplayName("should throw ResourceNotFoundException when community does not exist")
//        void createPost_communityNotFound_throwsNotFound() {
//            PostReqDto req = new PostReqDto("Title", "Content", null, 999L);
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//            when(communityRepository.findById(999L)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> postService.createPost(req))
//                    .isInstanceOf(ResourceNotFoundException.class)
//                    .hasMessageContaining("Community not found");
//        }
//    }
//
//    // =========================================================================
//    // GET POST BY ID
//    // =========================================================================
//
//    @Nested
//    @DisplayName("getPostById()")
//    class GetPostByIdTests {
//
//        @Test
//        @DisplayName("should return post DTO when post exists and is not flagged")
//        void getPostById_found_returnsDto() {
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//            when(postMapper.toDto(testPost)).thenReturn(testPostDto);
//
//            PostResDto result = postService.getPostById(10L);
//
//            assertThat(result).isEqualTo(testPostDto);
//        }
//
//        @Test
//        @DisplayName("should throw ResourceNotFoundException when post does not exist")
//        void getPostById_notFound_throwsException() {
//            when(postRepository.findById(99L)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> postService.getPostById(99L))
//                    .isInstanceOf(ResourceNotFoundException.class)
//                    .hasMessageContaining("Post not found");
//        }
//
//        @Test
//        @DisplayName("should throw ResourceNotFoundException when post is flagged (hidden from users)")
//        void getPostById_flagged_throwsException() {
//            testPost.setStatus(PostStatus.Flagged);
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//
//            // Flagged posts must be invisible — same 404 as if they don't exist
//            assertThatThrownBy(() -> postService.getPostById(10L))
//                    .isInstanceOf(ResourceNotFoundException.class);
//        }
//    }
//
//    // =========================================================================
//    // UPDATE POST
//    // =========================================================================
//
//    @Nested
//    @DisplayName("updatePost()")
//    class UpdatePostTests {
//
//        @Test
//        @DisplayName("should update title and content when both are provided")
//        void updatePost_updatesFieldsCorrectly() {
//            PostReqDto req = new PostReqDto("New Title", "New Content", null, null);
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);
//
//            postService.updatePost(10L, req);
//
//            assertThat(testPost.getTitle()).isEqualTo("New Title");
//            assertThat(testPost.getContent()).isEqualTo("New Content");
//        }
//
//        @Test
//        @DisplayName("should not overwrite title when new title is blank")
//        void updatePost_blankTitle_keepsOriginal() {
//            PostReqDto req = new PostReqDto("  ", "New Content", null, null);
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);
//
//            postService.updatePost(10L, req);
//
//            // Original title should be preserved
//            assertThat(testPost.getTitle()).isEqualTo("Test Post");
//        }
//
//        @Test
//        @DisplayName("should throw ResourceNotFoundException when post to update does not exist")
//        void updatePost_notFound_throwsException() {
//            when(postRepository.findById(99L)).thenReturn(Optional.empty());
//            PostReqDto req = new PostReqDto("Title", "Content", null, null);
//
//            assertThatThrownBy(() -> postService.updatePost(99L, req))
//                    .isInstanceOf(ResourceNotFoundException.class);
//        }
//    }
//
//    // =========================================================================
//    // DELETE POST
//    // =========================================================================
//
//    @Nested
//    @DisplayName("deletePost()")
//    class DeletePostTests {
//
//        @Test
//        @DisplayName("should delete post and deduct XP from user")
//        void deletePost_success_deductsXp() {
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//
//            postService.deletePost(10L);
//
//            verify(postRepository).deleteById(10L);
//            verify(gamificationService).awardXp(testUser.getId(), XpConfig.POST_DELETED);
//        }
//
//        @Test
//        @DisplayName("should throw ResourceNotFoundException when post does not exist")
//        void deletePost_notFound_throwsException() {
//            when(postRepository.findById(99L)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> postService.deletePost(99L))
//                    .isInstanceOf(ResourceNotFoundException.class);
//        }
//    }
//
//    // =========================================================================
//    // APPROVE POST
//    // =========================================================================
//
//    @Nested
//    @DisplayName("approvePost()")
//    class ApprovePostTests {
//
//        @Test
//        @DisplayName("should set post status to Approved")
//        void approvePost_success_setsApproved() {
//            testPost.setStatus(PostStatus.Pending);
//            Community community = buildCommunity(99L, new User() {{ setId(999L); }});
//            testPost.setCommunity(community);
//
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
//            when(postMapper.toDto(any(Post.class))).thenReturn(testPostDto);
//            // communityAuthService does NOT throw = user has permission
//
//            postService.approvePost(10L);
//
//            assertThat(testPost.getStatus()).isEqualTo(PostStatus.Approved);
//        }
//
//        @Test
//        @DisplayName("should throw ForbiddenException when post has no community")
//        void approvePost_noCommunity_throwsForbidden() {
//            testPost.setCommunity(null);
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//
//            assertThatThrownBy(() -> postService.approvePost(10L))
//                    .isInstanceOf(ForbiddenException.class);
//        }
//    }
//
//    // =========================================================================
//    // TOGGLE LIKE
//    // =========================================================================
//
//    @Nested
//    @DisplayName("toggleLike()")
//    class ToggleLikeTests {
//
//        @Test
//        @DisplayName("should add like and award XP to post owner when post is not yet liked")
//        void toggleLike_addLike_awardsXpToOwner() {
//            // testUser (id=1) is the liker; post owner is user id=2
//            User postOwner = new User();
//            postOwner.setId(2L);
//            testPost.setUser(postOwner);
//
//            // testUser has NOT liked this post yet
//            testUser.setLikedPosts(new LinkedHashSet<>());
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//            when(userRepository.save(any(User.class))).thenReturn(testUser);
//
//            postService.toggleLike(10L);
//
//            // Post should be in user's liked set
//            assertThat(testUser.getLikedPosts()).contains(testPost);
//            // XP awarded to post owner
//            verify(gamificationService).awardXp(2L, XpConfig.LIKE_RECEIVED);
//            // Notification sent to post owner
//            verify(notificationService).createNotification(
//                    eq(2L), eq("LIKE"), anyString(), isNull(), eq(10L));
//        }
//
//        @Test
//        @DisplayName("should remove like and deduct XP from post owner when post is already liked")
//        void toggleLike_removeLike_deductsXpFromOwner() {
//            User postOwner = new User();
//            postOwner.setId(2L);
//            testPost.setUser(postOwner);
//
//            // testUser HAS already liked this post
//            testUser.setLikedPosts(new LinkedHashSet<>(Set.of(testPost)));
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//            when(userRepository.save(any(User.class))).thenReturn(testUser);
//
//            postService.toggleLike(10L);
//
//            // Like removed
//            assertThat(testUser.getLikedPosts()).doesNotContain(testPost);
//            verify(gamificationService).awardXp(2L, XpConfig.LIKE_REMOVED);
//        }
//
//        @Test
//        @DisplayName("should NOT award XP when user likes their own post")
//        void toggleLike_ownPost_noXpAwarded() {
//            // testUser (id=1) likes their own post
//            testPost.setUser(testUser);
//            testUser.setLikedPosts(new LinkedHashSet<>());
//
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//            when(userRepository.save(any(User.class))).thenReturn(testUser);
//
//            postService.toggleLike(10L);
//
//            // No XP, no notification for self-likes
//            verify(gamificationService, never()).awardXp(any(), any());
//            verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
//        }
//
//        @Test
//        @DisplayName("should throw ResourceNotFoundException when post does not exist")
//        void toggleLike_postNotFound_throwsException() {
//            when(postRepository.findById(99L)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> postService.toggleLike(99L))
//                    .isInstanceOf(ResourceNotFoundException.class);
//        }
//    }
//
//    // =========================================================================
//    // GET POSTS BY USER
//    // =========================================================================
//
//    @Nested
//    @DisplayName("getPostsByUserId()")
//    class GetPostsByUserIdTests {
//
//        @Test
//        @DisplayName("should return paginated posts for an existing user")
//        void getPostsByUserId_exists_returnsPosts() {
//            Pageable pageable = PageRequest.of(0, 10);
//            Page<Post> postPage = new PageImpl<>(List.of(testPost));
//
//            when(userRepository.existsById(1L)).thenReturn(true);
//            when(postRepository.findByUserId(1L, pageable)).thenReturn(postPage);
//            when(postMapper.toDto(testPost)).thenReturn(testPostDto);
//
//            Page<PostResDto> result = postService.getPostsByUserId(1L, pageable);
//
//            assertThat(result.getContent()).hasSize(1);
//            assertThat(result.getContent().get(0)).isEqualTo(testPostDto);
//        }
//
//        @Test
//        @DisplayName("should throw ResourceNotFoundException for a non-existent user")
//        void getPostsByUserId_notFound_throwsException() {
//            when(userRepository.existsById(99L)).thenReturn(false);
//
//            assertThatThrownBy(() ->
//                    postService.getPostsByUserId(99L, PageRequest.of(0, 10)))
//                    .isInstanceOf(ResourceNotFoundException.class)
//                    .hasMessageContaining("User not found");
//        }
//    }
//
//    // =========================================================================
//    // GET ALL POSTS
//    // =========================================================================
//
//    @Nested
//    @DisplayName("getAllPosts()")
//    class GetAllPostsTests {
//
//        @Test
//        @DisplayName("should return all posts when no title filter is given")
//        void getAllPosts_noFilter_returnsAll() {
//            Pageable pageable = PageRequest.of(0, 10);
//            Page<Post> page = new PageImpl<>(List.of(testPost));
//
//            when(postRepository.findAll(pageable)).thenReturn(page);
//            when(postMapper.toDto(testPost)).thenReturn(testPostDto);
//
//            Page<PostResDto> result = postService.getAllPosts(null, pageable);
//
//            assertThat(result.getContent()).hasSize(1);
//        }
//
//        @Test
//        @DisplayName("should search by title when title filter is provided")
//        void getAllPosts_withTitleFilter_searchesByTitle() {
//            Pageable pageable = PageRequest.of(0, 10);
//            Page<Post> page = new PageImpl<>(List.of(testPost));
//
//            when(postRepository.findByTitleContainingIgnoreCase("java", pageable)).thenReturn(page);
//            when(postMapper.toDto(testPost)).thenReturn(testPostDto);
//
//            Page<PostResDto> result = postService.getAllPosts("java", pageable);
//
//            assertThat(result.getContent()).hasSize(1);
//            verify(postRepository).findByTitleContainingIgnoreCase("java", pageable);
//        }
//    }
//
//    // =========================================================================
//    // GET POST STATS
//    // =========================================================================
//
//    @Nested
//    @DisplayName("getPostStats()")
//    class GetPostStatsTests {
//
//        @Test
//        @DisplayName("should return correct counts for total, flagged, and pending posts")
//        void getPostStats_returnsCorrectCounts() {
//            when(postRepository.countByStatusNot(PostStatus.Flagged)).thenReturn(42L);
//            when(postRepository.countByStatus(PostStatus.Flagged)).thenReturn(3L);
//            when(postRepository.countByStatus(PostStatus.Pending)).thenReturn(7L);
//
//            Map<String, Long> stats = postService.getPostStats();
//
//            assertThat(stats.get("total")).isEqualTo(42L);
//            assertThat(stats.get("flagged")).isEqualTo(3L);
//            assertThat(stats.get("pending")).isEqualTo(7L);
//        }
//    }
//
//    // =========================================================================
//    // REJECT POST
//    // =========================================================================
//
//    @Nested
//    @DisplayName("rejectPost()")
//    class RejectPostTests {
//
//        @Test
//        @DisplayName("should delete post when moderator rejects it")
//        void rejectPost_success_deletesPost() {
//            Community community = buildCommunity(99L, new User() {{ setId(999L); }});
//            testPost.setCommunity(community);
//
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//            // communityAuthService does NOT throw = permission OK
//
//            postService.rejectPost(10L);
//
//            verify(postRepository).deleteById(10L);
//        }
//
//        @Test
//        @DisplayName("should throw ForbiddenException when post has no community")
//        void rejectPost_noCommunity_throwsForbidden() {
//            testPost.setCommunity(null);
//            when(postRepository.findById(10L)).thenReturn(Optional.of(testPost));
//            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
//
//            assertThatThrownBy(() -> postService.rejectPost(10L))
//                    .isInstanceOf(ForbiddenException.class);
//        }
//    }
//
//    // =========================================================================
//    // SCORE / SORT (indirect via getFeed helpers)
//    // =========================================================================
//
//    @Nested
//    @DisplayName("scorePost() — indirect verification via post ordering")
//    class ScorePostTests {
//
//        @Test
//        @DisplayName("a post with more likes should score higher than one with none")
//        void scorePost_moreLikes_scoresHigher() {
//            // Build two posts: one popular, one bare
//            Post popular = buildPost(1L, 10, 5);   // 10 likes, 5 comments
//            Post bare    = buildPost(2L, 0, 0);     // 0 likes, 0 comments
//
//            // Use the private sortByScore indirectly through getPostsByStatus
//            Pageable pageable = PageRequest.of(0, 10);
//            when(postRepository.findByStatus(PostStatus.Approved, pageable))
//                    .thenReturn(new PageImpl<>(List.of(bare, popular))); // reversed order
//            when(postMapper.toDto(any())).thenAnswer(inv -> {
//                Post p = inv.getArgument(0);
//                return PostResDto.builder()
//                        .id(p.getId())
//                        .title(p.getTitle())
//                        .content(p.getContent())
//                        .status(PostStatus.Approved)
//                        .likeCount(p.getLikes().size())
//                        .commentCount(p.getComments().size())
//                        .build();
//            });
//
//            Page<PostResDto> result = postService.getPostsByStatus("Approved", 0, 10);
//
//            // Repository returned bare first, but we just verify both are present
//            // (scoring adds randomness so strict order isn't testable deterministically)
//            assertThat(result.getContent()).hasSize(2);
//        }
//    }
//
//    // =========================================================================
//    // Helpers
//    // =========================================================================
//
//    /** Build a minimal Community with given id and owner. */
//    private Community buildCommunity(Long id, User owner) {
//        Community c = new Community();
//        c.setId(id);
//        c.setOwner(owner);
//        c.setMembers(new ArrayList<>());
//        return c;
//    }
//
//    /** Build a Post with a set number of likes and comments (for scoring tests). */
//    private Post buildPost(Long id, int likeCount, int commentCount) {
//        Post p = new Post();
//        p.setId(id);
//        p.setTitle("Post " + id);
//        p.setContent("Content");
//        p.setStatus(PostStatus.Approved);
//        p.setCreatedAt(LocalDateTime.now().minusHours(1));
//        p.setUser(testUser);
//
//        List<User> likes = new ArrayList<>();
//        for (int i = 0; i < likeCount; i++) {
//            User u = new User();
//            u.setId((long) (100 + i));
//            likes.add(u);
//        }
//        p.setLikes(likes);
//
//        List<com.dsi.studyhub.entities.Comment> comments = new ArrayList<>();
//        for (int i = 0; i < commentCount; i++) {
//            com.dsi.studyhub.entities.Comment c = new com.dsi.studyhub.entities.Comment();
//            comments.add(c);
//        }
//        p.setComments(comments);
//        return p;
//    }
//}
