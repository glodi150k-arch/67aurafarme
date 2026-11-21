# Painel Nexus

App externo para Free Fire — menu flutuante preto/vermelho neon com ESP e Aimbot via bridge HTTP local (127.0.0.1:7755).

## Features

**ESP** (tudo branco)
- Box ESP
- Health Bar (cor original)
- Name ESP
- Line ESP
- Skeleton ESP
- Distance ESP

**Aimbot**
- Head Lock
- Body Lock

**FOV Circle**
- Círculo branco sobre a tela
- Slider de tamanho (10–300)

**Misc**
- No Recoil
- No Spread
- Fast Switch

## Build

### GitHub Actions (automático)
Push na `main` → workflow gera Debug + Release APK nos Artifacts da run.

### Local
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Requisitos
- Android 8.0+ (API 26)
- Permissão SYSTEM_ALERT_WINDOW (sobreposição)
- Módulo (Assembly-CSharp-patch.bytes) instalado no Free Fire via ADB

## Arquitetura
```
MainActivity ──► FloatingService
                    ├── layout_floating_panel (menu arrastável)
                    ├── FovCircleOverlay (canvas MATCH_PARENT, não intercepta toque)
                    └── ModuleBridge ──► HTTP 127.0.0.1:7755 (módulo no jogo)
```
