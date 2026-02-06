package ru.practicum.shareit.request.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.shareit.request.dto.ItemForRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.model.ItemRequest;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ItemRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requestor", ignore = true)
    @Mapping(target = "created", ignore = true)
    ItemRequest toEntity(ItemRequestDto itemRequestDto);

    @Mapping(source = "request.id", target = "id")
    @Mapping(source = "request.description", target = "description")
    @Mapping(source = "request.created", target = "created")
    ItemRequestResponseDto toResponseDto(ItemRequest request, List<ItemForRequestDto> items);

    @Mapping(source = "request.id", target = "id")
    @Mapping(source = "request.description", target = "description")
    @Mapping(source = "request.requestor.id", target = "requestorId")
    @Mapping(source = "request.created", target = "created")
    ItemRequestWithItemsDto toWithItemsDto(ItemRequest request, List<ItemForRequestDto> items);


}