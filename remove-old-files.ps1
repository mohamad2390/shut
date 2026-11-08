# Run from the project root (the folder that contains pom.xml). Removes the Mini App and MinIO code.
$paths = @(
    "frontend",
    "src\main\java\com\shut\miniapp",
    "src\test\java\com\shut\miniapp",
    "src\main\java\com\shut\common\storage"
)
foreach ($p in $paths) {
    if (Test-Path $p) { Remove-Item -Recurse -Force $p; Write-Host "removed $p" }
}
