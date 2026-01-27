package com.learning.ytrep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.learning.ytrep.model.UserLike;

@Repository
public interface UserLikeRepository extends JpaRepository<UserLike, Long> {
    Optional<UserLike> findByUserUserIdAndVideoVideoId(Long userId, Long videoId);
    List<UserLike> findByUserUserId(Long userId);
    boolean existsByUserUserIdAndVideoVideoId(Long userId, Long videoId);
}