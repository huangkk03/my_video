package com.video.repository;

import com.video.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findAllByOrderByNameAsc();
    List<Folder> findByParentIdOrderByNameAsc(Long parentId);
    List<Folder> findByParentIdIsNullOrderByNameAsc();
    boolean existsById(Long id);
}