package com.video.repository;

import com.video.entity.TranscodeTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TranscodeTaskRepository extends JpaRepository<TranscodeTask, Long> {
    Optional<TranscodeTask> findByVideoUuid(String videoUuid);
    
    List<TranscodeTask> findByStatusOrderByCreatedAtAsc(String status);
    
    List<TranscodeTask> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
    
    long countByStatus(String status);
}