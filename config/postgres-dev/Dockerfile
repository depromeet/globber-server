# 개발 DB 이미지: postgres:16 + pg_bigm (도시 검색에서 사용)
# pg_bigm은 apt 패키지가 없어서 공식 소스로 빌드하고, 빌드 도구는 같은 레이어에서 지운다
FROM postgres:16

ARG PG_BIGM_VERSION=1.2-20250903

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        build-essential postgresql-server-dev-16 libicu-dev curl ca-certificates; \
    curl -fsSL "https://github.com/pgbigm/pg_bigm/archive/refs/tags/v${PG_BIGM_VERSION}.tar.gz" | tar xz -C /tmp; \
    make -C "/tmp/pg_bigm-${PG_BIGM_VERSION}" USE_PGXS=1; \
    make -C "/tmp/pg_bigm-${PG_BIGM_VERSION}" USE_PGXS=1 install; \
    apt-get purge -y --auto-remove build-essential postgresql-server-dev-16 libicu-dev curl; \
    rm -rf "/tmp/pg_bigm-${PG_BIGM_VERSION}" /var/lib/apt/lists/*
