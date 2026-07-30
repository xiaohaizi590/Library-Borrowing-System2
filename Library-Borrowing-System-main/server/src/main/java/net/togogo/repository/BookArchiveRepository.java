package net.togogo.repository;

import net.togogo.entity.BookArchive;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface BookArchiveRepository extends JpaRepository<BookArchive, Long> {
    // 分页查询所有未删除的图书档案
    // 按删除时间降序排序
    // 分页大小为 pageable.getSize()
    Page<BookArchive> findAllByOrderByDeleteTimeDesc(Pageable pageable);
    Page<BookArchive> findByStatusOrderByDeleteTimeDesc(String status, Pageable pageable);
    void deleteByDeleteTimeBefore(LocalDateTime time);
}
