#!/usr/bin/env bash
# 后端部署脚本（服务器 /opt/alphaclass/scripts/deploy.sh，由 CI 调用）
# 用法: deploy.sh <image-tag>
# 例:   deploy.sh sha-1a2b3c4d
# 行为: 更新 .env.v2 中 V2_TAG → 拉新镜像 → 重建 backend-v2 容器 → 健康检查
# 回滚: 用旧 tag 重跑同一命令即可（tag 持久化在 .env.v2 里）
set -euo pipefail

TAG=$1
BASE=/opt/alphaclass
ENV_FILE="$BASE/.env.v2"
TAG_KEY="V2_TAG"

[ -f "$ENV_FILE" ] || { echo "缺少 $ENV_FILE" >&2; exit 1; }

# 1. 更新镜像 tag（幂等：已存在则替换，不存在则追加）
if grep -q "^${TAG_KEY}=" "$ENV_FILE"; then
  sed -i "s|^${TAG_KEY}=.*|${TAG_KEY}=${TAG}|" "$ENV_FILE"
else
  echo "${TAG_KEY}=${TAG}" >> "$ENV_FILE"
fi

# 2. 先拉镜像验证可用（拉失败则不动现有容器，线上服务不中断）
docker pull "ghcr.io/jiamingzang/alphaclass-backend:$TAG"

# 3. 清理存量 tomcat9v2（无挂载的旧部署）避免容器名冲突，然后重建容器
cd "$BASE"
docker rm -f tomcat9v2 2>/dev/null || true
docker compose --env-file "$ENV_FILE" up -d --pull always backend-v2

# 4. 健康检查：等待应用就绪（公开接口 /users 可达即视为启动成功；不用 /v3/api-docs，
#    因为 SWAGGER_ENABLED=false 时该端点 404 会误判）
#    用 docker port 取宿主映射端口（SERVER_PORT 是容器内端口；curl 127.0.0.1:容器内端口
#    会打到原版 tomcat9 的 8080，其 context 是 /alphaclassV2，/users 404 会误判失败）
HOST_PORT=$(docker port tomcat9v2 8080/tcp | head -1 | sed 's/.*://')
for i in $(seq 1 30); do
  if curl -sf "http://127.0.0.1:${HOST_PORT}/users" >/dev/null 2>&1; then
    echo "部署成功: $TAG（宿主端口 $HOST_PORT）"
    exit 0
  fi
  sleep 2
done

echo "健康检查失败: $TAG（30 秒内未就绪）" >&2
echo "查看日志: docker logs tomcat9v2 --tail 50" >&2
exit 1
