package com.video.repository;

import com.video.entity.Subtitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubtitleRepository extends JpaRepository<Subtitle, Long> {

    List<Subtitle> findByVideoUuid(String videoUuid);

    Optional<Subtitle> findByVideoUuidAndLanguage(String videoUuid, String language);

    List<Subtitle> findByVideoUuidAndStatus(String videoUuid, String status);

    boolean existsByVideoUuidAndLanguage(String videoUuid, String language);

    void deleteByVideoUuid(String videoUuid);
}