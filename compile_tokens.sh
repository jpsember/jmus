#!/usr/bin/env bash
set -eu

tokncompile tokens.rxp > src/main/resources/jmus/tokens.dfa
