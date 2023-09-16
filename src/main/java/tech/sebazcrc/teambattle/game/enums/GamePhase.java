package tech.sebazcrc.teambattle.game.enums;

public enum GamePhase {
    WAITING("Esperando"), PVE("PvE"), PVP("PvP");

    private String name;

    GamePhase(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
