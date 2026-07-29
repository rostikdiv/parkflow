package com.parkflow.security.infra;

import com.parkflow.security.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
}
