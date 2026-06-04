package com.minimarket.api.repository;

import com.minimarket.api.entity.CashSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CashSessionRepository extends JpaRepository<CashSession, Integer> {

    @EntityGraph(attributePaths = {"user", "movements"})
    Optional<CashSession> findFirstByUserIdAndStatusOrderByOpenedAtDesc(Integer userId, String status);

    @EntityGraph(attributePaths = {"user", "movements"})
    List<CashSession> findTop10ByUserIdOrderByOpenedAtDesc(Integer userId);

    @Override
    @EntityGraph(attributePaths = {"user", "movements"})
    Optional<CashSession> findById(Integer integer);
}
