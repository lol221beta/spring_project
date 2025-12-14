package com.amusementpark.repository;

import com.amusementpark.model.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttractionRepository extends JpaRepository<Attraction, Long> {
    List<Attraction> findByType(String type);
    List<Attraction> findByStatus(String status);
    List<Attraction> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT DISTINCT a FROM Attraction a LEFT JOIN FETCH a.owner")
    List<Attraction> findAllWithOwner();
    
    @Query("SELECT a FROM Attraction a LEFT JOIN FETCH a.owner WHERE a.id = :id")
    Optional<Attraction> findByIdWithOwner(@Param("id") Long id);
    
    @Query("SELECT a FROM Attraction a LEFT JOIN FETCH a.owner WHERE " +
           "(:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:type IS NULL OR a.type = :type) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:location IS NULL OR LOWER(a.location) LIKE LOWER(CONCAT('%', :location, '%')))")
    List<Attraction> searchAttractions(@Param("name") String name,
                                       @Param("type") String type,
                                       @Param("status") String status,
                                       @Param("location") String location);
}

