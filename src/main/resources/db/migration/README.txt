# Flyway 迁移脚本目录：V{版本}__{描述}.sql 随代码进 git，启动时自动执行
# 现有表结构收集方法：服务器执行
#   mysqldump -h127.0.0.1 -uroot -p --no-data alphaclass > V1__init.sql
# 然后放入本目录（删掉 DROP/CREATE DATABASE 语句，保留建表语句），提交后两库自动对齐。
# 纪律：只往前修——改错写新的 V{n+1} 纠正，不修改已执行的 V 文件（checksum 会报错）。
