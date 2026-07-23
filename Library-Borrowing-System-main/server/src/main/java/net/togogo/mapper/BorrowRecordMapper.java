package net.togogo.mapper;

import net.togogo.dto.BorrowRecordDTO;
import net.togogo.entity.BorrowRecord;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class BorrowRecordMapper {

    public static BorrowRecordDTO toDTO(BorrowRecord record, String bookTitle,
                                        String bookAuthor, String username) {
        if (record == null) return null;

        Long overdueDays = null;
        if (record.getStatus() == BorrowRecord.Borrowstatus.BORROWED
                && record.getDueTime().isBefore(LocalDateTime.now())) {
            overdueDays = ChronoUnit.DAYS.between(record.getDueTime(), LocalDateTime.now());
        }

        return BorrowRecordDTO.builder()
                .id(record.getId())
                .bookId(record.getBookId())
                .bookTitle(bookTitle)
                .bookAuthor(bookAuthor)
                .userId(record.getUserId())
                .userName(username)
                .borrowTime(record.getBorrowTime())
                .dueTime(record.getDueTime())
                .returnTime(record.getReturnTime())
                .renewCount(record.getRenewCount())
                .status(record.getStatus())
                .overdueDays(overdueDays)
                .build();
    }
}
