package ru.practicum.shareit.booking.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends PagingAndSortingRepository<Booking, Long> {

    List<Booking> findByBookerIdOrderByStartDesc(Long bookerId);

    List<Booking> findByBookerIdAndStatusOrderByStartDesc(Long bookerId, BookingStatus status);

    List<Booking> findByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(
            Long bookerId, LocalDateTime start, LocalDateTime end);

    List<Booking> findByBookerIdAndEndBeforeOrderByStartDesc(Long bookerId, LocalDateTime end);

    List<Booking> findByBookerIdAndStartAfterOrderByStartDesc(Long bookerId, LocalDateTime start);

    Optional<Booking> findFirstByItemIdAndBookerIdAndStatusAndEndBefore(
            Long itemId, Long bookerId, BookingStatus status, LocalDateTime end);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.itemId IN :itemIds " +
            "ORDER BY b.start DESC")
    List<Booking> findByItemIdInOrderByStartDesc(@Param("itemIds") List<Long> itemIds);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.itemId = :itemId " +
            "AND b.status = 'APPROVED' " +
            "AND b.start <= :currentTime " +
            "ORDER BY b.end DESC")
    List<Booking> findLastBookingForItem(
            @Param("itemId") Long itemId,
            @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.itemId = :itemId " +
            "AND b.status = 'APPROVED' " +
            "AND b.start > :currentTime " +
            "ORDER BY b.start ASC")
    List<Booking> findNextBookingForItem(
            @Param("itemId") Long itemId,
            @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT b FROM Booking b " +
            "JOIN Item i ON b.itemId = i.id " +
            "WHERE i.ownerId = :ownerId " +
            "ORDER BY b.start DESC")
    List<Booking> findByOwnerIdOrderByStartDesc(@Param("ownerId") Long ownerId);

    @Query("SELECT b FROM Booking b " +
            "JOIN Item i ON b.itemId = i.id " +
            "WHERE i.ownerId = :ownerId " +
            "AND b.status = :status " +
            "ORDER BY b.start DESC")
    List<Booking> findByOwnerIdAndStatusOrderByStartDesc(
            @Param("ownerId") Long ownerId,
            @Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b " +
            "JOIN Item i ON b.itemId = i.id " +
            "WHERE i.ownerId = :ownerId " +
            "AND b.start < :currentTime " +
            "AND b.end > :currentTime " +
            "ORDER BY b.start DESC")
    List<Booking> findCurrentBookingsForOwner(
            @Param("ownerId") Long ownerId,
            @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT b FROM Booking b " +
            "JOIN Item i ON b.itemId = i.id " +
            "WHERE i.ownerId = :ownerId " +
            "AND b.end < :currentTime " +
            "ORDER BY b.start DESC")
    List<Booking> findPastBookingsForOwner(
            @Param("ownerId") Long ownerId,
            @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT b FROM Booking b " +
            "JOIN Item i ON b.itemId = i.id " +
            "WHERE i.ownerId = :ownerId " +
            "AND b.start > :currentTime " +
            "ORDER BY b.start DESC")
    List<Booking> findFutureBookingsForOwner(
            @Param("ownerId") Long ownerId,
            @Param("currentTime") LocalDateTime currentTime);

    boolean existsByItemIdAndBookerIdAndStatusAndEndBefore(
            Long itemId, Long bookerId, BookingStatus status, LocalDateTime end);
}