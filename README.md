# Painel Nexus

app android (debug) com:
- tela inicial "Bem-vindos ao Painel" com banner
- botão **Conectar VPN local (Free Fire)**: sobe o túnel local e o loop de leitura de memória
- botão **Abrir flutuante**: bolha com o logo, abre o menu vermelho com ESP Linha, ESP Caixa, ESP Distância, ESP Thumber e uma função de exemplo

## build
o workflow `.github/workflows/build-apk.yml` gera o apk debug a cada push.
o artefato sai em Actions > run > `painel-nexus-debug` (app-debug.apk).

## o que falta antes de funcionar
1. **root**: `/proc/<pid>/mem` só abre com root no aparelho.
2. **offsets**: preencher `app/src/main/java/com/painel/nexus/Offsets.kt` (todos marcados `// FILL`).
   - Cheat Engine: achar a posição do próprio player (3 floats), pointer scan, confirmar em outra partida.
   - ou dump do libil2cpp (Il2CppDumper / Cpp2IL) pra pegar as classes do player.
   - todo update do jogo muda esses valores.
3. **pacote**: `com.dts.freefireth`. se for Free Fire Max, troca em `MemReader.PKG`.

## aviso
Free Fire tem anti-cheat. ler memória e mostrar posição de inimigo é cheat, e o risco é ban na conta.
