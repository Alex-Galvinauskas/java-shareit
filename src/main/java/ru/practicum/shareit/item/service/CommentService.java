package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private static final String UNKNOWN_USER_NAME = "Неизвестный пользователь";

    private final CommentRepository commentRepository;
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final CommentMapper commentMapper;

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

        List<Comment> comments = commentRepository.findByItemIdIn(itemIds);

        if (comments.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> authorsMap = userService.getUsersByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return comments.stream()
                .collect(Collectors.groupingBy(
                        Comment::getItemId,
                        Collectors.mapping(comment -> mapCommentToDto(comment,
                                        authorsMap.get(comment.getAuthorId())),
                                Collectors.toList())
                ));
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsForItem(Long itemId) {
        List<Comment> comments = commentRepository.findByItemId(itemId);

        return comments.stream()
                .map(comment -> {
                    try {
                        User author = userService.getUserEntityById(comment.getAuthorId());
                        return commentMapper.toDto(comment, author);
                    } catch (NotFoundException e) {
                        log.warn("Не удалось найти автора комментария с ID={}", comment.getAuthorId());
                        return createCommentDtoWithUnknownAuthor(comment);
                    }
                })
                .collect(Collectors.toList());
    }

    private CommentDto mapCommentToDto(Comment comment, User author) {
        if (author != null) {
            return commentMapper.toDto(comment, author);
        } else {
            return createCommentDtoWithUnknownAuthor(comment);
        }
    }

    private CommentDto createCommentDtoWithUnknownAuthor(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setCreated(comment.getCreated());
        dto.setAuthorName(UNKNOWN_USER_NAME);
        return dto;
    }
}