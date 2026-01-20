# PD-MEWS Testing Guide

Follow these steps exactly to verify the **Risk Engine** and **Legal Module**.

## 1. Start the Server
1.  Open your terminal in `t:\DataSecure\pd-mews`.
2.  Run the command:
    ```powershell
    mvn spring-boot:run
    ```
3.  **WAIT** until you see the log message:
    `Seeding Demo Data... User ID: xxxxxxxx-...`
4.  **COPY** that User ID (UUID). You will need it.

---

## 2. Verify Risk Engine (High Risk Detection)
We will check if "Lucky Casino Free" is correctly flagged as High Risk.

**Option A: PowerShell (Recommended)**
Open a **new** terminal window and run:
```powershell
# Replace <UUID> with the ID you copied
.\test_demo.ps1 <UUID>
```

**Option B: Browser**
Visit this link (replace `<UUID>` with your ID):
`http://localhost:8081/api/risk/analyze/<UUID>`

**Expectation:**
-   You should see `"riskLevel": "HIGH"`
-   Reasoning: `"Misuse event (SPAM_LATEST) occurred within 2 days..."`

---

## 3. Generate Legal Evidence (The "Weapon")
Now we will generate the **Digital Affidavit**.

**Option A: Browser Link (Easiest)**
We added a special link for you to do this in the Chrome browser.
Visit:
`http://localhost:8081/api/legal/preserve-demo/<RISK_ASSESSMENT_ID>`

*Note: You get the `<RISK_ASSESSMENT_ID>` from the output of Step 2. It is the `id` field of the Risk Analysis result (NOT the User ID).*

**Option B: PowerShell Script**
This script automatically finds the Risk ID and generates the evidence for you.
```powershell
# Replace <UUID> with the User ID
.\test_legal.ps1 <UUID>
```

**Expectation:**
-   You will see a **"NOTICE OF DATA MISUSE"**.
-   You will see a **SHA-256 Hash**.
-   This proves the data is cryptographically frozen.

---

## Troubleshooting
-   **"Address already in use"**: This means a server is already running. Run `taskkill /F /IM java.exe` to stop them all, then try again.
-   **404 Not Found**: You are likely using an old Risk ID or the server hasn't been restarted with the latest code. Restart the server.
