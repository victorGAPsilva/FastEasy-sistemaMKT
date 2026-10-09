#!/usr/bin/env bash
#
# Sincroniza o projeto com o GitHub.
#
# Uso:
#   ./scripts/sync-github.sh "feat: minha alteração"
#
# Sem mensagem, usa um texto padrão com a data/hora atual.
# O commit é feito pelo git do WSL e o push pelo git do Windows
# (que usa o Git Credential Manager para autenticar no GitHub).
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WINGIT="/mnt/c/Program Files/Git/bin/git.exe"
BRANCH="main"
REMOTE="origin"

MSG="${1:-chore: atualiza projeto em $(date '+%Y-%m-%d %H:%M')}"

cd "$REPO_DIR"

git add -A
if git diff --cached --quiet; then
    echo "Nada para commitar."
else
    git commit -m "$MSG"
fi

if [ -x "$WINGIT" ] && command -v wslpath >/dev/null 2>&1; then
    WINPATH="$(wslpath -w "$REPO_DIR")"
    "$WINGIT" -C "$WINPATH" push -u "$REMOTE" "$BRANCH"
else
    git push -u "$REMOTE" "$BRANCH"
fi

echo "Sincronizado com o GitHub."
