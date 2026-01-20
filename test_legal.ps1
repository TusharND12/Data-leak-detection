# Test Legal Module
# 1. First, we need the Risk Assessment ID. 
#    We will run the analysis again to get it from the JSON.

param (
    [string]$UserId
)

if (-not $UserId) {
    Write-Host "Please provide the User ID: .\test_legal.ps1 <UUID>"
    exit
}

Write-Host "1. Fetching Risk Assessment..."
$analysis = Invoke-RestMethod -Uri "http://localhost:8081/api/risk/analyze/$UserId" -Method Get
$riskId = $analysis[0].id

Write-Host "   Found Risk Assessment ID: $riskId"

Write-Host "2. Preserving Evidence (Freezing)..."
$evidence = Invoke-RestMethod -Uri "http://localhost:8081/api/legal/preserve/$riskId" -Method Post

Write-Host "   EVIDENCE GENERATED!"
Write-Host "   Hash: $($evidence.contentHash)"
Write-Host "   Notice:"
Write-Host "---------------------------------------------------"
Write-Host $evidence.legalNoticeText
Write-Host "---------------------------------------------------"
