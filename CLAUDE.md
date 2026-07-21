# DouSports — Instructions Claude

## Workflow Git — à suivre pour chaque fix et chaque feature

### Étapes dans l'ordre

1. **Créer l'issue GitHub** (titre clair, labels, description)
2. **Créer la branche** depuis `develop` : `feat/issue-NN-titre` ou `fix/issue-NN-titre`
3. **Implémenter** et committer sur la branche
4. **Pousser** la branche : `git push -u origin <branche>`
5. **Créer la PR** ciblant `develop`, avec `Closes #NN` dans le body — lie l'issue à la PR pour fermeture automatique au merge
6. **Merger la PR via GitHub** avec `mcp__github__merge_pull_request` (squash) — ne jamais merger localement, GitHub ne verrait pas le merge et la PR resterait ouverte
7. **Récupérer develop** localement : `git checkout develop && git pull origin develop`
8. **Merger develop → main** avec `--no-ff` ET bump de version dans le même commit de merge
9. **Pousser main** : la CI déclenche la GitHub Release + APK signé automatiquement
10. **Supprimer la branche** locale : `git branch -d <branche>`

### Branches
- Toujours créer une branche dédiée : `feat/issue-NN-titre` ou `fix/issue-NN-titre`
- Ne jamais committer directement sur `develop` ou `main`

### Bump de version — au merge develop → main
- Patch (1.x.Y) pour des fixes, Minor (1.X.0) pour des nouvelles features
- `versionCode` s'incrémente de 1 à chaque release

### Release
- La CI (`release.yml`) se déclenche sur push `main` — ne pas pousser de tag manuellement

## Sécurité
- Ne jamais committer le keystore
- Les secrets de signature (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) restent uniquement dans GitHub Secrets
