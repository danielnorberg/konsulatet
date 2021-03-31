#!/usr/bin/env bash
while :; do
  mvn clean compile exec:java
  sleep 30
done