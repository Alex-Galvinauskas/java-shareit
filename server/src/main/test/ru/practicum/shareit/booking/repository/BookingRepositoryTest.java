package ru.practicum.shareit.booking.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Тестирование репозитория бронирований")
class BookingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    private User owner;
    private User booker;
    private Item item;
    private Booking pastBooking;
    private Booking currentBooking;
    private Booking futureBooking;

    @BeforeEach
    void setUp() {


        owner = User.builder()
                .name("Владелец")
                .email("owner@test.com")
                .build();
        entityManager.persist(owner);

        booker = User.builder()
                .name("Бронирующий")
                .email("booker@test.com")
                .build();
        entityManager.persist(booker);

        // Создание вещи
        item = Item.builder()
                .name("Тестовая вещь")
                .description("Описание тестовой вещи")
                .available(true)
                .ownerId(owner.getId())
                .build();
        entityManager.persist(item);

        LocalDateTime now = LocalDateTime.now();

        // Прошедшее бронирование
        pastBooking = Booking.builder()
                .start(now.minusDays(10))
                .end(now.minusDays(5))
                .status(BookingStatus.APPROVED)
                .itemId(item.getId())
                .bookerId(booker.getId())
                .ownerId(owner.getId())
                .created(now.minusDays(11))
                .build();
        entityManager.persist(pastBooking);

        // Текущее бронирование
        currentBooking = Booking.builder()
                .start(now.minusDays(2))
                .end(now.plusDays(2))
                .status(BookingStatus.APPROVED)
                .itemId(item.getId())
                .bookerId(booker.getId())
                .ownerId(owner.getId())
                .created(now.minusDays(3))
                .build();
        entityManager.persist(currentBooking);

        // Будущее бронирование
        futureBooking = Booking.builder()
                .start(now.plusDays(5))
                .end(now.plusDays(10))
                .status(BookingStatus.APPROVED)
                .itemId(item.getId())
                .bookerId(booker.getId())
                .ownerId(owner.getId())
                .created(now.minusDays(1))
                .build();
        entityManager.persist(futureBooking);

        // Ожидающее бронирование
        Booking waitingBooking = Booking.builder()
                .start(now.plusDays(15))
                .end(now.plusDays(20))
                .status(BookingStatus.WAITING)
                .itemId(item.getId())
                .bookerId(booker.getId())
                .ownerId(owner.getId())
                .created(now.minusDays(1))
                .build();
        entityManager.persist(waitingBooking);

        // Отклоненное бронирование
        Booking rejectedBooking = Booking.builder()
                .start(now.plusDays(25))
                .end(now.plusDays(30))
                .status(BookingStatus.REJECTED)
                .itemId(item.getId())
                .bookerId(booker.getId())
                .ownerId(owner.getId())
                .created(now.minusDays(1))
                .build();
        entityManager.persist(rejectedBooking);

        entityManager.flush();
    }

    @Test
    @DisplayName("Поиск бронирований по ID бронирующего")
    void findByBookerId_ShouldReturnBookings() {

        Pageable pageable = PageRequest.of(0, 10,
                Sort.by("start").descending());
        Page<Booking> result = bookingRepository.findByBookerId(booker.getId(), pageable);

        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent()).extracting(Booking::getBookerId)
                .containsOnly(booker.getId());
    }

    @Test
    @DisplayName("Поиск бронирований по ID бронирующего и статусу")
    void findByBookerIdAndStatus_ShouldReturnFilteredBookings() {

        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> result = bookingRepository.findByBookerIdAndStatus(
                booker.getId(), BookingStatus.WAITING, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    @DisplayName("Поиск текущих бронирований по ID бронирующего")
    void findByBookerIdAndStartLessThanEqualAndEndGreaterThanEqual_ShouldReturnCurrentBookings() {

        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> result = bookingRepository.findByBookerIdAndStartLessThanEqualAndEndGreaterThanEqual(
                booker.getId(), now, now, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(currentBooking.getId());
    }

    @Test
    @DisplayName("Поиск прошедших бронирований по ID бронирующего")
    void findByBookerIdAndEndLessThan_ShouldReturnPastBookings() {

        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> result = bookingRepository.findByBookerIdAndEndLessThan(
                booker.getId(), now, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(pastBooking.getId());
    }

    @Test
    @DisplayName("Поиск будущих бронирований по ID бронирующего")
    void findByBookerIdAndStartGreaterThan_ShouldReturnFutureBookings() {

        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> result = bookingRepository.findByBookerIdAndStartGreaterThan(
                booker.getId(), now, pageable);

        assertThat(result.getContent()).hasSize(3); // future, waiting, rejected
    }

    @Test
    @DisplayName("Поиск бронирований по ID владельца")
    void findByOwnerId_ShouldReturnOwnerBookings() {

        Pageable pageable = PageRequest.of(0, 10,
                Sort.by("start").descending());
        Page<Booking> result = bookingRepository.findByOwnerId(owner.getId(), pageable);

        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent()).extracting(Booking::getOwnerId)
                .containsOnly(owner.getId());
    }

    @Test
    @DisplayName("Поиск бронирований по ID владельца и статусу")
    void findByOwnerIdAndStatus_ShouldReturnFilteredOwnerBookings() {

        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> result = bookingRepository.findByOwnerIdAndStatus(
                owner.getId(), BookingStatus.REJECTED, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    @DisplayName("Поиск последних бронирований для нескольких вещей")
    void findLastBookingsForMultipleItems_ShouldReturnLastBookings() {

        LocalDateTime now = LocalDateTime.now();
        List<Long> itemIds = List.of(item.getId());

        List<Booking> result = bookingRepository.findLastBookingsForMultipleItems(
                itemIds, now, BookingStatus.APPROVED);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(pastBooking.getId());
    }

    @Test
    @DisplayName("Поиск следующих бронирований для нескольких вещей")
    void findNextBookingsForMultipleItems_ShouldReturnNextBookings() {

        LocalDateTime now = LocalDateTime.now();
        List<Long> itemIds = List.of(item.getId());

        List<Booking> result = bookingRepository.findNextBookingsForMultipleItems(
                itemIds, now, BookingStatus.APPROVED);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(futureBooking.getId());
    }

    @Test
    @DisplayName("Проверка пересечений бронирований - нет пересечений")
    void existsOverlappingBooking_ShouldReturnFalse_WhenNoOverlap() {

        LocalDateTime start = LocalDateTime.now().plusDays(100);
        LocalDateTime end = LocalDateTime.now().plusDays(105);

        boolean result = bookingRepository.existsOverlappingBooking(
                item.getId(), start, end);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Проверка пересечений бронирований - есть пересечение")
    void existsOverlappingBooking_ShouldReturnTrue_WhenOverlapExists() {

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        boolean result = bookingRepository.existsOverlappingBooking(
                item.getId(), start, end);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Поиск последнего бронирования для вещи")
    void findLastBooking_ShouldReturnLastBooking() {

        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 1);

        Page<Booking> result = bookingRepository.findLastBooking(
                item.getId(), now, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(pastBooking.getId());
    }

    @Test
    @DisplayName("Поиск следующего бронирования для вещи")
    void findNextBooking_ShouldReturnNextBooking() {

        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 1);

        Page<Booking> result = bookingRepository.findNextBooking(
                item.getId(), now, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(futureBooking.getId());
    }

    @Test
    @DisplayName("Поиск завершенных бронирований по ID вещи и статусу")
    void findByItemIdAndEndBeforeAndStatusOrderByEndDesc_ShouldReturnPastBookings() {

        LocalDateTime now = LocalDateTime.now();
        List<Booking> result = bookingRepository.findByItemIdAndEndBeforeAndStatusOrderByEndDesc(
                item.getId(), now, BookingStatus.APPROVED);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(pastBooking.getId());
    }

    @Test
    @DisplayName("Поиск будущих бронирований по ID вещи и статусу")
    void findByItemIdAndStartAfterAndStatusOrderByStartAsc_ShouldReturnFutureBookings() {

        LocalDateTime now = LocalDateTime.now();
        List<Booking> result = bookingRepository.findByItemIdAndStartAfterAndStatusOrderByStartAsc(
                item.getId(), now, BookingStatus.APPROVED);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(futureBooking.getId());
    }

    @Test
    @DisplayName("Поиск завершенных бронирований по ID вещи, ID бронирующего и статусу")
    void findByItemIdAndBookerIdAndEndBeforeAndStatus_ShouldReturnUserPastBookings() {

        LocalDateTime now = LocalDateTime.now();
        List<Booking> result = bookingRepository.findByItemIdAndBookerIdAndEndBeforeAndStatus(
                item.getId(), booker.getId(), now, BookingStatus.APPROVED);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(pastBooking.getId());
    }
}