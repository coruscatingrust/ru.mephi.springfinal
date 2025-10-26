# springfinal — микросервисы: Gateway, Booking, Hotel, Eureka

Реализация финального задания по Фреймворку SPRING и REST API.


## Содержание
- [1. Требования и совместимость](#1-требования-и-совместимость)
- [2. Быстрый старт](#2-быстрый-старт)
- [3. Архитектура и модули](#3-архитектура-и-модули)
- [4. Безопасность и роли](#4-безопасность-и-роли)
- [5. Маршруты Gateway (публичные vs внутренние)](#5-маршруты-gateway-публичные-vs-внутренние)
- [6. Сага и идемпотентность](#6-сагa-и-идемпотентность)
- [7. Полезные адреса](#7-полезные-адреса)
- [8. Тестовые сценарии (cURL)](#8-тестовые-сценарии-curl)
---

## 1. Требования и совместимость

- **JDK 17** (обязательное требование, проверить: `mvn -v`, `java -version`, `javac -version`)
- **Maven 3.9+**
- Порты: **8761** (Eureka), **8080** (Gateway), **8081** (Booking), **8082** (Hotel)

**Совместимость Spring Boot ↔ Spring Cloud**  
- **Spring Boot 3.4.0**
- **Spring Cloud 2024.0.0** 

---

## 2. Запуск

- Автоматически, быстро: Запустить run.sh: соберет проект, запустит tmux, запустит сервисы.
- Ручками, медленно:
  - mvn -U clean test
  - mvn -U clean -DskipTests clean package
  - mvn -f discovery-server/pom.xml spring-boot:run"
  - mvn -f hotel-service/pom.xml spring-boot:run"
  - mvn -f booking-service/pom.xml spring-boot:run"
  - mvn -f api-gateway/pom.xml spring-boot:run"
      

## 3. Архитектура и модули
Модули:

- **discovery-server** — Eureka Server (Servlet/Tomcat)
- **api-gateway** — Spring Cloud Gateway, глобальный фильтр корреляции:
  - добавляет X-Request-Id при отсутствии;
  - кладёт его в MDC (traceId) для сквозного логирования.
- **hotel-service** — отели/номера, проверка доступности, рекомендации (times_booked), внутренние операции confirm-availability/release.
- **booking-service** — пользователи (JWT), бронирования (создание/просмотр/отмена), сага, идемпотентность (request_log).

## 4. Безопасность и роли

- JWT: генерируется в booking-service. Тело токена содержит sub (username) и roles.
- Срок жизни (по умолчанию): 3600 секунд.
- Роли: USER и ADMIN.
- Hotel: публичные GET /api/hotels, GET /api/rooms, GET /api/rooms/recommend. 
- Внутренние confirm-availability/release требуют аутентификации.
- Booking: POST /user/register и POST /user/auth — публичные; всё остальное — только с JWT.

Стартовый админ создаётся при запуске:

admin / admin

## 5. Маршруты Gateway (публичные vs внутренние)

Прокидываются только публичные endpoint’ы hotel‑service:

- к booking-service: /booking/**, /bookings/**, /user/**

- к hotel-service: /api/hotels/**, /api/rooms, /api/rooms/recommend

Внутренние /api/rooms/{id}/confirm-availability и /api/rooms/{id}/release не публикуются через Gateway — их вызывает сам booking-service через lb://hotel-service.

## 6. Сага и идемпотентность

Сага (создание брони):

- Booking создаёт запись PENDING, генерирует bookingUid.

(Если autoSelect=true) запрашивает рекомендации номеров у Hotel.

- Вызывает Hotel.confirm-availability (временная блокировка на период).

При успехе → CONFIRMED; при ошибке → компенсация Hotel.release и CANCELLED.

Идемпотентность:

- Клиент обязан передавать заголовок X-Request-Id (если нет — Gateway сгенерирует новый).

- Booking фиксирует X-Request-Id в таблице request_log, чтобы избежать повторной обработки.

## 7. Полезные адреса

- Eureka: http://localhost:8761/

- Gateway (входная точка): http://localhost:8080/

Swagger:

- Booking: http://localhost:8081/swagger-ui/index.html

- Hotel: http://localhost:8082/swagger-ui/index.html

## 8. Ручные тесты

Требуется jq. Запускать в консоли.

#### 8.1. Аутентификация

Логин админом (для административных операций):

`ADMIN=$(curl -s -X POST http://localhost:8080/user/auth \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq -r .token)`
echo "ADMIN token: ${ADMIN:0:20}..."


Регистрация пользователя и получение JWT:

```
TOKEN=$(curl -s -X POST http://localhost:8080/user/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"secret"}' | jq -r .token)
echo "USER token: ${TOKEN:0:20}..."
```

#### 8.2. Операции администратора (Hotel CRUD)

Создать отель:

```
curl -s -X POST http://localhost:8080/api/hotels \
  -H "Authorization: Bearer ${ADMIN}" -H 'Content-Type: application/json' \
  -d '{"name":"Президент-Отель","address":"Большая Якиманка, 24"}' | jq
```

Создать номера:

```
# hotelId=1 (из ответа на создание отеля выше)
curl -s -X POST "http://localhost:8080/api/rooms?hotelId=1&number=101&available=true" \
  -H "Authorization: Bearer ${ADMIN}" | jq
curl -s -X POST "http://localhost:8080/api/rooms?hotelId=1&number=102&available=true" \
  -H "Authorization: Bearer ${ADMIN}" | jq
```

Список отелей (публично):

```
curl -s http://localhost:8080/api/hotels | jq
```

Список доступных номеров на период:

```
START=2025-11-01
END=2025-11-05
curl -s "http://localhost:8080/api/rooms?start=${START}&end=${END}" | jq
```

#### 8.3. Рекомендации и равномерное распределение

Рекомендации (сортировка: times_booked → id):

```
curl -s "http://localhost:8080/api/rooms/recommend?start=${START}&end=${END}" \
  -H "Authorization: Bearer ${TOKEN}" | jq
```

Номер с меньшим times_booked должен предлагаться раньше. После подтверждения брони счётчик увеличится.

#### 8.4. Создание брони (сага)

Автовыбор номера + идемпотентность через X-Request-Id:

```
REQ=demo-req-1
curl -s -X POST http://localhost:8080/booking \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "X-Request-Id: ${REQ}" \
  -d "{\"startDate\":\"${START}\",\"endDate\":\"${END}\",\"autoSelect\":true}" | jq
```

Повтор с тем же X-Request-Id — не должен породить вторую бронь:

```
curl -s -X POST http://localhost:8080/booking \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "X-Request-Id: ${REQ}" \
  -d "{\"startDate\":\"${START}\",\"endDate\":\"${END}\",\"autoSelect\":true}" | jq
```

Явный выбор номера (roomId известен):

```
# возьмём первый из рекомендаций
RID=$(curl -s "http://localhost:8080/api/rooms/recommend?start=${START}&end=${END}" \
  -H "Authorization: Bearer ${TOKEN}" | jq -r '.[0].id')
curl -s -X POST http://localhost:8080/booking \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "X-Request-Id: demo-req-2" \
  -d "{\"startDate\":\"${START}\",\"endDate\":\"${END}\",\"roomId\":${RID}}" | jq
```

Список своих броней:

```
curl -s http://localhost:8080/bookings -H "Authorization: Bearer ${TOKEN}" | jq
```

Получить бронь по id:

```
BID=$(curl -s http://localhost:8080/bookings -H "Authorization: Bearer ${TOKEN}" | jq -r '.[0].id')
curl -s http://localhost:8080/booking/${BID} -H "Authorization: Bearer ${TOKEN}" | jq
```


Отмена брони:

```
curl -s -X DELETE http://localhost:8080/booking/${BID} \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "X-Request-Id: cancel-1"`
```

#### 8.5. Конкурентный сценарий (две брони на один номер/период)
Предполагаем, что RID — явный номер (см. выше)

```
par() { bash -lc "$1"; }

par "curl -s -X POST http://localhost:8080/booking \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer ${TOKEN}' \
  -H 'X-Request-Id: race-1' \
  -d '{\"startDate\":\"'${START}'\",\"endDate\":\"'${END}'\",\"roomId\":'${RID}'}' | jq" &

par "curl -s -X POST http://localhost:8080/booking \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer ${TOKEN}' \
  -H 'X-Request-Id: race-2' \
  -d '{\"startDate\":\"'${START}'\",\"endDate\":\"'${END}'\",\"roomId\":'${RID}'}' | jq" &
wait
```


Ожидаемо, одна бронь должна быть CONFIRMED, вторая — ошибиться/уйти в компенсацию CANCELLED (см. логи, RoomLock исключает пересечения).

#### 8.6. Негативные сценарии (должны падать)


Без токена:

```
curl -i -X POST http://localhost:8080/booking \
  -H 'Content-Type: application/json' \
  -d "{\"startDate\":\"${START}\",\"endDate\":\"${END}\",\"autoSelect\":true}"
# → 401
```


Forbidden (нет роли ADMIN):

```
curl -i -X POST http://localhost:8080/api/hotels \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"name":"N/A","address":"N/A"}'
# → 403
```


Плохие даты:

```
curl -i -X POST http://localhost:8080/booking \
  -H 'Content-Type: application/json' -H "Authorization: Bearer ${TOKEN}" \
  -H 'X-Request-Id: bad-dates' \
  -d '{"startDate":"2020-01-01","endDate":"2019-12-31","autoSelect":true}'
# → 409 (или 400) по валидации
```


Внутренний эндпоинт через Gateway (не опубликован):

```
curl -i -X POST http://localhost:8080/api/rooms/1/confirm-availability \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
  -d '{"startDate":"'"$START"'","endDate":"'"$END"'","bookingId":"x","requestId":"y"}'
# → 404 (маршрут не существует в Gateway)
```

## 9. Автотесты

#### 9.1 API Gateway

- RoutesPresenceTest
Интеграционный тест SpringBootTest, дергает GET /actuator/gateway/routes и проверяет наличие маршрутов с id booking и hotel. Тем самым фиксируется корректная маршрутизация через Gateway.

- CorrelationFilterUnitTest
Юнит‑тест пользовательского фильтра корреляции. Проверяет, что для каждого запроса генерируется/прокидывается заголовок корреляции (например, X-Correlation-Id) и он уходит дальше по цепочке. Это базис для трассировки и сквозного логирования.

#### 9.2 Hotel Service

- HotelAdminSecurityTest
Интеграционные тесты безопасности контроллера отелей:

анонимный запрос на POST /api/hotels → 401;

пользователь с ролью USER → 403;

администратор (ADMIN) → 200/201.
Также проверяется, что статусы ошибок и тело ответа формируются централизованно (через GlobalExceptionHandler).

- RoomLockRepositoryTest
JPA‑тест репозитория блокировок номеров:

создаёт данные в H2;

проверяет выборку пересекающихся блокировок по датам;

заодно фиксирует уникальные ограничения (например, request_id, booking_id) и индекс по room_id — основа для идемпотентности и быстрого поиска.

- RoomFlowTest
Интеграционный сценарий бизнес‑логики:

публичная рекомендация номеров отсортирована по times_booked (при равенстве — по id);

поток подтверждения брони: confirm → idempotent повтор → конфликт пересечения (409).
Использует встраиваемую БД и тестовый профиль.

#### 9.3 Booking Service

- AuthControllerTest
Базовые сценарии аутентификации/доступности эндпойнтов (контекст безопасности поднимается, эндпойнты отвечают ожидаемыми статусами).

- BookingControllerIdempotencyTest
Проверяет идемпотентность POST /booking через заголовок X-Request-Id:

первый вызов с X-Request-Id=idem-1 → 200;

повтор тем же X-Request-Id → 409 (защита от дублей).
В тесте добавляется заголовок Authorization (без префикса Bearer), чтобы контроллер прошёл свои проверки, но не запускал парсинг JWT в кастомном фильтре — это позволяет изолированно тестировать идемпотентность.
