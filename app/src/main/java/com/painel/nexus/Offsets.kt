package com.painel.nexus

// offsets do Free Fire. todo valor 0 = pendente (// FILL), o painel nao le nada enquanto houver 0.
// regen: Cheat Engine -> pointer scan a partir da posicao do proprio player (x,y,z floats),
// depois confirma em outra partida. le o dump do libil2cpp com Il2CppDumper/Cpp2IL quando o jogo atualizar.
object Off {
    const val LIB_NAME = "libil2cpp.so"   // FILL: modulo que contem o game manager
    const val PLAYER_LIST = 0x0           // FILL: ponteiro pra lista de players
    const val PLAYER_COUNT = 0x0          // FILL: int com a quantidade de players
    const val PLAYER_POS = 0x0            // FILL: offset dos floats x,y,z dentro do player
    const val LOCAL_PLAYER = 0x0          // FILL: ponteiro do proprio player
    const val VIEW_MATRIX = 0x0           // FILL: 16 floats da matriz view*proj
    const val MAX_PLAYERS = 64

    fun ready(): Boolean =
        PLAYER_LIST != 0 && PLAYER_COUNT != 0 && PLAYER_POS != 0 && LOCAL_PLAYER != 0 && VIEW_MATRIX != 0
}
