#!/bin/bash

# 启动入口类，该脚本文件用于别的项目时要改这里
JAR_NAME=neucity-build.jar

# jar包路径
APP_BASE_PATH=$(cd `dirname $0`; pwd)
DATE_STR=$(date +%Y%m%d)

#rm -rf output.log

# 运行为后台进程，并且将信息输出到 output.log 文件
#nohup java -jar ${JAR_NAME} --spring.config.location=/data/robot21/config/application.yml > output-${DATE_STR}.log &

nohup java -jar ${JAR_NAME} --spring.config.location=/data/neucity/neucity-build/config/bootstrap.yml > output-${DATE_STR}.log &

tail -f output-${DATE_STR}.log
