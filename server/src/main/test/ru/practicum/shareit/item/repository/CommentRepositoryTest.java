package ru.practicum.shareit.item.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Тестирование репозитория комментариев")
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    private User author1;
    private Item item1;
    private Item item2;
    private Comment comment1;
    private Comment comment2;
    private Comment comment3;

    @BeforeEach
    void setUp() {
        // Создание пользователей
        author1 = User.builder()
                .name("Автор 1")
                .email("author1@test.com")
                .build();
        entityManager.persist(author1);

        User author2 = User.builder()
                .name("Автор 2")
                .email("author2@test.com")
                .build();
        entityManager.persist(author2);

        // Создание вещей
        item1 = Item.builder()
                .name("Вещь 1")
                .description("Описание вещи 1")
                .available(true)
                .ownerId(author1.getId())
                .build();
        entityManager.persist(item1);

        item2 = Item.builder()
                .name("Вещь 2")
                .description("Описание вещи 2")
                .available(true)
                .ownerId(author2.getId())
                .build();
        entityManager.persist(item2);

        LocalDateTime now = LocalDateTime.now();

        // Создание комментариев
        comment1 = Comment.builder()
                .text("Отличная вещь!")
                .itemId(item1.getId())
                .authorId(author1.getId())
                .created(now.minusDays(2))
                .build();
        entityManager.persist(comment1);

        comment2 = Comment.builder()
                .text("Очень удобно пользоваться")
                .itemId(item1.getId())
                .authorId(author2.getId())
                .created(now.minusDays(1))
                .build();
        entityManager.persist(comment2);

        comment3 = Comment.builder()
                .text("Качество на высоте")
                .itemId(item2.getId())
                .authorId(author1.getId())
                .created(now)
                .build();
        entityManager.persist(comment3);

        entityManager.flush();
    }

    @Test
    @DisplayName("Поиск комментариев по ID вещи - один комментарий")
    void findByItemId_ShouldReturnComments_ForItemWithOneComment() {

        List<Comment> result = commentRepository.findByItemId(item2.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(comment3.getId());
        assertThat(result.getFirst().getItemId()).isEqualTo(item2.getId());
    }

    @Test
    @DisplayName("Поиск комментариев по ID вещи - несколько комментариев")
    void findByItemId_ShouldReturnComments_ForItemWithMultipleComments() {

        List<Comment> result = commentRepository.findByItemId(item1.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Comment::getItemId)
                .containsOnly(item1.getId());
    }

    @Test
    @DisplayName("Поиск комментариев по ID вещи - правильный порядок (по дате создания)")
    void findByItemId_ShouldReturnComments_InOrder() {

        List<Comment> result = commentRepository.findByItemId(item1.getId());

        assertThat(result.get(0).getId()).isEqualTo(comment1.getId());
        assertThat(result.get(1).getId()).isEqualTo(comment2.getId());
    }

    @Test
    @DisplayName("Поиск комментариев по несуществующему ID вещи")
    void findByItemId_ShouldReturnEmpty_WhenItemNotFound() {

        List<Comment> result = commentRepository.findByItemId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Сохранение нового комментария")
    void save_ShouldPersistComment() {

        Comment newComment = Comment.builder()
                .text("Новый тестовый комментарий")
                .itemId(item1.getId())
                .authorId(author1.getId())
                .created(LocalDateTime.now())
                .build();

        Comment savedComment = commentRepository.save(newComment);
        entityManager.flush();
        entityManager.clear();

        Comment retrievedComment = entityManager.find(Comment.class, savedComment.getId());

        assertThat(retrievedComment).isNotNull();
        assertThat(retrievedComment.getText()).isEqualTo("Новый тестовый комментарий");
        assertThat(retrievedComment.getItemId()).isEqualTo(item1.getId());
        assertThat(retrievedComment.getAuthorId()).isEqualTo(author1.getId());
    }

    @Test
    @DisplayName("Обновление комментария")
    void save_ShouldUpdateComment() {

        String updatedText = "Обновленный текст комментария";
        comment1.setText(updatedText);

        Comment updatedComment = commentRepository.save(comment1);
        entityManager.flush();
        entityManager.clear();

        Comment retrievedComment = entityManager.find(Comment.class, comment1.getId());

        assertThat(retrievedComment).isNotNull();
        assertThat(retrievedComment.getText()).isEqualTo(updatedText);
    }

    @Test
    @DisplayName("Удаление комментария")
    void delete_ShouldRemoveComment() {

        Long commentId = comment1.getId();
        commentRepository.delete(comment1);
        entityManager.flush();
        entityManager.clear();

        Comment deletedComment = entityManager.find(Comment.class, commentId);
        assertThat(deletedComment).isNull();
    }

    @Test
    @DisplayName("Поиск всех комментариев")
    void findAll_ShouldReturnAllComments() {

        List<Comment> result = commentRepository.findAll();

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("Поиск комментария по ID")
    void findById_ShouldReturnComment() {

        var result = commentRepository.findById(comment2.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(comment2.getId());
        assertThat(result.get().getText()).isEqualTo("Очень удобно пользоваться");
    }
}