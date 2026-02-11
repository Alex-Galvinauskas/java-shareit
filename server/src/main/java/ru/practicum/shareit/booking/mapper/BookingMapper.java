package ru.practicum.shareit.booking.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;

@Mapper(componentModel = "spring", uses = {ItemMapper.class, UserMapper.class})
public interface BookingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "itemId", ignore = true)
    @Mapping(target = "bookerId", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "created", ignore = true)
    Booking toEntity(BookingRequestDto bookingRequestDto);

    @Mapping(source = "booking.id", target = "id")
    @Mapping(source = "booking.start", target = "start")
    @Mapping(source = "booking.end", target = "end")
    @Mapping(source = "booking.status", target = "status")
    @Mapping(source = "booking.created", target = "created")
    BookingResponseDto toResponseDto(Booking booking, Item item, User booker);
}