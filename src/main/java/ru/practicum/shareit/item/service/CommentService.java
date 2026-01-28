package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private static final String UNKNOWN_USER_NAME = "Неизвестный пользователь";
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ITEMS_PER_QUERY = 2000;

    private final CommentRepository commentRepository;
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final CommentMapper commentMapper;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentCreateDto commentCreateDto) {
        log.info("Добавление комментария к вещи ID={} пользователем ID={}", itemId, userId);

        User author = userService.getUserEntityById(userId);
        Item item = Item.builder().id(itemId).build();

        LocalDateTime now = LocalDateTime.now();
        List<Booking> userBookings = bookingRepository
                .findByItemIdAndBookerIdAndEndBeforeAndStatus(
                        itemId, userId, now, BookingStatus.APPROVED);

        if (userBookings.isEmpty()) {
            throw new BadRequestException("Пользователь не брал эту вещь в аренду");
        }

        Comment comment = commentMapper.toEntity(commentCreateDto, item, author);
        comment.setCreated(now);
        comment = commentRepository.save(comment);

        CommentDto commentDto = commentMapper.toDto(comment, author);

        log.info("Комментарий успешно добавлен с ID={}, authorId={}, itemId={}",
                comment.getId(), author.getId(), itemId);
        return commentDto;
    }

    @Transactional(readOnly = true)
    public Map<Long, List<CommentDto>> getCommentsForItems(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        if (itemIds.size() > MAX_ITEMS_PER_QUERY) {
            log.warn("Слишком большой список itemIds ({}). Ограничение: {}",
                    itemIds.size(), MAX_ITEMS_PER_QUERY);
            itemIds = itemIds.subList(0, MAX_ITEMS_PER_QUERY);
        }

        return getCommentsWithAuthorsOptimized(itemIds);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsForItem(Long itemId) {
        List<Comment> comments = commentRepository.findByItemId(itemId);

        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());

        Map<Long, User> authorsMap = getUsersByIdsOptimized(authorIds);

        return comments.stream()
                .map(comment -> mapCommentToDto(comment,
                        authorsMap.get(comment.getAuthorId())))
                .collect(Collectors.toList());
    }


    private Map<Long, List<CommentDto>> getCommentsWithAuthorsOptimized(List<Long> itemIds) {
        Map<Long, List<CommentDto>> result = new HashMap<>();

        for (int i = 0; i < itemIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, itemIds.size());
            List<Long> batch = itemIds.subList(i, end);

            Map<Long, List<CommentDto>> batchResult = getCommentsWithAuthorsBatch(batch);

            batchResult.forEach((itemId, comments) ->
                    result.computeIfAbsent(itemId, k ->
                            new ArrayList<>()).addAll(comments));
        }

        return result;
    }

    private Map<Long, List<CommentDto>> getCommentsWithAuthorsBatch(List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = String.join(",", Collections.nCopies(itemIds.size(), "?"));
        String sql = """
            SELECT
                c.id as comment_id,
                c.text as comment_text,
                c.item_id as item_id,
                c.author_id as author_id,
                c.created as created,
                u.name as author_name
            FROM comments c
            LEFT JOIN users u ON c.author_id = u.id
            WHERE c.item_id IN (%s)
            ORDER BY c.item_id, c.created DESC
            """.formatted(placeholders);

        RowMapper<CommentBatchResult> rowMapper = (rs, rowNum) -> CommentBatchResult.builder()
                .itemId(rs.getLong("item_id"))
                .commentDto(CommentDto.builder()
                        .id(rs.getLong("comment_id"))
                        .text(rs.getString("comment_text"))
                        .created(rs.getTimestamp("created").toLocalDateTime())
                        .authorName(rs.getString("author_name") != null ?
                                rs.getString("author_name") : UNKNOWN_USER_NAME)
                        .build())
                .build();

        List<CommentBatchResult> batchResults = jdbcTemplate.query(
                sql, rowMapper, itemIds.toArray());

        Map<Long, List<CommentDto>> result = new HashMap<>();
        for (CommentBatchResult batchResult : batchResults) {
            result.computeIfAbsent(batchResult.getItemId(), k -> new ArrayList<>())
                    .add(batchResult.getCommentDto());
        }

        return result;
    }


    private Map<Long, User> getUsersByIdsOptimized(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        if (userIds.size() <= 100) {
            return userService.getUsersByIds(new ArrayList<>(userIds)).stream()
                    .collect(Collectors.toMap(User::getId, user -> user));
        }

        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
        String sql = """
            SELECT id, name, email
            FROM users
            WHERE id IN (%s)
            """.formatted(placeholders);

        return jdbcTemplate.query(sql,
                        (ResultSet rs, int rowNum) -> User.builder()
                                .id(rs.getLong("id"))
                                .name(rs.getString("name"))
                                .email(rs.getString("email"))
                                .build(),
                        userIds.toArray())
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private CommentDto mapCommentToDto(Comment comment, User author) {
        if (author != null) {
            return commentMapper.toDto(comment, author);
        } else {
            return createCommentDtoWithUnknownAuthor(comment);
        }
    }

    private CommentDto createCommentDtoWithUnknownAuthor(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .created(comment.getCreated())
                .authorName(UNKNOWN_USER_NAME)
                .build();
    }

    @lombok.Builder
    @lombok.Data
    private static class CommentBatchResult {
        private Long itemId;
        private CommentDto commentDto;
    }
}