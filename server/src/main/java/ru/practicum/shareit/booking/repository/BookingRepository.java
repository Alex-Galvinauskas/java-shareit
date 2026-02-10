package ru.practicum.shareit.booking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByBookerId(Long bookerId, Pageable pageable);

    Page<Booking> findByBookerIdAndStatus(Long bookerId, BookingStatus status, Pageable pageable);

    Page<Booking> findByBookerIdAndStartLessThanEqualAndEndGreaterThanEqual(
            Long bookerId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Booking> findByBookerIdAndEndLessThan(
            Long bookerId, LocalDateTime date, Pageable pageable);

    Page<Booking> findByBookerIdAndStartGreaterThan(
            Long bookerId, LocalDateTime date, Pageable pageable);

    Page<Booking> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Booking> findByOwnerIdAndStatus(Long ownerId, BookingStatus status, Pageable pageable);

    Page<Booking> findByOwnerIdAndStartLessThanEqualAndEndGreaterThanEqual(
            Long ownerId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Booking> findByOwnerIdAndEndLessThan(
            Long ownerId, LocalDateTime date, Pageable pageable);

    Page<Booking> findByOwnerIdAndStartGreaterThan(
            Long ownerId, LocalDateTime date, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.itemId IN :itemIds " +
            "AND b.status = :status " +
            "AND b.end < :now " +
            "ORDER BY b.itemId, b.end DESC")
    List<Booking> findLastBookingsForMultipleItems(
            @Param("itemIds") List<Long> itemIds,
            @Param("now") LocalDateTime now,
            @Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.itemId IN :itemIds " +
            "AND b.status = :status " +
            "AND b.start > :now " +
            "ORDER BY b.itemId, b.start ASC")
    List<Booking> findNextBookingsForMultipleItems(
            @Param("itemIds") List<Long> itemIds,
            @Param("now") LocalDateTime now,
            @Param("status") BookingStatus status);

    @Query(value = "SELECT EXISTS (" +
            "SELECT 1 FROM bookings b " +
            "WHERE b.item_id = :itemId " +
            "AND b.status IN ('APPROVED', 'WAITING') " +
            "AND b.start_date < :end " +
            "AND b.end_date > :start " +
            "LIMIT 1" +
            ")", nativeQuery = true)
    boolean existsOverlappingBooking(
            @Param("itemId") Long itemId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT b FROM Booking b WHERE b.itemId = :itemId " +
            "AND b.status = 'APPROVED' " +
            "AND b.end < :now " +
            "ORDER BY b.end DESC")
    Page<Booking> findLastBooking(@Param("itemId") Long itemId,
                                  @Param("now") LocalDateTime now,
                                  Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.itemId = :itemId " +
            "AND b.status = 'APPROVED' " +
            "AND b.start > :now " +
            "ORDER BY b.start ASC")
    Page<Booking> findNextBooking(@Param("itemId") Long itemId,
                                  @Param("now") LocalDateTime now,
                                  Pageable pageable);



    List<Booking> findByItemIdAndEndBeforeAndStatusOrderByEndDesc(
            Long itemId, LocalDateTime end, BookingStatus status);

    List<Booking> findByItemIdAndStartAfterAndStatusOrderByStartAsc(
            Long itemId, LocalDateTime start, BookingStatus status);

    List<Booking> findByItemIdAndBookerIdAndEndBeforeAndStatus(
            Long itemId, Long bookerId, LocalDateTime end, BookingStatus status);
}