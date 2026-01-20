$base = "http://localhost:8081/api"
$randomId = Get-Random -Minimum 1000 -Maximum 9999
$username = "citizen_user_$randomId"

Clear-Host
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "   PD-MEWS: PERSONAL DATA MISUSE EARLY-WARNING    " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "`nCore Principle: We match exposure events, we don't spy." -ForegroundColor Gray
Write-Host "---------------------------------------------------"

# 1. Mandatory Identifier
Write-Host "`n[STEP 1] Identity Anchor" -ForegroundColor White
Write-Host "Choose an identifier type to use as a correlation anchor:"
Write-Host "1. Email Address"
Write-Host "2. Phone Number"
$idChoice = Read-Host "Select (1-2)"

if ($idChoice -eq "2") {
    $idType = "PHONE"
    $identityValue = Read-Host "Enter Phone Number (e.g. +1234567890)"
} else {
    $idType = "EMAIL"
    $identityValue = Read-Host "Enter Email Address"
}

if ([string]::IsNullOrWhiteSpace($identityValue)) {
    $identityValue = "tushardhokane12@gmail.com"
    $idType = "EMAIL"
    Write-Host "No input. Proceeding with demo anchor: $identityValue" -ForegroundColor Gray
}

# Backend: Create User
Write-Host "`nRegistering secure session..." -ForegroundColor Gray
$userBody = @{ username = $username; password = "secure_password" } | ConvertTo-Json
$userResponse = Invoke-RestMethod -Uri "$base/identity/users" -Method Post -Body $userBody -ContentType "application/json"
$userId = $userResponse.id

# Backend: Bind Identifier
$idBody = @{ userId = $userId; type = $idType; identifier = $identityValue; label = "Primary Anchor" } | ConvertTo-Json
Invoke-RestMethod -Uri "$base/identity/identifiers" -Method Post -Body $idBody -ContentType "application/json" | Out-Null

# Helper: Date Validation
function Get-ValidDate {
    param([string]$prompt, [string]$default)
    $valid = $false
    $date = $null
    while (-not $valid) {
        $inputStr = Read-Host "$prompt (YYYY-MM-DD, e.g. 2024-03-01)"
        if ([string]::IsNullOrWhiteSpace($inputStr) -and $default) {
            return $default
        }
        if ($inputStr -match "^\d{4}-\d{2}-\d{2}$") {
            try {
                $date = [DateTime]::ParseExact($inputStr, "yyyy-MM-dd", $null)
                $valid = $true
                return $inputStr
            } catch {
                Write-Host "Invalid date value. Please try again." -ForegroundColor Red
            }
        } else {
            Write-Host "Invalid format. Use YYYY-MM-DD." -ForegroundColor Red
        }
    }
}

# 2. App Exposure Collection
Write-Host "`n[STEP 2] Exposure Timeline" -ForegroundColor White
Write-Host "Add apps or websites where you've shared your data (e.g., Adobe, Canva)."
$exposures = @()
do {
    $appName = Read-Host "App/Website Name (Enter to finish)"
    if (![string]::IsNullOrWhiteSpace($appName)) {
        $signupDate = Get-ValidDate -prompt "Approximate Signup Date" -default (Get-Date -Format "yyyy-MM-dd")
        
        Write-Host "Category (Optional: Shopping, Finance, Social, etc.)"
        $category = Read-Host "Enter Category"
        
        $expBody = @{ userId = $userId; appName = $appName; signupDate = $signupDate; category = $category.ToUpper() } | ConvertTo-Json
        Invoke-RestMethod -Uri "$base/exposures" -Method Post -Body $expBody -ContentType "application/json" | Out-Null
        Write-Host "Added $appName to timeline." -ForegroundColor Green
    }
} while (![string]::IsNullOrWhiteSpace($appName))

