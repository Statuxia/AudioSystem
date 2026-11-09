### Подготовка среды

Перед запуском необходимо создать кластер и применить общие манифесты и подготовить значения для переменных секретов.

Предполагается, что выполнение всех команд происходит из корневой папки проекта. Выполняя из k8s директории исключите из команд префикс k8s/.

```bash
kind create cluster --name audiosystem
kubectl apply -f k8s/namespaces.yml -f k8s/component-limit-range.yml
```

```bash
# Silo credentials
export AUDIOSYSTEM_SILO_ROOT_USER='silo-s3'
export AUDIOSYSTEM_SILO_ROOT_PASSWORD='<password>'
export AUDIOSYSTEM_SILO_PRESIGNED_USER='silo-presigned'
export AUDIOSYSTEM_SILO_PRESIGNED_PASSWORD='<password>'

export AUDIOSYSTEM_KAFKA_KRAFT_CLUSTER_ID='<cluster-id>'
```

### Redis

Для Redis сервиса достаточно применить соответствующие манифесты
```bash
kubectl apply -f k8s/redis/pvcs.yml -f k8s/redis/deployment.yml -f k8s/redis/service.yml
```

Проверить что сервис поднялся можно через
```bash
# проверяет только под, не сам сервис kubectl rollout status deployment/redis -n audiosystem
kubectl run redis-check --rm -i --restart=Never -n audiosystem --image=redis:alpine3.22 -- \
 sh -c "redis-cli -h redis ping"
```

### Silo

Перед выполнением необходимо установить значения для переменных в плейсхолдерах

```bash
kubectl create secret generic silo-credentials -n audiosystem \
  --from-literal=root-user="$AUDIOSYSTEM_SILO_ROOT_USER" \
  --from-literal=root-password="$AUDIOSYSTEM_SILO_ROOT_PASSWORD" \
  --from-literal=presigned-user="$AUDIOSYSTEM_SILO_PRESIGNED_USER" \
  --from-literal=presigned-password="$AUDIOSYSTEM_SILO_PRESIGNED_PASSWORD"
```

Проверить, что секрет создался:

```bash
kubectl describe secret silo-credentials -n audiosystem
```

После этого можно применить все манифесты
```bash
kubectl apply -f k8s/silo/pvcs.yml -f k8s/silo/deployment.yml -f k8s/silo/service.yml
```

И отдельно применить job на init операцию для добавления presigned user и правил для хранения сурсов
```bash
kubectl delete job -n audiosystem silo-init
kubectl apply -f k8s/silo/init.yml
```

Проверить что сервис поднялся можно через
```bash
kubectl run s3-check --rm -i --restart=Never -n audiosystem --image=redis:alpine3.22 -- \
 sh -c "wget -q -T 3 -O /dev/null http://silo:9000/minio/health/live && echo SILO-OK || echo SILO-FAIL"
```

### Kafka

Перед выполнением необходимо установить значения для переменных в плейсхолдерах

```bash
kubectl create secret generic kafka-credentials -n audiosystem \
  --from-literal=kraft-cluster-id="$AUDIOSYSTEM_KAFKA_KRAFT_CLUSTER_ID"
```

Проверить, что секрет создался:

```bash
kubectl describe secret kafka-credentials -n audiosystem
```

После этого можно применить все манифесты, по умолчанию создающие кворум из 3 реплик
```bash
kubectl apply -f k8s/kafka/statefulset.yml -f k8s/kafka/service.yml
```

И отдельно применить job на init операцию для создания топиков
```bash
kubectl delete job -n audiosystem kafka-init
kubectl apply -f k8s/kafka/init.yml
```

Проверить что сервис поднялся можно через

```bash
# открытие туннеля
kubectl port-forward pod/kafka-0 9094:9094 -n audiosystem
# проверка поднятой кафки
docker run --rm --network host bitnamilegacy/kafka@sha256:aa0b2aee8c5610dd1d18d48b4f1df0dbe3267b5d4c338d36c9af9cbf0529c0b0 \
 /opt/bitnami/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server 127.0.0.1:9094
# проверка выполненной job (изнутри кластера)
kubectl run kafka-check --rm -i --restart=Never -n audiosystem \
  --image=bitnamilegacy/kafka@sha256:aa0b2aee8c5610dd1d18d48b4f1df0dbe3267b5d4c338d36c9af9cbf0529c0b0 \
  --command -- /opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --describe
```
