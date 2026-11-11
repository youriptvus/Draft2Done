$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$jar=Join-Path $projectRoot 'tools\java-test\json-20250517.jar'
$androidJar=Join-Path $projectRoot 'tools\android-sdk\platforms\android-36\android.jar'
if(!(Test-Path -LiteralPath $androidJar)){throw 'Android API 36 is required for native research request checks. Run scripts/setup-android.ps1.'}
$classpath="$jar;$androidJar"
$classes=Join-Path $projectRoot 'build\oauth-tests'
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $jar),$classes | Out-Null
if(!(Test-Path -LiteralPath $jar)){Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar' -OutFile $jar}
$javac='C:\Program Files\Java\jdk-27.0.1\bin\javac.exe'
$java='C:\Program Files\Java\jdk-27.0.1\bin\java.exe'
$sources=@(
 'android\src\io\draft2done\app\OAuthProtocol.java',
 'android\src\io\draft2done\app\LoopbackCallbackServer.java',
 'android\src\io\draft2done\app\SignInKeepAliveService.java',
 'android\src\io\draft2done\app\RequestRegistry.java',
 'android\src\io\draft2done\app\DnsRetry.java',
 'android\src\io\draft2done\app\ChatGPTRegistration.java',
 'android\src\io\draft2done\app\ChatGPTResearchStatus.java',
 'android\src\io\draft2done\app\AssistRequest.java',
 'android\src\io\draft2done\app\ChatGPTSessionState.java',
 'android\src\io\draft2done\app\ChatGPTConnection.java',
 'android\src\io\draft2done\app\AppVersion.java',
 'tests\OAuthProtocolTest.java',
 'tests\LoopbackCallbackServerTest.java',
 'tests\RequestRegistryTest.java',
 'tests\ChatGPTConnectionSupportTest.java',
 'tests\AssistRequestTest.java',
 'tests\ResearchRequestTest.java'
) | ForEach-Object { Join-Path $projectRoot $_ }
& $javac --release 8 -encoding UTF-8 -cp $classpath -d $classes @sources
if($LASTEXITCODE -ne 0){throw 'OAuth protocol test compilation failed.'}
& $java -cp "$classes;$jar" io.draft2done.app.OAuthProtocolTest
if($LASTEXITCODE -ne 0){throw 'OAuth protocol tests failed.'}
& $java -cp "$classes;$jar" io.draft2done.app.LoopbackCallbackServerTest
if($LASTEXITCODE -ne 0){throw 'Loopback callback TCP tests failed.'}
& $java -cp "$classes;$jar" io.draft2done.app.RequestRegistryTest
if($LASTEXITCODE -ne 0){throw 'Request lifecycle tests failed.'}
& $java -cp "$classes;$jar" io.draft2done.app.ChatGPTConnectionSupportTest
if($LASTEXITCODE -ne 0){throw 'ChatGPT DNS and registration tests failed.'}
& $java -cp "$classes;$jar" io.draft2done.app.AssistRequestTest
if($LASTEXITCODE -ne 0){throw 'Assist request and session tests failed.'}
& $java -cp "$classes;$classpath" io.draft2done.app.ResearchRequestTest
if($LASTEXITCODE -ne 0){throw 'Research request boundary tests failed.'}
