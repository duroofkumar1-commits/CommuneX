Set-Location $PSScriptRoot

Get-Content ".env" | ForEach-Object {
    $line = $_.Trim()

    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line -split '=', 2

        if ($parts.Count -eq 2) {
            $key = $parts[0].Trim()
            $value = $parts[1].Trim()

            if ($value.Length -ge 2 -and
                (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                 ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }

            if ($key -match '^[A-Za-z_][A-Za-z0-9_]*$') {
                Set-Item "Env:$key" $value
            }
        }
    }
}

if ($env:DATABASE_URL -match '^postgres(ql)?://') {
    $env:DATABASE_URL = $env:DATABASE_URL -replace '^postgres(ql)?://', 'jdbc:postgresql://'
}

if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    $env:JWT_SECRET = [Environment]::GetEnvironmentVariable("JWT_SECRET", "User")
}

if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    Write-Host "JWT_SECRET missing. Configure it first." -ForegroundColor Red
    Read-Host "Press Enter to close"
    return
}

if ([string]::IsNullOrWhiteSpace($env:DATABASE_URL) -or
    $env:DATABASE_URL -notmatch '^jdbc:postgresql://') {
    Write-Host "DATABASE_URL missing or invalid in .env" -ForegroundColor Red
    Read-Host "Press Enter to close"
    return
}

Write-Host "Starting CommuneX backend..." -ForegroundColor Green
mvn spring-boot:run