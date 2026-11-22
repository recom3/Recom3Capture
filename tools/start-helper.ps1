param([Parameter(Mandatory=$true)][string]$Serial,[string]$Adb=(Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'))
$ErrorActionPreference='Stop'
if($Serial -notmatch '^[A-Za-z0-9._:-]+$'){throw 'Invalid serial.'}
& $Adb -s $Serial shell am start -W -n com.recom3.capture/.CaptureActivity
if($LASTEXITCODE -ne 0){throw 'Cannot start capture app.'}
$socketPresent=& $Adb -s $Serial shell cat /proc/net/unix
if($socketPresent -match '@recom3.capture.screen.v1'){Write-Output 'Display helper is already running.';return}
$key=((& $Adb -s $Serial shell run-as com.recom3.capture /system/bin/cat files/helper.auth) -join '').Trim()
if($key -notmatch '^[a-f0-9]{32}$'){throw 'Private helper bootstrap key unavailable. Start the glasses app first.'}
$packagePath=((& $Adb -s $Serial shell pm path com.recom3.capture) -join '').Trim().Replace('package:','')
if($packagePath -notmatch '^/[A-Za-z0-9/._-]+$'){throw 'Invalid package path.'}
& $Adb -s $Serial shell 'mkdir -p /data/local/tmp/recom3-capture; chmod 700 /data/local/tmp/recom3-capture'
$bootstrapFile=Join-Path $PSScriptRoot '../.local/bootstrap.key'
[IO.File]::WriteAllText($bootstrapFile,$key+"`n",(New-Object Text.UTF8Encoding($false)))
& $Adb -s $Serial push $bootstrapFile /data/local/tmp/recom3-capture/bootstrap.key
if($LASTEXITCODE -ne 0){throw 'Cannot provision helper key.'}
try {
    & $Adb -s $Serial shell "trap '' HUP; CLASSPATH=$packagePath app_process /system/bin com.recom3.capture.ScreenHelper < /data/local/tmp/recom3-capture/bootstrap.key > /data/local/tmp/recom3-capture-helper.log 2>&1 &"
    Start-Sleep -Milliseconds 2500
    $socketPresent=& $Adb -s $Serial shell cat /proc/net/unix
    if(-not ($socketPresent -match '@recom3.capture.screen.v1')){throw 'Display helper did not start; inspect its sanitized local log.'}
    Write-Output 'Display helper running. Disconnecting USB does not stop it; reboot requires starting it again.'
} finally {& $Adb -s $Serial shell rm /data/local/tmp/recom3-capture/bootstrap.key;if(Test-Path -LiteralPath $bootstrapFile){Remove-Item -LiteralPath $bootstrapFile -Force};$key=$null}