# Explore With Me

## Этап 1. Подготовка к облачной среде

Проект реорганизован для дальнейшего перехода к микросервисной архитектуре: основной сервис перенесён в группирующий модуль `core`, а инфраструктурные компоненты выделены в модуль `infra`.

На этом этапе добавлены:

- Eureka Discovery Server для регистрации и обнаружения сервисов;
- Spring Cloud Config Server для централизованного хранения настроек;
- Spring Cloud Gateway на порту `8080` с маршрутами ко всему API `event-service`;
- регистрация прикладных сервисов, Config Server и Gateway в Eureka;
- запуск прикладных сервисов и Config Server на динамических портах.

Прикладные сервисы получают внешние настройки через Config Server, обнаруживая его через Eureka.

Gateway использует маршруты вида `lb://event-service`: актуальный экземпляр сервиса событий выбирается через Eureka и Spring Cloud LoadBalancer.

## Этап 2. Переход к микросервисной архитектуре

Основное приложение разделено на сервисы по предметным областям. Каждый прикладной сервис хранит свои данные в отдельной базе PostgreSQL, получает конфигурацию через Config Server и регистрируется в Eureka. Внешние запросы проходят через Gateway на порту `8080`, а межсервисные запросы выполняются с помощью OpenFeign и обнаружения сервисов по имени.

### Структура проекта

```text
java-plus-graduation
├── core
│   ├── event-service       события, категории и подборки
│   ├── user-service        пользователи
│   ├── request-service     заявки на участие
│   └── location-service    справочник мест
├── stat
│   ├── collector           приём действий пользователей
│   ├── aggregator          расчёт сходства мероприятий
│   ├── analyzer            расчёт рекомендаций
│   └── stats-client        gRPC-клиенты рекомендательной системы
└── infra
    ├── discovery-server    Eureka Server
    ├── config-server       централизованная конфигурация
    └── gateway-server      единая внешняя точка входа
```

Используемые базы данных:

- `event-service` — `ewm`;
- `user-service` — `ewm-users`;
- `request-service` — `ewm-requests`;
- `location-service` — `ewm-locations`;

Основные межсервисные зависимости:

- `event-service` обращается к `user-service`, `request-service`, `location-service` и сервисам рекомендаций;
- `request-service` обращается к `user-service` и `event-service`;
- `event-service` получает из `request-service` количество подтверждённых заявок, а `request-service` получает из `event-service` данные события, необходимые для обработки заявок.

### Распределение внешнего API

| Сервис | Эндпоинты | Назначение |
|---|---|---|
| `event-service` | `GET /events`, `GET /events/{id}` | Публичный поиск и получение события |
| `event-service` | `POST /users/{userId}/events` | Создание события пользователем |
| `event-service` | `GET /users/{userId}/events`, `GET /users/{userId}/events/{eventId}` | Получение событий пользователя |
| `event-service` | `PATCH /users/{userId}/events/{eventId}` | Изменение события инициатором |
| `event-service` | `PUT /users/{userId}/events/{eventId}/place/{placeId}`, `DELETE /users/{userId}/events/{eventId}/place` | Привязка и отвязка места инициатором |
| `event-service` | `GET /admin/events`, `PATCH /admin/events/{eventId}` | Административный поиск, изменение и модерация событий |
| `event-service` | `PUT /admin/events/{eventId}/place/{placeId}`, `DELETE /admin/events/{eventId}/place` | Административная привязка и отвязка места |
| `event-service` | `GET /categories`, `GET /categories/{catId}` | Получение категорий |
| `event-service` | `POST /admin/categories`, `PATCH /admin/categories/{catId}`, `DELETE /admin/categories/{catId}` | Управление категориями |
| `event-service` | `GET /compilations`, `GET /compilations/{id}` | Получение подборок событий |
| `event-service` | `POST /admin/compilations`, `PATCH /admin/compilations/{id}`, `DELETE /admin/compilations/{id}` | Управление подборками |
| `user-service` | `POST /admin/users`, `GET /admin/users`, `DELETE /admin/users/{id}` | Создание, поиск и удаление пользователей |
| `request-service` | `GET /users/{userId}/requests`, `POST /users/{userId}/requests` | Получение и создание заявок пользователя |
| `request-service` | `PATCH /users/{userId}/requests/{requestId}/cancel` | Отмена заявки пользователем |
| `request-service` | `GET /users/{userId}/events/{eventId}/requests` | Получение заявок на событие инициатором |
| `request-service` | `PATCH /users/{userId}/events/{eventId}/requests` | Подтверждение и отклонение заявок |
| `location-service` | `GET /places`, `GET /places/{placeId}` | Получение справочника мест |
| `location-service` | `POST /admin/places`, `PUT /admin/places/{placeId}`, `DELETE /admin/places/{placeId}` | Управление местами |

