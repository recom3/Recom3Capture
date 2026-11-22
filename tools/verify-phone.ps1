param([Parameter(Mandatory=$true)][string]$GlassesSerial,[Parameter(Mandatory=$true)][string]$PhoneSerial,[Parameter(Mandatory=$true)][string]$HostAddress,[ValidateSet('screen','camera')][string]$Source='screen',[string]$Adb=(Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'))
$ErrorActionPreference='Stop'
foreach($serial in @($GlassesSerial,$PhoneSerial)){if($serial -notmatch '^[A-Za-z0-9._:-]+$'){throw 'Invalid serial.'}}
if($HostAddress -notmatch '^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$'){throw 'Pass the glasses IPv4 address.'}
$setup=((& $Adb -s $GlassesSerial shell run-as com.recom3.capture /system/bin/cat files/session.json)-join '')|ConvertFrom-Json
$config=@{host=$HostAddress;code=[string]$setup.code}|ConvertTo-Json -Compress
try {
    $config | & $Adb -s $PhoneSerial shell -T 'run-as com.recom3.snow3 /system/bin/sh -c "cat > files/capture-test.json"'
    if($LASTEXITCODE -ne 0){throw 'Cannot provision private development test configuration.'}
    $output=& $Adb -s $PhoneSerial shell am instrument -w -e source $Source com.recom3.snow3/com.recom3.capturetests.CaptureInstrumentation
    $output
    if(-not ($output -match 'INSTRUMENTATION_RESULT: result=PASS')){throw 'Device acceptance did not report PASS.'}
    if($LASTEXITCODE -ne 0){throw 'Instrumentation failed.'}
} finally {& $Adb -s $PhoneSerial shell run-as com.recom3.snow3 /system/bin/rm files/capture-test.json;$config=$null}