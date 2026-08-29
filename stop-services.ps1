$services = 'discovery-server|config-server|stats-server|user-service|request-service|event-service|gateway-server'

Get-CimInstance Win32_Process -Filter "Name='powershell.exe'" |
    Where-Object {
        $_.CommandLine -match 'spring-boot:run' -and
        $_.CommandLine -match $services
    } |
    ForEach-Object {
        taskkill.exe /PID $_.ProcessId /T /F
    }
