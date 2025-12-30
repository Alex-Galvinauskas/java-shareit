package ru.practicum.shareit.item.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "requestId", source = "requestId")
    ItemDto toDto(Item item);

    @Mapping(target = "id", source = "itemDto.id")
    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "requestId", source = "itemDto.requestId")
    Item toEntity(ItemDto itemDto, Long ownerId);
}