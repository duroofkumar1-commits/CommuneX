param(
    [switch]$InspectDatabase
)

Set-Location $PSScriptRoot
$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $envFile)) {
    Write-Host "backend/.env not found. Copy .env.example to .env and fill in your own secrets." -ForegroundColor Red
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line -split "=", 2
        if ($parts.Count -eq 2) {
            [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
        }
    }
}

$required = @("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD", "JWT_SECRET")
foreach ($name in $required) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, "Process"))) {
        Write-Host "$name is missing from backend/.env" -ForegroundColor Red
        exit 1
    }
}

if ($InspectDatabase) {
    # Read-only inspection mode: no migrations and no Hibernate schema changes.
    $env:SPRING_PROFILES_ACTIVE = "db-inspect"
    $env:SPRING_FLYWAY_ENABLED = "false"
    $env:DDL_AUTO = "none"
    Write-Host "Inspecting schema metadata only. No schema changes will be applied." -ForegroundColor Cyan
} else {
    Write-Host "Starting CommuneX backend with configured environment." -ForegroundColor Green
}

mvn spring-boot:run
