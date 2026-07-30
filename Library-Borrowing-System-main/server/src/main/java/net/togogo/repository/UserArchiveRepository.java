package net.togogo.repository;

import net.togogo.entity.UserArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface UserArchiveRepository extends JpaRepository<UserArchive, Long> {
    // 分页查询所有未删除的档案
    // 按删除时间降序排序
    // 分页大小为 pageable.getSize()
    Page<UserArchive> findAllByOrderByDeleteTimeDesc(Pageable pageable);
    Page<UserArchive> findByStatusOrderByDeleteTimeDesc(String status, Pageable pageable);
    void deleteByDeleteTimeBefore(LocalDateTime time);
}
