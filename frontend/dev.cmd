@echo off
cd /d "%~dp0"
if not exist "node_modules\.bin\vite.cmd" (
  echo Installing frontend dependencies...
  npm.cmd install --cache "%TEMP%\user-management-npm-cache"
)
npm.cmd run dev
