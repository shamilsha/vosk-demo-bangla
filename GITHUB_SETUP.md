# Adding This Android App as a Separate GitHub Repo

## 1. Create a new repository on GitHub

1. On GitHub: click **New** (green button) or go to [github.com/new](https://github.com/new).
2. Set **Repository name** (e.g. `vosk-demo-bengali`).
3. Choose visibility (Public or Private).
4. **Do not** add a README, .gitignore, or license if you are pushing existing code.
5. Click **Create repository**.

## 2. Push this Android app to the new repo

On your machine, in the **Android project folder** (e.g. `vosk-demo-bengali`):

**If this folder is not yet a git repo:**

```bash
git init
git add .
git commit -m "Initial commit: Android Bengali/Vosk demo app"
git branch -M main
git remote add origin https://github.com/shamilsha/vosk-demo-bengali.git
git push -u origin main
```

**If it already has a git repo** (e.g. cloned from elsewhere):

```bash
git remote add origin https://github.com/shamilsha/vosk-demo-bengali.git
git branch -M main
git push -u origin main
```

Use **SSH** if you prefer: `git@github.com:shamilsha/vosk-demo-bengali.git` (replace repo name if different).

## 3. Repo size and .gitignore

The project `.gitignore` is set up to avoid committing:

- Build outputs (`build/`, `*.apk`, `*.aab`)
- IDE files (`.idea/`, `*.iml`)
- Gradle cache (`.gradle/`)
- Logs and temp files (`logcat*.txt`, `*.log`)
- Keystores (`*.jks`, `*.keystore`)

**Optional:** To keep the repo smaller, you can exclude the Bengali model in `app/src/main/assets/model-bn/` by uncommenting those lines in `.gitignore`, then add a README note on how to obtain the model (e.g. download link or script).
