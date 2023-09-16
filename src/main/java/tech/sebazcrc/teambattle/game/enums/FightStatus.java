package tech.sebazcrc.teambattle.game.enums;

public enum FightStatus {
    IDLING("&aSeguro"), FIGHTING("&cEn lucha");

    private String name;

    FightStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
