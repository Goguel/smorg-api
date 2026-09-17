[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$Token = $env:GITHUB_TOKEN
)

if ([string]::IsNullOrWhiteSpace($Token)) {
    Write-Host "=================================================================" -ForegroundColor Yellow
    Write-Host "ATENCAO: Token do GitHub nao fornecido." -ForegroundColor Yellow
    Write-Host "Para executar a atualizacao automatica, forneca seu token do GitHub:" -ForegroundColor Yellow
    Write-Host ".\scripts\sync_github_issues.ps1 -Token <SEU_GITHUB_TOKEN>" -ForegroundColor Cyan
    Write-Host "=================================================================" -ForegroundColor Yellow
    $Token = Read-Host "Insira seu Personal Access Token do GitHub"
    if ([string]::IsNullOrWhiteSpace($Token)) {
        Write-Warning "Operacao cancelada."
        exit 0
    }
}

$RepoOwner = "Goguel"
$RepoName = "smorg-api"
$BaseApi = "https://api.github.com/repos/$RepoOwner/$RepoName"

$Headers = @{
    "Authorization"        = "Bearer $Token"
    "Accept"               = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
    "User-Agent"           = "SmOrg-Automation-Script"
}

Write-Host "`n[START] Iniciando sincronizacao no repositorio $RepoOwner/$RepoName...`n" -ForegroundColor Cyan

# 1. Configurar Labels
$Labels = @(
    @{ name = "prio:P1"; color = "B60205"; description = "Alta prioridade (essencial para o MVP)" },
    @{ name = "prio:P2"; color = "D93F0B"; description = "Media prioridade (auditoria e gestao)" },
    @{ name = "prio:P3"; color = "FBCA04"; description = "Baixa prioridade (otimizacao e cache)" },
    @{ name = "sprint:sprint-1"; color = "0E8A16"; description = "Tarefas da Sprint 1" },
    @{ name = "sprint:sprint-2"; color = "1D76DB"; description = "Tarefas da Sprint 2" },
    @{ name = "sprint:sprint-3"; color = "5319E7"; description = "Tarefas da Sprint 3" },
    @{ name = "type:user-story"; color = "0075CA"; description = "Historia de Usuario (User Story)" },
    @{ name = "type:tech-task"; color = "A2EEEF"; description = "Tarefa tecnica de infraestrutura ou CI/CD" },
    @{ name = "component:api-ktor"; color = "F9D0C4"; description = "Servico principal Kotlin/Ktor" },
    @{ name = "component:svc-go"; color = "C2E0C6"; description = "Microsservico de regras Go" },
    @{ name = "component:grpc"; color = "D4C5F9"; description = "Protocol Buffers e gRPC" },
    @{ name = "component:database"; color = "FEF2C0"; description = "PostgreSQL e migracoes Flyway" },
    @{ name = "component:cache"; color = "BFDADC"; description = "Camada de cache e metricas" }
)

Write-Host "[LABELS] Criando/Atualizando labels padronizadas..." -ForegroundColor Yellow
foreach ($lbl in $Labels) {
    $bodyJson = @{
        name        = $lbl.name
        color       = $lbl.color
        description = $lbl.description
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$BaseApi/labels" -Method Post -Headers $Headers -Body $bodyJson -ContentType "application/json; charset=utf-8" -ErrorAction Stop | Out-Null
        Write-Host "  [+] Label criada: $($lbl.name)" -ForegroundColor Green
    } catch {
        try {
            $encodedName = [System.Uri]::EscapeDataString($lbl.name)
            Invoke-RestMethod -Uri "$BaseApi/labels/$encodedName" -Method Patch -Headers $Headers -Body $bodyJson -ContentType "application/json; charset=utf-8" -ErrorAction Stop | Out-Null
            Write-Host "  [~] Label atualizada: $($lbl.name)" -ForegroundColor Gray
        } catch {
            Write-Host "  [*] Label existente: $($lbl.name)" -ForegroundColor DarkGray
        }
    }
}

# 2. Configurar Milestones
$Milestones = @(
    @{ title = "Sprint 1 - MVP Basico e Arquitetura Base"; description = "CRUD de entidades relacionadas, migracoes Flyway, Problem Details RFC 7807, Testcontainers e ArchUnit." },
    @{ title = "Sprint 2 - Microsservico Go e gRPC"; description = "Motor de regras em Go, gRPC sincrono, Buf lint/breaking, stubs sincronizados e arch-go." },
    @{ title = "Sprint 3 - Cache, Banco Neon e Deploy"; description = "Cache com politica declarada, metricas hit/miss, deploy em nuvem, health checks e logs." },
    @{ title = "Sprint Final - Seguranca OWASP e Apresentacao"; description = "Auth anti-BOLA, seguranca OWASP API Top 10, documentacao e entrega." }
)

Write-Host "`n[MILESTONES] Criando milestones..." -ForegroundColor Yellow
$MilestoneMap = @{}
try {
    $existingMilestones = Invoke-RestMethod -Uri "$BaseApi/milestones" -Method Get -Headers $Headers -ErrorAction SilentlyContinue
    foreach ($m in $existingMilestones) {
        $MilestoneMap[$m.title] = $m.number
    }
} catch {
    $null = $null
}

