# GitLab CI/CD Setup Guide

This guide will help you build your Android APK using GitLab's free CI/CD service.

## Step 1: Create a GitLab Account

1. Go to [https://gitlab.com](https://gitlab.com)
2. Sign up for a free account (if you don't have one)
3. Verify your email address

## Step 2: Create a New Project

1. Click the **"New project"** button (or go to https://gitlab.com/projects/new)
2. Choose **"Create blank project"**
3. Fill in:
   - **Project name**: `android-camera` (or any name you prefer)
   - **Visibility Level**: Choose **Private** (free tier includes 2,000 CI/CD minutes/month) or **Public** (unlimited minutes)
4. Click **"Create project"**

## Step 3: Initialize Git (if needed)

If you haven't initialized git yet, run this in your project directory:

```powershell
git init
git add .
git commit -m "Initial commit"
```

## Step 4: Push Your Code to GitLab

### Option A: Using GitLab Web Interface

1. In your new GitLab project, you'll see instructions to push existing code
2. Open PowerShell in your project directory (`D:\work\android-camera`)
3. Run these commands:

```powershell
# Initialize git (if not already done)
git init

# Add GitLab remote (replace YOUR_USERNAME with your GitLab username)
git remote add origin https://gitlab.com/YOUR_USERNAME/android-camera.git

# Add all files
git add .

# Commit
git commit -m "Initial commit"

# Push to GitLab
git push -u origin main
```

**Note**: If you get an error about "main" branch, try `master` instead:
```powershell
git push -u origin master
```

### Option B: Using GitLab Desktop or Git GUI

You can also use any Git GUI tool to push your code.

## Step 5: Build Your APK

1. After pushing your code, GitLab will automatically start building
2. Go to your project on GitLab.com
3. Click on **"CI/CD"** → **"Pipelines"** in the left sidebar
4. You'll see a pipeline running (it may take 5-10 minutes the first time)
5. Once it's complete (green checkmark), click on the pipeline
6. Click on the **"build"** job
7. In the job details, look for **"Job artifacts"** section
8. Click **"Download"** next to `app-debug.apk`

## Step 6: Install the APK

1. Transfer the downloaded APK to your Android device
2. Enable **"Install from Unknown Sources"** in your device settings
3. Tap the APK file to install

## Troubleshooting

### Build Fails
- Check the pipeline logs by clicking on the failed job
- Common issues:
  - Missing dependencies (should be auto-downloaded)
  - Gradle wrapper issues (the CI Docker image has Gradle pre-installed, so this should work)
  - If you see "gradlew not found", the CI will use system Gradle from the Docker image

### Can't Find Artifacts
- Make sure the pipeline completed successfully (green checkmark)
- Artifacts are available for 1 week after build
- Click on the job (not just the pipeline) to see artifacts

### Need to Rebuild
- Push any new commit to trigger a new build
- Or go to **CI/CD** → **Pipelines** → Click **"Run pipeline"**

## Building Release APK (Optional)

To build a release APK instead of debug, you'll need to:
1. Set up signing keys (requires additional configuration)
2. Modify `.gitlab-ci.yml` to use `assembleRelease` instead of `assembleDebug`

For now, the debug APK works fine for testing and installation.

## GitLab CI/CD Benefits

- ✅ **Free**: 2,000 CI/CD minutes/month for free accounts
- ✅ **No Installation**: Everything runs in the cloud
- ✅ **Automatic**: Builds automatically on every push
- ✅ **Private Repos**: Free CI/CD even for private projects
- ✅ **Artifacts**: Download APK directly from GitLab

## Next Steps

- Every time you make changes, just push to GitLab and it will build automatically
- You can set up branch protection, automated testing, and more
- Consider setting up release builds with proper signing for production apps
