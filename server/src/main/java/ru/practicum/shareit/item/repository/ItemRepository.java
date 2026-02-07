package ru.practicum.shareit.item.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT i FROM Item i " +
            "WHERE i.available = true " +
            "AND (i.name ILIKE CONCAT('%', :text, '%') " +
            "OR i.description ILIKE CONCAT('%', :text, '%'))")
    Page<Item> searchAvailableItems(@Param("text") String text, Pageable pageable);

    Optional<Item> findByIdAndOwnerId(Long itemId, Long ownerId);

    @Query("SELECT i FROM Item i WHERE i.id = :itemId AND (i.ownerId = :ownerId OR i.available = true)")
    Optional<Item> findByIdAccessibleByUser(@Param("itemId") Long itemId, @Param("ownerId") Long ownerId);

    List<Item> findByRequestId(Long requestId);

    List<Item> findByRequestIdIn(List<Long> requestIds);
}