foreach ($ms in $Milestones) {
    if (-not $MilestoneMap.ContainsKey($ms.title)) {
        $bodyJson = @{
            title       = $ms.title
            description = $ms.description
        } | ConvertTo-Json
        try {
            $created = Invoke-RestMethod -Uri "$BaseApi/milestones" -Method Post -Headers $Headers -Body $bodyJson -ContentType "application/json; charset=utf-8" -ErrorAction Stop
            $MilestoneMap[$ms.title] = $created.number
            Write-Host "  [+] Milestone criada: $($ms.title) (#$($created.number))" -ForegroundColor Green
        } catch {
            Write-Warning "  [!] Falha ao criar milestone $($ms.title): $_"
        }
    } else {
        Write-Host "  [*] Milestone ja existe: $($ms.title)" -ForegroundColor Gray
    }
}

# 3. Atualizar Issues
$IssueDefs = @(
    @{
        number    = 1
        title     = "[US01] CRUD de equipamentos com identificacao por Tag NFC"
        file      = "docs/backlog/issue-1-crud-equipamentos.md"
        labels    = @("type:user-story", "prio:P1", "sprint:sprint-1", "component:api-ktor", "component:database")
        milestone = "Sprint 1 - MVP Basico e Arquitetura Base"
    },
    @{
        number    = 2
        title     = "[US02] Registrar retirada de equipamento via leitura NFC"
        file      = "docs/backlog/issue-2-registrar-retirada.md"
        labels    = @("type:user-story", "prio:P1", "sprint:sprint-1", "component:api-ktor", "component:database")
        milestone = "Sprint 1 - MVP Basico e Arquitetura Base"
    },
    @{
        number    = 3
        title     = "[US03] Comunicacao gRPC sincrona com microsservico Go para calculo de prazos"
        file      = "docs/backlog/issue-3-comunicar-com-go.md"
        labels    = @("type:user-story", "prio:P1", "sprint:sprint-2", "component:api-ktor", "component:svc-go", "component:grpc")
        milestone = "Sprint 2 - Microsservico Go e gRPC"
    },
    @{
        number    = 4
        title     = "[US04] Registrar devolucao de equipamento via leitura NFC"
        file      = "docs/backlog/issue-4-registrar-devolucao.md"
        labels    = @("type:user-story", "prio:P1", "sprint:sprint-1", "component:api-ktor", "component:database")
        milestone = "Sprint 1 - MVP Basico e Arquitetura Base"
    },
    @{
        number    = 5
        title     = "[US05] Consultar inventario de equipamentos e auditoria de emprestimos"
        file      = "docs/backlog/issue-5-consultar-inventario.md"
        labels    = @("type:user-story", "prio:P2", "sprint:sprint-1", "component:api-ktor", "component:database")
        milestone = "Sprint 1 - MVP Basico e Arquitetura Base"
    },
    @{
        number    = 6
        title     = "[US06] Armazenamento em cache de equipamentos frequentes e metricas hit/miss"
        file      = "docs/backlog/issue-6-cache-itens-requisitados.md"
        labels    = @("type:user-story", "prio:P3", "sprint:sprint-3", "component:api-ktor", "component:cache")
        milestone = "Sprint 3 - Cache, Banco Neon e Deploy"
    }
)

Write-Host "`n[ISSUES] Atualizando corpos, titulos, labels e milestones das Issues 1 a 6..." -ForegroundColor Yellow
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = Get-Location
}

foreach ($iss in $IssueDefs) {
    $filePath = Join-Path $ProjectRoot $iss.file
    if (-not (Test-Path $filePath)) {
        Write-Warning "Arquivo $($iss.file) nao encontrado. Pulando issue #$($iss.number)."
        continue
    }

    $content = [System.IO.File]::ReadAllText($filePath, [System.Text.Encoding]::UTF8)

    $payload = @{
        title  = $iss.title
        body   = $content
        labels = $iss.labels
    }

    if ($MilestoneMap.ContainsKey($iss.milestone)) {
        $payload["milestone"] = $MilestoneMap[$iss.milestone]
    }

    $bodyJson = $payload | ConvertTo-Json -Depth 5

    try {
        $response = Invoke-RestMethod -Uri "$BaseApi/issues/$($iss.number)" -Method Patch -Headers $Headers -Body $bodyJson -ContentType "application/json; charset=utf-8" -ErrorAction Stop
        Write-Host "  [OK] Issue #$($iss.number) atualizada com sucesso: $($iss.title)" -ForegroundColor Green
    } catch {
        Write-Error "  [FALHA] Nao foi possivel atualizar Issue #$($iss.number): $_"
    }
}

Write-Host "`n[SUCCESS] Sincronizacao concluida com sucesso! Acesse https://github.com/$RepoOwner/$RepoName/issues" -ForegroundColor Cyan
