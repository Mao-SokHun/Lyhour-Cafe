$base = if ($env:BASE_URL) { $env:BASE_URL.TrimEnd('/') } else { "http://localhost:8080" }
$results = @()
$token = $null

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method = "GET",
        [string]$Path,
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [int[]]$ExpectStatus = @(200),
        [scriptblock]$Assert = $null
    )
    $uri = "$base$Path"
    try {
        $params = @{
            Uri             = $uri
            Method          = $Method
            Headers         = $Headers
            UseBasicParsing = $true
            TimeoutSec      = 30
        }
        if ($Body) {
            $params["Body"] = $Body
            $params["ContentType"] = "application/json"
        }
        $r = Invoke-WebRequest @params
        $ok = $ExpectStatus -contains $r.StatusCode
        $extra = ""
        if ($Assert) {
            $extra = & $Assert $r
            if ($extra -ne $true -and $extra -ne $null) { $ok = $false }
        }
        $script:results += [pscustomobject]@{
            Feature = $Name
            Status  = if ($ok) { "PASS" } else { "FAIL" }
            Code    = $r.StatusCode
            Note    = if ($extra -and $extra -ne $true) { $extra } else { "" }
        }
    }
    catch {
        $code = $null
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        $ok = $code -and ($ExpectStatus -contains $code)
        $script:results += [pscustomobject]@{
            Feature = $Name
            Status  = if ($ok) { "PASS" } else { "FAIL" }
            Code    = $code
            Note    = $_.Exception.Message
        }
    }
}

Write-Host "Smoke test base: $base`n"

# Public pages
Test-Endpoint "Home page" -Path "/home"
Test-Endpoint "Sign in page" -Path "/signin"
Test-Endpoint "Cart page" -Path "/cart"
Test-Endpoint "Reservations" -Path "/reservations"
Test-Endpoint "PWA manifest" -Path "/manifest.json"
Test-Endpoint "Mobile config API" -Path "/api/v1/mobile/config"
Test-Endpoint "Products API" -Path "/api/v1/products" -Assert { param($r) ($r.Content | ConvertFrom-Json).Count -gt 0 }
Test-Endpoint "Branches API" -Path "/api/v1/branches"

# QR menu (table seeded as T1-MAIN or similar)
Test-Endpoint "QR table menu" -Path "/qr/T1-MAIN" -ExpectStatus @(200, 404)

# Auth API
$loginBody = '{"username":"walkin","password":"walkin123"}'
Test-Endpoint "JWT login" -Method POST -Path "/api/v1/auth/login" -Body $loginBody -Assert {
    param($r)
    $j = $r.Content | ConvertFrom-Json
    if (-not $j.token) { return "no token" }
    $script:token = $j.token
    $true
}

if ($token) {
    $auth = @{ Authorization = "Bearer $token" }
    Test-Endpoint "Orders API (JWT)" -Path "/api/v1/orders" -Headers $auth
    Test-Endpoint "Mobile home (JWT)" -Path "/api/v1/mobile/home" -Headers $auth -Assert {
        param($r) ($r.Content | ConvertFrom-Json).menu.Count -gt 0
    }
    Test-Endpoint "Profile API (JWT)" -Path "/api/v1/me" -Headers $auth
}

# Admin login (session) - check redirect to dashboard after POST
try {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $signBody = @{ username = "admin"; password = "admin123" }
    Invoke-WebRequest -Uri "$base/signin" -Method POST -Body $signBody -WebSession $session -UseBasicParsing -MaximumRedirection 0 -ErrorAction SilentlyContinue | Out-Null
    $dash = Invoke-WebRequest -Uri "$base/dashboard" -WebSession $session -UseBasicParsing
    $adminOk = $dash.StatusCode -eq 200 -and $dash.Content -match "Dashboard|Sales"
    $results += [pscustomobject]@{ Feature = "Admin session login"; Status = if ($adminOk) { "PASS" } else { "FAIL" }; Code = $dash.StatusCode; Note = "" }
}
catch {
    $results += [pscustomobject]@{ Feature = "Admin session login"; Status = "FAIL"; Code = ""; Note = $_.Exception.Message }
}

