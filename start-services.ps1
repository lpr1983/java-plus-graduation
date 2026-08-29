$root = $PSScriptRoot
$mvn = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin\mvn.cmd'

$services = @(
    'infra/discovery-server',
    'infra/config-server',
    'stat/stats-server',
    'core/user-service',
    'core/request-service',
    'core/event-service',
    'infra/gateway-server'
)

& $mvn install -DskipTests

if ($LASTEXITCODE -ne 0) {
    throw 'Failed to build and install project modules.'
}

foreach ($service in $services) {
    $command = "& '$mvn' -pl '$service' spring-boot:run"
    Start-Process powershell.exe -WorkingDirectory $root `
        -ArgumentList '-NoExit', '-Command', $command
    Start-Sleep -Seconds 10
}
