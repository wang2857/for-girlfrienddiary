#!/usr/bin/env bash
# 归档一批产物：重打源码包 + 生成 SHA256SUMS。
#
# 用法：
#   ./scripts/archive.sh            # 按当前 HEAD 重打源码包、算校验值
#   ./scripts/archive.sh --verify   # 只校验现有 dist/ 产物与 SHA256SUMS 是否一致
#
# 约定：
#   - 源码包走 `git archive HEAD`，只含**已提交**的文件，不含 .git / build/ / dist/，
#     时间戳取 commit 时间，所以同一个 commit 重打出来的 zip 字节一致（可复现）。
#   - APK 不打进源码包：它本身就在 dist/ 里随版本入库，避免套娃。
#   - 校验值统一记在 dist/SHA256SUMS，用 `shasum -a 256 -c dist/SHA256SUMS` 复核。
set -euo pipefail

cd "$(dirname "$0")/.."

DIST="dist"

version_name() {
  grep -oE 'versionName = "[^"]+"' app/build.gradle.kts | head -1 | sed -E 's/.*"(.*)"/\1/'
}

version_code() {
  grep -oE 'versionCode = [0-9]+' app/build.gradle.kts | head -1 | sed -E 's/[^0-9]//g'
}

write_sums() {
  # 固定顺序，输出稳定；只列 dist 里的产物
  (
    cd "$DIST"
    ls -1 *.apk *.zip 2>/dev/null | LC_ALL=C sort | while read -r f; do
      shasum -a 256 "$f"
    done
  ) > "$DIST/SHA256SUMS"
}

if [[ "${1:-}" == "--verify" ]]; then
  [[ -f "$DIST/SHA256SUMS" ]] || { echo "缺少 $DIST/SHA256SUMS" >&2; exit 1; }
  ( cd "$DIST" && shasum -a 256 -c SHA256SUMS )
  exit 0
fi

VER="$(version_name)"
CODE="$(version_code)"
[[ -n "$VER" ]] || { echo "读不到 versionName" >&2; exit 1; }

# 已跟踪文件的改动会让源码包和 HEAD 对不上，直接拦下；
# dist/ 是脚本自己的输出目录（且不进源码包），被上次归档改脏属正常，不参与判断。
# 未跟踪文件本来就不进包，只提醒一声（dist/ 里的 APK 就属于这种）
if [[ -n "$(git status --porcelain --untracked-files=no -- . ':(exclude)dist')" ]]; then
  echo "已跟踪文件还有未提交的改动，源码包会和 HEAD 对不上：" >&2
  git status --short --untracked-files=no -- . ':(exclude)dist' >&2
  echo "先提交，再归档。" >&2
  exit 1
fi

untracked="$(git ls-files --others --exclude-standard)"
if [[ -n "$untracked" ]]; then
  echo "提醒：以下未跟踪文件不会进源码包：" >&2
  echo "$untracked" | sed 's/^/  /' >&2
fi

ZIP="$DIST/日记-${VER}-源码.zip"
mkdir -p "$DIST"

# 排除 dist/ 自身（APK、旧源码包都在里面，不该套进源码包）
git archive --format=zip --output="$ZIP" HEAD -- . ':(exclude)dist'

write_sums

echo "版本：${VER}（versionCode ${CODE}）"
echo "源码包：${ZIP}  $(du -h "$ZIP" | cut -f1)"
echo
cat "$DIST/SHA256SUMS"
