package tech.sebazcrc.teambattle.game.player;

import tech.sebazcrc.teambattle.game.GamePlayer;

public class KillAssist {
    private GamePlayer assist;
    private int reaming;

    public KillAssist(GamePlayer assist) {
        this.assist = assist;
        this.reaming = 15;
    }

    public boolean tick() {
        if (reaming <= 0) {
            return true;
        }
        reaming--;
        return false;
    }

    public GamePlayer getAssist() {
        return assist;
    }

    public int getReaming() {
        return reaming;
    }

    public void setReaming(int reaming) {
        this.reaming = reaming;
    }
}
