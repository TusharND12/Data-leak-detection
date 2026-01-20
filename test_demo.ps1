# Fetch the default demo user ID using a known pattern or just hardcoded logic if possible
# Since ID is random UUID, we can't hardcode it. 
# But for the USER to use this, they need to see the log.
# This script just does a sample call assuming they pass the ID.

param (
    [string]$UserId
)

if (-not $UserId) {
    Write-Host "Please provide the User ID from the application execution logs."
    Write-Host "Example: .\test_demo.ps1 <UUID>"
    exit
}

Write-Host "Analyzing Risk for User: $UserId"
$response = Invoke-RestMethod -Uri "http://localhost:8081/api/risk/analyze/$UserId" -Method Get
Write-Host "Risk Analysis Result:"
$response | Format-List
