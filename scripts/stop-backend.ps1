$ports = 8761, 8088, 9001, 9002, 9003
foreach ($port in $ports) {
    Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object {
            Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
        }
}
Write-Host "Stopped backend ports: $($ports -join ', ')"
