# Credit Platform backend launcher

$PROJECT_DIR = $PSScriptRoot
$BACKEND_PORT = 8081
$FRONTEND_LOGIN_URL = "http://localhost:4200/login.html"

function Write-Color($text, $color) {
    Write-Host $text -ForegroundColor $color
}

Write-Host ""
Write-Color "Credit Platform API launcher" Cyan
Write-Host ""

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Color "Java was not found in PATH. Please install Java 21 or newer." Red
    exit 1
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Color "Maven was not found in PATH. Please install Maven." Red
    exit 1
}

Write-Color "Starting Spring Boot backend on port $BACKEND_PORT..." Cyan
Start-Process powershell -ArgumentList `
    "-NoExit", "-Command", `
    "Write-Host '[ BACKEND ]' -ForegroundColor Cyan; " + `
    "Set-Location '$PROJECT_DIR'; " + `
    "mvn spring-boot:run" `
    -PassThru | Out-Null

Write-Host ""
Write-Color "Backend URL: http://localhost:$BACKEND_PORT" Green
Write-Color "Frontend login: $FRONTEND_LOGIN_URL" Green
Write-Host ""
Write-Color "Run the Angular frontend from projet_stage_front with: npm.cmd start" DarkCyan
Write-Host ""
Read-Host "Press ENTER to close this launcher"
