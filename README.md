# Explore With Me

Explore With Me — сервис для публикации мероприятий и поиска событий. Пользователи могут создавать мероприятия,
подавать заявки на участие, работать с подборками и получать рекомендации.

Проект состоит из нескольких приложений. Внешние запросы принимает Gateway. Сервисы находят друг друга через
Eureka, получают настройки из Config Server и обращаются друг к другу по REST или gRPC. Действия пользователей
передаются через Kafka и используются для расчёта рейтинга и рекомендаций.

## Возможности

- создание и редактирование мероприятий;
- публикация и отклонение мероприятий администратором;
- поиск опубликованных событий;
- управление категориями и подборками;
- создание пользователей;
- подача, подтверждение, отклонение и отмена заявок на участие;
- ведение справочника мест;
- поиск мероприятий рядом с указанным местом;
- сбор просмотров, регистраций и лайков;
- расчёт сходства мероприятий;
- выдача персональных рекомендаций;
- расчёт рейтинга мероприятия по действиям пользователей.

## Структура проекта

```text
java-plus-graduation
├── core
│   ├── event-service        мероприятия, категории и подборки
│   ├── user-service         пользователи
│   ├── request-service      заявки на участие
│   └── location-service     справочник мест
├── stat
│   ├── serialization
│   │   ├── proto-schemas    сообщения и сервисы gRPC
│   │   └── avro-schemas     сообщения Kafka
│   ├── collector            приём действий пользователей
│   ├── aggregator           расчёт сходства мероприятий
│   ├── analyzer             хранение статистики и расчёт рекомендаций
│   └── stats-client         gRPC-клиенты Collector и Analyzer
└── infra
    ├── discovery-server     Eureka Server
    ├── config-server        общие настройки приложений
    └── gateway-server       входная точка REST API
```

## Как взаимодействуют сервисы

```text
Клиент
└── HTTP → Gateway :8080
    ├── event-service
    │   ├── Feign → user-service
    │   ├── Feign → request-service
    │   ├── Feign → location-service
    │   ├── gRPC → Collector: VIEW, LIKE
    │   └── gRPC → Analyzer: рейтинг и рекомендации
    ├── user-service
    ├── request-service
    │   ├── Feign → user-service
    │   ├── Feign → event-service
    │   └── gRPC → Collector: REGISTER
    └── location-service

Collector
└── Kafka: stats.user-actions.v1
    ├── Aggregator
    │   └── Kafka: stats.events-similarity.v1
    │       └── Analyzer
    └── Analyzer

Analyzer
└── PostgreSQL: analyzer
```

Все внешние REST-запросы проходят через Gateway на порту `8080`. Внутренние REST-вызовы выполняются через
OpenFeign. Адрес нужного экземпляра определяется по имени сервиса в Eureka.

Collector и Analyzer предоставляют gRPC API. Их порты выбираются при запуске, а клиенты находят приложения через
Eureka. Aggregator работает без HTTP-сервера.

## Сервисы предметной области

### event-service

Хранит мероприятия, категории и подборки. Сервис отвечает за публичный поиск, операции инициатора и
административную модерацию.

При формировании DTO сервис получает:

- данные инициатора из `user-service`;
- количество подтверждённых заявок из `request-service`;
- данные места из `location-service`;
- рейтинг и рекомендации из `analyzer`.

Запрос `GET /events` не считается просмотром. После успешного `GET /events/{id}` сервис отправляет в Collector
действие `VIEW`. Для этого запроса обязателен заголовок `X-EWM-USER-ID`.

### user-service

Создаёт, возвращает и удаляет пользователей. Другие приложения получают краткие данные пользователей через
внутренний API.

### request-service

Хранит заявки на участие. Проверяет существование пользователя и мероприятия, лимит участников и правила
подтверждения заявки. После создания заявки отправляет в Collector действие `REGISTER`.

### location-service

Хранит места с названием и координатами. Координаты используются для поиска мероприятий в заданном радиусе.

## Сервис рекомендаций

Сервис рекомендаций состоит из Collector, Aggregator и Analyzer.

### Collector

Collector принимает по gRPC действие пользователя и синхронно отправляет его в Kafka. Поддерживаются три типа
действий:

| Действие | Вес | Когда отправляется |
|---|---:|---|
| `VIEW` | `0.4` | пользователь открыл страницу мероприятия |
| `REGISTER` | `0.8` | пользователь подал заявку на участие |
| `LIKE` | `1.0` | пользователь с подтверждённой заявкой поставил лайк |

