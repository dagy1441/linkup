<#
.SYNOPSIS
  Fetch an OAuth2 access token from local Keycloak for a seeded user.

.PARAMETER User
  Username. Default: alice@linkup.io (organizer + user).
  Other seeded user: bob@linkup.io (user only).

.PARAMETER Password
  Password. Default depends on -User (alice123 / bob123).

.PARAMETER Copy
  Also copy the token to the clipboard.

.EXAMPLE
  .\scripts\get-token.ps1                       # Alice's token to stdout
  .\scripts\get-token.ps1 -User bob@linkup.io   # Bob's token
  .\scripts\get-token.ps1 -Copy                 # also clipboard
  $TOKEN = .\scripts\get-token.ps1              # capture into variable
#>
param(
    [string]$User = "alice@linkup.io",
    [string]$Password,
    [switch]$Copy
)

if (-not $Password) {
    $Password = switch ($User) {
        "alice@linkup.io" { "alice123" }
        "bob@linkup.io"   { "bob123" }
        default           { Write-Host "Provide -Password for $User" -ForegroundColor Red; exit 1 }
    }
}

$ErrorActionPreference = "Stop"
$tokenUrl = "http://localhost:8081/realms/linkup/protocol/openid-connect/token"

try {
    $response = Invoke-RestMethod -Method Post -Uri $tokenUrl `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
            grant_type = "password"
            client_id  = "linkup-web"
            username   = $User
            password   = $Password
        }
} catch {
    Write-Host "Failed to reach Keycloak at $tokenUrl" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Is Keycloak running? Try: docker compose ps keycloak" -ForegroundColor Yellow
    exit 1
}

if ($Copy) {
    $response.access_token | Set-Clipboard
    Write-Host "Token copied to clipboard (user=$User, expires in $($response.expires_in)s)" -ForegroundColor Green
}

# Print just the token to stdout so it can be captured into a variable.
$response.access_token
