<#
.SYNOPSIS
    Script de build multi-arquitetura (ARM64) para a TV Box TX9.
.DESCRIPTION
    Compila o backend e o frontend visando a arquitetura linux/arm64.
    Pode enviar as imagens para um Registry (Docker Hub) ou salvá-las como arquivos .tar.gz para transferir via SCP/pendrive.
.PARAMETER Registry
    Nome de usuário ou endereço do Registry (ex: seudockerhub). Se omitido, salva as imagens localmente em arquivos .tar.gz.
.EXAMPLE
    .\build-homelab.ps1 -Registry "meuusuario"
.EXAMPLE
    .\build-homelab.ps1
#>
param(
    [string]$Registry = ""
)

$ErrorActionPreference = "Stop"

Write-Host "================================================" -ForegroundColor Cyan
Write-Host " DaFabi Frangos - Build para Homelab (ARM64)   " -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

# Verifica se o Docker está em execução
try {
    docker version > $null
} catch {
    Write-Error "Docker não está em execução. Inicie o Docker Desktop antes de continuar."
    exit 1
}

# Garante o builder do buildx
Write-Host "`n[1/4] Verificando Docker Buildx..." -ForegroundColor Yellow
$builderName = "arm64-builder"
$builders = docker buildx ls
if ($builders -notmatch $builderName) {
    Write-Host "Criando builder multi-plataforma ($builderName)..."
    docker buildx create --name $builderName --use
} else {
    docker buildx use $builderName
}

$backendTag = if ($Registry) { "$Registry/dafabi-backend:latest" } else { "dafabi-backend:homelab" }
$frontendTag = if ($Registry) { "$Registry/dafabi-frontend:latest" } else { "dafabi-frontend:homelab" }

if ($Registry) {
    Write-Host "`n[2/4] Compilando e enviando Backend para o Registry ($backendTag)..." -ForegroundColor Yellow
    docker buildx build --platform linux/arm64 -t $backendTag ./back --push

    Write-Host "`n[3/4] Compilando e enviando Frontend para o Registry ($frontendTag)..." -ForegroundColor Yellow
    docker buildx build --platform linux/arm64 -t $frontendTag ./front --push

    Write-Host "`n[4/4] Concluído com sucesso!" -ForegroundColor Green
    Write-Host "Na sua TV Box, configure as variáveis no arquivo .env:"
    Write-Host "BACKEND_IMAGE=$backendTag"
    Write-Host "FRONTEND_IMAGE=$frontendTag"
    Write-Host "E execute: docker compose -f docker-compose.homelab.yml pull && docker compose -f docker-compose.homelab.yml up -d"
} else {
    Write-Host "`n[2/4] Compilando Backend para ARM64..." -ForegroundColor Yellow
    docker buildx build --platform linux/arm64 -t $backendTag ./back --load

    Write-Host "`n[3/4] Compilando Frontend para ARM64..." -ForegroundColor Yellow
    docker buildx build --platform linux/arm64 -t $frontendTag ./front --load

    Write-Host "`n[4/4] Exportando imagens para arquivos compactados..." -ForegroundColor Yellow
    $distDir = "./dist-homelab"
    if (!(Test-Path $distDir)) { New-Item -ItemType Directory -Path $distDir | Out-Null }

    Write-Host "Salvando $distDir/backend-arm64.tar..."
    docker save $backendTag -o "$distDir/backend-arm64.tar"

    Write-Host "Salvando $distDir/frontend-arm64.tar..."
    docker save $frontendTag -o "$distDir/frontend-arm64.tar"

    Write-Host "`nArquivos prontos em $distDir/!" -ForegroundColor Green
    Write-Host "Transfira a pasta $distDir e o docker-compose.homelab.yml para a TV Box e execute na TV Box:"
    Write-Host "  docker load -i backend-arm64.tar"
    Write-Host "  docker load -i frontend-arm64.tar"
    Write-Host "  docker compose -f docker-compose.homelab.yml up -d"
}
