param([string]$ModelsDirectory=(Join-Path $env:USERPROFILE '.ollama\models'))
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
try{$models=Invoke-RestMethod -Uri 'http://127.0.0.1:11435/api/tags' -TimeoutSec 2;Write-Host 'A local Ollama server already responds on port 11435.';exit 0}catch{}
$ollama=Get-Command ollama.exe -ErrorAction Stop
if(!(Test-Path -LiteralPath (Join-Path $ModelsDirectory 'manifests'))){throw 'Point -ModelsDirectory at an existing Ollama models directory. This script does not download models.'}
$logRoot=Join-Path $projectRoot 'data'
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null
$values=@{OLLAMA_HOST='127.0.0.1:11435';OLLAMA_MODELS=$ModelsDirectory;OLLAMA_NOPRUNE='1';OLLAMA_NO_CLOUD='1';OLLAMA_NUM_PARALLEL='1';OLLAMA_MAX_LOADED_MODELS='1';OLLAMA_CONTEXT_LENGTH='8192'}
$previous=@{}
try{
 foreach($name in $values.Keys){$previous[$name]=[Environment]::GetEnvironmentVariable($name,'Process');[Environment]::SetEnvironmentVariable($name,$values[$name],'Process')}
 $server=Start-Process -FilePath $ollama.Source -ArgumentList @('serve') -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logRoot 'ollama-out.log') -RedirectStandardError (Join-Path $logRoot 'ollama-error.log') -PassThru
 @{pid=$server.Id;started=$server.StartTime.ToUniversalTime().ToString('o');executable=$ollama.Source} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $logRoot 'ollama-process.json') -Encoding UTF8
 Write-Host 'Started a separate loopback-only Ollama instance on port 11435. Existing models are reused; pruning and cloud features are disabled.'
 Write-Host 'For USB phone access, run scripts\test-samsung.ps1 -UsbAI and use http://127.0.0.1:11435 in the app.'
}finally{foreach($name in $previous.Keys){[Environment]::SetEnvironmentVariable($name,$previous[$name],'Process')}}
