<#
.SYNOPSIS
    Builds the FoldNav Android APK using an out-of-Drive Gradle build directory and copies the artifact to exports/.
#>
[CmdletBinding()]
param(
    [ValidateSet('assembleDebug', 'assembleRelease')]
    [string]$GradleTask = 'assembleRelease'
)

$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $ScriptDir) { $ScriptDir = $PSScriptRoot }
Set-Location -Path $ScriptDir

$RepoRoot = Split-Path -Parent $ScriptDir
$AndroidDir = Join-Path -Path $RepoRoot -ChildPath 'android-app'
$SdkDir = Join-Path -Path $env:LOCALAPPDATA -ChildPath 'Android\Sdk'

if (-not (Test-Path -Path $SdkDir)) {
    throw [System.IO.DirectoryNotFoundException]::new("Android SDK not found at $SdkDir")
}

$JavaHomePath = $null
$PortableJdk = Get-ChildItem -Path (Join-Path $env:LOCALAPPDATA 'Java') -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($null -ne $PortableJdk -and (Test-Path (Join-Path $PortableJdk.FullName 'bin\java.exe'))) {
    $JavaHomePath = $PortableJdk.FullName
}
if (-not $JavaHomePath -and $env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $JavaHomePath = $env:JAVA_HOME
}
if (-not $JavaHomePath) {
    $SysJdk = Get-ChildItem -Path 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -ne $SysJdk -and (Test-Path (Join-Path $SysJdk.FullName 'bin\java.exe'))) {
        $JavaHomePath = $SysJdk.FullName
    }
}
if (-not $JavaHomePath) {
    throw [System.InvalidOperationException]::new("JDK 17+ not found.")
}

$env:JAVA_HOME = $JavaHomePath
$env:ANDROID_HOME = $SdkDir
$env:ANDROID_SDK_ROOT = $SdkDir
$env:PATH = "$(Join-Path $JavaHomePath 'bin');$(Join-Path $SdkDir 'platform-tools');$env:PATH"

$LocalPropsPath = Join-Path -Path $AndroidDir -ChildPath 'local.properties'
$EscapedSdk = $SdkDir.Replace('\', '\\')
Set-Content -Path $LocalPropsPath -Value "sdk.dir=$EscapedSdk" -Encoding ASCII

$BuildRoot = Join-Path -Path $env:LOCALAPPDATA -ChildPath 'FoldNavGradleBuild'
$CacheDir = Join-Path -Path $BuildRoot -ChildPath 'gradle-cache'
New-Item -ItemType Directory -Path $BuildRoot -Force | Out-Null

Set-Location -Path $AndroidDir
& .\gradlew.bat --no-daemon --project-cache-dir $CacheDir $GradleTask "-PfoldnavBuildDir=$BuildRoot"
if ($LASTEXITCODE -ne 0) {
    throw [System.InvalidOperationException]::new("Gradle $GradleTask failed with exit code $LASTEXITCODE")
}

$VariantDir = if ($GradleTask -eq 'assembleRelease') { 'release' } else { 'debug' }
$ApkName = if ($GradleTask -eq 'assembleRelease') { 'app-release.apk' } else { 'app-debug.apk' }
$BuiltApk = Join-Path -Path $BuildRoot -ChildPath "app\outputs\apk\$VariantDir\$ApkName"
if (-not (Test-Path -Path $BuiltApk)) {
    throw [System.IO.FileNotFoundException]::new("Expected APK not found at $BuiltApk")
}

$ExportsDir = Join-Path -Path $RepoRoot -ChildPath 'exports'
New-Item -ItemType Directory -Path $ExportsDir -Force | Out-Null
$OutApkName = if ($GradleTask -eq 'assembleRelease') { 'FoldNav-ZFold6.apk' } else { 'FoldNav-ZFold6-debug.apk' }
$OutApk = Join-Path -Path $ExportsDir -ChildPath $OutApkName
Copy-Item -Path $BuiltApk -Destination $OutApk -Force

$Item = Get-Item -Path $OutApk
[PSCustomObject]@{
    Task      = $GradleTask
    BuiltApk  = $BuiltApk
    ExportApk = $Item.FullName
    SizeMB    = [Math]::Round($Item.Length / 1MB, 2)
}
