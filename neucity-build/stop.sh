#!/bin/bash

# 启动入口类，该脚本文件用于别的项目时要改这里
JAR_NAME=neucity-build.jar

# kill
kill -9 `pgrep -f ${JAR_NAME}` 2>/dev/null
