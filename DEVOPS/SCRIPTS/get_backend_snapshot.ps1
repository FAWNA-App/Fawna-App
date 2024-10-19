# PowerShell script to get a snapshot of the backend
param (
    [Parameter(Mandatory=$true)]
    [string]$SnapshotName
)

# Set the repo path
$RepoPath = "C:\Users\Spencer\AndroidStudioProjects\Fawna"

# Function to find the root directory
function Find-RootDirectory {
    param ([string]$StartPath)
    $currentPath = Resolve-Path $StartPath
    while ($currentPath -ne $null -and $currentPath -ne "") {
        if (Test-Path "$currentPath\.git") {
            return $currentPath
        }
        $parentPath = Split-Path $currentPath -Parent
        if ($parentPath -eq $currentPath) {
            # We've reached the root of the drive
            break
        }
        $currentPath = $parentPath
    }
    throw "Root directory not found. Make sure you're providing a path within a Git repository."
}

# Find the root directory
try {
    $rootDir = Find-RootDirectory $RepoPath
    Write-Host "Root directory found: $rootDir"
} catch {
    Write-Host "Error: $_"
    exit 1
}

# Define paths
$outputDir = "$rootDir\DEVOPS\SCRIPTS\snapshot"
$backendDir = "$rootDir\backend"  # Assuming the backend folder is named 'backend'

# Ensure the output directory exists
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

# Generate the snapshot filename
$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$snapshotFile = "$outputDir\${SnapshotName}_backend_${timestamp}.snapshot.txt"

# File extensions to include in the search
$FileExtensions = @("kt", "java", "xml", "gradle", "properties", "pro", "py")

# Files and folders to ignore
$IgnoreFiles = @("local.properties")
$IgnoreFolders = @(".gradle", "build", "venv")  # Added "venv" to ignore Python virtual environment

# Function to generate ignore patterns
function Get-IgnorePatterns {
    param ([string]$BasePath)
    $GitignorePath = "$BasePath\.gitignore"
    $Patterns = @()
    if (Test-Path $GitignorePath) {
        Get-Content $GitignorePath | ForEach-Object {
            $line = $_.Trim()
            if ($line -and -not $line.StartsWith("#")) {
                $Patterns += [regex]::Escape($line.TrimEnd('/'))
            }
        }
    }
    return $Patterns
}

# Function to collect text from files
function Get-TextFromFiles {
    param (
        [string]$BasePath,
        [string[]]$Extensions,
        [string[]]$IgnorePatterns,
        [string[]]$IgnoreFiles,
        [string[]]$IgnoreFolders
    )
    $CollectedText = ""
    $FileCount = 0
    Write-Host "Collecting text from files in the backend directory..."
    $Files = Get-ChildItem -Path $BasePath -Recurse -File | Where-Object {
        $_.Extension -in ($Extensions | ForEach-Object { ".$_" }) -and
                ($IgnorePatterns.Count -eq 0 -or -not ($IgnorePatterns | Where-Object { $_.FullName -match $_ })) -and
                ($_.Name -notin $IgnoreFiles) -and
                ($_.FullName.Split([IO.Path]::DirectorySeparatorChar) | Where-Object { $_ -in $IgnoreFolders }).Count -eq 0
    }
    foreach ($File in $Files) {
        $RelativePath = $File.FullName.Substring($BasePath.Length + 1)
        Write-Host "Processing file: $RelativePath"
        $CollectedText += "`n--- Text from file: $RelativePath ---`n"
        $CollectedText += Get-Content $File.FullName -Raw
        $FileCount++
    }
    Write-Host "Text collection complete."
    Write-Host "Total files processed: $FileCount"
    return $CollectedText
}

Write-Host "Starting text collection for the backend..."
$CollectedText = ""
$IgnorePatterns = Get-IgnorePatterns $backendDir
$CollectedText = Get-TextFromFiles -BasePath $backendDir -Extensions $FileExtensions -IgnorePatterns $IgnorePatterns -IgnoreFiles $IgnoreFiles -IgnoreFolders $IgnoreFolders

# Write the collected text to the file
$CollectedText | Out-File $snapshotFile -Encoding utf8

Write-Host "`nBackend snapshot complete.`n"
Write-Host "Snapshot saved to: $snapshotFile"