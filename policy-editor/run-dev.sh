#!/bin/sh

../gradlew quarkusDev \
        -Dquarkus.http.port=8081 \
        -Dvaadin.devmode.enabled=true \
        -Dvaadin.productionMode=false \
        -Dvaadin.devmode.optimizeBundle=true \
        -Dvaadin.project.basedir="$(pwd)" \
        -Dvaadin.frontend.generatedDir="$(pwd)/build/vaadin/generated" \
        -Dvaadin.frontend.webComponentsDirectory="$(pwd)/build/vaadin/web-components" \
        -Djboss.threads.eqe.add-opens=java.base/java.lang=ALL-UNNAMED \
        --no-configuration-cache
