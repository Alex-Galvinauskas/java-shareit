package ru.practicum.shareit.request.repository;

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
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Тестирование репозитория запросов")
class ItemRequestRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    private User requester1;
    private User requester2;
    private User owner;
    private ItemRequest request1;
    private ItemRequest request2;
    private ItemRequest request3;
    private Item itemWithRequest;

    @BeforeEach
    void setUp() {
        requester1 = User.builder()
                .name("Запрашивающий 1")
                .email("requester1@test.com")
                .build();
        entityManager.persist(requester1);

        requester2 = User.builder()
                .name("Запрашивающий 2")
                .email("requester2@test.com")
                .build();
        entityManager.persist(requester2);

        owner = User.builder()
                .name("Владелец вещи")
                .email("owner@test.com")
                .build();
        entityManager.persist(owner);

        LocalDateTime now = LocalDateTime.now();

        request1 = ItemRequest.builder()
                .description("Нужна дрель для ремонта")
                .requestor(requester1)
                .created(now.minusDays(3))
                .build();
        entityManager.persist(request1);

        request2 = ItemRequest.builder()
                .description("Ищу шуруповерт на выходные")
                .requestor(requester2)
                .created(now.minusDays(2))
                .build();
        entityManager.persist(request2);

        request3 = ItemRequest.builder()
                .description("Требуется перфоратор для стройки")
                .requestor(requester1)
                .created(now.minusDays(1))
                .build();
        entityManager.persist(request3);

        itemWithRequest = Item.builder()
                .name("Дрель электрическая")
                .description("Мощная дрель для ремонта")
                .available(true)
                .ownerId(owner.getId())
                .requestId(request1.getId())
                .build();
        entityManager.persist(itemWithRequest);

        entityManager.flush();
    }

    @Test
    @DisplayName("Поиск запросов по ID запрашивающего - сортировка по дате создания DESC")
    void findByRequestorIdOrderByCreatedDesc_ShouldReturnRequestsInOrder() {

        List<ItemRequest> result = itemRequestRepository
                .findByRequestorIdOrderByCreatedDesc(requester1.getId());

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo(request3.getId());
        assertThat(result.get(1).getId()).isEqualTo(request1.getId());
    }

    @Test
    @DisplayName("Поиск запросов по ID запрашивающего - пользователь без запросов")
    void findByRequestorIdOrderByCreatedDesc_ShouldReturnEmpty_WhenUserHasNoRequests() {

        User newUser = User.builder()
                .name("Новый пользователь")
                .email("new@test.com")
                .build();
        entityManager.persist(newUser);
        entityManager.flush();

        List<ItemRequest> result = itemRequestRepository
                .findByRequestorIdOrderByCreatedDesc(newUser.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Поиск запросов других пользователей - исключая запросы пользователя и его ответы")
    void findOtherUsersRequestsWithoutUserResponses_ShouldReturnFilteredRequests() {

        Pageable pageable = PageRequest.of(0, 10);
        Page<ItemRequest> result = itemRequestRepository
                .findOtherUsersRequestsWithoutUserResponses(owner.getId(), pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(ItemRequest::getId)
                .containsExactlyInAnyOrder(request2.getId(), request3.getId());

        assertThat(result.getContent())
                .extracting(ItemRequest::getId)
                .doesNotContain(request1.getId());
    }

    @Test
    @DisplayName("Поиск запросов других пользователей - постраничный результат")
    void findOtherUsersRequestsWithoutUserResponses_ShouldReturnPagedResults() {


        for (int i = 4; i <= 6; i++) {
            ItemRequest newRequest = ItemRequest.builder()
                    .description("Запрос " + i)
                    .requestor(requester2)
                    .created(LocalDateTime.now().minusHours(i))
                    .build();
            entityManager.persist(newRequest);
        }
        entityManager.flush();

        Pageable firstPage = PageRequest.of(0, 2);
        Page<ItemRequest> firstPageResult = itemRequestRepository
                .findOtherUsersRequestsWithoutUserResponses(requester1.getId(), firstPage);

        assertThat(firstPageResult.getContent()).hasSize(2);
        assertThat(firstPageResult.getTotalElements()).isEqualTo(4);
    }

    @Test
    @DisplayName("Поиск запроса по ID с загрузкой запрашивающего")
    void findByIdWithRequestor_ShouldReturnRequestWithRequestorLoaded() {

        Optional<ItemRequest> result = itemRequestRepository
                .findByIdWithRequestor(request2.getId());

        assertThat(result).isPresent();

        ItemRequest foundRequest = result.get();
        assertThat(foundRequest.getId()).isEqualTo(request2.getId());
        assertThat(foundRequest.getDescription()).isEqualTo("Ищу шуруповерт на выходные");

        assertThat(foundRequest.getRequestor()).isNotNull();
        assertThat(foundRequest.getRequestor().getId()).isEqualTo(requester2.getId());
        assertThat(foundRequest.getRequestor().getName()).isEqualTo("Запрашивающий 2");
    }

    @Test
    @DisplayName("Поиск запроса по ID с загрузкой запрашивающего - запрос не найден")
    void findByIdWithRequestor_ShouldReturnEmpty_WhenRequestNotFound() {

        Optional<ItemRequest> result = itemRequestRepository
                .findByIdWithRequestor(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Сохранение нового запроса")
    void save_ShouldPersistItemRequest() {

        ItemRequest newRequest = ItemRequest.builder()
                .description("Нужен молоток для работы")
                .requestor(requester1)
                .created(LocalDateTime.now())
                .build();

        ItemRequest savedRequest = itemRequestRepository.save(newRequest);
        entityManager.flush();
        entityManager.clear();

        ItemRequest retrievedRequest = entityManager.find(ItemRequest.class, savedRequest.getId());

        assertThat(retrievedRequest).isNotNull();
        assertThat(retrievedRequest.getDescription()).isEqualTo("Нужен молоток для работы");
        assertThat(retrievedRequest.getRequestor().getId()).isEqualTo(requester1.getId());
    }

    @Test
    @DisplayName("Обновление запроса")
    void save_ShouldUpdateItemRequest() {

        String updatedDescription = "Обновленное описание запроса";
        request1.setDescription(updatedDescription);

        ItemRequest updatedRequest = itemRequestRepository.save(request1);
        entityManager.flush();
        entityManager.clear();

        ItemRequest retrievedRequest = entityManager.find(ItemRequest.class, request1.getId());

        assertThat(retrievedRequest).isNotNull();
        assertThat(retrievedRequest.getDescription()).isEqualTo(updatedDescription);
    }

    @Test
    @DisplayName("Удаление запроса")
    void delete_ShouldRemoveItemRequest() {

        Long requestId = request1.getId();
        itemRequestRepository.delete(request1);
        entityManager.flush();
        entityManager.clear();

        ItemRequest deletedRequest = entityManager.find(ItemRequest.class, requestId);
        assertThat(deletedRequest).isNull();
    }

    @Test
    @DisplayName("Поиск всех запросов")
    void findAll_ShouldReturnAllRequests() {

        List<ItemRequest> result = itemRequestRepository.findAll();

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("Поиск запроса по ID")
    void findById_ShouldReturnRequest() {

        var result = itemRequestRepository.findById(request3.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(request3.getId());
        assertThat(result.get().getDescription()).isEqualTo("Требуется перфоратор для стройки");
    }

    @Test
    @DisplayName("Проверка связи запроса с вещью через requestId")
    void requestItemRelationship_ShouldWorkCorrectly() {

        assertThat(itemWithRequest.getRequestId()).isEqualTo(request1.getId());

        Optional<ItemRequest> foundRequest = itemRequestRepository.findById(itemWithRequest.getRequestId());
        assertThat(foundRequest).isPresent();
        assertThat(foundRequest.get().getDescription()).isEqualTo("Нужна дрель для ремонта");
    }
}