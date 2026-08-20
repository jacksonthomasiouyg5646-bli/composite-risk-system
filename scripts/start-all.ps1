$ErrorActionPreference = "Stop"

& "$PSScriptRoot\start-backend.ps1"
Start-Sleep -Seconds 8
Start-Process powershell.exe -WindowStyle Hidden -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit", "-Command", "& '$PSScriptRoot\start-frontend.ps1'"

Write-Host "System is starting."
Write-Host "Frontend: http://localhost:5173"
Write-Host "Gateway:  http://localhost:8088"
Write-Host "Eureka:   http://localhost:8761"
