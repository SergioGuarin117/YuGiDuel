import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Reglas del duelo: una carta usada sale de la mano y gana quien llegue a dos rondas. */
public class Duel {
    public enum Position { ATTACK, DEFENSE }

    private final List<Card> playerHand;
    private final List<Card> aiHand;
    private final BattleListener listener;
    private final Random random = new Random();
    private int playerScore;
    private int aiScore;
    private boolean playerStarts;

    public Duel(List<Card> playerCards, List<Card> aiCards, BattleListener listener) {
        if (playerCards.size() != 3 || aiCards.size() != 3) {
            throw new IllegalArgumentException("Cada jugador debe tener exactamente 3 cartas.");
        }
        this.playerHand = new ArrayList<>(playerCards);
        this.aiHand = new ArrayList<>(aiCards);
        this.listener = listener;
        this.playerStarts = random.nextBoolean();
    }

    public boolean isPlayerTurn() { return playerStarts; }
    public List<Card> getPlayerHand() { return Collections.unmodifiableList(playerHand); }
    public List<Card> getAiHand() { return Collections.unmodifiableList(aiHand); }
    public int getPlayerScore() { return playerScore; }
    public int getAiScore() { return aiScore; }

    public void play(Card playerCard, Position playerPosition) {
        if (!playerHand.remove(playerCard)) throw new IllegalArgumentException("Esa carta ya fue usada.");
        Card aiCard = aiHand.remove(random.nextInt(aiHand.size()));
        Position aiPosition = random.nextBoolean() ? Position.ATTACK : Position.DEFENSE;
        finishRound(playerCard, playerPosition, aiCard, aiPosition);
    }

    private void finishRound(Card playerCard, Position playerPosition, Card aiCard, Position aiPosition) {
        int playerValue = playerPosition == Position.ATTACK ? playerCard.getAtk() : playerCard.getDef();
        int aiValue = aiPosition == Position.ATTACK ? aiCard.getAtk() : aiCard.getDef();
        String result;
        if (playerValue > aiValue) {
            playerScore++;
            result = "Jugador";
        } else if (aiValue > playerValue) {
            aiScore++;
            result = "Máquina";
        } else {
            result = "Empate";
        }

        listener.onTurn(playerCard + " [" + positionName(playerPosition) + "]",
                aiCard + " [" + positionName(aiPosition) + "]", result);
        listener.onScoreChanged(playerScore, aiScore);

        if (playerScore == 2 || aiScore == 2 || playerHand.isEmpty() || aiHand.isEmpty()) {
            listener.onDuelEnded(playerScore > aiScore ? "Jugador" : aiScore > playerScore ? "Máquina" : "Empate");
        } else {
            playerStarts = !playerStarts;
        }
    }

    private String positionName(Position position) {
        return position == Position.ATTACK ? "Ataque" : "Defensa";
    }
}
