package com.minimarket.api.repository;

import com.minimarket.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    List<User> findAllByOrderByFullNameAsc();
    Optional<User> findByUsername(String username);
}
