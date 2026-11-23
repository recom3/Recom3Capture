param([string]$Magick)
$ErrorActionPreference='Stop'
if(-not $Magick){$Magick=(Get-Command magick.exe -ErrorAction Stop).Source}
$root=Split-Path $PSScriptRoot
$source=Join-Path $root 'assets/capture.svg'
$densities=@{'ldpi'=36;'mdpi'=48;'hdpi'=72;'xhdpi'=96}
foreach($density in $densities.Keys){
    $folder=Join-Path $root ('app/src/main/res/drawable-'+$density)
    New-Item -ItemType Directory -Force -Path $folder | Out-Null
    $output=Join-Path $folder 'ic_launcher.png'
    & $Magick -background none -density 384 $source -resize ($densities[$density].ToString()+'x'+$densities[$density]) -strip ('PNG32:'+$output)
    if($LASTEXITCODE -ne 0){throw ('Launcher icon rendering failed: '+$density)}
    & $Magick identify -format '%f: %wx%h, %[channels]\n' $output
    if($LASTEXITCODE -ne 0){throw 'Launcher icon inspection failed.'}
}
