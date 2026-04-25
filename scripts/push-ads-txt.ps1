# Push ads.txt + Datenschutzerklärung auf GitHub Pages
#
# Verwendung:
#   .\push-ads-txt.ps1 -RepoUrl "https://github.com/<user>/<repo>.git"
#
# Voraussetzungen:
#   * Git ist installiert (winget install Git.Git)
#   * GitHub-Auth ist eingerichtet (Personal Access Token oder GitHub Desktop login)
#   * Im Repo ist GitHub Pages auf den Ordner /docs (Branch main) konfiguriert.
#
# Ergebnis: docs/ads.txt liegt unter https://<user>.github.io/<repo>/ads.txt
#           (bzw. https://<user>.github.io/ads.txt wenn Repo-Name = <user>.github.io)

param(
    [Parameter(Mandatory = $true)]
    [string]$RepoUrl
)

$ErrorActionPreference = 'Stop'
$gitPath = "C:\Program Files\Git\bin\git.exe"
if (-not (Test-Path $gitPath)) { $gitPath = "git" }

$repoDir = Join-Path $PSScriptRoot ".."
Set-Location $repoDir

if (-not (Test-Path .git)) {
    & $gitPath init
    & $gitPath branch -M main
}

# Remote setzen / aktualisieren
$existing = & $gitPath remote 2>$null
if ($existing -notcontains 'origin') {
    & $gitPath remote add origin $RepoUrl
} else {
    & $gitPath remote set-url origin $RepoUrl
}

& $gitPath add docs/ads.txt docs/privacy.md docs/index.md
& $gitPath commit -m "chore(ads): publish ads.txt + privacy policy for AdMob" --allow-empty
& $gitPath push -u origin main

Write-Host ""
Write-Host "✅ Push abgeschlossen."
Write-Host "Aktiviere jetzt in GitHub: Settings → Pages → Source = 'Deploy from a branch' → Branch = main, Folder = /docs"
