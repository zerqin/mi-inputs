param(
    [string]$Jdk = "",
    [string]$Tools = "",
    [string]$PlatformJar = "",
    [string]$StorePass = "mibutton123"
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$App = Join-Path $Root "app"
$Out = Join-Path $Root "dist"
$Work = Join-Path $Root "build\work"
$Ks = Join-Path $Root "build\mibutton.keystore"
$ApkName = "Mi-Inputs-2.5.apk"

function First-Existing([string[]]$paths, [string]$probe) {
    foreach ($p in $paths) {
        if ($p -and (Test-Path (Join-Path $p $probe))) { return $p }
    }
    return $null
}

# --- JDK ---
$jdkCandidates = @($Jdk, $env:JAVA_HOME,
    (Join-Path $Root "build\tools\jdk"),
    "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
    "C:\Program Files\Android\Android Studio\jbr")
$JdkDir = First-Existing $jdkCandidates "bin\javac.exe"
if (-not $JdkDir) { throw "JDK not found. Pass -Jdk <jdk-dir> or set JAVA_HOME." }
$JdkDir = (Resolve-Path $JdkDir).Path

# --- Android build-tools ---
$btCandidates = @($Tools, (Join-Path $Root "build\tools\android-13"))
if ($env:ANDROID_HOME) {
    $bt = Get-ChildItem (Join-Path $env:ANDROID_HOME "build-tools") -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($bt) { $btCandidates += $bt.FullName }
}
$BtDir = First-Existing $btCandidates "aapt2.exe"
if (-not $BtDir) { throw "Android build-tools not found. Pass -Tools <build-tools-dir> or set ANDROID_HOME." }
$BtDir = (Resolve-Path $BtDir).Path

# --- android.jar (API 28) ---
$jarCandidates = @($PlatformJar, (Join-Path $Root "build\tools\android-9\android.jar"))
if ($env:ANDROID_HOME) {
    $jarCandidates += (Join-Path $env:ANDROID_HOME "platforms\android-28\android.jar")
}
$AndroidJar = $null
foreach ($p in $jarCandidates) {
    if ($p -and (Test-Path $p)) { $AndroidJar = (Resolve-Path $p).Path; break }
}
if (-not $AndroidJar) { throw "android.jar (API 28) not found. Pass -PlatformJar <path> or set ANDROID_HOME." }

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    throw "Python 3 not found in PATH (needed by build\inject.py)."
}

Write-Host "JDK:      $JdkDir"
Write-Host "Tools:    $BtDir"
Write-Host "android:  $AndroidJar"

$env:JAVA_HOME = $JdkDir
$env:PATH = "$JdkDir\bin;$BtDir;$env:PATH"

New-Item -ItemType Directory -Force -Path $Out | Out-Null
Remove-Item $Work -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory "$Work\classes", "$Work\dex" -Force | Out-Null

Write-Host "== aapt2 compile =="
& "$BtDir\aapt2.exe" compile --dir (Join-Path $App "res") -o "$Work\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

Write-Host "== aapt2 link =="
New-Item -ItemType Directory -Force -Path "$Work\gen" | Out-Null
& "$BtDir\aapt2.exe" link -o "$Work\base.apk" -I $AndroidJar `
    --manifest (Join-Path $App "AndroidManifest.xml") "$Work\res.zip" `
    --java "$Work\gen" --auto-add-overlay
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "== javac =="
$srcs = @((Get-ChildItem (Join-Path $App "src") -Recurse -Filter *.java).FullName)
$srcs += (Get-ChildItem "$Work\gen" -Recurse -Filter *.java).FullName
& "$JdkDir\bin\javac.exe" --release 8 -encoding UTF-8 -classpath $AndroidJar -d "$Work\classes" $srcs
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "== d8 =="
$cls = (Get-ChildItem "$Work\classes" -Recurse -Filter *.class).FullName
& "$JdkDir\bin\java.exe" -Xmx1024M -cp "$BtDir\lib\d8.jar" com.android.tools.r8.D8 `
    --min-api 23 --release --lib $AndroidJar --output "$Work\dex" $cls
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host "== inject dex =="
python (Join-Path $PSScriptRoot "inject.py") "$Work\base.apk" "$Work\dex\classes.dex" "$Work\withdex.apk"
if ($LASTEXITCODE -ne 0) { throw "dex inject failed" }

Write-Host "== zipalign =="
& "$BtDir\zipalign.exe" -f 4 "$Work\withdex.apk" "$Work\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

if (-not (Test-Path $Ks)) {
    Write-Host "== generate signing key =="
    & "$JdkDir\bin\keytool.exe" -genkeypair -keystore $Ks -alias mibutton `
        -keyalg RSA -keysize 2048 -validity 10000 `
        -storepass $StorePass -keypass $StorePass `
        -dname "CN=Mi Inputs, O=Mi Inputs, C=US"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed" }
    Write-Host "Created $Ks"
    Write-Host "Keep this file: future updates must be signed with the same key."
}

Write-Host "== apksigner =="
$signed = Join-Path $Out $ApkName
& "$JdkDir\bin\java.exe" -jar "$BtDir\lib\apksigner.jar" sign `
    --ks $Ks --ks-pass "pass:$StorePass" --key-pass "pass:$StorePass" `
    --min-sdk-version 23 --out $signed "$Work\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

& "$JdkDir\bin\java.exe" -jar "$BtDir\lib\apksigner.jar" verify --print-certs $signed
Write-Host "OK: $signed"
Get-Item $signed | Select-Object Name, Length
