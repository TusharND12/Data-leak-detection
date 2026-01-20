$base = "http://localhost:8081/api"

$randomId = Get-Random -Minimum 1000 -Maximum 9999
$username = "correlation_test_$randomId"
Write-Host "--- PD-MEWS Correlation Engine Test ---" -ForegroundColor Cyan

# 1. Create User
Write-Host "1. Creating Test User ($username)..." -ForegroundColor Cyan
$userBody = @{
    username = $username
    password = "test_password"
} | ConvertTo-Json
$userResponse = Invoke-RestMethod -Uri "$base/identity/users" -Method Post -Body $userBody -ContentType "application/json"
$userId = $userResponse.id
Write-Host "User Created ID: $userId" -ForegroundColor Green

# 2. Add App Sources
Write-Host "2. Adding App Sources (Exposure Timeline)..." -ForegroundColor Cyan

# App 1: SafeBank (Finance) - Signed up 10 days ago
$signupDate1 = (Get-Date).AddDays(-10).ToString("yyyy-MM-dd")
$app1Body = @{
    userId = $userId
    appName = "SafeBank"
    signupDate = $signupDate1
    category = "FINANCE"
} | ConvertTo-Json
Invoke-RestMethod -Uri "$base/exposures" -Method Post -Body $app1Body -ContentType "application/json" | Out-Null
Write-Host "- Added 'SafeBank' (FINANCE), Signup: $signupDate1" -ForegroundColor Gray

# App 2: ShadyGames (Social/Entertainment) - Signed up 2 days ago
$signupDate2 = (Get-Date).AddDays(-2).ToString("yyyy-MM-dd")
$app2Body = @{
    userId = $userId
    appName = "ShadyGames"
    signupDate = $signupDate2
    category = "SOCIAL"
} | ConvertTo-Json
Invoke-RestMethod -Uri "$base/exposures" -Method Post -Body $app2Body -ContentType "application/json" | Out-Null
Write-Host "- Added 'ShadyGames' (SOCIAL), Signup: $signupDate2" -ForegroundColor Gray

# 3. Record Misuse Events
Write-Host "3. Recording Misuse Events..." -ForegroundColor Cyan

# Event 1: Phishing SMS - 1 day ago (Correlates well with ShadyGames)
$event1Body = @{
    userId = $userId
    type = "PHISHING_ATTEMPT"
    timestamp = (Get-Date).AddDays(-1).ToString("yyyy-MM-ddTHH:mm:ss")
    description = "Received suspicious SMS with a link to verify ShadyGames account."
    severity = "HIGH"
    metadata = "sender: +123456789, domain: shady-verify.com"
} | ConvertTo-Json
Invoke-RestMethod -Uri "$base/events" -Method Post -Body $event1Body -ContentType "application/json" | Out-Null
Write-Host "- Recorded PHISHING_ATTEMPT (1 day after ShadyGames signup)" -ForegroundColor Gray

# Event 2: Spam Call - Today (Multiple events = higher confidence)
$event2Body = @{
    userId = $userId
    type = "SPAM_CALL"
    timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
    description = "Insurance sales call."
    severity = "MEDIUM"
} | ConvertTo-Json
Invoke-RestMethod -Uri "$base/events" -Method Post -Body $event2Body -ContentType "application/json" | Out-Null
Write-Host "- Recorded SPAM_CALL (Today)" -ForegroundColor Gray

# 4. Trigger Risk Analysis
Write-Host "`n4. Running Probabilistic Risk Correlation Engine..." -ForegroundColor Cyan
$report = Invoke-RestMethod -Uri "$base/risk/analyze/$userId" -Method Get

if ($report) {
    Write-Host "`n--- RISK CORRELATION REPORT ---" -ForegroundColor White
    foreach ($r in $report) {
        $color = "White"
        if ($r.riskLevel -eq "CRITICAL") { $color = "Red" }
        elseif ($r.riskLevel -eq "HIGH") { $color = "Magenta" }
        elseif ($r.riskLevel -eq "MEDIUM") { $color = "Yellow" }
        
        Write-Host "APP: $($r.appExposure.appName)" -ForegroundColor $color -NoNewline
        Write-Host " (Likelihood: $($r.riskScore)%)" -ForegroundColor White
        Write-Host "Reasoning:" -ForegroundColor Gray
        Write-Host $r.reasoning -ForegroundColor Gray
        Write-Host "---------------------------------------------------" -ForegroundColor White
    }
} else {
    Write-Host "No report generated. Check backend logs." -ForegroundColor Red
}