Collector не рассчитывает итоговый вес и не хранит состояние. Для одной пары `userId` и `eventId` максимальный
полученный вес выбирают Aggregator и Analyzer.

### Aggregator

Aggregator читает действия из топика `stats.user-actions.v1` и рассчитывает сходство мероприятий:

```text
similarity(A, B) = S_min(A, B) / (sqrt(S(A)) * sqrt(S(B)))
```

`S(A)` и `S(B)` — суммы весов взаимодействий с мероприятиями. `S_min(A, B)` — сумма минимальных весов
взаимодействий одного пользователя с мероприятиями `A` и `B`.

Результат отправляется в топик `stats.events-similarity.v1`. Идентификаторы пары всегда записываются по
возрастанию, поэтому пары `(A, B)` и `(B, A)` не дублируются.

Промежуточные суммы Aggregator хранит в памяти. После перезапуска состояние начинается с пустого.

### Analyzer

Analyzer последовательно читает оба Kafka-топика и сохраняет:

- максимальный вес взаимодействия пользователя с мероприятием;
- время последнего взаимодействия;
- последнее значение сходства каждой пары мероприятий.

Analyzer предоставляет три gRPC-операции:

| Операция | Результат |
|---|---|
| `GetRecommendationsForUser` | персональные рекомендации с предсказанной оценкой |
| `GetSimilarEvents` | похожие мероприятия, с которыми пользователь ещё не взаимодействовал |
| `GetInteractionsCount` | сумма максимальных весов пользователей для каждого мероприятия |

Для персональных рекомендаций Analyzer берёт последние `N` взаимодействий пользователя, находит похожие новые
мероприятия и выбирает `N` кандидатов. Оценка кандидата рассчитывается по `K` ближайшим мероприятиям, с которыми
пользователь уже взаимодействовал:

```text
R(u, A) = sum(similarity(A, B) * w(u, B)) / sum(similarity(A, B))
```

В текущей реализации `K = 20`. Параметр `N` передаётся в запросе как `maxResults`.

## REST API

Основные внешние эндпоинты:

| Сервис | Эндпоинты | Назначение |
|---|---|---|
| `event-service` | `GET /events`, `GET /events/{id}` | поиск и получение опубликованных мероприятий |
| `event-service` | `GET /events/recommendations` | рекомендации для пользователя |
| `event-service` | `PUT /events/{eventId}/like` | лайк посещённого мероприятия |
| `event-service` | `POST /users/{userId}/events` | создание мероприятия |
| `event-service` | `GET /users/{userId}/events`, `GET /users/{userId}/events/{eventId}` | мероприятия инициатора |
| `event-service` | `PATCH /users/{userId}/events/{eventId}` | изменение мероприятия инициатором |
| `event-service` | `PUT /users/{userId}/events/{eventId}/place/{placeId}` | привязка места к мероприятию |
| `event-service` | `DELETE /users/{userId}/events/{eventId}/place` | удаление привязки к месту |
| `event-service` | `GET /admin/events`, `PATCH /admin/events/{eventId}` | поиск и модерация мероприятий |
| `event-service` | `PUT /admin/events/{eventId}/place/{placeId}` | привязка места администратором |
| `event-service` | `DELETE /admin/events/{eventId}/place` | удаление привязки администратором |
| `event-service` | `GET /categories`, `GET /categories/{catId}` | получение категорий |
| `event-service` | `POST /admin/categories`, `PATCH /admin/categories/{catId}`, `DELETE /admin/categories/{catId}` | управление категориями |
| `event-service` | `GET /compilations`, `GET /compilations/{id}` | получение подборок |
| `event-service` | `POST /admin/compilations`, `PATCH /admin/compilations/{id}`, `DELETE /admin/compilations/{id}` | управление подборками |
| `user-service` | `POST /admin/users`, `GET /admin/users`, `DELETE /admin/users/{id}` | управление пользователями |
| `request-service` | `GET /users/{userId}/requests`, `POST /users/{userId}/requests` | получение и создание заявок |
| `request-service` | `PATCH /users/{userId}/requests/{requestId}/cancel` | отмена заявки |
| `request-service` | `GET /users/{userId}/events/{eventId}/requests` | заявки на мероприятие инициатора |
| `request-service` | `PATCH /users/{userId}/events/{eventId}/requests` | подтверждение и отклонение заявок |
| `location-service` | `GET /places`, `GET /places/{placeId}` | получение мест |
| `location-service` | `POST /admin/places`, `PUT /admin/places/{placeId}`, `DELETE /admin/places/{placeId}` | управление местами |

