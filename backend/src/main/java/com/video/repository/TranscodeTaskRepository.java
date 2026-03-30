package com.video.repository;

import com.video.entity.TranscodeTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TranscodeTaskRepository extends JpaRepository<TranscodeTask, Long> {
    Optional<TranscodeTask> findByVideoUuid(String videoUuid);
}