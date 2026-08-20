@echo off
setlocal
set ROOT=%~dp0..
set BACKEND=%ROOT%\backend
set JAVA_TOOL_OPTIONS=--enable-native-access=ALL-UNNAMED
if "%MYSQL_USER%"=="" set MYSQL_USER=root
if "%MYSQL_URL%"=="" set MYSQL_URL=jdbc:mysql://localhost:3306/user_management?useUnicode=true^&characterEncoding=utf8^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true^&useSSL=false
if "%APOLLO_ENABLED%"=="" set APOLLO_ENABLED=false
if "%APOLLO_META%"=="" set APOLLO_META=http://localhost:8080
if "%APOLLO_NAMESPACES%"=="" set APOLLO_NAMESPACES=application,database,security
if "%REDIS_HOST%"=="" set REDIS_HOST=localhost
if "%REDIS_PORT%"=="" set REDIS_PORT=6379
if "%REDIS_DATABASE%"=="" set REDIS_DATABASE=0
if "%SESSION_TTL_SECONDS%"=="" set SESSION_TTL_SECONDS=900

if "%MYSQL_PASSWORD%"=="" goto missing_secret
if "%REDIS_PASSWORD%"=="" goto missing_secret
if "%JWT_RSA_PRIVATE_KEY%"=="" goto missing_secret
if "%JWT_RSA_PUBLIC_KEY%"=="" goto missing_secret

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":9001" ^| findstr "LISTENING"') do taskkill /PID %%a /F >nul 2>nul

cd /d "%BACKEND%"
mvn.cmd -pl auth-service -am clean spring-boot:run
exit /b %ERRORLEVEL%

:missing_secret
echo Required secret environment variables are missing. See .env.example.
exit /b 1
