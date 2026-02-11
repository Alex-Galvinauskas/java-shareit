package ru.practicum.shareit.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemForRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование сервиса запросов на аренду")
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @Mock
    private ItemRequestMapper itemRequestMapper;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemRequestServiceImpl itemRequestService;

    private User user;
    private User otherUser;
    private ItemRequest itemRequest;
    private ItemRequestDto itemRequestDto;
    private ItemRequestResponseDto itemRequestResponseDto;
    private ItemRequestWithItemsDto itemRequestWithItemsDto;
    private Item item;
    private ItemForRequestDto itemForRequestDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@email.com")
                .build();

        otherUser = User.builder()
                .id(2L)
                .name("Other User")
                .email("other@email.com")
                .build();

        itemRequest = ItemRequest.builder()
                .id(1L)
                .description("Нужна дрель")
                .requestor(user)
                .created(LocalDateTime.now())
                .build();

        itemRequestDto = ItemRequestDto.builder()
                .description("Нужна дрель")
                .build();

        itemForRequestDto = ItemForRequestDto.builder()
                .id(1L)
                .name("Дрель")
                .description("Мощная дрель")
                .available(true)
                .ownerId(2L)
                .requestId(1L)
                .build();

        itemRequestResponseDto = ItemRequestResponseDto.builder()
                .id(1L)
                .description("Нужна дрель")
                .created(LocalDateTime.now())
                .items(List.of(itemForRequestDto))
                .build();

        itemRequestWithItemsDto = ItemRequestWithItemsDto.builder()
                .id(1L)
                .description("Нужна дрель")
                .requestorId(1L)
                .created(LocalDateTime.now())
                .items(List.of(itemForRequestDto))
                .build();

        item = Item.builder()
                .id(1L)
                .name("Дрель")
                .description("Мощная дрель")
                .available(true)
                .ownerId(2L)
                .requestId(1L)
                .build();
    }

    @Test
    @DisplayName("Создание запроса - успешный сценарий")
    void create_ShouldCreateRequestSuccessfully() {
        when(userService.getUserEntityById(anyLong())).thenReturn(user);
        when(itemRequestRepository.save(any(ItemRequest.class))).thenReturn(itemRequest);
        when(itemRequestMapper.toResponseDto(any(ItemRequest.class), anyList()))
                .thenReturn(itemRequestResponseDto);

        ItemRequestResponseDto result = itemRequestService.create(user.getId(), itemRequestDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Нужна дрель");

        verify(userService).getUserEntityById(user.getId());
        verify(itemRequestRepository).save(any(ItemRequest.class));
        verify(itemRequestMapper).toResponseDto(any(ItemRequest.class),
                eq(Collections.emptyList()));
    }

    @Test
    @DisplayName("Создание запроса - пользователь не найден")
    void create_ShouldThrowException_WhenUserNotFound() {
        when(userService.getUserEntityById(anyLong())).thenThrow(new NotFoundException("User not found"));

        assertThatThrownBy(() -> itemRequestService.create(999L, itemRequestDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userService).getUserEntityById(999L);
        verifyNoInteractions(itemRequestRepository);
    }

    @Test
    @DisplayName("Получение собственных запросов - успешный сценарий")
    void getOwnRequests_ShouldReturnUserRequests() {
        when(userService.getUserEntityById(anyLong())).thenReturn(user);
        when(itemRequestRepository.findByRequestorIdOrderByCreatedDesc(anyLong()))
                .thenReturn(List.of(itemRequest));
        when(itemRepository.findByRequestIdIn(anyList())).thenReturn(List.of(item));
        when(itemMapper.toForRequestDto(any(Item.class))).thenReturn(itemForRequestDto);
        when(itemRequestMapper.toResponseDto(any(ItemRequest.class), anyList()))
                .thenReturn(itemRequestResponseDto);

        List<ItemRequestResponseDto> result = itemRequestService.getOwnRequests(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        assertThat(result.getFirst().getItems()).hasSize(1);

        verify(userService).getUserEntityById(user.getId());
        verify(itemRequestRepository).findByRequestorIdOrderByCreatedDesc(user.getId());
        verify(itemRepository).findByRequestIdIn(List.of(1L));
    }

    @Test
    @DisplayName("Получение собственных запросов - пустой список")
    void getOwnRequests_ShouldReturnEmptyList_WhenNoRequests() {
        when(userService.getUserEntityById(anyLong())).thenReturn(user);
        when(itemRequestRepository.findByRequestorIdOrderByCreatedDesc(anyLong()))
                .thenReturn(Collections.emptyList());

        List<ItemRequestResponseDto> result = itemRequestService.getOwnRequests(user.getId());

        assertThat(result).isEmpty();

        verify(userService).getUserEntityById(user.getId());
        verify(itemRequestRepository).findByRequestorIdOrderByCreatedDesc(user.getId());
        verifyNoInteractions(itemRepository);
    }

    @Test
    @DisplayName("Получение запросов других пользователей - успешный сценарий")
    void getOtherUsersRequests_ShouldReturnOtherUsersRequests() {
        ItemRequest otherUserRequest = ItemRequest.builder()
                .id(2L)
                .description("Нужен шуруповерт")
                .requestor(otherUser)
                .created(LocalDateTime.now().minusDays(1))
                .build();

        Page<ItemRequest> page = new PageImpl<>(List.of(otherUserRequest));
        when(userService.getUserEntityById(anyLong())).thenReturn(user);
        when(itemRequestRepository.findOtherUsersRequestsWithoutUserResponses(anyLong(),
                any(Pageable.class)))
                .thenReturn(page);
        when(itemRepository.findByRequestIdIn(anyList())).thenReturn(Collections.emptyList());
        when(itemRequestMapper.toWithItemsDto(any(ItemRequest.class), anyList()))
                .thenReturn(itemRequestWithItemsDto);

        List<ItemRequestWithItemsDto> result =
                itemRequestService.getOtherUsersRequests(user.getId(), 0, 10);

        assertThat(result).hasSize(1);

        verify(userService).getUserEntityById(user.getId());
        verify(itemRequestRepository).findOtherUsersRequestsWithoutUserResponses(eq(user.getId()),
                any(Pageable.class));
    }

    @Test
    @DisplayName("Получение запросов других пользователей - невалидные параметры пагинации")
    void getOtherUsersRequests_ShouldThrowException_WhenInvalidPagination() {
        when(userService.getUserEntityById(anyLong())).thenReturn(user);

        assertThatThrownBy(() -> itemRequestService.getOtherUsersRequests(user.getId(),
                -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Начальный индекс не может быть отрицательным");

        assertThatThrownBy(() -> itemRequestService.getOtherUsersRequests(user.getId(),
                0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Размер страницы должен быть больше 0");
    }

    @Test
    @DisplayName("Получение запроса по ID - успешный сценарий")
    void getById_ShouldReturnRequest() {
        when(userService.getUserEntityById(anyLong())).thenReturn(user);
        when(itemRequestRepository.findByIdWithRequestor(anyLong()))
                .thenReturn(Optional.of(itemRequest));
        when(itemRepository.findByRequestId(anyLong())).thenReturn(List.of(item));
        when(itemMapper.toForRequestDto(any(Item.class))).thenReturn(itemForRequestDto);
        when(itemRequestMapper.toWithItemsDto(any(ItemRequest.class), anyList()))
                .thenReturn(itemRequestWithItemsDto);

        ItemRequestWithItemsDto result = itemRequestService.getById(user.getId(), 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);

        verify(userService).getUserEntityById(user.getId());
        verify(itemRequestRepository).findByIdWithRequestor(1L);
        verify(itemRepository).findByRequestId(1L);
    }

    @Test
    @DisplayName("Получение запроса по ID - запрос не найден")
    void getById_ShouldThrowException_WhenRequestNotFound() {
        when(userService.getUserEntityById(anyLong())).thenReturn(user);
        when(itemRequestRepository.findByIdWithRequestor(anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                itemRequestService.getById(user.getId(), 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Запрос с id=999 не найден");

        verify(userService).getUserEntityById(user.getId());
        verify(itemRequestRepository).findByIdWithRequestor(999L);
    }

    @Test
    @DisplayName("Получение запроса по ID - пользователь не найден")
    void getById_ShouldThrowException_WhenUserNotFound() {
        when(userService.getUserEntityById(anyLong()))
                .thenThrow(new NotFoundException("User not found"));

        assertThatThrownBy(() ->
                itemRequestService.getById(999L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userService).getUserEntityById(999L);
        verifyNoInteractions(itemRequestRepository);
    }

    @Test
    @DisplayName("Проверка пагинации - корректная работа с реальными данными")
    void getOtherUsersRequests_ShouldHandlePaginationCorrectly() {
        ItemRequest request1 = ItemRequest.builder()
                .id(1L)
                .description("Запрос 1")
                .requestor(otherUser)
                .created(LocalDateTime.now().minusDays(3))
                .build();

        ItemRequest request2 = ItemRequest.builder()
                .id(2L)
                .description("Запрос 2")
                .requestor(otherUser)
                .created(LocalDateTime.now().minusDays(2))
                .build();

        ItemRequest request3 = ItemRequest.builder()
                .id(3L)
                .description("Запрос 3")
                .requestor(otherUser)
                .created(LocalDateTime.now().minusDays(1))
                .build();

        when(userService.getUserEntityById(anyLong())).thenReturn(user);
        Page<ItemRequest> firstPage = new PageImpl<>(
                List.of(request3, request2),
                PageRequest.of(0, 2),
                3
        );

        when(itemRequestRepository.findOtherUsersRequestsWithoutUserResponses(
                eq(user.getId()),
                any(Pageable.class)))
                .thenReturn(firstPage);

        when(itemRepository.findByRequestIdIn(anyList()))
                .thenReturn(Collections.emptyList());
        when(itemRequestMapper.toWithItemsDto(any(ItemRequest.class), anyList()))
                .thenReturn(itemRequestWithItemsDto);

        List<ItemRequestWithItemsDto> firstPageResult =
                itemRequestService.getOtherUsersRequests(user.getId(), 0, 2);

        assertThat(firstPageResult).hasSize(2);

        verify(itemRequestRepository).findOtherUsersRequestsWithoutUserResponses(
                eq(user.getId()),
                argThat(pageable -> {
                    PageRequest pr = (PageRequest) pageable;
                    return pr.getPageNumber() == 0 && pr.getPageSize() == 2;
                }));

        reset(itemRequestRepository);

        Page<ItemRequest> secondPage = new PageImpl<>(
                List.of(request1),
                PageRequest.of(1, 2),
                3
        );

        when(itemRequestRepository.findOtherUsersRequestsWithoutUserResponses(
                eq(user.getId()),
                any(Pageable.class)))
                .thenReturn(secondPage);

        when(itemRepository.findByRequestIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        List<ItemRequestWithItemsDto> secondPageResult =
                itemRequestService.getOtherUsersRequests(user.getId(), 2, 2);

        assertThat(secondPageResult).hasSize(1);

        verify(itemRequestRepository).findOtherUsersRequestsWithoutUserResponses(
                eq(user.getId()),
                argThat(pageable -> {
                    PageRequest pr = (PageRequest) pageable;
                    return pr.getPageNumber() == 1 && pr.getPageSize() == 2;
                }));
    }

    @Test
    @DisplayName("Проверка пагинации - граничные случаи")
    void getOtherUsersRequests_ShouldHandleBoundaryCases() {
        when(userService.getUserEntityById(anyLong())).thenReturn(user);
        Page<ItemRequest> emptyPage = new PageImpl<>(Collections.emptyList());
        when(itemRequestRepository.findOtherUsersRequestsWithoutUserResponses(anyLong(),
                any(Pageable.class)))
                .thenReturn(emptyPage);

        itemRequestService.getOtherUsersRequests(user.getId(), 0, 10);
        verify(itemRequestRepository).findOtherUsersRequestsWithoutUserResponses(
                eq(user.getId()),
                argThat(pageable -> {
                    PageRequest pr = (PageRequest) pageable;
                    return pr.getPageNumber() == 0 && pr.getPageSize() == 10;
                }));

        itemRequestService.getOtherUsersRequests(user.getId(), 15, 10);
        verify(itemRequestRepository).findOtherUsersRequestsWithoutUserResponses(
                eq(user.getId()),
                argThat(pageable -> {
                    PageRequest pr = (PageRequest) pageable;
                    return pr.getPageNumber() == 1 && pr.getPageSize() == 10;
                }));

        itemRequestService.getOtherUsersRequests(user.getId(), 1000, 100);
        verify(itemRequestRepository).findOtherUsersRequestsWithoutUserResponses(
                eq(user.getId()),
                argThat(pageable -> {
                    PageRequest pr = (PageRequest) pageable;
                    return pr.getPageNumber() == 10 && pr.getPageSize() == 100;
                }));
    }
}