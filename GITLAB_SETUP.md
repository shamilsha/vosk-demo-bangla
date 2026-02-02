# Adding This Android App as a Separate GitLab Repo

## 1. Create a new project in GitLab

1. In GitLab: **New project** → **Create blank project**.
2. Set **Project name** (e.g. `vosk-demo-bengali`).
3. Choose visibility (Private/Internal/Public).
4. **Do not** initialize with a README if you will push existing code.
5. Create the project.

## 2. Push this Android app to the new repo

From your machine, in the **Android project folder** (e.g. `vosk-demo-bengali`):

```bash
# If this folder is not yet a git repo:
git init

# Add GitLab as remote (replace with your GitLab URL and project path)
git remote add origin https://gitlab.com/YOUR_GROUP_OR_USER/vosk-demo-bengali.git
# or SSH:
# git remote add origin git@gitlab.com:YOUR_GROUP_OR_USER/vosk-demo-bengali.git

# Add and commit (after .gitignore is in place)
git add .
git commit -m "Initial commit: Android Bengali/Vosk demo app"

# Push (main or master depending on your default branch)
git push -u origin main
```

If the folder **already has** a git repo (e.g. cloned from elsewhere):

```bash
# Add the new GitLab repo as a remote
git remote add gitlab https://gitlab.com/YOUR_GROUP_OR_USER/vosk-demo-bengali.git

# Push your current branch
git push -u gitlab main
```

## 3. Large files and .gitignore

- Use the updated `.gitignore` in this project to avoid committing build outputs, IDE files, and (optionally) large assets.
- **Model files** (`app/src/main/assets/model-bn/*.onnx`) are large. Options:
  - **Ignore them** (see below): repo stays small; document in README how to obtain and add the model (e.g. download script or link).
  - **Keep them in repo**: one-click clone and build, but repo size grows by the model size (~tens of MB).

See the project `.gitignore` for what is excluded.
