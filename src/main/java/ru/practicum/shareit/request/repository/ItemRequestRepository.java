package ru.practicum.shareit.request.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.request.model.ItemRequest;


import java.util.List;

@Repository
public interface ItemRequestRepository extends PagingAndSortingRepository<ItemRequest, Long> {
    List<ItemRequest> findByRequestorIdOrderByCreatedDesc(Long requestorId);

    @Query("SELECT r FROM ItemRequest r " +
            "WHERE r.requestorId <> :userId " +
            "ORDER BY r.created DESC")
    List<ItemRequest> findAllByRequestorIdNot(@Param("userId") Long userId);
}