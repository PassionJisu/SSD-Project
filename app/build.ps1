<#
.SYNOPSIS
  Builds Enough Procrastination with the plain JDK (no Maven/Gradle needed).

.EXAMPLE
  .\build.ps1            # compile + jar  -> out\enough-procrastination.jar
  .\build.ps1 -Run       # build, then start the app with your own data
  .\build.ps1 -Demo      # build, then start with sample data
  .\build.ps1 -Docs      # build + generate the Javadoc API manual -> ..\docs\api
  .\build.ps1 -Screenshot # build, write beta screenshots to ..\docs\screenshots
#>
param(
    [switch]$Run,
    [switch]$Demo,
    [switch]$Docs,
    [switch]$Screenshot
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$src = Join-Path $root 'src\main\java'
$out = Join-Path $root 'out'
$classes = Join-Path $out 'classes'
$jar = Join-Path $out 'enough-procrastination.jar'

function Resolve-JdkTool([string]$name) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $javaHome = $env:JAVA_HOME
    if (-not $javaHome) {
        $props = (cmd /c "java -XshowSettings:properties -version 2>&1") -join "`n"
        if ($props -match 'java\.home = (.+)') { $javaHome = $Matches[1].Trim() }
    }
    $candidate = Join-Path $javaHome "bin\$name.exe"
    if (Test-Path $candidate) { return $candidate }
    throw "Cannot find $name. Install a JDK 17+ or set JAVA_HOME."
}

$javac = Resolve-JdkTool 'javac'
$jarTool = Resolve-JdkTool 'jar'

if (Test-Path $classes) { Remove-Item $classes -Recurse -Force }
New-Item -ItemType Directory -Path $classes -Force | Out-Null

$sources = Get-ChildItem -Path $src -Filter *.java -Recurse | ForEach-Object { $_.FullName }
$listFile = Join-Path $out 'sources.txt'
$sources | Set-Content -Path $listFile -Encoding ASCII

Write-Host "Compiling $($sources.Count) files..."
& $javac -encoding UTF-8 -Xlint:all,-serial -d $classes "@$listFile"
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "Packaging $jar"
& $jarTool --create --file $jar --main-class ep.Main -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

if ($Docs) {
    $javadoc = Resolve-JdkTool 'javadoc'
    $docDir = Join-Path (Split-Path $root -Parent) 'docs\api'
    if (Test-Path $docDir) { Remove-Item $docDir -Recurse -Force }
    Write-Host "Generating Javadoc -> $docDir"
    & $javadoc -encoding UTF-8 -charset UTF-8 -quiet -Xdoclint:none `
        -d $docDir `
        -windowtitle 'Enough Procrastination API' `
        -doctitle 'Enough Procrastination &mdash; API Manual (beta)' `
        -header '<b>Enough Procrastination</b> beta' `
        -sourcepath $src -subpackages ep
    if ($LASTEXITCODE -ne 0) { throw "javadoc failed" }
}

if ($Screenshot) {
    $shotDir = Join-Path (Split-Path $root -Parent) 'docs\screenshots'
    & java -jar $jar --demo --screenshot $shotDir
}

if ($Run) { & java -jar $jar }
if ($Demo) { & java -jar $jar --demo }
