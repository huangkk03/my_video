#!/bin/bash

# Start the Java application with proxy parameters
exec java $JAVA_OPTS \
    ${HTTP_PROXY_HOST:+-Dhttp.proxyHost=$HTTP_PROXY_HOST} \
    ${HTTP_PROXY_PORT:+-Dhttp.proxyPort=$HTTP_PROXY_PORT} \
    ${HTTPS_PROXY_HOST:+-Dhttps.proxyHost=$HTTPS_PROXY_HOST} \
    ${HTTPS_PROXY_PORT:+-Dhttps.proxyPort=$HTTPS_PROXY_PORT} \
    ${NON_PROXY:+-Dhttp.nonProxyHosts="$NON_PROXY"} \
    -jar /app.jar
