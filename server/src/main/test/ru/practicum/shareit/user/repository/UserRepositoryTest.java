package ru.practicum.shareit.user.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@DisplayName("Тестирование репозитория пользователей")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .name("Пользователь 1")
                .email("user1@test.com")
                .build();
        entityManager.persist(user1);

        user2 = User.builder()
                .name("Пользователь 2")
                .email("user2@test.com")
                .build();
        entityManager.persist(user2);

        user3 = User.builder()
                .name("Пользователь 3")
                .email("user3@test.com")
                .build();
        entityManager.persist(user3);

        entityManager.flush();
    }

    @Test
    @DisplayName("Поиск пользователя по email - пользователь существует")
    void findByEmail_ShouldReturnUser_WhenEmailExists() {

        Optional<User> result = userRepository.findByEmail("user2@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("user2@test.com");
        assertThat(result.get().getName()).isEqualTo("Пользователь 2");
    }

    @Test
    @DisplayName("Поиск пользователя по email - пользователь не существует")
    void findByEmail_ShouldReturnEmpty_WhenEmailNotFound() {

        Optional<User> result = userRepository.findByEmail("nonexistent@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Проверка существования email у другого пользователя - email существует у другого")
    void existsByEmailAndIdNot_ShouldReturnTrue_WhenEmailExistsForOtherUser() {

        boolean result = userRepository.existsByEmailAndIdNot(
                "user2@test.com", user1.getId());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Проверка существования email у другого пользователя - email принадлежит тому же пользователю")
    void existsByEmailAndIdNot_ShouldReturnFalse_WhenEmailBelongsToSameUser() {

        boolean result = userRepository.existsByEmailAndIdNot(
                "user2@test.com", user2.getId());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Проверка существования email у другого пользователя - email не существует")
    void existsByEmailAndIdNot_ShouldReturnFalse_WhenEmailNotFound() {

        boolean result = userRepository.existsByEmailAndIdNot(
                "nonexistent@test.com", user1.getId());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Поиск пользователей по списку ID - все ID существуют")
    void findByIdIn_ShouldReturnUsers_WhenAllIdsExist() {

        List<Long> userIds = List.of(user1.getId(), user3.getId());
        List<User> result = userRepository.findByIdIn(userIds);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getId)
                .containsExactlyInAnyOrder(user1.getId(), user3.getId());
    }

    @Test
    @DisplayName("Поиск пользователей по списку ID - некоторые ID не существуют")
    void findByIdIn_ShouldReturnOnlyExistingUsers() {

        List<Long> userIds = List.of(user1.getId(), 999L, user2.getId());
        List<User> result = userRepository.findByIdIn(userIds);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getId)
                .containsExactlyInAnyOrder(user1.getId(), user2.getId());
    }

    @Test
    @DisplayName("Поиск пользователей по пустому списку ID")
    void findByIdIn_ShouldReturnEmpty_WhenIdsListIsEmpty() {

        List<User> result = userRepository.findByIdIn(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Поиск пользователей по списку ID - null список")
    void findByIdIn_ShouldReturnEmpty_WhenIdsListIsNull() {

        List<User> result = userRepository.findByIdIn(null);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Постраничный поиск всех пользователей")
    void findAll_ShouldReturnPagedResults() {

        Pageable firstPage = PageRequest.of(0, 2);
        Page<User> firstPageResult = userRepository.findAll(firstPage);



        assertThat(firstPageResult.getContent()).hasSize(2);
        assertThat(firstPageResult.getTotalElements()).isEqualTo(3);
        assertThat(firstPageResult.getTotalPages()).isEqualTo(2);

        Pageable secondPage = PageRequest.of(1, 2);
        Page<User> secondPageResult = userRepository.findAll(secondPage);

        assertThat(secondPageResult.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Сохранение нового пользователя")
    void save_ShouldPersistUser() {

        User newUser = User.builder()
                .name("Новый пользователь")
                .email("newuser@test.com")
                .build();

        User savedUser = userRepository.save(newUser);
        entityManager.flush();
        entityManager.clear();


        User retrievedUser = entityManager.find(User.class, savedUser.getId());

        assertThat(retrievedUser).isNotNull();
        assertThat(retrievedUser.getName()).isEqualTo("Новый пользователь");
        assertThat(retrievedUser.getEmail()).isEqualTo("newuser@test.com");
    }

    @Test
    @DisplayName("Обновление пользователя")
    void save_ShouldUpdateUser() {

        String updatedName = "Обновленное имя";
        String updatedEmail = "updated@test.com";

        user1.setName(updatedName);
        user1.setEmail(updatedEmail);

        User updatedUser = userRepository.save(user1);
        entityManager.flush();
        entityManager.clear();


        User retrievedUser = entityManager.find(User.class, user1.getId());

        assertThat(retrievedUser).isNotNull();
        assertThat(retrievedUser.getName()).isEqualTo(updatedName);
        assertThat(retrievedUser.getEmail()).isEqualTo(updatedEmail);
    }

    @Test
    @DisplayName("Удаление пользователя")
    void delete_ShouldRemoveUser() {

        Long userId = user1.getId();
        userRepository.delete(user1);
        entityManager.flush();
        entityManager.clear();


        User deletedUser = entityManager.find(User.class, userId);
        assertThat(deletedUser).isNull();
    }

    @Test
    @DisplayName("Проверка уникальности email при сохранении")
    void save_ShouldThrowException_WhenEmailAlreadyExists() {
        User duplicateEmailUser = User.builder()
                .name("Дубликат email")
                .email("user1@test.com")
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(duplicateEmailUser);
            entityManager.flush();
        });
    }
}