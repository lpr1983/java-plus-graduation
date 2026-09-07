$root = $PSScriptRoot
$mvn = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin\mvn.cmd'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'

$databases = @(
    'ewm',
    'ewm-stats',
    'ewm-users',
    'ewm-requests',
    'ewm-locations'
)

$services = @(
    'infra/discovery-server',
    'infra/config-server'
)

$env:PGPASSWORD = '12345'

try {
    foreach ($database in $databases) {
        $quotedDatabase = '"' + $database + '"'
        $dropSql = "DROP DATABASE IF EXISTS $quotedDatabase WITH (FORCE);"
        $dropSql | & $psql -h localhost -p 5432 -U dbuser -d postgres `
            -v ON_ERROR_STOP=1

        if ($LASTEXITCODE -ne 0) {
            throw "Failed to drop database: $database"
        }

        $createSql = "CREATE DATABASE $quotedDatabase OWNER dbuser;"
        $createSql | & $psql -h localhost -p 5432 -U dbuser -d postgres `
            -v ON_ERROR_STOP=1

        if ($LASTEXITCODE -ne 0) {
            throw "Failed to create database: $database"
        }
    }
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

& $mvn -pl 'infra/discovery-server,infra/config-server' -am install -DskipTests

if ($LASTEXITCODE -ne 0) {
    throw 'Failed to build and install infrastructure modules.'
}

foreach ($service in $services) {
    $command = "& '$mvn' -pl '$service' spring-boot:run"
    Start-Process powershell.exe -WorkingDirectory $root `
        -ArgumentList '-NoExit', '-Command', $command
    Start-Sleep -Seconds 10
}
