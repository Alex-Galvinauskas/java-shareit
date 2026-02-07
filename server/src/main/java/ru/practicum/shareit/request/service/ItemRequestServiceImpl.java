package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository itemRequestRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ItemRequestMapper itemRequestMapper;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemRequestResponseDto create(Long userId, ItemRequestDto itemRequestDto) {
        log.info("Создание нового запроса на вещь пользователем с ID={}", userId);

        User requester = userService.getUserEntityById(userId);

        ItemRequest itemRequest = ItemRequest.builder()
                .description(itemRequestDto.getDescription())
                .requestor(requester)
                .created(LocalDateTime.now())
                .build();

        ItemRequest savedRequest = itemRequestRepository.save(itemRequest);

        log.info("Запрос успешно создан с ID={}", savedRequest.getId());
        return itemRequestMapper.toResponseDto(savedRequest, Collections.emptyList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemRequestResponseDto> getOwnRequests(Long userId) {
        log.info("Получение собственных запросов пользователя с ID={}", userId);

        userService.getUserEntityById(userId);

        // Получаем запросы пользователя
        List<ItemRequest> requests = itemRequestRepository.findByRequestorIdOrderByCreatedDesc(userId);

        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        // Получаем ID всех запросов
        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .collect(Collectors.toList());

        // Загружаем все связанные вещи за один запрос
        List<Item> items = itemRepository.findByRequestIdIn(requestIds);

        // Группируем вещи по ID запроса
        Map<Long, List<ItemForRequestDto>> itemsByRequestId = items.stream()
                .collect(Collectors.groupingBy(
                        Item::getRequestId,
                        Collectors.mapping(itemMapper::toForRequestDto, Collectors.toList())
                ));

        // Маппим результат
        return requests.stream()
                .map(request -> itemRequestMapper.toResponseDto(
                        request,
                        itemsByRequestId.getOrDefault(request.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemRequestWithItemsDto> getOtherUsersRequests(Long userId, int from, int size) {
        log.info("Получение запросов других пользователей для пользователя с ID={}, from={}, size={}",
                userId, from, size);

        userService.getUserEntityById(userId);
        validatePaginationParams(from, size);

        Pageable pageable = PageRequest.of(from / size, size);

        // Используем оптимизированный запрос для получения запросов, на которые пользователь не отвечал
        Page<ItemRequest> requestPage = itemRequestRepository
                .findOtherUsersRequestsWithoutUserResponses(userId, pageable);

        List<ItemRequest> requests = requestPage.getContent();

        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        // Получаем ID всех запросов
        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .collect(Collectors.toList());

        // Загружаем все связанные вещи за один запрос
        List<Item> items = itemRepository.findByRequestIdIn(requestIds);

        // Группируем вещи по ID запроса
        Map<Long, List<ItemForRequestDto>> itemsByRequestId = items.stream()
                .collect(Collectors.groupingBy(
                        Item::getRequestId,
                        Collectors.mapping(itemMapper::toForRequestDto, Collectors.toList())
                ));

        // Маппим результат
        return requests.stream()
                .map(request -> itemRequestMapper.toWithItemsDto(
                        request,
                        itemsByRequestId.getOrDefault(request.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ItemRequestWithItemsDto getById(Long userId, Long requestId) {
        log.info("Получение запроса с ID={} пользователем с ID={}", requestId, userId);

        userService.getUserEntityById(userId);

        // Используем оптимизированный запрос с JOIN FETCH
        ItemRequest request = itemRequestRepository.findByIdWithRequestor(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));

        // Загружаем все связанные вещи
        List<Item> items = itemRepository.findByRequestId(requestId);
        List<ItemForRequestDto> itemDtos = items.stream()
                .map(itemMapper::toForRequestDto)
                .collect(Collectors.toList());

        return itemRequestMapper.toWithItemsDto(request, itemDtos);
    }

    private void validatePaginationParams(int from, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Размер страницы должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Начальный индекс не может быть отрицательным");
        }
    }
}