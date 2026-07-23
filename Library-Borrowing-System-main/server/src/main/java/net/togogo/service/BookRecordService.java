package net.togogo.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import net.togogo.dto.BorrowRecordDTO;
import net.togogo.dto.BorrowRequest;
import net.togogo.dto.PageResponse;

public interface BookRecordService {

      BorrowRecordDTO borrowBook(BorrowRequest request);
    BorrowRecordDTO returnBook(Long recordId);
    BorrowRecordDTO renewBook(Long recordId);
    PageResponse<BorrowRecordDTO> getBorrowRecordsByUser(Long userId, Pageable pageable);
    List<BorrowRecordDTO> getBorrowRecordsByBook(Long bookId);
    PageResponse<BorrowRecordDTO> getOverdueRecords(Pageable pageable);
    PageResponse<BorrowRecordDTO> getAllBorrowRecords(Pageable pageable);
    
}