# 3. Optional Issue Reporting
Write-Host "`n[STEP 3] Observed Issue (Optional)" -ForegroundColor White
Write-Host "Do you have specific examples of misuse (spam, phishing)?" -ForegroundColor Gray
$reportIssue = Read-Host "Report an issue? (y/n)"
if ($reportIssue -eq "y") {
    Write-Host "`nSelect issue type:" -ForegroundColor Gray
    Write-Host "1. Spam Calls"
    Write-Host "2. Spam SMS"
    Write-Host "3. Spam Email"
    Write-Host "4. Phishing Attempt"
    $issueChoice = Read-Host "Selection (1-4)"
    $issueType = switch ($issueChoice) {
        "1" { "SPAM_CALL" }
        "2" { "SPAM_SMS" }
        "3" { "SPAM_EMAIL" }
        "4" { "PHISHING_ATTEMPT" }
        Default { "UNKNOWN" }
    }
    $issueDesc = Read-Host "Description (e.g. 'Daily insurance calls')"
    $issueDateStr = Get-ValidDate -prompt "Approximate start date" -default (Get-Date -Format "yyyy-MM-dd")
    $issueTimestamp = $issueDateStr + "T10:00:00"

    $eventBody = @{
        userId = $userId
        type = $issueType
        description = $issueDesc
        timestamp = $issueTimestamp
        severity = "MEDIUM"
    } | ConvertTo-Json
    
    Invoke-RestMethod -Uri "$base/events" -Method Post -Body $eventBody -ContentType "application/json" | Out-Null
    Write-Host "Signal recorded. Updating behavioral baseline..." -ForegroundColor Green | Out-Host
}

# 4. Trigger Analysis
Write-Host "`n[STEP 4] Correlation Engine" -ForegroundColor White | Out-Host
Write-Host "Synchronizing identity anchors and exposure events..." -ForegroundColor Gray | Out-Host
$checkBody = @{ userId = $userId; email = $identityValue } | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$base/risk/check-leak" -Method Post -Body $checkBody -ContentType "application/json" | Out-Null
    Write-Host "Analyzing timelines for statistical anomalies..." -ForegroundColor Gray | Out-Host
    Start-Sleep -Seconds 1
    
    Write-Host "`nPress [ENTER] to view the Correlation Report..." -ForegroundColor Cyan | Out-Host
    [void][System.Console]::ReadLine()
    
    $report = Invoke-RestMethod -Uri "$base/risk/analyze/$userId" -Method Get
    
    # CLEAR SCREEN FOR THE FINAL REPORT
    Clear-Host
    
    if ($report -and $report.Count -gt 0) {
         $sep = "=" * 60
         Write-Host $sep -ForegroundColor Cyan
         Write-Host "            PD-MEWS CORE RISK ASSESSMENT REPORT" -ForegroundColor Cyan
         Write-Host $sep -ForegroundColor Cyan
         
         foreach ($r in $report) {
            Write-Host "`n[ANALYSIS TARGET]: $($r.appExposure.appName.ToUpper())" -ForegroundColor Yellow
            Write-Host "CONFIDENCE LEVEL:  " -NoNewline
            
            if ($r.riskScore -ge 65.0) { Write-Host "$($r.riskScore)% (HIGH PROBABILITY)" -ForegroundColor Red }
            elseif ($r.riskScore -ge 40.0) { Write-Host "$($r.riskScore)% (MONITORING)" -ForegroundColor Magenta }
            else { Write-Host "$($r.riskScore)% (LOW/INCONCLUSIVE)" -ForegroundColor Gray }
            
            Write-Host ("-" * 60) -ForegroundColor DarkGray
            
            # Print reasoning line by line for better formatting
            $lines = $r.reasoning.Split("`n")
            foreach ($line in $lines) {
                if (![string]::IsNullOrWhiteSpace($line)) {
                    $cleanLine = $line.Replace("### Hypothesis:", "[HYPOTHESIS]:").Replace("**", "")
                    Write-Host "  $($cleanLine.Trim())" -ForegroundColor White
                }
            }
            Write-Host ("-" * 60) -ForegroundColor DarkGray
         }
    } else {
         Write-Host "`n" + ("=" * 60)
         Write-Host " [IDENTIFIED RISK]: NONE" -ForegroundColor Green
         Write-Host " No registered application explains the observed signals."
         Write-Host ("=" * 60)
    }
} catch {
    Write-Host "`nEngine Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n[DONE] Scan complete. Personal session secured.`n" -ForegroundColor Cyan
