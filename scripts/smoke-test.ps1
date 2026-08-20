$ErrorActionPreference = "Stop"

@("SMOKE_USERNAME", "SMOKE_PASSWORD", "SMOKE_CAPTCHA_ID", "SMOKE_CAPTCHA_CODE") | ForEach-Object {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_))) {
        throw "Required environment variable $_ is not set."
    }
}

$loginBody = @{
    username = $env:SMOKE_USERNAME
    password = $env:SMOKE_PASSWORD
    captchaId = $env:SMOKE_CAPTCHA_ID
    captchaCode = $env:SMOKE_CAPTCHA_CODE
} | ConvertTo-Json

$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8088/api/auth/login" -Body $loginBody -ContentType "application/json"
if ($login.code -ne 0) {
    throw "Login failed: $($login.message)"
}

$headers = @{
    Authorization = "Bearer $($login.data.token)"
}

$profile = Invoke-RestMethod -Method Get -Uri "http://localhost:8088/api/auth/profile" -Headers $headers
$users = Invoke-RestMethod -Method Get -Uri "http://localhost:8088/api/users?page=1&size=5" -Headers $headers

Write-Host "Smoke test passed."
Write-Host "Current user: $($profile.data.user.username)"
Write-Host "Users returned: $($users.data.items.Count)"
