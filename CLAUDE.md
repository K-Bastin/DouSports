# DouSports — Instructions Claude

## Workflow Git

### Branches
- Toujours créer une branche dédiée par feature/fix : `feat/issue-NN-titre` ou `fix/titre`
- Ne jamais committer directement sur `develop` ou `main`

### Pull Requests — OBLIGATOIRES
- **Créer une PR systématiquement** pour chaque branche avant de merger, sans attendre que l'utilisateur le demande
- PR cible : `develop`
- Après merge de la PR dans develop : merger develop → main avec `--no-ff`

### Bump de version — à faire au merge main
- **Toujours bumper la version lors du merge develop → main**, dans le même commit
- Patch (1.x.Y) pour des fixes, Minor (1.X.0) pour des nouvelles features
- `versionCode` s'incrémente de 1 à chaque release

### Release
- La CI (`release.yml`) se déclenche sur push `main` et crée automatiquement la GitHub Release + APK signé
- Ne pas pousser de tag manuellement (le workflow le fait)

### Clôture des issues
- Fermer les issues GitHub correspondantes après merge dans main

## Sécurité
- Ne jamais committer le keystore
- Les secrets de signature (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) restent uniquement dans GitHub Secrets
