#!/usr/bin/env bash
# 后端部署脚本（服务器 /opt/alphaclass/scripts/deploy.sh，由 CI 调用）
# 用法: deploy.sh <staging|prod> <image-tag>
# 例:   deploy.sh staging sha-1a2b3c4d
# 行为: 更新目标环境 env 文件中的 <ENV>_TAG → 拉新镜像 → 重建目标容器 → 健康检查
# 回滚: 用旧 tag 重跑同一命令即可（tag 持久化在 env 文件里）
set -euo pipefail

ENV=$1
TAG=$2
BASE=/opt/alphaclass
ENV_FILE="$BASE/.env.$ENV"
TAG_KEY="${ENV^^}_TAG"

[ -f "$ENV_FILE" ] || { echo "缺少 $ENV_FILE" >&2; exit 1; }

# 1. 更新目标环境的镜像 tag（幂等：已存在则替换，不存在则追加）
if grep -q "^${TAG_KEY}=" "$ENV_FILE"; then
  sed -i "s|^${TAG_KEY}=.*|${TAG_KEY}=${TAG}|" "$ENV_FILE"
else
  echo "${TAG_KEY}=${TAG}" >> "$ENV_FILE"
fi

# 2. 拉镜像并重建目标容器（只动指定服务，另一环境不受影响）
cd "$BASE"
docker compose --env-file "$ENV_FILE" up -d --pull always "backend-$ENV"

# 3. 健康检查：等待应用就绪（公开接口 /users 可达即视为启动成功；不用 /v3/api-docs，
#    因为 SWAGGER_ENABLED=false 时该端点 404 会误判）
PORT=$(grep "^SERVER_PORT=" "$ENV_FILE" | cut -d= -f2)
for i in $(seq 1 30); do
  if curl -sf "http://127.0.0.1:${PORT}/users" >/dev/null 2>&1; then
    echo "部署成功: $ENV @ $TAG（端口 $PORT）"
    exit 0
  fi
  sleep 2
done

echo "健康检查失败: $ENV @ $TAG（30 秒内未就绪）" >&2
echo "查看日志: docker logs alphaclass-backend-$ENV --tail 50" >&2
exit 1