Для `GET /events/{id}`, `GET /events/recommendations` и `PUT /events/{eventId}/like` идентификатор пользователя
передаётся в заголовке `X-EWM-USER-ID`.

### Внутренний REST API

Внутренние эндпоинты не публикуются через Gateway. Они используются Feign-клиентами.

| Сервис | Эндпоинты | Назначение |
|---|---|---|
| `user-service` | `GET /internal/users/{userId}`, `GET /internal/users?ids=...` | данные пользователей |
| `event-service` | `GET /internal/events/{eventId}` | данные мероприятия для обработки заявки |
| `request-service` | `GET /internal/requests/confirmed-counts?eventIds=...` | подтверждённые заявки |
| `request-service` | `GET /internal/requests/confirmed-participation?userId=...&eventId=...` | проверка права поставить лайк |
| `location-service` | `GET /internal/places/{placeId}`, `GET /internal/places?ids=...` | данные мест |

## Хранение данных

Один контейнер PostgreSQL содержит пять баз данных:

| Приложение | База данных |
|---|---|
| `event-service` | `ewm` |
| `user-service` | `ewm-users` |
| `request-service` | `ewm-requests` |
| `location-service` | `ewm-locations` |
| `analyzer` | `analyzer` |

Дополнительные базы создаёт скрипт `docker/init-databases.sql`. Таблицы создаются приложениями при запуске.

В `docker-compose.yml` нет постоянного volume для PostgreSQL и Kafka. Команда `docker compose down` удаляет
контейнеры вместе с их данными. Следующий запуск начинает работу с пустыми базами и топиками.

## Обработка сбоев

Feign-клиенты в `event-service` и `request-service` используют Resilience4j Circuit Breaker. Ошибка соединения,
тайм-аут или ответ `5xx` зависимого сервиса учитывается Circuit Breaker и обычно возвращается клиенту как
`503 Service Unavailable`. Ответы `4xx` остаются бизнес-ошибками.

При чтении мероприятий часть дополнительных данных может отсутствовать:

- вместо недоступного имени пользователя возвращается служебное значение;
- `confirmedRequests` может быть `null`, если `request-service` недоступен;
- `place` может быть `null`, если `location-service` недоступен.

Изменяющие операции не используют такую деградацию: если обязательный зависимый сервис недоступен, запрос
завершается ошибкой.

Каждый сервис изменяет только свою базу данных. Общей транзакции между сервисами нет. Например, удаление
пользователя не удаляет автоматически его мероприятия и заявки.

## Технологии

- Java 21;
- Spring Boot 3.3;
- Spring Cloud Config, Gateway, Eureka и OpenFeign;
- Spring Data JPA и JDBC;
- PostgreSQL 16 и H2 для тестов;
- Apache Kafka;
- gRPC и Protocol Buffers;
- Apache Avro;
- Resilience4j;
- Maven.

## Запуск

Для запуска нужны Java 21, Maven и Docker.

1. Запустите PostgreSQL, Kafka и создание Kafka-топиков:

   ```bash
   docker compose up -d
   ```

2. Соберите проект:

   ```bash
   mvn clean package
   ```

3. Запустите приложения в следующем порядке:

   1. `DiscoveryServerApplication`;
   2. `ConfigServerApplication`;
   3. `UserServiceApplication`, `LocationServiceApplication`, `RequestServiceApplication`;
   4. `CollectorApplication`, `AggregatorApplication`, `AnalyzerApplication`;
   5. `EventServiceApplication`;
   6. `GatewayServerApplication`.

   Config Server и прикладные сервисы используют динамические порты. Gateway доступен по адресу `http://localhost:8080`, Eureka — по адресу
   `http://localhost:8761`.

4. Для полной остановки инфраструктуры и удаления тестовых данных выполните:

   ```bash
   docker compose down
   ```

## Тесты

Для проверки Collector, Aggregator и Analyzer в корне проекта находится `tester-0.0.1.jar`. Его параметры описаны в
[`tester-README.md`](tester-README.md).
