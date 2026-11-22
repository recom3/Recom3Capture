param([string]$Jdk=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(-not $Jdk){$Jdk=Split-Path (Split-Path (Get-Command javac.exe -ErrorAction Stop).Source)}
$classes=Join-Path $PSScriptRoot 'build/test-classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
& (Join-Path $Jdk 'bin/javac.exe') -source 7 -target 7 -d $classes (Join-Path $PSScriptRoot 'app/src/main/java/com/recom3/capture/HttpWire.java') (Join-Path $PSScriptRoot 'tests/com/recom3/capture/HttpWireTest.java')
if($LASTEXITCODE -ne 0){throw 'Host test compilation failed.'}
& (Join-Path $Jdk 'bin/java.exe') -cp $classes com.recom3.capture.HttpWireTest
if($LASTEXITCODE -ne 0){throw 'Host tests failed.'}