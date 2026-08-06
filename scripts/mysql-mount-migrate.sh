#!/usr/bin/env bash
# MySQL 数据卷挂载迁移（一次性保命操作，幂等：已挂载则跳过并提示）
# 背景：mysql-test 容器无挂载，数据在容器可写层，容器删除 = 数据丢失
# 用法: MYSQL_ROOT_PASSWORD=你的库密码 bash mysql-mount-migrate.sh
# 流程: dump 保命备份 -> 停容器 -> 数据目录拷出 -> 挂载重建 -> 验证
# 停机窗口: 约 1-2 分钟（拷贝数据目录的时间），建议低峰执行
set -euo pipefail

PASS="${MYSQL_ROOT_PASSWORD:?用法: MYSQL_ROOT_PASSWORD=xxx bash mysql-mount-migrate.sh}"
TS=$(date +%Y%m%d-%H%M%S)
DATA_DIR=/opt/mysql-data/mysql
BACKUP_DIR=/opt/mysql-backup

echo "==> [1/6] 确认当前容器状态与挂载情况"
docker ps --filter name=mysql-test --format '{{.Names}} {{.Status}}' || { echo "mysql-test 未运行，请先确认" >&2; exit 1; }
MOUNTS=$(docker inspect mysql-test --format '{{json .Mounts}}')
if [ "$MOUNTS" != "[]" ] && [ -n "$MOUNTS" ]; then
  echo "  mysql-test 已有挂载: $MOUNTS，无需迁移，退出。"
  exit 0
fi
echo "  确认无挂载，开始迁移。"

echo "==> [2/6] 保命备份：mysqldump 全库导出（容器仍在运行，一致性快照）"
mkdir -p "$BACKUP_DIR"
BACKUP_FILE="$BACKUP_DIR/mysql-full-$TS.sql"
docker exec mysql-test mysqldump -uroot -p"$PASS" --all-databases --single-transaction > "$BACKUP_FILE"
ls -lh "$BACKUP_FILE"
echo "  备份完成: $BACKUP_FILE（先别删，验证通过后再清理）"

echo "==> [3/6] 停止容器并拷贝数据目录到宿主机"
docker stop mysql-test
mkdir -p /opt/mysql-data
rm -rf "$DATA_DIR"    # 迁移可重跑：上次残留目录先清掉（数据已在备份文件里）
docker cp mysql-test:/var/lib/mysql/. "$DATA_DIR/" 2>/dev/null || {
  echo "  docker cp 失败，容器可能已删除；用备份恢复: docker exec ... mysql < $BACKUP_FILE" >&2
  exit 1
}
echo "  数据目录已拷出: $DATA_DIR（$(du -sh "$DATA_DIR" | cut -f1)）"

echo "==> [4/6] 修复数据目录属主（mysql 镜像内 UID=999）"
chown -R 999:999 "$DATA_DIR"

echo "==> [5/6] 删除旧容器并用挂载重建（restart: unless-stopped，重启/升级不丢数据）"
docker rm mysql-test
docker run -d --name mysql-test --restart unless-stopped -p 3306:3306 \
  -v "$DATA_DIR:/var/lib/mysql" mysql

echo "==> [6/6] 等待就绪并验证数据完整"
for i in $(seq 1 30); do
  if docker exec mysql-test mysql -uroot -p"$PASS" -e "SHOW DATABASES;" >/dev/null 2>&1; then
    echo "  新容器就绪，数据库列表:"
    docker exec mysql-test mysql -uroot -p"$PASS" -e "SHOW DATABASES;" | grep -v '^Database$'
    echo "============================================"
    echo "迁移成功！当前挂载:"
    docker inspect mysql-test --format '{{range .Mounts}}{{.Source}} -> {{.Destination}}{{println}}{{end}}'
    echo "备份文件保留: $BACKUP_FILE（建议验证无误后 7 天再删）"
    exit 0
  fi
  sleep 2
done
echo "  容器 60 秒内未就绪，查看日志: docker logs mysql-test --tail 50" >&2
echo "  数据未丢失，备份在: $BACKUP_FILE" >&2
exit 1
