param(
    [ValidateSet('Apk')][string]$Mode = 'Apk',
    [string]$Sdk = $env:ANDROID_HOME,
    [string]$Jdk = $env:JAVA_HOME,
    [string]$BuildToolsVersion = '27.0.3',
    [string]$Keystore,
    [string]$KeyAlias
)
$ErrorActionPreference = 'Stop'
if (-not $Jdk) { $Jdk = Split-Path (Split-Path (Get-Command javac.exe -ErrorAction Stop).Source) }
if (-not $Sdk) { $Sdk = $env:ANDROID_SDK_ROOT }
$javac = Join-Path $Jdk 'bin/javac.exe'
function Invoke-Tool {
    param([string]$Tool, [string[]]$ToolArgs)
    & $Tool @ToolArgs
    if ($LASTEXITCODE -ne 0) { throw "$Tool failed with exit code $LASTEXITCODE" }
}
function Write-Args {
    param([string]$Path, [string[]]$Items)
    $quoted = $Items | ForEach-Object { '"' + $_.Replace('\', '/') + '"' }
    [IO.File]::WriteAllLines((Join-Path $PSScriptRoot $Path), $quoted, (New-Object Text.UTF8Encoding $false))
}
Push-Location $PSScriptRoot
try {
    # Clean only these fixed, resolved build directories inside this project.
    foreach ($relative in @('build/tests', 'build/classes', 'build/gen', 'build/dex')) {
        $target = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot $relative))
        $prefix = [IO.Path]::GetFullPath($PSScriptRoot).TrimEnd('\') + '\'
        if (-not $target.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid build path.' }
        if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
        New-Item -ItemType Directory -Force -Path $target | Out-Null
    }
    if ($Mode -ne 'Test') {
        if (-not $Sdk) { throw 'Set ANDROID_HOME or pass -Sdk. Requires platform android-16.' }
        $tools = Join-Path $Sdk "build-tools/$BuildToolsVersion"
        $androidJar = Join-Path $Sdk 'platforms/android-16/android.jar'
        if (-not (Test-Path -LiteralPath $androidJar)) { throw 'Install Android SDK platform 16.' }
        New-Item -ItemType Directory -Force -Path dist,.local | Out-Null
        Invoke-Tool "$tools/aapt.exe" @('package', '-f', '-m', '-J', 'build/gen',
            '-M', 'app/src/main/AndroidManifest.xml', '-S', 'app/src/main/res',
            '-I', $androidJar, '-F', 'build/resources.ap_')
        $sources = @(Get-ChildItem app/src/main/java,build/gen -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
        Write-Args 'build/app-sources.txt' $sources
        # Compiling against the API 16 bootclasspath rejects accidental newer APIs.
        Invoke-Tool $javac @('-source', '7', '-target', '7', '-encoding', 'UTF-8',
            '-bootclasspath', $androidJar, '-d', 'build/classes', '@build/app-sources.txt')
        Invoke-Tool "$tools/dx.bat" @('--dex', '--min-sdk-version=16', '--output=build/dex/classes.dex', 'build/classes')
        Copy-Item -LiteralPath build/resources.ap_ -Destination build/unsigned.apk -Force
        Push-Location build/dex
        try { Invoke-Tool (Join-Path $Jdk 'bin/jar.exe') @('uf', '../unsigned.apk', 'classes.dex') }
        finally { Pop-Location }
        Invoke-Tool "$tools/zipalign.exe" @('-f', '4', 'build/unsigned.apk', 'build/aligned.apk')
        if ($Keystore) {
            if (-not $KeyAlias) { throw 'Pass -KeyAlias with -Keystore.' }
            $signingKey = (Resolve-Path -LiteralPath $Keystore).ProviderPath
            $apk = 'dist/Recom3Capture-0.1.1.apk'
            # apksigner prompts for credentials; passwords are never stored in project files.
            Invoke-Tool "$tools/apksigner.bat" @('sign', '--ks', $signingKey, '--ks-key-alias', $KeyAlias,
                '--min-sdk-version', '16', '--v1-signing-enabled', 'true', '--v2-signing-enabled', 'false',
                '--out', $apk, 'build/aligned.apk')
        } else {
            $signingKey = '.local/development.keystore'
            if (-not (Test-Path -LiteralPath $signingKey)) {
                # Public development credentials, never a production signing identity.
                Invoke-Tool (Join-Path $Jdk 'bin/keytool.exe') @('-genkeypair', '-keystore', $signingKey,
                    '-alias', 'development', '-storepass', 'android', '-keypass', 'android',
                    '-dname', 'CN=Recom3 Development,O=Recom3,C=ES', '-keyalg', 'RSA',
                    '-keysize', '2048', '-validity', '10000', '-sigalg', 'SHA256withRSA')
            }
            $apk = 'dist/Recom3Capture-0.1.1-dev.apk'
            Invoke-Tool "$tools/apksigner.bat" @('sign', '--ks', $signingKey, '--ks-key-alias', 'development',
                '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--min-sdk-version', '16',
                '--v1-signing-enabled', 'true', '--v2-signing-enabled', 'false', '--out', $apk, 'build/aligned.apk')
        }
        Invoke-Tool "$tools/apksigner.bat" @('verify', '--verbose', '--min-sdk-version', '16', '--max-sdk-version', '16', $apk)
        Invoke-Tool "$tools/zipalign.exe" @('-c', '4', $apk)
        Invoke-Tool "$tools/aapt.exe" @('dump', 'badging', $apk)
        Get-FileHash -Algorithm SHA256 -LiteralPath $apk
    }
} finally { Pop-Location }
