$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$jar=Join-Path $projectRoot 'tools\java-test\json-20250517.jar'
$classes=Join-Path $projectRoot 'build\update-tests'
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $jar),$classes | Out-Null
if(!(Test-Path -LiteralPath $jar)){Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar' -OutFile $jar}
$javac='C:\Program Files\Java\jdk-27.0.1\bin\javac.exe'
$java='C:\Program Files\Java\jdk-27.0.1\bin\java.exe'
& $javac --release 8 -encoding UTF-8 -cp $jar -d $classes (Join-Path $projectRoot 'android\src\io\draft2done\app\UpdateValidation.java') (Join-Path $projectRoot 'tests\UpdateValidationTest.java')
if($LASTEXITCODE -ne 0){throw 'Update validation test compilation failed.'}
& $java -cp "$classes;$jar" io.draft2done.app.UpdateValidationTest
if($LASTEXITCODE -ne 0){throw 'Update validation tests failed.'}
