# Generador de sprites de mascota con Nano Banana (Gemini image models).
# Uso:
#   . .\gen.ps1
#   Invoke-Mascot -Prompt "..." -OutFile "out.png" [-InImage "ref.webp"] [-Model "gemini-3.1-flash-image"]
#
# Lee la API key de $env:GEMINI_API_KEY.

function Invoke-Mascot {
    param(
        [Parameter(Mandatory=$true)][string]$Prompt,
        [Parameter(Mandatory=$true)][string]$OutFile,
        [string[]]$InImage = @(),
        [string]$Model = "gemini-3.1-flash-image"
    )

    if (-not $env:GEMINI_API_KEY) { throw "Falta `$env:GEMINI_API_KEY" }

    $parts = @( @{ text = $Prompt } )

    foreach ($img in $InImage) {
        $bytes = [System.IO.File]::ReadAllBytes((Resolve-Path $img))
        $b64 = [System.Convert]::ToBase64String($bytes)
        $ext = [System.IO.Path]::GetExtension($img).TrimStart('.').ToLower()
        $mime = switch ($ext) {
            "png"  { "image/png" }
            "jpg"  { "image/jpeg" }
            "jpeg" { "image/jpeg" }
            "webp" { "image/webp" }
            default { "image/png" }
        }
        $parts += @{ inline_data = @{ mime_type = $mime; data = $b64 } }
    }

    $body = @{
        contents = @( @{ parts = $parts } )
        generationConfig = @{ responseModalities = @("IMAGE") }
    } | ConvertTo-Json -Depth 12 -Compress

    $uri = "https://generativelanguage.googleapis.com/v1beta/models/$Model`:generateContent?key=$($env:GEMINI_API_KEY)"

    $resp = Invoke-RestMethod -Method Post -Uri $uri -ContentType "application/json" -Body $body -ErrorAction Stop

    $imgPart = $resp.candidates[0].content.parts | Where-Object { $_.inlineData } | Select-Object -First 1
    if (-not $imgPart) {
        $txt = ($resp.candidates[0].content.parts | Where-Object { $_.text } | ForEach-Object { $_.text }) -join " "
        throw "Sin imagen en la respuesta. Texto: $txt"
    }
    $outBytes = [System.Convert]::FromBase64String($imgPart.inlineData.data)
    [System.IO.File]::WriteAllBytes((Join-Path (Get-Location) $OutFile), $outBytes)
    Write-Output "OK -> $OutFile ($($outBytes.Length) bytes)"
}