# Admin pages (with session)
if ($session) {
    foreach ($p in @(
            @{ N = "Dashboard"; U = "/dashboard" },
            @{ N = "POS"; U = "/admin/pos" },
            @{ N = "Kitchen KDS"; U = "/admin/kitchen" },
            @{ N = "Tables"; U = "/admin/tables" },
            @{ N = "Reports"; U = "/admin/reports" },
            @{ N = "Coupons"; U = "/admin/coupons" },
            @{ N = "Ingredients"; U = "/admin/ingredients" },
            @{ N = "Suppliers"; U = "/admin/suppliers" },
            @{ N = "Promotions"; U = "/admin/promotions" },
            @{ N = "Employees"; U = "/admin/employees" },
            @{ N = "Accounting"; U = "/admin/accounting" },
            @{ N = "Branches"; U = "/admin/branches" },
            @{ N = "Audit logs"; U = "/admin/audit-logs" },
            @{ N = "Settings"; U = "/admin/settings" },
            @{ N = "Table layout"; U = "/admin/table-layout" }
        )) {
        try {
            $pg = Invoke-WebRequest -Uri "$base$($p.U)" -WebSession $session -UseBasicParsing
            $results += [pscustomobject]@{ Feature = $p.N; Status = if ($pg.StatusCode -eq 200) { "PASS" } else { "FAIL" }; Code = $pg.StatusCode; Note = "" }
        }
        catch {
            $results += [pscustomobject]@{ Feature = $p.N; Status = "FAIL"; Code = ""; Note = $_.Exception.Message }
        }
    }
}

# Guest QR checkout (table order)
try {
    $products = Invoke-RestMethod -Uri "$base/api/v1/products" -Method GET
    if ($products.Count -gt 0) {
        $pid = $products[0].id
        $checkout = @{
            items          = @(@{ productId = $pid; quantity = 1; size = "Medium"; price = $products[0].price })
            paymentMethod  = "PAY_AT_PICKUP"
            fulfillmentType = "DINE_IN"
            tableId        = 1
            guestName      = "SmokeTest"
        } | ConvertTo-Json -Depth 5
        $co = Invoke-WebRequest -Uri "$base/order/checkout" -Method POST -Body $checkout -ContentType "application/json" -UseBasicParsing
        $redirect = ($co.Content | ConvertFrom-Json).redirectUrl
        $results += [pscustomobject]@{
            Feature = "QR guest checkout"
            Status  = if ($co.StatusCode -eq 200 -and $redirect) { "PASS" } else { "FAIL" }
            Code    = $co.StatusCode
            Note    = $redirect
        }
    }
}
catch {
    $results += [pscustomobject]@{ Feature = "QR guest checkout"; Status = "FAIL"; Code = ""; Note = $_.Exception.Message }
}

# Customer pages
if ($session) {
    try {
        $custSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        $cb = @{ username = "walkin"; password = "walkin123" }
        Invoke-WebRequest -Uri "$base/signin" -Method POST -Body $cb -WebSession $custSession -UseBasicParsing -MaximumRedirection 5 | Out-Null
        $prof = Invoke-WebRequest -Uri "$base/customer/profile" -WebSession $custSession -UseBasicParsing
        $results += [pscustomobject]@{ Feature = "Customer profile"; Status = if ($prof.StatusCode -eq 200) { "PASS" } else { "FAIL" }; Code = $prof.StatusCode; Note = "" }
        $ords = Invoke-WebRequest -Uri "$base/customer/orders" -WebSession $custSession -UseBasicParsing
        $results += [pscustomobject]@{ Feature = "Customer orders"; Status = if ($ords.StatusCode -eq 200) { "PASS" } else { "FAIL" }; Code = $ords.StatusCode; Note = "" }
    }
    catch {
        $results += [pscustomobject]@{ Feature = "Customer pages"; Status = "FAIL"; Code = ""; Note = $_.Exception.Message }
    }
}

Write-Host "`n=== SMOKE TEST RESULTS ===`n"
$results | Format-Table -AutoSize
$pass = ($results | Where-Object Status -eq "PASS").Count
$fail = ($results | Where-Object Status -eq "FAIL").Count
Write-Host "TOTAL: $pass passed, $fail failed / $($results.Count) checks"
if ($fail -gt 0) { exit 1 }
