package com.urbanservices.repository;

import com.urbanservices.model.ProviderProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, Long> {
    Optional<ProviderProfile> findByUser_Id(Long userId);
    List<ProviderProfile> findAllByOrderByUser_NameAsc();
    long count();
}
