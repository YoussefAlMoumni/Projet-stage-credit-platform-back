# ============================================================
#   Credit Platform - Full Stack Startup Script
#   Starts: Spring Boot Backend + JavaFX Desktop Frontend
# ============================================================

$PROJECT_DIR = $PSScriptRoot
$BACKEND_PORT = 8081
$HEALTH_URL   = "http://localhost:$BACKEND_PORT/actuator/health"

# ANSI color helpers
function Write-Color($text, $color) { Write-Host $text -ForegroundColor $color }
function Banner {
    Write-Host ""
    Write-Color "╔══════════════════════════════════════════╗" Cyan
    Write-Color "║       CREDIT PLATFORM  —  Launcher       ║" Cyan
    Write-Color "╚══════════════════════════════════════════╝" Cyan
    Write-Host ""
}

# ── 1. Banner ─────────────────────────────────────────────
Banner

# ── 2. Pre-flight checks ──────────────────────────────────
Write-Color "🔍  Running pre-flight checks..." Yellow

# Java
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Color "❌  Java not found in PATH. Please install Java 21+." Red
    exit 1
}
$javaVersion = (java -version 2>&1 | Select-String "version").ToString()
Write-Color "✅  Java found: $javaVersion" Green

# Maven
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Color "❌  Maven (mvn) not found in PATH. Please install Maven." Red
    exit 1
}
Write-Color "✅  Maven found." Green

# PostgreSQL (check if port 5432 is listening)
$pgRunning = (Test-NetConnection -ComputerName localhost -Port 5432 -WarningAction SilentlyContinue).TcpTestSucceeded
if (-not $pgRunning) {
    Write-Color "⚠️   PostgreSQL does not appear to be listening on port 5432." Yellow
    $answer = Read-Host "    Continue anyway? (y/N)"
    if ($answer -ne 'y' -and $answer -ne 'Y') { exit 1 }
} else {
    Write-Color "✅  PostgreSQL is running on port 5432." Green
}

Write-Host ""

# ── 3. Start Spring Boot backend in a new window ──────────
Write-Color "🚀  Starting Spring Boot backend (port $BACKEND_PORT)..." Cyan

$backendProcess = Start-Process powershell -ArgumentList `
    "-NoExit", "-Command", `
    "Write-Host '[ BACKEND ]' -ForegroundColor Cyan; " + `
    "Set-Location '$PROJECT_DIR'; " + `
    "mvn spring-boot:run" `
    -PassThru

Write-Color "    Backend PID: $($backendProcess.Id)" DarkGray

# ── 4. Wait for backend to be ready ───────────────────────
Write-Host ""
Write-Color "⏳  Waiting for backend to be ready..." Yellow

$maxWait  = 90   # seconds
$elapsed  = 0
$interval = 3
$ready    = $false

while ($elapsed -lt $maxWait) {
    Start-Sleep -Seconds $interval
    $elapsed += $interval

    try {
        $response = Invoke-WebRequest -Uri $HEALTH_URL -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            $ready = $true
            break
        }
    } catch {
        # Backend not yet ready — keep trying
        # Fallback: try a plain TCP connect on port 8081
        $tcpReady = (Test-NetConnection -ComputerName localhost -Port $BACKEND_PORT -WarningAction SilentlyContinue).TcpTestSucceeded
        if ($tcpReady) { $ready = $true; break }
    }

    Write-Host "    [$elapsed s] Still starting..." -ForegroundColor DarkGray
}

if (-not $ready) {
    Write-Color "❌  Backend did not start within $maxWait seconds." Red
    Write-Color "    Check the backend window for errors." Red
    exit 1
}

Write-Host ""
Write-Color "✅  Backend is UP and running!" Green

# ── 5. Launch JavaFX Desktop App ──────────────────────────
Write-Host ""
Write-Color "🖥️   Launching JavaFX desktop application..." Cyan

Start-Process powershell -ArgumentList `
    "-NoExit", "-Command", `
    "Write-Host '[ DESKTOP ]' -ForegroundColor Magenta; " + `
    "Set-Location '$PROJECT_DIR'; " + `
    "mvn exec:java '-Dexec.mainClass=com.talan.creditplatform.view.DesktopLauncher'"

Write-Host ""
Write-Color "═══════════════════════════════════════════════" Cyan
Write-Color "  ✔  All systems GO — enjoy Credit Platform!  " Green
Write-Color "═══════════════════════════════════════════════" Cyan
Write-Host ""
Write-Color "  Backend  ➜  http://localhost:$BACKEND_PORT" DarkCyan
Write-Color "  Desktop  ➜  JavaFX window opened" DarkCyan
Write-Host ""
Write-Color "  Press ENTER to close this launcher window..." DarkGray
Read-Host
