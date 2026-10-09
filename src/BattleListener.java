/** Eventos que la interfaz escucha para mostrar lo que ocurre en el duelo. */
public interface BattleListener {
    void onTurn(String playerCard, String aiCard, String winner);
    void onScoreChanged(int playerScore, int aiScore);
    void onDuelEnded(String winner);
}
