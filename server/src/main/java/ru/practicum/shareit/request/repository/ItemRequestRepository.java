package ru.practicum.shareit.request.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.request.model.ItemRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {

    @Query("SELECT ir FROM ItemRequest ir " +
            "WHERE ir.requestor.id = :requesterId " +
            "ORDER BY ir.created DESC")
    List<ItemRequest> findByRequestorIdOrderByCreatedDesc(@Param("requesterId") Long requesterId);

    @Query("SELECT ir FROM ItemRequest ir " +
            "WHERE ir.requestor.id != :userId " +
            "AND NOT EXISTS (SELECT 1 FROM Item i WHERE i.requestId = ir.id AND i.ownerId = :userId) " +
            "ORDER BY ir.created DESC")
    Page<ItemRequest> findOtherUsersRequestsWithoutUserResponses(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("SELECT ir FROM ItemRequest ir " +
            "LEFT JOIN FETCH ir.requestor " +
            "WHERE ir.id = :requestId")
    Optional<ItemRequest> findByIdWithRequestor(@Param("requestId") Long requestId);
}