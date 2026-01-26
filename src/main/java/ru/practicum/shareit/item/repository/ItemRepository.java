package ru.practicum.shareit.item.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.List;

@Repository
public interface ItemRepository extends CrudRepository<Item, Long> {

    List<Item> findByOwnerId(Long ownerId, Pageable pageable);

    default List<Item> findByOwnerId(Long ownerId) {
        return findByOwnerId(ownerId, Pageable.unpaged());
    }

    List<Item> findByRequestId(Long requestId);

    @Query("SELECT i FROM Item i " +
            "WHERE i.available = true " +
            "AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :text, '%')))")
    List<Item> searchAvailableItems(@Param("text") String text, Pageable pageable);

    @Query("SELECT i FROM Item i " +
            "WHERE i.available = true " +
            "AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :text, '%')))")
    default List<Item> searchAvailableItems(@Param("text") String text) {
        return searchAvailableItems(text, Pageable.unpaged());
    }

    List<Item> findByRequestIdIn(List<Long> requestIds);

    List<Item> findAllById(Iterable<Long> ids);
}