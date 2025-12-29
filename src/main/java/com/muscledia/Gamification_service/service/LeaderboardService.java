package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.dto.response.LeaderboardPageResponse;
import com.muscledia.Gamification_service.dto.response.LeaderboardResponse;
import com.muscledia.Gamification_service.mapper.LeaderboardMapper;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling leaderboard operations.
 * Provides leaderboard data in DTO format suitable for frontend consumption.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final UserGamificationProfileRepository userProfileRepository;
    private final LeaderboardMapper leaderboardMapper;

    private static final int DEFAULT_NEARBY_RANGE = 5; // 5 users above, 5 below

    // ===========================================
    // POINTS LEADERBOARD
    // ===========================================

    /**
     * Get points leaderboard with user context and pagination
     */
    public LeaderboardPageResponse getPointsLeaderboardWithContext(
            Long currentUserId,
            int page,
            int size) {

        log.info("Getting points leaderboard - page: {}, size: {}, user: {}",
                page, size, currentUserId);

        // Get requested page of top users
        Pageable pageable = PageRequest.of(page, size);
        List<UserGamificationProfile> pageUsers = userProfileRepository
                .findAllByOrderByPointsDesc(pageable);

        // Map to responses with correct ranks
        List<LeaderboardResponse> leaderboardResponses = mapWithRanks(pageUsers, page, size);

        // Get current user's info
        LeaderboardResponse currentUserInfo = getCurrentUserPointsInfo(currentUserId);

        // Get nearby users (users around current user's rank)
        List<LeaderboardResponse> nearbyUsers = new ArrayList<>();
        if (currentUserInfo != null && currentUserInfo.getRank() != null) {
            nearbyUsers = getUsersAroundRank(
                    currentUserInfo.getRank(),
                    DEFAULT_NEARBY_RANGE,
                    "points");
        }

        // Check if current user is in current page
        boolean inTopList = pageUsers.stream()
                .anyMatch(profile -> profile.getUserId().equals(currentUserId));

        // Calculate pagination info
        int totalUsers = (int) userProfileRepository.count();
        int totalPages = (int) Math.ceil((double) totalUsers / size);

        return LeaderboardPageResponse.builder()
                .leaderboard(leaderboardResponses)
                .currentUser(currentUserInfo)
                .nearbyUsers(nearbyUsers)
                .currentPage(page)
                .pageSize(size)
                .totalPages(totalPages)
                .totalUsers(totalUsers)
                .leaderboardType("POINTS")
                .currentUserInTopList(inTopList)
                .build();
    }

    /**
     * Get simple points leaderboard (backward compatibility)
     */
    public LeaderboardPageResponse getPointsLeaderboardWithContext(Long currentUserId, int limit) {
        return getPointsLeaderboardWithContext(currentUserId, 0, limit);
    }

    /**
     * Get simple points leaderboard without pagination
     */
    public List<LeaderboardResponse> getPointsLeaderboard(int limit) {
        log.debug("Getting points leaderboard with limit {}", limit);
        List<UserGamificationProfile> topUsers = getTopUsersByPoints(limit);
        return leaderboardMapper.toLeaderboardResponseList(topUsers);
    }

    private List<UserGamificationProfile> getTopUsersByPoints(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return userProfileRepository.findAllByOrderByPointsDesc(pageable);
    }

    /**
     * Get current user's points info with CORRECT rank calculation
     */
    private LeaderboardResponse getCurrentUserPointsInfo(Long userId) {
        try {
            UserGamificationProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));

            // FIX: Calculate actual rank from sorted list
            int rank = calculateActualPointsRank(userId);
            return leaderboardMapper.toLeaderboardResponse(profile, rank);
        } catch (Exception e) {
            log.error("Error getting current user points info for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate actual points rank from sorted list
     */
    private int calculateActualPointsRank(Long userId) {
        List<UserGamificationProfile> allUsers = userProfileRepository.findAllByOrderByPointsDesc();

        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getUserId().equals(userId)) {
                return i + 1;
            }
        }

        log.warn("User {} not found in points leaderboard", userId);
        return -1;
    }

    // ===========================================
    // LEVEL LEADERBOARD
    // ===========================================

    /**
     * Get level leaderboard with user context and pagination
     */
    public LeaderboardPageResponse getLevelLeaderboardWithContext(
            Long currentUserId,
            int page,
            int size) {

        log.info("Getting level leaderboard - page: {}, size: {}, user: {}",
                page, size, currentUserId);

        Pageable pageable = PageRequest.of(page, size);
        List<UserGamificationProfile> pageUsers = userProfileRepository
                .findAllByOrderByLevelDesc(pageable);

        List<LeaderboardResponse> leaderboardResponses = mapWithRanks(pageUsers, page, size);

        LeaderboardResponse currentUserInfo = getCurrentUserLevelInfo(currentUserId);

        List<LeaderboardResponse> nearbyUsers = new ArrayList<>();
        if (currentUserInfo != null && currentUserInfo.getRank() != null) {
            nearbyUsers = getUsersAroundRank(
                    currentUserInfo.getRank(),
                    DEFAULT_NEARBY_RANGE,
                    "level");
        }

        boolean inTopList = pageUsers.stream()
                .anyMatch(profile -> profile.getUserId().equals(currentUserId));

        int totalUsers = (int) userProfileRepository.count();
        int totalPages = (int) Math.ceil((double) totalUsers / size);

        return LeaderboardPageResponse.builder()
                .leaderboard(leaderboardResponses)
                .currentUser(currentUserInfo)
                .nearbyUsers(nearbyUsers)
                .currentPage(page)
                .pageSize(size)
                .totalPages(totalPages)
                .totalUsers(totalUsers)
                .leaderboardType("LEVEL")
                .currentUserInTopList(inTopList)
                .build();
    }

    /**
     * Get simple level leaderboard (backward compatibility)
     */
    public LeaderboardPageResponse getLevelLeaderboardWithContext(Long currentUserId, int limit) {
        return getLevelLeaderboardWithContext(currentUserId, 0, limit);
    }

    /**
     * Get simple level leaderboard without pagination
     */
    public List<LeaderboardResponse> getLevelLeaderboard(int limit) {
        log.debug("Getting level leaderboard with limit {}", limit);
        List<UserGamificationProfile> topUsers = getTopUsersByLevel(limit);
        return leaderboardMapper.toLeaderboardResponseList(topUsers);
    }

    private List<UserGamificationProfile> getTopUsersByLevel(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return userProfileRepository.findAllByOrderByLevelDesc(pageable);
    }

    /**
     * Get current user's level info with CORRECT rank calculation
     */
    private LeaderboardResponse getCurrentUserLevelInfo(Long userId) {
        try {
            UserGamificationProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));

            // FIX: Calculate actual rank from sorted list
            int rank = calculateActualLevelRank(userId);
            return leaderboardMapper.toLeaderboardResponse(profile, rank);
        } catch (Exception e) {
            log.error("Error getting current user level info for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate actual level rank from sorted list
     */
    private int calculateActualLevelRank(Long userId) {
        // Get ALL users sorted by level (same way as leaderboard)
        List<UserGamificationProfile> allUsers = userProfileRepository.findAllByOrderByLevelDesc();

        // Find user's position in sorted list
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getUserId().equals(userId)) {
                return i + 1; // Rank is 1-indexed
            }
        }

        log.warn("User {} not found in level leaderboard", userId);
        return -1; // User not found
    }

    // ===========================================
    // WEEKLY STREAK LEADERBOARD
    // ===========================================

    /**
     * Get weekly streak leaderboard with user context and pagination
     */
    public LeaderboardPageResponse getWeeklyStreakLeaderboardWithContext(
            Long currentUserId,
            int page,
            int size) {

        log.info("Getting weekly streak leaderboard - page: {}, size: {}, user: {}",
                page, size, currentUserId);

        Pageable pageable = PageRequest.of(page, size);
        List<UserGamificationProfile> pageUsers = userProfileRepository
                .findAllByOrderByWeeklyStreakDesc(pageable);

        List<LeaderboardResponse> leaderboardResponses = mapWeeklyStreakWithRanks(pageUsers, page, size);

        LeaderboardResponse currentUserInfo = getCurrentUserWeeklyStreakInfo(currentUserId);

        List<LeaderboardResponse> nearbyUsers = new ArrayList<>();
        if (currentUserInfo != null && currentUserInfo.getRank() != null) {
            nearbyUsers = getUsersAroundRank(
                    currentUserInfo.getRank(),
                    DEFAULT_NEARBY_RANGE,
                    "weeklyStreak");
        }

        boolean inTopList = pageUsers.stream()
                .anyMatch(profile -> profile.getUserId().equals(currentUserId));

        int totalUsers = (int) userProfileRepository.count();
        int totalPages = (int) Math.ceil((double) totalUsers / size);

        return LeaderboardPageResponse.builder()
                .leaderboard(leaderboardResponses)
                .currentUser(currentUserInfo)
                .nearbyUsers(nearbyUsers)
                .currentPage(page)
                .pageSize(size)
                .totalPages(totalPages)
                .totalUsers(totalUsers)
                .leaderboardType("WEEKLY_STREAK")
                .currentUserInTopList(inTopList)
                .build();
    }

    /**
     * Get simple weekly streak leaderboard (backward compatibility)
     */
    public LeaderboardPageResponse getWeeklyStreakLeaderboardWithContext(Long currentUserId, int limit) {
        return getWeeklyStreakLeaderboardWithContext(currentUserId, 0, limit);
    }

    /**
     * Get simple weekly streak leaderboard without pagination
     */
    public List<LeaderboardResponse> getWeeklyStreakLeaderboard(int limit) {
        log.info("Getting weekly streak leaderboard with limit {}", limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<UserGamificationProfile> topUsers = userProfileRepository.findAllByOrderByWeeklyStreakDesc(pageable);
        return leaderboardMapper.toWeeklyStreakResponseList(topUsers);
    }

    /**
     * Get current user's weekly streak info with CORRECT rank calculation
     */
    private LeaderboardResponse getCurrentUserWeeklyStreakInfo(Long userId) {
        try {
            UserGamificationProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));

            // FIX: Calculate actual rank from sorted list
            int rank = calculateActualWeeklyStreakRank(userId);
            return leaderboardMapper.toWeeklyStreakResponse(profile, rank);
        } catch (Exception e) {
            log.error("Error getting current user weekly streak info for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate actual weekly streak rank from sorted list
     */
    private int calculateActualWeeklyStreakRank(Long userId) {
        List<UserGamificationProfile> allUsers = userProfileRepository.findAllByOrderByWeeklyStreakDesc();

        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getUserId().equals(userId)) {
                return i + 1;
            }
        }

        log.warn("User {} not found in weekly streak leaderboard", userId);
        return -1;
    }


    /**
     * Get current user's monthly streak info with CORRECT rank calculation
     */
    private LeaderboardResponse getCurrentUserMonthlyStreakInfo(Long userId) {
        try {
            UserGamificationProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));

            // ⬅️ FIX: Calculate actual rank from sorted list
            int rank = calculateActualMonthlyStreakRank(userId);
            return leaderboardMapper.toMonthlyStreakResponse(profile, rank);
        } catch (Exception e) {
            log.error("Error getting current user monthly streak info for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate actual monthly streak rank from sorted list
     */
    private int calculateActualMonthlyStreakRank(Long userId) {
        List<UserGamificationProfile> allUsers = userProfileRepository.findAllByOrderByMonthlyStreakDesc();

        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getUserId().equals(userId)) {
                return i + 1;
            }
        }

        log.warn("User {} not found in monthly streak leaderboard", userId);
        return -1;
    }

    private int calculateWeeklyStreakRank(Integer weeklyStreak) {
        if (weeklyStreak == null) weeklyStreak = 0;
        long usersWithHigherStreak = userProfileRepository
                .findByWeeklyStreakGreaterThanEqual(weeklyStreak + 1).size();
        return (int) (usersWithHigherStreak + 1);
    }

    // ===========================================
    // MONTHLY STREAK LEADERBOARD
    // ===========================================

    /**
     * Get monthly streak leaderboard with user context and pagination
     */
    public LeaderboardPageResponse getMonthlyStreakLeaderboardWithContext(
            Long currentUserId,
            int page,
            int size) {

        log.info("Getting monthly streak leaderboard - page: {}, size: {}, user: {}",
                page, size, currentUserId);

        Pageable pageable = PageRequest.of(page, size);
        List<UserGamificationProfile> pageUsers = userProfileRepository
                .findAllByOrderByMonthlyStreakDesc(pageable);

        List<LeaderboardResponse> leaderboardResponses = mapMonthlyStreakWithRanks(pageUsers, page, size);

        LeaderboardResponse currentUserInfo = getCurrentUserMonthlyStreakInfo(currentUserId);

        List<LeaderboardResponse> nearbyUsers = new ArrayList<>();
        if (currentUserInfo != null && currentUserInfo.getRank() != null) {
            nearbyUsers = getUsersAroundRank(
                    currentUserInfo.getRank(),
                    DEFAULT_NEARBY_RANGE,
                    "monthlyStreak");
        }

        boolean inTopList = pageUsers.stream()
                .anyMatch(profile -> profile.getUserId().equals(currentUserId));

        int totalUsers = (int) userProfileRepository.count();
        int totalPages = (int) Math.ceil((double) totalUsers / size);

        return LeaderboardPageResponse.builder()
                .leaderboard(leaderboardResponses)
                .currentUser(currentUserInfo)
                .nearbyUsers(nearbyUsers)
                .currentPage(page)
                .pageSize(size)
                .totalPages(totalPages)
                .totalUsers(totalUsers)
                .leaderboardType("MONTHLY_STREAK")
                .currentUserInTopList(inTopList)
                .build();
    }

    /**
     * Get simple monthly streak leaderboard (backward compatibility)
     */
    public LeaderboardPageResponse getMonthlyStreakLeaderboardWithContext(Long currentUserId, int limit) {
        return getMonthlyStreakLeaderboardWithContext(currentUserId, 0, limit);
    }

    /**
     * Get simple monthly streak leaderboard without pagination
     */
    public List<LeaderboardResponse> getMonthlyStreakLeaderboard(int limit) {
        log.info("Getting monthly streak leaderboard with limit {}", limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<UserGamificationProfile> topUsers = userProfileRepository.findAllByOrderByMonthlyStreakDesc(pageable);
        return leaderboardMapper.toMonthlyStreakResponseList(topUsers);
    }



    // ===========================================
    // HELPER METHODS
    // ===========================================

    /**
     * Get users around a specific rank (for nearby users feature)
     */
    private List<LeaderboardResponse> getUsersAroundRank(
            int rank,
            int range,
            String type) {

        log.debug("Getting users around rank {} with range {} for type {}", rank, range, type);

        // Calculate start and end positions
        int startRank = Math.max(1, rank - range);
        int endRank = rank + range;
        int totalToFetch = endRank - startRank + 1;

        // Skip to start position (rank - 1 because ranks are 1-indexed)
        int skip = startRank - 1;

        Pageable pageable = PageRequest.of(skip, totalToFetch);
        List<UserGamificationProfile> profiles;

        // Get profiles based on type
        profiles = switch (type) {
            case "points" -> userProfileRepository.findAllByOrderByPointsDesc(pageable);
            case "level" -> userProfileRepository.findAllByOrderByLevelDesc(pageable);
            case "weeklyStreak" -> userProfileRepository.findAllByOrderByWeeklyStreakDesc(pageable);
            case "monthlyStreak" -> userProfileRepository.findAllByOrderByMonthlyStreakDesc(pageable);
            default -> new ArrayList<>();
        };

        // Map to responses with correct ranks
        return switch (type) {
            case "weeklyStreak" -> mapWeeklyStreakWithRanks(profiles, skip, totalToFetch);
            case "monthlyStreak" -> mapMonthlyStreakWithRanks(profiles, skip, totalToFetch);
            default -> mapWithRanks(profiles, skip, totalToFetch);
        };
    }

    /**
     * Map profiles to responses with proper ranks
     */
    private List<LeaderboardResponse> mapWithRanks(
            List<UserGamificationProfile> profiles,
            int page,
            int size) {

        int startRank = (page * size) + 1;

        return profiles.stream()
                .map(profile -> {
                    int rank = startRank + profiles.indexOf(profile);
                    return leaderboardMapper.toLeaderboardResponse(profile, rank);
                })
                .collect(Collectors.toList());
    }

    /**
     * Map profiles to weekly streak responses with proper ranks
     */
    private List<LeaderboardResponse> mapWeeklyStreakWithRanks(
            List<UserGamificationProfile> profiles,
            int page,
            int size) {

        int startRank = (page * size) + 1;

        return profiles.stream()
                .map(profile -> {
                    int rank = startRank + profiles.indexOf(profile);
                    return leaderboardMapper.toWeeklyStreakResponse(profile, rank);
                })
                .collect(Collectors.toList());
    }

    /**
     * Map profiles to monthly streak responses with proper ranks
     */
    private List<LeaderboardResponse> mapMonthlyStreakWithRanks(
            List<UserGamificationProfile> profiles,
            int page,
            int size) {

        int startRank = (page * size) + 1;

        return profiles.stream()
                .map(profile -> {
                    int rank = startRank + profiles.indexOf(profile);
                    return leaderboardMapper.toMonthlyStreakResponse(profile, rank);
                })
                .collect(Collectors.toList());
    }

    // ===========================================
    // LEGACY SUPPORT (for existing streak system)
    // ===========================================

    public List<LeaderboardResponse> getStreakLeaderboard(String streakType, int limit) {
        log.debug("Getting {} streak leaderboard with limit {}", streakType, limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<UserGamificationProfile> topUsers = userProfileRepository
                .findTopUsersByStreak(streakType, pageable);
        return leaderboardMapper.toLeaderboardResponseList(topUsers);
    }

    public List<LeaderboardResponse> getLongestStreakLeaderboard(String streakType, int limit) {
        log.debug("Getting {} longest streak leaderboard with limit {}", streakType, limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<UserGamificationProfile> topUsers = userProfileRepository
                .findTopUsersByLongestStreak(streakType, pageable);
        return leaderboardMapper.toLeaderboardResponseList(topUsers);
    }

    // ===========================================
    // RANK METHODS
    // ===========================================

    public long getUserPointsRank(Long userId) {
        try {
            UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));
            return userProfileRepository.countUsersWithHigherPoints(userProfile.getPoints()) + 1;
        } catch (Exception e) {
            log.error("Error getting points rank for user {}: {}", userId, e.getMessage());
            return -1;
        }
    }

    public long getUserLevelRank(Long userId) {
        try {
            UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));
            return userProfileRepository.countUsersWithHigherLevel(userProfile.getLevel()) + 1;
        } catch (Exception e) {
            log.error("Error getting level rank for user {}: {}", userId, e.getMessage());
            return -1;
        }
    }
}