## REST API:

### Получение пресетов
Предполагается, что клиент может получить готовые пресеты и предложить их пользователю

GET `api/v1/job/presets`
Response[200]: `Array<PresetSettings>`

### Создание задачи на обработку аудиофайла
POST `/api/v1/job`
Limits:
- File size: 64mb
- RPM: 3 for each ip
Request:
`file`: file # подумать о защите от не аудиофайлов или зип бомб
`settings`: `AudioSettingsRequest`
Response[200]: `JobResponse`
``
Response[400]: `ApiResponse` // bad data in fields
Response[413]: `ApiResponse` // file too large. limit 64mb
Response[429]: `ApiResponse` // too many requests. limit 3 per minute
Response[503]: `ApiResponse` // when redis service unavailable for remote rate limiting

### Force-check состояния обработки
Limits:
- RPM: 60 for each ip
GET `/api/v1/job/{job_id}`
PathVariable: `job_id: string;` // UUIDv7
Response[200]: `JobStateResponse`
Response[404]: `ApiResponse` // not found
Response[503]: `ApiResponse` // when redis service unavailable for remote rate limiting

### Скачивание файла
Limits:
- RPM: 6 for each ip
GET `/api/v1/job/{job_id}/download`
PathVariable: `job_id: string;` // UUIDv7
Response[302]: redirect to silo presigned url
Response[404]: Silo-format response
Response[429]: `ApiResponse` // too many requests. limit 6 per minute
Response[503]: `ApiResponse` // when redis service unavailable for remote rate limiting

## WebSockets:

### Отправка сигнала о завершии работы (DONE/ERROR) и передача ссылки
Destination: `/topic/job/{job_id}`
PathVariable: `job_id: string;` // UUIDv7
Response: `JobStateResponse`

### Отправка сигнала о текущей очереди
Destination: `/topic/job/{job_id}/queue`
PathVariable: `job_id: string;` // UUIDv7
Response: `JobQueueResponse`

```ts
export interface PresetSettings {
    mode: string; // NIGHTCORE/DAYCORE/DOUBLE_TIME/HALF_TIME
    speed: number; // 1.5/0.75/1.5/0.75
    pitchSemitones: number; // 4/-4/0/0
}
export interface AudioSettingsRequest {
    format: string; // default: mp3
    speed: number; // default: 1
    pitchSemitones: number; // default 0
}
export interface JobResponse {
    jobId: string; // UUIDv7
}
export interface ApiResponse {
    success: boolean;
    message?: string; // on error
    validationErrors?: Map<string, string>; // key - field name, value - reason
}
export interface JobStateResponse {
    jobId: string; // UUIDv7
    status: string; // IN_QUEUE|ERROR|DONE
    expireAt: number; // timestamp
}
export interface JobQueueResponse {
    jobId: string; // UUIDv7
    queuePosition: number;
}

```

---

# Kafka Contracts

### Задача в очереди на обработку
topic: `queue`
key: `job_id` (UUID строкой)
message: `JobQueueMessage`

### Задача выполнена
topic: `result`
key: `job_id` (UUID строкой)
message: `JobResultMessage`

Сообщения, которые не удалось разобрать или которые не прошли проверку, уходят в `queue-dlt` и `result-dlt`. Поля, которых нет в контракте, считаются ошибкой.

```ts
export interface JobQueueMessage {
    format: string;
    speed: number;
    pitchSemitones: number;
}

export interface JobResultMessage {
    status: string; // ERROR|DONE
    expireAt: number; // timestamp
}
```
