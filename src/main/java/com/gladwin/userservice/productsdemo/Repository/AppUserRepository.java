package com.gladwin.userservice.productsdemo.Repository;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    List<AppUser> findByRole(String role);
}