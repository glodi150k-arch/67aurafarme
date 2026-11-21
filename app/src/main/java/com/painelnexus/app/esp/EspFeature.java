package com.painelnexus.app.esp;

/**
 * Enum de todas as features ESP/Misc com seus ACK strings
 * Mapeados direto do protocolo HTTP 7755 do módulo original
 */
public enum EspFeature {
    BOX("BOX", "BOX_ACK"),
    HEALTH("HEALTH", "HEALTH_ACK"),
    NAME("NAME", "NAME_ACK"),
    LINE("LINE", "LINE_ACK"),
    SKELETON("SKELETON", "SKELETON_ACK"),
    DISTANCE("DISTANCE", "DISTANCE_ACK"),
    AIM_HEAD("HEAD", "HEAD_ACK"),
    AIM_BODY("BODY", "BODY_ACK"),
    FOV_CIRCLE("IXYZFOV3", "FOV_ACK"),
    FOV_HIDE("FOV_HIDE", "FOV_HIDE_ACK"),
    FOV_VALUE("FOV_VALUE", "FOV_VALUE_ACK"),
    NO_RECOIL("NO_RECOIL", "NO_RECOIL_ACK"),
    NO_SPREAD("NO_SPREAD", "NO_SPREAD_ACK"),
    FAST_SWITCH("FAST", "FAST_SWITCH_ACK"),
    SMALL_BUTTON("SMALL_BUTTON", null),
    HIDE_ICON("HIDE", null);

    public final String cmd;
    public final String ack;

    EspFeature(String cmd, String ack) {
        this.cmd = cmd;
        this.ack = ack;
    }
}
