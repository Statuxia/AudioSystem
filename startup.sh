#!/usr/bin/env bash
set -Eeuo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

kafka=compact
build_flag=--no-build
action=up
required_vars=(
  AUDIOSYSTEM_SILO_ROOT_USER
  AUDIOSYSTEM_SILO_ROOT_PASSWORD
  AUDIOSYSTEM_SILO_PRESIGNED_USER
  AUDIOSYSTEM_SILO_PRESIGNED_PASSWORD
  AUDIOSYSTEM_KAFKA_KRAFT_CLUSTER_ID
)
missing=()

while (($#)); do
  case "$1" in
   -kafka)
     if (($# < 2)); then
       echo "Ошибка. после -kafka укажите вариант: compact или common" >&2
       exit 1
     fi
     kafka="$2"
     shift 2
     ;;
   --no-build)
     build_flag=--no-build
     shift
     ;;
   --build)
     build_flag=--build
     shift
     ;;
   up|down)
     action="$1"
     shift
     ;;
   *)
     echo "Ошибка. Неизвестный аргумент: $1" >&2
     echo "Использование: ./startup.sh [up|down] [-kafka compact|common] [--build|--no-build]" >&2
     exit 1
     ;;
   esac
done

case "$kafka" in
 compact) kafka_compose_file="docker-compose-kafka-compact.yml" ;;
 common) kafka_compose_file="docker-compose-kafka-common.yml" ;;
 *)
   echo "Ошибка. Неизвестный вариант Kafka: $kafka" >&2
   echo "Допустимые варианты: compact, common" >&2
   exit 1
   ;;
esac

if [[ $action == "up" ]]; then
  for name in "${required_vars[@]}"; do
    if [[ -z "${!name:-}" ]]; then
      missing+=("$name")
    fi
  done
fi

if (( ${#missing[@]} > 0 )); then
  echo "Не заданы обязательные переменные: ${missing[*]}"
  echo "Запуск в тестовом режиме со значениями из .env.example"

  example_file=".env.example"

  if [[ ! -f "$example_file" ]]; then
    echo "Ошибка: файл $example_file не найден." >&2
    exit 1
  fi

  set -a
  source "$example_file"
  set +a

  still_missing=()

  for name in "${required_vars[@]}"; do
    if [[ -z "${!name:-}" ]]; then
      still_missing+=("$name")
    fi
  done

  if (( ${#still_missing[@]} > 0 )); then
    echo "Ошибка: в $example_file отсутствуют значения: ${still_missing[*]}" >&2
    exit 1
  fi
fi

compose_files=(
  -f docker-compose-interface.yml
  -f "$kafka_compose_file"
   -f docker-compose-redis.yml
   -f docker-compose-silo.yml
   -f docker-compose-processor.yml
)

if [[ $action == "down" ]]; then
  exec docker compose "${compose_files[@]}" down
else
  exec docker compose "${compose_files[@]}" up "${build_flag}"
fi
