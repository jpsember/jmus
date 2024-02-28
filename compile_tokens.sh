#!/usr/bin/env bash
set -eu

dev dfa tokens.rxp src/main/resources/jmus/tokens.dfa ids src/main/java/jmus/MusUtil.java
