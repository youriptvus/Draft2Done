$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$jar=Join-Path $projectRoot 'tools\java-test\json-20250517.jar'
$classes=Join-Path $projectRoot 'build\oauth-tests'
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $jar),$classes | Out-Null
if(!(Test-Path -LiteralPath $jar)){Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar' -OutFile $jar}
$javac='C:\Program Files\Java\jdk-27.0.1\bin\javac.exe'
$java='C:\Program Files\Java\jdk-27.0.1\bin\java.exe'
$sources=@(
 'android\src\io\draft2done\app\OAuthProtocol.java',
 'android\src\io\draft2done\app\RequestRegistry.java',
 'android\src\io\draft2done\app\DnsRetry.java',
 'android\src\io\draft2done\app\ChatGPTRegistration.java',
 'android\src\io\draft2done\app\ChatGPTResearchStatus.java',
 'android\src\io\draft2done\app\AssistRequest.java',
 'android\src\io\draft2done\app\ChatGPTSessionState.java',
 'tests\OAuthProtocolTest.java',
 'tests\RequestRegistryTest.java',
 'tests\ChatGPTConnectionSupportTest.java',
 'tests\AssistRequestTest.java'
) | ForEach-Object { Join-Path $projectRoot $_ }
& $javac --release 8 -encoding UTF-8 -cp $jar -d $classes @sources
if($LASTEXITCODE -ne 0){throw 'OAuth protocol test compilation failed.'}
& $java -cp "$classes;$jar" io.draft2done.app.OAuthProtocolTest
if($LASTEXITCODE -ne 0){throw 'OAuth protocol tests failed.'}
& $java -cp "$classes;$jar" io.draft2done.app.RequestRegistryTest
if($LASTEXITCODE -ne 0){throw 'Request lifecycle tests failed.'}
& $java -cp "$classes;$jar" io.draft2done.app.ChatGPTConnectionSupportTest
if($LASTEXITCODE -ne 0){throw 'ChatGPT DNS and registration tests failed.'}
& $java -cp "$classes;$jar" io.draft2done.app.AssistRequestTest
if($LASTEXITCODE -ne 0){throw 'Assist request and session tests failed.'}
