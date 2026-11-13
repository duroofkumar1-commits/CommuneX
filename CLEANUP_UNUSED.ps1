param(
    [string]$ProjectRoot = (Get-Location).Path,
    [switch]$RemoveGeneratedFolders
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $ProjectRoot).Path

# Refuse to run from the wrong directory; this is a project-root-only cleanup.
$projectMarker = Join-Path $root 'frontend/src/pages/Games.jsx'
if (-not (Test-Path -LiteralPath $projectMarker -PathType Leaf)) {
    throw "ProjectRoot does not look like the CommuneX root: $root"
}

# These files have no import, route, HTML, CSS, or build-config references in the supplied ZIP.
$unusedFiles = @(
    'frontend/src/pages/LinkedInInspiredGames.jsx',
    'frontend/src/pages/logic-lab.css',
    'frontend/src/assets/vite.svg',
    'frontend/src/assets/hero.png',
    'frontend/src/assets/communex-logo.svg',
    'frontend/versol.json',
    'frontend/Dockerfile.bak'
)

# Empty placeholders only. Keep their parent folders if they contain real files.
$emptyPlaceholders = @(
    'frontend/src/utils/dsa/.gitkeep',
    'frontend/src/features/ai/.gitkeep',
    'frontend/src/features/community/.gitkeep',
    'frontend/src/features/editor/.gitkeep',
    'frontend/src/features/events/.gitkeep',
    'frontend/src/features/games/.gitkeep',
    'frontend/src/features/messaging/.gitkeep',
    'frontend/src/features/music/.gitkeep',
    'frontend/src/features/resources/.gitkeep'
)

foreach ($relativePath in ($unusedFiles + $emptyPlaceholders)) {
    $fullPath = Join-Path $root $relativePath
    if (Test-Path -LiteralPath $fullPath -PathType Leaf) {
        Remove-Item -LiteralPath $fullPath -Force
        Write-Host "Deleted: $relativePath"
    }
}

# Optional packaging cleanup. Dependencies can be restored with npm ci / Maven builds.
# The .git folder and local .env files are deliberately preserved.
if ($RemoveGeneratedFolders) {
    foreach ($relativePath in @('frontend/node_modules', 'frontend/dist', 'backend/target')) {
        $fullPath = Join-Path $root $relativePath
        if (Test-Path -LiteralPath $fullPath -PathType Container) {
            Remove-Item -LiteralPath $fullPath -Recurse -Force
            Write-Host "Removed generated folder: $relativePath"
        }
    }
}

# Remove only directories made empty by this cleanup; never remove source-bearing folders.
$cleanupParents = @('frontend/src/assets', 'frontend/src/features', 'frontend/src/utils/dsa')
foreach ($relativeParent in $cleanupParents) {
    $parentPath = Join-Path $root $relativeParent
    if (-not (Test-Path -LiteralPath $parentPath -PathType Container)) { continue }
    $directories = Get-ChildItem -LiteralPath $parentPath -Directory -Recurse -Force | Sort-Object FullName -Descending
    foreach ($directory in $directories) {
        if ((Get-ChildItem -LiteralPath $directory.FullName -Force | Measure-Object).Count -eq 0) {
            Remove-Item -LiteralPath $directory.FullName -Force
        }
    }
    if ((Get-ChildItem -LiteralPath $parentPath -Force | Measure-Object).Count -eq 0) {
        Remove-Item -LiteralPath $parentPath -Force
    }
}

Write-Host 'Cleanup completed. Local .env files and Git history were preserved.'
