param(
    [Parameter(Mandatory = $true)][string]$RifTool,
    [string]$Apk,
    [string]$Jdk = $env:JAVA_HOME,
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
if (-not $Jdk) { $Jdk = Split-Path (Split-Path (Get-Command javac.exe -ErrorAction Stop).Source) }
$java = Join-Path $Jdk 'bin/java.exe'
$tool = (Resolve-Path -LiteralPath $RifTool).ProviderPath
if ($Apk -and -not $SkipBuild) { throw 'Use -SkipBuild with -Apk to wrap an existing signed APK.' }
# Resolve a caller-supplied relative path before changing directories.
if ($Apk) { $Apk = (Resolve-Path -LiteralPath $Apk).ProviderPath }
Push-Location $PSScriptRoot
try {
    if (-not $SkipBuild) { & ./build.ps1 -Mode Apk -Jdk $Jdk }
    if (-not $Apk) { $Apk = (Resolve-Path -LiteralPath 'dist/Recom3Capture-0.1.1-dev.apk').ProviderPath }
    New-Item -ItemType Directory -Force -Path build/rif-stage,dist | Out-Null
    $stage = (Resolve-Path -LiteralPath 'build/rif-stage').ProviderPath
    $output = Join-Path $stage 'output.rif'
    # This utility prints binary output. Keep its logs private to the ignored build directory.
    if (Test-Path -LiteralPath $output) { Remove-Item -LiteralPath $output }
    Push-Location $stage
    try {
        & $java -jar $tool $Apk *> tool.log
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $output)) { throw 'The RIF tool did not produce a package.' }
        Copy-Item -LiteralPath $output -Destination verify.apk -Force
        # The decoder modifies its input: decode only this disposable copy.
        & $java -jar $tool (Join-Path $stage 'verify.apk') d *> verify.log
        if ($LASTEXITCODE -ne 0) { throw 'RIF round-trip decoder failed.' }
        $expected = (Get-FileHash -LiteralPath $Apk -Algorithm SHA256).Hash
        $actual = (Get-FileHash -LiteralPath verify.apk -Algorithm SHA256).Hash
        if ($expected -ne $actual) { throw 'Decoded RIF differs from the signed APK.' }
    } finally { Pop-Location }
    # Validate the actual PNG header before publishing the catalog thumbnail locally.
    $thumbnail = Join-Path $PSScriptRoot 'assets/thumbnail.png'
    $png = [IO.File]::ReadAllBytes($thumbnail)
    $signature = [byte[]]@(137,80,78,71,13,10,26,10)
    if ($png.Length -lt 33) { throw 'Invalid thumbnail PNG.' }
    for ($i = 0; $i -lt 8; $i++) { if ($png[$i] -ne $signature[$i]) { throw 'Invalid thumbnail PNG signature.' } }
    if (($png[16..19] -join ',') -ne '0,0,0,64' -or ($png[20..23] -join ',') -ne '0,0,0,64') {
        throw 'The thumbnail must be exactly 64 x 64 pixels.'
    }
    Copy-Item -LiteralPath $output -Destination dist/Recom3Capture.rif -Force
    Copy-Item -LiteralPath $thumbnail -Destination dist/thumbnail.png -Force
    $items = @($Apk, (Join-Path $PSScriptRoot 'dist/Recom3Capture.rif'), (Join-Path $PSScriptRoot 'dist/thumbnail.png'))
    $lines = foreach ($item in $items) {
        $hash = (Get-FileHash -LiteralPath $item -Algorithm SHA256).Hash.ToLowerInvariant()
        "$hash  $([IO.Path]::GetFileName($item))"
    }
    [IO.File]::WriteAllLines((Join-Path $PSScriptRoot 'dist/SHA256SUMS.txt'), $lines, (New-Object Text.UTF8Encoding $false))
    Write-Output 'Verified RIF round-trip and 64 x 64 PNG. Prepared local artifacts only.'
    Get-Item -LiteralPath $items | Select-Object Name,Length
} finally { Pop-Location }
