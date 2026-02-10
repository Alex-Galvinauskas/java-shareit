# ShareIt - Серверная часть приложения для шеринга вещей (Полная документация)

## 📌 Обзор проекта
ShareIt - это полнофункциональное RESTful веб-приложение для шеринга вещей, позволяющее пользователям:

🛠️ Добавлять вещи для аренды  
📅 Бронировать вещи других пользователей  
💬 Оставлять отзывы о вещах  
🔍 Искать доступные вещи  
📝 Создавать запросы на вещи

Серверная часть реализована на Java с использованием Spring Boot и предоставляет полный API для управления всеми сущностями системы.

## 🚀 Технологический стек
- **Java 17+**
- **Spring Boot 3+**
- **Spring Data JPA**
- **Hibernate**
- **PostgreSQL / H2** (в зависимости от профиля)
- **Lombok**
- **MapStruct**
- **Maven**
- **JUnit 5**
- **Jakarta Validation**
- **SLF4J + Logback**

## 📁 Архитектура проекта
### Структура пакетов:
ru.practicum.shareit
├── booking/ # Модуль бронирований  
│ ├── controller/  
│ ├── service/  
│ ├── repository/  
│ ├── model/  
│ ├── dto/  
│ └── mapper/  
├── item/ # Модуль вещей  
│ ├── controller/  
│ ├── service/  
│ ├── repository/  
│ ├── model/  
│ ├── dto/  
│ └── mapper/  
├── user/ # Модуль пользователей  
│ ├── controller/  
│ ├── service/  
│ ├── repository/  
│ ├── model/  
│ ├── dto/  
│ └── mapper/  
├── request/ # Модуль запросов  
│ ├── controller/  
│ ├── service/  
│ ├── repository/  
│ ├── model/  
│ ├── dto/  
│ └── mapper/  
└── exception/ # Обработка исключений  

## 🛠️ Установка и запуск
### Предварительные требования:
- Java 17 или выше
- Maven 3.8+
- PostgreSQL 14+ или H2 (для разработки)

### Конфигурационные файлы:
Проект использует несколько конфигурационных файлов:

#### 1. Основной конфигурационный файл (`application.yaml`)
```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      adjust-dates-to-context-time-zone: false
```
#### 2. Конфигурация для разработки (application.properties)
Использует H2 базу данных в режиме совместимости с PostgreSQL:

```properties
server.port=9090

spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.format_sql=true
spring.sql.init.mode=always

spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.url=jdbc:h2:mem:shareitdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
spring.datasource.username=shareit
spring.datasource.password=shareit
```  

##### Консоль H2 доступна по http://localhost:9090/h2-console
```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```
#### 3. Конфигурация для тестов (application-test.properties)
```properties
server.port=9090

spring.datasource.driverClassName=org.h2.Driver
spring.datasource.url=jdbc:h2:mem:shareit
spring.datasource.username=shareit
spring.datasource.password=shareit
```
### Шаги установки:  
Клонирование репозитория  
```bash
  git clone <repository-url>
  cd shareit
```
#### Настройка базы данных
```sql
CREATE DATABASE shareit;
CREATE USER shareit WITH PASSWORD 'shareit';
GRANT ALL PRIVILEGES ON DATABASE shareit TO shareit;
```
#### Сборка проекта
``` bash
  mvn clean install
```
#### Запуск приложения
```bash
  # Запуск с конфигурацией по умолчанию (H2)
  mvn spring-boot:run
```
#### Или собранным JAR файлом
```
java -jar target/shareit-server.jar  
```
## 📚 API Документация
##### Базовый URL
```http
http://localhost:9090
```
```text
Все запросы, требующие авторизации, должны включать заголовок:
X-Sharer-User-Id: <user_id>  
```
## Основные эндпоинты API

### 1. Пользователи (`/users`)

| Метод  | Эндпоинт      | Описание                                    |
|--------|---------------|---------------------------------------------|
| POST   | `/users`      | Создание нового пользователя                |
| GET    | `/users/{id}` | Получение пользователя по ID                |
| PATCH  | `/users/{id}` | Обновление пользователя                     |
| GET    | `/users`      | Получение всех пользователей (с пагинацией) |
| DELETE | `/users/{id}` | Удаление пользователя                       |

### 2. Вещи (`/items`)

| Метод  | Эндпоинт                    | Описание                               |
|--------|-----------------------------|----------------------------------------|
| POST   | `/items`                    | Создание новой вещи                    |
| GET    | `/items/{id}`               | Получение вещи по ID                   |
| PATCH  | `/items/{id}`               | Обновление вещи                        |
| GET    | `/items`                    | Получение всех вещей владельца         |
| GET    | `/items/search`             | Поиск доступных вещей                  |
| POST   | `/items/{itemId}/comment`   | Добавление комментария к вещи          |

### 3. Бронирования (`/bookings`)

