param([Parameter(Mandatory=$true)][string]$Serial,[string]$Adb=(Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'))
$ErrorActionPreference='Stop'
if($Serial -notmatch '^[A-Za-z0-9._:-]+$'){throw 'Invalid serial.'}
$key=((& $Adb -s $Serial shell run-as com.recom3.capture /system/bin/cat files/helper.auth)-join '').Trim()
if($key -notmatch '^[a-f0-9]{32}$'){throw 'Private helper key unavailable.'}
$packagePath=((& $Adb -s $Serial shell pm path com.recom3.capture)-join '').Trim().Replace('package:','')
if($packagePath -notmatch '^/[A-Za-z0-9/._-]+$'){throw 'Invalid package path.'}
$bootstrapFile=Join-Path $PSScriptRoot '../.local/bootstrap.key'
& $Adb -s $Serial shell 'mkdir -p /data/local/tmp/recom3-capture; chmod 700 /data/local/tmp/recom3-capture'
[IO.File]::WriteAllText($bootstrapFile,$key+"`n",(New-Object Text.UTF8Encoding($false)))
try {
    & $Adb -s $Serial push $bootstrapFile /data/local/tmp/recom3-capture/bootstrap.key
    & $Adb -s $Serial shell "CLASSPATH=$packagePath app_process /system/bin com.recom3.capture.ScreenHelper --stop < /data/local/tmp/recom3-capture/bootstrap.key"
} finally {& $Adb -s $Serial shell rm /data/local/tmp/recom3-capture/bootstrap.key;if(Test-Path -LiteralPath $bootstrapFile){Remove-Item -LiteralPath $bootstrapFile -Force};$key=$null}