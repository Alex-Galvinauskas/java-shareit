package ru.practicum.shareit.item.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Тестирование репозитория вещей")
class ItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ItemRepository itemRepository;

    private User owner1;
    private User owner2;
    private Item availableItem1;
    private Item unavailableItem;
    private Item itemWithRequest;

    @BeforeEach
    void setUp() {
        owner1 = User.builder()
                .name("Владелец 1")
                .email("owner1@test.com")
                .build();
        entityManager.persist(owner1);

        owner2 = User.builder()
                .name("Владелец 2")
                .email("owner2@test.com")
                .build();
        entityManager.persist(owner2);

        availableItem1 = Item.builder()
                .name("Дрель мощная")
                .description("Электрическая дрель для любых работ")
                .available(true)
                .ownerId(owner1.getId())
                .build();
        entityManager.persist(availableItem1);

        Item availableItem2 = Item.builder()
                .name("Отвертка крестовая")
                .description("Набор отверток разного размера")
                .available(true)
                .ownerId(owner2.getId())
                .build();
        entityManager.persist(availableItem2);

        unavailableItem = Item.builder()
                .name("Перфоратор")
                .description("Мощный перфоратор для бетона")
                .available(false)
                .ownerId(owner1.getId())
                .build();
        entityManager.persist(unavailableItem);

        itemWithRequest = Item.builder()
                .name("Шуруповерт")
                .description("Аккумуляторный шуруповерт")
                .available(true)
                .ownerId(owner1.getId())
                .requestId(100L)
                .build();
        entityManager.persist(itemWithRequest);

        entityManager.flush();
    }

    @Test
    @DisplayName("Поиск вещей по ID владельца")
    void findByOwnerId_ShouldReturnOwnerItems() {

        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> result = itemRepository.findByOwnerId(owner1.getId(), pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).extracting(Item::getOwnerId)
                .containsOnly(owner1.getId());
    }

    @Test
    @DisplayName("Поиск доступных вещей по тексту - полное совпадение названия")
    void searchAvailableItems_ShouldReturnItems_WhenTextMatchesName() {

        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> result = itemRepository.searchAvailableItems("дрель", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Дрель мощная");
    }

    @Test
    @DisplayName("Поиск доступных вещей по тексту - совпадение в описании")
    void searchAvailableItems_ShouldReturnItems_WhenTextMatchesDescription() {

        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> result = itemRepository.searchAvailableItems("электрическая", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getDescription()).contains("Электрическая");
    }

    @Test
    @DisplayName("Поиск доступных вещей по тексту - регистронезависимый поиск")
    void searchAvailableItems_ShouldBeCaseInsensitive() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Item> resultLower = itemRepository.searchAvailableItems("дрель", pageable);
        Page<Item> resultUpper = itemRepository.searchAvailableItems("ДРЕЛЬ", pageable);
        Page<Item> resultMixed = itemRepository.searchAvailableItems("ДрЕлЬ", pageable);

        assertThat(resultLower.getTotalElements()).isEqualTo(1);
        assertThat(resultUpper.getTotalElements()).isEqualTo(1);
        assertThat(resultMixed.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Поиск доступных вещей по тексту - недоступные вещи не возвращаются")
    void searchAvailableItems_ShouldNotReturnUnavailableItems() {

        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> result = itemRepository.searchAvailableItems("перфоратор", pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Поиск вещи по ID и ID владельца")
    void findByIdAndOwnerId_ShouldReturnItem_WhenOwnerMatches() {

        Optional<Item> result = itemRepository.findByIdAndOwnerId(
                availableItem1.getId(), owner1.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(availableItem1.getId());
        assertThat(result.get().getOwnerId()).isEqualTo(owner1.getId());
    }

    @Test
    @DisplayName("Поиск вещи по ID и ID владельца - владелец не совпадает")
    void findByIdAndOwnerId_ShouldReturnEmpty_WhenOwnerDoesNotMatch() {

        Optional<Item> result = itemRepository.findByIdAndOwnerId(
                availableItem1.getId(), owner2.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Поиск вещи с доступом пользователя - владелец имеет доступ")
    void findByIdAccessibleByUser_ShouldReturnItem_ForOwner() {

        Optional<Item> result = itemRepository.findByIdAccessibleByUser(
                availableItem1.getId(), owner1.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(availableItem1.getId());
    }

    @Test
    @DisplayName("Поиск вещи с доступом пользователя - доступная вещь доступна всем")
    void findByIdAccessibleByUser_ShouldReturnItem_ForAvailableItem() {

        User otherUser = User.builder()
                .name("Другой пользователь")
                .email("other@test.com")
                .build();
        entityManager.persist(otherUser);
        entityManager.flush();

        Optional<Item> result = itemRepository.findByIdAccessibleByUser(
                availableItem1.getId(), otherUser.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(availableItem1.getId());
    }

    @Test
    @DisplayName("Поиск вещи с доступом пользователя - недоступная вещь недоступна другим")
    void findByIdAccessibleByUser_ShouldReturnEmpty_ForUnavailableItemAndNotOwner() {

        User otherUser = User.builder()
                .name("Другой пользователь")
                .email("other@test.com")
                .build();
        entityManager.persist(otherUser);
        entityManager.flush();

        Optional<Item> result = itemRepository.findByIdAccessibleByUser(
                unavailableItem.getId(), otherUser.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Поиск вещей по ID запроса")
    void findByRequestId_ShouldReturnItemsWithRequest() {

        List<Item> result = itemRepository.findByRequestId(100L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(itemWithRequest.getId());
        assertThat(result.getFirst().getRequestId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Поиск вещей по списку ID запросов")
    void findByRequestIdIn_ShouldReturnItemsWithRequests() {

        List<Long> requestIds = List.of(100L, 200L);
        List<Item> result = itemRepository.findByRequestIdIn(requestIds);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getRequestId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Поиск вещей по несуществующему запросу")
    void findByRequestId_ShouldReturnEmpty_WhenRequestNotFound() {

        List<Item> result = itemRepository.findByRequestId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Постраничный поиск вещей владельца")
    void findByOwnerId_ShouldReturnPagedResults() {

        Pageable firstPage = PageRequest.of(0, 2);
        Page<Item> firstPageResult = itemRepository.findByOwnerId(owner1.getId(), firstPage);



        assertThat(firstPageResult.getContent()).hasSize(2);
        assertThat(firstPageResult.getTotalElements()).isEqualTo(3);
        assertThat(firstPageResult.getTotalPages()).isEqualTo(2);

        Pageable secondPage = PageRequest.of(1, 2);
        Page<Item> secondPageResult = itemRepository.findByOwnerId(owner1.getId(), secondPage);

        assertThat(secondPageResult.getContent()).hasSize(1);
    }
}