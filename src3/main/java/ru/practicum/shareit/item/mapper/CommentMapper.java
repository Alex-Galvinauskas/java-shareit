package ru.practicum.shareit.item.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "text", source = "comment.text")
    @Mapping(target = "created", source = "comment.created")
    @Mapping(target = "authorName", source = "author.name")
    CommentDto toDto(Comment comment, User author);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "text", source = "commentCreateDto.text")
    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "created", ignore = true)
    Comment toEntity(CommentCreateDto commentCreateDto, Item item, User author);
}