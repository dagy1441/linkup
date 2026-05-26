<#
.SYNOPSIS
  Boot the LinkUp dev environment: infra in Docker, backend on host.

.DESCRIPTION
  1. Verify Docker is reachable
  2. Load .env into the current process
  3. docker compose up -d
  4. Wait until postgres + keycloak are healthy
  5. Set SPRING_PROFILES_ACTIVE=dev
  6. ./mvnw spring-boot:run

.PARAMETER SpringProfile
  Spring profile. Default: dev.

.PARAMETER SkipInfra
  Skip docker compose (assume infra is already running).

.PARAMETER Tools
  Also start pgAdmin + Mailhog.

.EXAMPLE
  .\scripts\run-dev.ps1
  .\scripts\run-dev.ps1 -Tools
  .\scripts\run-dev.ps1 -SkipInfra
#>
param(
    [string]$SpringProfile = "dev",
    [switch]$SkipInfra,
    [switch]$Tools
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

function Write-Step($msg) { Write-Host ">> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "   OK   $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "   WARN $msg" -ForegroundColor Yellow }
function Die($msg)        { Write-Host "   FAIL $msg" -ForegroundColor Red; exit 1 }

# 1. Docker reachable?
Write-Step "Checking Docker"
$null = docker info --format '{{json .ServerVersion}}' 2>&1
if ($LASTEXITCODE -ne 0) {
    Die "Docker is not reachable. Start Docker Desktop and retry."
}
Write-Ok "Docker daemon is up"

# 2. Load .env into process env
Write-Step "Loading .env"
if (-not (Test-Path .env)) {
    if (Test-Path .env.exampl) {
        Copy-Item .env.exampl .env
        Write-Warn "Created .env from .env.exampl"
    } else {
        Die ".env not found and no .env.exampl to copy from"
    }
}

Get-Content .env | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $kv = $line -split '=', 2
    if ($kv.Count -ne 2) { return }
    $name  = $kv[0].Trim()
    $value = $kv[1].Trim().Trim('"').Trim("'")
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}
Write-Ok ".env loaded into process env"

# 3+4. docker compose up + wait healthy
if (-not $SkipInfra) {
    Write-Step "Starting infrastructure (postgres + redis + keycloak)"
    if ($Tools) {
        docker compose --profile tools up -d
    } else {
        docker compose up -d
    }
    if ($LASTEXITCODE -ne 0) { Die "docker compose up failed" }

    Write-Step "Waiting for postgres + keycloak + minio healthchecks (max 180s)"
    $deadline = (Get-Date).AddSeconds(180)
    $ready = $false
    while ((Get-Date) -lt $deadline) {
        $pgId = (docker compose ps -q postgres) 2>$null
        $kcId = (docker compose ps -q keycloak) 2>$null
        $mnId = (docker compose ps -q minio) 2>$null
        if ($pgId -and $kcId -and $mnId) {
            $pg = docker inspect --format '{{.State.Health.Status}}' $pgId 2>$null
            $kc = docker inspect --format '{{.State.Health.Status}}' $kcId 2>$null
            $mn = docker inspect --format '{{.State.Health.Status}}' $mnId 2>$null
            if ($pg -eq "healthy" -and $kc -eq "healthy" -and $mn -eq "healthy") {
                $ready = $true
                break
            }
            Write-Host ("   postgres={0}  keycloak={1}  minio={2}" -f $pg, $kc, $mn) -ForegroundColor DarkGray
        } else {
            Write-Host "   waiting for containers..." -ForegroundColor DarkGray
        }
        Start-Sleep -Seconds 5
    }
    if (-not $ready) {
        Die "Timeout: infra not healthy after 180s. Run 'docker compose logs' to investigate."
    }
    Write-Ok "Infrastructure healthy"
} else {
    Write-Step "Skipping infra (SkipInfra flag set)"
}

# 5. Activate Spring profile
Write-Step ("Activating Spring profile '{0}'" -f $SpringProfile)
$env:SPRING_PROFILES_ACTIVE = $SpringProfile

# Sanity-check DB credentials before launching the JVM (saves a 30s startup).
Write-Step "Sanity-check: DB credentials"
$dbUser = $env:DB_USERNAME
$dbName = $env:DB_NAME
$pgCheck = docker compose exec -T postgres psql -U $dbUser -d $dbName -c "SELECT 1;" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host $pgCheck -ForegroundColor Red
    Die ("Cannot connect to Postgres with DB_USERNAME='{0}'. Wipe the volume if credentials drifted: docker compose down -v" -f $dbUser)
}
Write-Ok ("DB reachable as '{0}'" -f $dbUser)

# 6. Sanity-check port 8080 -- a previous Ctrl+C often leaves the JVM hanging.
#    Failing fast here with a clean message beats Spring's wall-of-stack ten seconds later.
Write-Step "Sanity-check: port 8080 is free"
$listener = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($listener) {
    $procIds = ($listener.OwningProcess | Sort-Object -Unique)
    $procNames = $procIds | ForEach-Object {
        $p = Get-Process -Id $_ -ErrorAction SilentlyContinue
        if ($p) { "$($p.ProcessName)(PID $_)" } else { "PID $_" }
    }
    Write-Warn ("Port 8080 already in use by: {0}" -f ($procNames -join ', '))
    $answer = Read-Host "Kill the listening process and continue? [y/N]"
    if ($answer -match '^[yY]') {
        $procIds | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
        Start-Sleep -Seconds 2
        if (Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue) {
            Die "Port 8080 still in use after kill attempt -- investigate manually."
        }
        Write-Ok "Port 8080 freed"
    } else {
        Die "Aborting. Free port 8080 and re-run."
    }
} else {
    Write-Ok "Port 8080 is free"
}

# 7. Launch backend
Write-Step "Starting backend on http://localhost:8080  (Ctrl+C to stop)"
Write-Host ""
Write-Host "   Swagger UI : http://localhost:8080/swagger-ui.html" -ForegroundColor Gray
Write-Host "   Health     : http://localhost:8080/actuator/health" -ForegroundColor Gray
Write-Host "   Keycloak   : http://localhost:8081  [admin / admin]" -ForegroundColor Gray
Write-Host ""

.\mvnw spring-boot:run