| Метод  | Эндпоинт                    | Описание                               |
|--------|-----------------------------|----------------------------------------|
| POST   | `/bookings`                 | Создание бронирования                  |
| GET    | `/bookings/{bookingId}`     | Получение бронирования по ID           |
| PATCH  | `/bookings/{bookingId}`     | Подтверждение/отклонение бронирования  |
| GET    | `/bookings`                 | Получение бронирований пользователя    |
| GET    | `/bookings/owner`           | Получение бронирований владельца       |

### 4. Запросы на вещи (`/requests`)

| Метод | Эндпоинт                | Описание                                |
|-------|-------------------------|-----------------------------------------|
| POST  | `/requests`             | Создание запроса на вещь                |
| GET   | `/requests`             | Получение своих запросов                |
| GET   | `/requests/all`         | Получение запросов других пользователей |
| GET   | `/requests/{requestId}` | Получение запроса по ID                 |

### Примеры запросов  
#### Создание пользователя:  
```bash
  curl -X POST http://localhost:9090/users \
    -H "Content-Type: application/json" \
    -d '{"name": "John Doe", "email": "john@example.com"}'
Создание вещи:
bash
curl -X POST http://localhost:9090/items \
  -H "Content-Type: application/json" \
  -H "X-Sharer-User-Id: 1" \
  -d '{
    "name": "Дрель",
    "description": "Аккумуляторная дрель",
    "available": true
  }'
  
Создание бронирования:
bash
curl -X POST http://localhost:9090/bookings \
  -H "Content-Type: application/json" \
  -H "X-Sharer-User-Id: 2" \
  -d '{
    "itemId": 1,
    "start": "2024-12-01T10:00:00",
    "end": "2024-12-05T18:00:00"
  }'
```
## 🗄️ Модели данных  
Схема базы данных доступна в файле:[schema.sql](src/main/resources/schema.sql) ![BD-Diagramm.png](../BD-Diagramm.png)  
## 🔧 Особенности реализации
### 1. Пагинация
Все методы, возвращающие списки, поддерживают пагинацию через параметры:

- from - начальный индекс (по умолчанию 0)
- size - количество элементов на странице (по умолчанию 10)

### 2. Валидация
Валидация входных данных на всех уровнях  
Проверка уникальности email пользователей  
Валидация дат бронирования  
Проверка доступности вещей  
Проверка минимальной длины текстовых полей  

### 3. Безопасность
Проверка прав доступа к операциям  
Валидация владельцев вещей  
Защита от пересечения бронирований  
Валидация статусов бронирований  

### 4. Обработка дат и времени
Сериализация дат в формате ISO-8601  
Отключение автоматической коррекции временных зон  
Проверка корректности временных интервалов  

## 🧪 Тестирование
Конфигурация логирования для тестов (logback-test.xml)
Проект использует отдельную конфигурацию логирования для тестов с выводом в консоль и файл.

### Запуск тестов:
```bash
  # Все тесты  
mvn test

# Интеграционные тесты  
  mvn verify
```
#### Профили тестирования  
Используется H2 in-memory база данных  
Логирование настраивается через logback-test.xml  
Тестовые логи сохраняются в target/test-logs/test.log  

## 📊 Логирование

Приложение использует SLF4J с Logback для логирования. Настроены различные уровни логирования для разных пакетов:

### Уровни логирования:
- **INFO** - основные операции приложения
- **DEBUG** - детальная информация сервиса вещей
- **WARN** - для Spring, Hibernate и Mockito

### Настройки вывода:
- Консольный вывод с цветной подсветкой
- Файловый вывод с детальной информацией

## 🔒 Обработка исключений

Приложение обрабатывает следующие типы исключений:

| Исключение                  | HTTP Status | Описание                       |
|-----------------------------|-------------|--------------------------------|
| `NotFoundException`         | 404         | Ресурс не найден               |
| `ValidationException`       | 400         | Ошибка валидации               |
| `AccessDeniedException`     | 403         | Отказано в доступе             |
| `ItemNotAvailableException` | 400         | Вещь недоступна                |
| `InvalidDateException`      | 400         | Некорректные даты              |
| `InvalidStatusException`    | 400         | Некорректный статус            |
| `BadRequestException`       | 400         | Некорректный запрос            |

## 🐛 Устранение неполадок

### Распространенные проблемы:

#### 1. Ошибка подключения к БД
- Проверьте настройки подключения в `application.properties`
- Убедитесь, что база данных запущена
- Для H2: проверьте доступность консоли по `http://localhost:9090/h2-console`

#### 2. Ошибки валидации
- Проверьте формат JSON запросов
- Убедитесь, что все обязательные поля заполнены
- Проверьте уникальность email

#### 3. Ошибки доступа
- Проверьте заголовок `X-Sharer-User-Id`
- Убедитесь, что пользователь существует
- Проверьте права доступа к операциям