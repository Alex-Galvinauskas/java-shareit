# ShareIt Gateway Service

Микросервис-шлюз (Gateway) для приложения ShareIt, реализованный на Spring Boot. Служит промежуточным слоем между клиентскими приложениями и основным сервером ShareIt, обеспечивая валидацию запросов, маршрутизацию и единую точку входа.

## 📁 Структура проекта
ru.practicum.shareit  
├── annotation # Кастомные аннотации валидации  
│ ├── EndDateAfterStartDate.java  
│ └── validation  
│ └── EndDateAfterStartDateValidator.java  
├── client # REST-клиенты для взаимодействия с сервером  
│ ├── BaseClient.java  
│ ├── BookingClient.java  
│ ├── ItemClient.java  
│ ├── ItemRequestClient.java  
│ └── UserClient.java  
├── controller # Контроллеры Gateway (обработка HTTP-запросов)  
│ ├── BookingController.java  
│ ├── ItemController.java  
│ ├── ItemRequestController.java  
│ └── UserController.java  
├── dto # Data Transfer Objects  
│ ├── BookingInfoDto.java  
│ ├── BookingRequestDto.java  
│ ├── BookingState.java  
│ ├── BookItemRequestDto.java  
│ ├── CommentCreateDto.java  
│ ├── CommentDto.javav  
│ ├── ItemDto.java  
│ ├── ItemRequestDto.java  
│ └── UserDto.java  
└── ShareItGateway.java # Главный класс приложения  

## 🚀 Запуск приложения

1. **Предварительные требования:**
    - Java 17+
    - Maven 3.8+
    - Запущенный сервер ShareIt (указать его URL в `application.properties`)

2. **Конфигурация:**
   В файле `application.properties` укажите URL сервера ShareIt:
   ```properties
   shareit-server.url=http://localhost:9090
 ### Сборка и запуск:

```bash
mvn clean package
java -jar target/shareit-gateway.jar
```

# 🌐 REST API

## Бронирования (`/bookings`)

| Метод     | Путь                                                    | Описание                              |
|-----------|---------------------------------------------------------|---------------------------------------|
| **POST**  | `/bookings`                                             | Создание нового бронирования          |
| **PATCH** | `/bookings/{bookingId}?approved={boolean}`              | Подтверждение/отклонение бронирования |
| **GET**   | `/bookings/{bookingId}`                                 | Получение бронирования по ID          |
| **GET**   | `/bookings?state={state}&from={from}&size={size}`       | Получение бронирований пользователя   |
| **GET**   | `/bookings/owner?state={state}&from={from}&size={size}` | Получение бронирований владельца      |

## Вещи (`/items`)

| Метод     | Путь                                                | Описание                       |
|-----------|-----------------------------------------------------|--------------------------------|
| **POST**  | `/items`                                            | Добавление новой вещи          |
| **PATCH** | `/items/{itemId}`                                   | Обновление вещи                |
| **GET**   | `/items/{itemId}`                                   | Получение вещи по ID           |
| **GET**   | `/items?from={from}&size={size}`                    | Получение всех вещей владельца |
| **GET**   | `/items/search?text={text}&from={from}&size={size}` | Поиск вещей                    |
| **POST**  | `/items/{itemId}/comment`                           | Добавление комментария к вещи  |

## Запросы на вещи (`/requests`)

| Метод    | Путь                                    | Описание                                |
|----------|-----------------------------------------|-----------------------------------------|
| **POST** | `/requests`                             | Создание запроса на вещь                |
| **GET**  | `/requests`                             | Получение собственных запросов          |
| **GET**  | `/requests/all?from={from}&size={size}` | Получение запросов других пользователей |
| **GET**  | `/requests/{requestId}`                 | Получение запроса по ID                 |

## Пользователи (`/users`)

| Метод      | Путь                             | Описание                     |
|------------|----------------------------------|------------------------------|
| **POST**   | `/users`                         | Создание пользователя        |
| **PATCH**  | `/users/{id}`                    | Обновление пользователя      |
| **GET**    | `/users/{id}`                    | Получение пользователя по ID |
| **GET**    | `/users?from={from}&size={size}` | Получение всех пользователей |
| **DELETE** | `/users/{id}`                    | Удаление пользователя        |

## ✅ Валидация

- **Аннотации Jakarta Validation:** `@NotBlank`, `@NotNull`, `@Email`, `@Future`, `@FutureOrPresent`, `@Positive`, `@PositiveOrZero`
- **Кастомная аннотация:** `@EndDateAfterStartDate` — проверяет, что дата окончания бронирования позже даты начала
- **Групповая валидация:** `UserDto.OnCreate` и `UserDto.OnUpdate` для разных сценариев создания/обновления

## 🔧 Технологии

- **Spring Boot 3.x** (Web, Validation)
- **Spring WebClient** (через RestTemplate)
- **Lombok**
- **Jakarta Validation API**
- **Maven**

## 📌 Особенности реализации

- **Единый базовый клиент:** `BaseClient` содержит общую логику HTTP-запросов
- **Заголовок `X-Sharer-User-Id`:** передается во всех запросах для идентификации пользователя
- **Обработка ошибок:** ошибки от сервера пробрасываются клиенту с сохранением статуса и тела ответа
- **Поддержка пагинации:** параметры `from` и `size` для постраничного вывода

## 🧪 Пример запроса  
### Создание бронирования:  

```http
POST /bookings
X-Sharer-User-Id: 1
Content-Type: application/json

{
"itemId": 1,
"start": "2025-12-01T10:00:00",
"end": "2025-12-02T12:00:00"
}
Ответ (успех):

json
{
"id": 1,
"start": "2025-12-01T10:00:00",
"end": "2025-12-02T12:00:00",
"itemId": 1,
"bookerId": 1,
"status": "WAITING"
}
```