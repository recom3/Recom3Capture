param([string]$Magick)
$ErrorActionPreference='Stop'
if(-not $Magick){$Magick=(Get-Command magick.exe -ErrorAction Stop).Source}
$root=Split-Path $PSScriptRoot
$source=Join-Path $root 'assets/capture.svg'
$output=Join-Path $root 'assets/thumbnail.png'
# Code-native SVG artwork; no captured photos or video frames are used.
& $Magick -background none -density 384 $source -resize 64x64 -strip ('PNG32:'+$output)
if($LASTEXITCODE -ne 0){throw 'Thumbnail rendering failed.'}
& $Magick identify -format 'Thumbnail: %wx%h, %[channels], %b' $output
if($LASTEXITCODE -ne 0){throw 'Thumbnail inspection failed.'}
' '
'Prepared a 64 x 64 RGBA catalog icon.'