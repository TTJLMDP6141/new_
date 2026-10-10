package com.urbanservices.repository;

import com.urbanservices.model.ServiceOffering;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {
    List<ServiceOffering> findByActiveTrueOrderByNameAsc();
    Optional<ServiceOffering> findByIdAndActiveTrue(Long id);

    @Query("""
        select s from ServiceOffering s
        where s.active = true and (
            lower(s.name) like lower(concat('%', :term, '%')) or
            lower(s.category) like lower(concat('%', :term, '%')) or
            lower(s.description) like lower(concat('%', :term, '%'))
        )
        order by s.name
        """)
    List<ServiceOffering> searchActive(@Param("term") String term);

    List<ServiceOffering> findAllByOrderByNameAsc();
    List<ServiceOffering> findTop8ByActiveTrueOrderByNameAsc();
}
