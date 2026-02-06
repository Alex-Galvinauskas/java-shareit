package ru.practicum.shareit.item.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemForRequestDto;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "lastBooking", ignore = true)
    @Mapping(target = "nextBooking", ignore = true)
    @Mapping(target = "comments", ignore = true)
    ItemDto toDto(Item item);

    @Mapping(target = "id", source = "itemDto.id")
    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "requestId", source = "itemDto.requestId")
    Item toEntity(ItemDto itemDto, Long ownerId);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "available", source = "available")
    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "requestId", source = "requestId")
    ItemForRequestDto toForRequestDto(Item item);
}