package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Тестирование сервиса комментариев")
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserService userService;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CommentService commentService;

    private User user;
    private CommentCreateDto commentCreateDto;
    private Comment comment;
    private CommentDto commentDto;
    private Booking booking;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Тестовый пользователь")
                .email("test@email.com")
                .build();

        Item item = Item.builder()
                .id(1L)
                .name("Тестовый предмет")
                .ownerId(2L)
                .build();

        commentCreateDto = CommentCreateDto.builder()
                .text("Тестовый комментарий")
                .build();

        comment = Comment.builder()
                .id(1L)
                .text("Тестовый комментарий")
                .itemId(1L)
                .authorId(1L)
                .created(LocalDateTime.now())
                .build();

        commentDto = CommentDto.builder()
                .id(1L)
                .text("Тестовый комментарий")
                .authorName("Тестовый пользователь")
                .created(LocalDateTime.now())
                .build();

        booking = Booking.builder()
                .id(1L)
                .itemId(1L)
                .bookerId(1L)
                .status(BookingStatus.APPROVED)
                .end(LocalDateTime.now().minusHours(1))
                .build();

    }

    @Test
    @DisplayName("Добавление комментария при наличии бронирования должно возвращать DTO комментария")
    void addComment_whenUserHasBookedItem_shouldReturnCommentDto() {
        when(userService.getUserEntityById(1L)).thenReturn(user);
        when(bookingRepository.findByItemIdAndBookerIdAndEndBeforeAndStatus(
                eq(1L), eq(1L), any(LocalDateTime.class),
                eq(BookingStatus.APPROVED)))
                .thenReturn(List.of(booking));

        when(commentMapper.toEntity(eq(commentCreateDto),
                any(Item.class), eq(user)))
                .thenReturn(comment);

        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toDto(comment, user)).thenReturn(commentDto);

        CommentDto result = commentService.addComment(1L, 1L, commentCreateDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Тестовый комментарий", result.getText());
        assertEquals("Тестовый пользователь", result.getAuthorName());


        verify(commentRepository).save(comment);
        verify(commentMapper).toDto(comment, user);
    }

    @Test
    @DisplayName("Добавление комментария без бронирования должно выбрасывать исключение")
    void addComment_whenUserHasNotBookedItem_shouldThrowBadRequestException() {
        when(userService.getUserEntityById(1L)).thenReturn(user);
        when(bookingRepository.findByItemIdAndBookerIdAndEndBeforeAndStatus(
                eq(1L), eq(1L), any(LocalDateTime.class),
                eq(BookingStatus.APPROVED)))
                .thenReturn(Collections.emptyList());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> commentService.addComment(1L, 1L, commentCreateDto));


        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Получение комментариев для пустого списка предметов должно возвращать пустую мапу")
    void getCommentsForItems_whenItemIdsIsEmpty_shouldReturnEmptyMap() {
        Map<Long, List<CommentDto>> result = commentService.getCommentsForItems(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Получение комментариев для null списка предметов должно возвращать пустую мапу")
    void getCommentsForItems_whenItemIdsIsNull_shouldReturnEmptyMap() {
        Map<Long, List<CommentDto>> result = commentService.getCommentsForItems(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Получение комментариев для предмета без комментариев должно возвращать пустой список")
    void getCommentsForItem_whenNoComments_shouldReturnEmptyList() {
        when(commentRepository.findByItemId(1L)).thenReturn(Collections.emptyList());

        List<CommentDto> result = commentService.getCommentsForItem(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Получение комментариев для предмета с комментариями должно возвращать список комментариев")
    void getCommentsForItem_whenCommentsExist_shouldReturnCommentList() {
        Comment comment1 = Comment.builder()
                .id(1L)
                .text("Комментарий 1")
                .itemId(1L)
                .authorId(1L)
                .created(LocalDateTime.now())
                .build();

        Comment comment2 = Comment.builder()
                .id(2L)
                .text("Комментарий 2")
                .itemId(1L)
                .authorId(2L)
                .created(LocalDateTime.now())
                .build();

        when(commentRepository.findByItemId(1L))
                .thenReturn(List.of(comment1, comment2));
        when(userService.getUsersByIds(anyList())).thenReturn(List.of(
                User.builder().id(1L).name("Пользователь 1").build(),
                User.builder().id(2L).name("Пользователь 2").build()
        ));
        when(commentMapper.toDto(any(Comment.class), any(User.class)))
                .thenAnswer(invocation -> {
                    Comment c = invocation.getArgument(0);
                    User u = invocation.getArgument(1);
                    return CommentDto.builder()
                            .id(c.getId())
                            .text(c.getText())
                            .authorName(u.getName())
                            .created(c.getCreated())
                            .build();
                });

        List<CommentDto> result = commentService.getCommentsForItem(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Получение комментариев для большого списка предметов должно ограничивать количество")
    @SuppressWarnings("unchecked")
    void getCommentsForItems_whenTooManyItemIds_shouldLimitAndLogWarning() {

        List<Long> itemIds = Collections.nCopies(3000, 1L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class),
                any(Object[].class)))
                .thenReturn(Collections.emptyList());

        Map<Long, List<CommentDto>> result = commentService.getCommentsForItems(itemIds);

        assertNotNull(result);
        verify(jdbcTemplate, atLeastOnce()).query(
                anyString(),
                any(RowMapper.class),
                any(Object[].class));

    }

    @Test
    @DisplayName("Добавление комментария с null пользователем должно выбрасывать исключение")
    void addComment_whenUserIsNull_shouldThrowNotFoundException() {
        when(userService.getUserEntityById(1L))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> commentService.addComment(1L, 1L, commentCreateDto));

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Получение комментариев для предмета с null автором должно создавать DTO с неизвестным автором")
    void getCommentsForItem_whenAuthorNotFound_shouldCreateDtoWithUnknownAuthor() {
        Comment comment = Comment.builder()
                .id(1L)
                .text("Комментарий")
                .itemId(1L)
                .authorId(999L)
                .created(LocalDateTime.now())
                .build();

        when(commentRepository.findByItemId(1L)).thenReturn(List.of(comment));
        when(userService.getUsersByIds(anyList())).thenReturn(Collections.emptyList());

        List<CommentDto> result = commentService.getCommentsForItem(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Неизвестный пользователь", result.getFirst().getAuthorName());
    }

    @Test
    @DisplayName("Пакетное получение комментариев для одного предмета должно возвращать корректные данные")
    void getCommentsForItems_whenSingleItem_shouldReturnCorrectData() {
        List<Long> itemIds = List.of(1L);

        when(jdbcTemplate.query(anyString(),
                any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(
                        new CommentService.CommentBatchResult(1L, commentDto)
                ));

        Map<Long, List<CommentDto>> result = commentService.getCommentsForItems(itemIds);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(1L));
        assertEquals(1, result.get(1L).size());
        assertEquals("Тестовый комментарий", result.get(1L).getFirst().getText());
    }

    @Test
    @DisplayName("Получение комментариев с большим количеством авторов должно использовать оптимизированный запрос")
    void getCommentsForItem_whenManyAuthors_shouldUseOptimizedQuery() {

        List<Comment> comments = new ArrayList<>();
        for (long i = 1; i <= 150; i++) {
            comments.add(Comment.builder()
                    .id(i)
                    .text("Комментарий " + i)
                    .itemId(1L)
                    .authorId(i)
                    .created(LocalDateTime.now())
                    .build());
        }

        when(commentRepository.findByItemId(1L)).thenReturn(comments);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class),
                any(Object[].class)))
                .thenReturn(createTestUsers(150));

        List<CommentDto> result = commentService.getCommentsForItem(1L);

        assertNotNull(result);
        assertEquals(150, result.size());

        verify(userService, never()).getUsersByIds(anyList());

        verify(jdbcTemplate, atLeastOnce()).query(anyString(),
                any(RowMapper.class), any(Object[].class));
    }

    @Test
    @DisplayName("Получение комментариев с малым количеством авторов должно использовать userService")
    void getCommentsForItem_whenFewAuthors_shouldUseUserService() {

        List<Comment> comments = new ArrayList<>();
        for (long i = 1; i <= 50; i++) {
            comments.add(Comment.builder()
                    .id(i)
                    .text("Комментарий " + i)
                    .itemId(1L)
                    .authorId(i)
                    .created(LocalDateTime.now())
                    .build());
        }

        when(commentRepository.findByItemId(1L)).thenReturn(comments);

        when(userService.getUsersByIds(anyList()))
                .thenReturn(createTestUsers(50));

        List<CommentDto> result = commentService.getCommentsForItem(1L);

        assertNotNull(result);
        assertEquals(50, result.size());

        verify(userService, times(1)).getUsersByIds(anyList());

        verify(jdbcTemplate, never()).query(anyString(),
                any(RowMapper.class), any(Object[].class));

    }

    private List<User> createTestUsers(int count) {
        List<User> users = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            users.add(User.builder()
                    .id(i)
                    .name("Пользователь " + i)
                    .email("user" + i + "@test.com")
                    .build());
        }
        return users;
    }

    @Test
    @DisplayName("Получение комментариев с ровно 100 авторами должно использовать userService")
    void getCommentsForItem_whenExactly100Authors_shouldUseUserService() {

        List<Comment> comments = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            comments.add(Comment.builder()
                    .id(i)
                    .text("Комментарий " + i)
                    .itemId(1L)
                    .authorId(i)
                    .created(LocalDateTime.now())
                    .build());
        }

        when(commentRepository.findByItemId(1L)).thenReturn(comments);

        when(userService.getUsersByIds(anyList()))
                .thenReturn(createTestUsers(100));

        List<CommentDto> result = commentService.getCommentsForItem(1L);

        assertNotNull(result);
        assertEquals(100, result.size());

        verify(userService, times(1)).getUsersByIds(anyList());

        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class),
                any(Object[].class));

    }

    @Test
    @DisplayName("Получение комментариев с более чем 100 авторами должно использовать оптимизированный запрос")
    void getCommentsForItem_when101Authors_shouldUseOptimizedQuery() {

        List<Comment> comments = new ArrayList<>();
        for (long i = 1; i <= 101; i++) {
            comments.add(Comment.builder()
                    .id(i)
                    .text("Комментарий " + i)
                    .itemId(1L)
                    .authorId(i)
                    .created(LocalDateTime.now())
                    .build());
        }

        when(commentRepository.findByItemId(1L)).thenReturn(comments);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class),
                any(Object[].class)))
                .thenReturn(createTestUsers(101));

        List<CommentDto> result = commentService.getCommentsForItem(1L);

        assertNotNull(result);
        assertEquals(101, result.size());

        verify(userService, never()).getUsersByIds(anyList());

        verify(jdbcTemplate, atLeastOnce()).query(anyString(), any(RowMapper.class),
                any(Object[].class));

    }

    @Test
    @DisplayName("Добавление комментария с пустым текстом должно обрабатываться корректно")
    void addComment_whenEmptyText_shouldProcessCorrectly() {
        CommentCreateDto emptyComment = CommentCreateDto.builder().text("").build();

        when(userService.getUserEntityById(1L)).thenReturn(user);
        when(bookingRepository.findByItemIdAndBookerIdAndEndBeforeAndStatus(
                eq(1L), eq(1L), any(LocalDateTime.class),
                eq(BookingStatus.APPROVED)))
                .thenReturn(List.of(booking));

        Comment savedComment = Comment.builder()
                .id(1L)
                .text("")
                .itemId(1L)
                .authorId(1L)
                .created(LocalDateTime.now())
                .build();

        when(commentMapper.toEntity(any(), any(),
                any())).thenReturn(savedComment);
        when(commentRepository.save(any())).thenReturn(savedComment);
        when(commentMapper.toDto(any(), any())).thenReturn(CommentDto.builder()
                .id(1L)
                .text("")
                .authorName("Тестовый пользователь")
                .created(LocalDateTime.now())
                .build());

        CommentDto result = commentService.addComment(1L, 1L, emptyComment);

        assertNotNull(result);
        assertEquals("", result.getText());
    }
}