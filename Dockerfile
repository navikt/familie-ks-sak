FROM ghcr.io/navikt/baseimages/temurin:17

ENV APPD_ENABLED=true
ENV APP_NAME=familie-ks-sak

COPY ./build/libs/familie-ks-sak.jar "app.jar"