### Внутренний API

Внутренние эндпоинты не публикуются через Gateway и предназначены для Feign-клиентов:

| Сервис | Эндпоинты | Назначение |
|---|---|---|
| `user-service` | `GET /internal/users/{userId}`, `GET /internal/users?ids=...` | Краткие данные одного или нескольких пользователей |
| `event-service` | `GET /internal/events/{eventId}` | Данные события, необходимые для обработки заявок |
| `request-service` | `GET /internal/requests/confirmed-counts?eventIds=...` | Число подтверждённых заявок по событиям |
| `location-service` | `GET /internal/places/{placeId}`, `GET /internal/places?ids=...` | Данные одного или нескольких мест |

### Отказоустойчивость Feign-клиентов

В `event-service` и `request-service` используется Resilience4j Circuit Breaker со следующими настройками:

- тайм-аут соединения — 1 секунда;
- тайм-аут ответа — 2 секунды;
- окно — 10 вызовов, минимальное число вызовов для расчёта — 5;
- порог ошибок — 50%;
- в полуоткрытом состоянии разрешены 3 пробных вызова;
- открытое состояние сохраняется 10 секунд;
- ошибками Circuit Breaker считаются сетевые сбои, тайм-ауты и ответы `5xx`; ответы `4xx` не учитываются;
- автоматические повторные вызовы не настроены.

По умолчанию недоступность зависимого бизнес-сервиса преобразуется в `503 Service Unavailable`. Ответы `4xx` зависимого сервиса сохраняются и обрабатываются прикладной логикой как раньше.

При сборке DTO для операций чтения `event-service` использует мягкую деградацию:

- если `user-service` недоступен, в `UserShortDto` сохраняется идентификатор пользователя, а имя заменяется на `Данные временно недоступны`;
- если пользователь удалён и `user-service` вернул `404`, используется имя `Объект не найден`;
- если `request-service` недоступен, `confirmedRequests` возвращается как `null`;
- если `location-service` недоступен, дополнительное поле `place` возвращается как `null`; координаты события остаются доступны из БД `event-service`;

Мягкая деградация применяется только к дополнительным данным ответного DTO. Если данные другого сервиса нужны для выполнения самой операции, например для поиска событий относительно `placeId`, запрос завершается с `503`. Изменяющие операции используют строгую сборку DTO и при недоступности бизнес-сервиса также возвращают `503`. Отправка просмотра в Collector является частью обработки `GET /events/{id}`.

### Ограничения межсервисной согласованности

Каждый сервис изменяет только свою базу. Автоматического согласования данных между сервисами пока нет, поэтому возможны следующие ситуации:

- при удалении пользователя его события и заявки не удаляются. В событиях вместо имени инициатора выводится `Объект не найден`. Операции с заявками, которые проверяют пользователя, возвращают `404`, хотя сами заявки остаются в базе;
- при удалении места его идентификатор остаётся в событиях, но поле `place` возвращается как `null`. Поиск по идентификатору удалённого места возвращает `404`. Координаты самого события не теряются и продолжают участвовать в поиске по радиусу;
- пользователь или место могут быть удалены между проверкой их существования и сохранением связанных данных в другом сервисе;
- сервис может успеть сохранить изменение, а затем вернуть `503` из-за ошибки при сборке ответного DTO. Поэтому безусловно повторять такой запрос небезопасно.

Проверка возможности удаления пользователя, каскадная очистка данных и компенсация уже выполненных операций пока не реализованы.
