import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Conecta los componentes de YgoApiClient.form con la API y las reglas del duelo. */
public class YgoApiClient implements BattleListener {
    private static final String API_URL = "https://db.ygoprodeck.com/api/v7/randomcard.php";

    // Estos campos están enlazados con los nombres de binding de YgoApiClient.form.
    private JPanel panel1;
    private JLabel cartaMaquina1;
    private JLabel imagenCartaPlayer1;
    private JLabel cartaMaquina2;
    private JLabel imagenCartaPlayer2;
    private JLabel imagenCartaPlayer3;
    private JLabel cartaImagen3;
    private JLabel lblEstadisticas1;
    private JLabel lblEstadisticas2;
    private JLabel lblEstadisticas3;
    private JRadioButton seleccionarCarta1;
    private JRadioButton seleccionarCarta2;
    private JRadioButton seleccionarCarta3;
    private JButton btnSeleccionarCarta;
    private JButton btnDuelo;
    private JLabel lblMaquina;
    private JTextArea textArea1;
    private JLabel lblTu;
    private JLabel lblMarcadorMaquina;
    private JLabel lblMarcadorPlayer;
    private JRadioButton rbAtaqueCarta1;
    private JRadioButton rbAtaqueCarta2;
    private JRadioButton rbAtaqueCarta3;
    private JRadioButton rbDefensaCarta3;
    private JRadioButton rbDefensaCarta2;
    private JRadioButton rbDefensaCarta1;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(6);
    private final JFrame frame = new JFrame("Yu-Gi-Oh! Duel Lite");
    private final List<JLabel> playerImages = new ArrayList<>();
    private final List<JLabel> playerStats = new ArrayList<>();
    private final List<JRadioButton> playerSelectors = new ArrayList<>();
    private final List<JRadioButton> attackSelectors = new ArrayList<>();
    private final List<JRadioButton> defenseSelectors = new ArrayList<>();
    private final List<JLabel> machineImages = new ArrayList<>();
    private final List<Card> playerCards = new ArrayList<>();
    private final List<Card> machineCards = new ArrayList<>();
    private Duel duel;
    private int loadNumber;

    public YgoApiClient() {
        // IntelliJ crea el contenido de panel1 desde el formulario vinculado.
        playerImages.addAll(Arrays.asList(imagenCartaPlayer1, imagenCartaPlayer2, imagenCartaPlayer3));
        playerStats.addAll(Arrays.asList(lblEstadisticas1, lblEstadisticas2, lblEstadisticas3));
        playerSelectors.addAll(Arrays.asList(seleccionarCarta1, seleccionarCarta2, seleccionarCarta3));
        attackSelectors.addAll(Arrays.asList(rbAtaqueCarta1, rbAtaqueCarta2, rbAtaqueCarta3));
        defenseSelectors.addAll(Arrays.asList(rbDefensaCarta1, rbDefensaCarta2, rbDefensaCarta3));
        machineImages.addAll(Arrays.asList(cartaMaquina1, cartaMaquina2, cartaImagen3));
        configureForm();
    }

    public void show() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(panel1);
        frame.setMinimumSize(new Dimension(900, 560));
        frame.setSize(1200, 760);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void configureForm() {
        ButtonGroup selection = new ButtonGroup();
        for (JRadioButton selector : playerSelectors) {
            selection.add(selector);
            selector.setEnabled(false);
        }
        for (int i = 0; i < 3; i++) {
            ButtonGroup position = new ButtonGroup();
            position.add(attackSelectors.get(i));
            position.add(defenseSelectors.get(i));
            attackSelectors.get(i).setSelected(true);
            attackSelectors.get(i).setEnabled(false);
            defenseSelectors.get(i).setEnabled(false);
        }
        for (JLabel label : playerImages) {
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setText("Esperando duelo");
        }
        for (JLabel label : machineImages) {
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setHorizontalTextPosition(SwingConstants.CENTER);
            label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setText("Carta aleatoria");
        }
        textArea1.setEditable(false);
        textArea1.setLineWrap(true);
        textArea1.setWrapStyleWord(true);
        textArea1.setBorder(new EmptyBorder(8, 8, 8, 8));
        lblMaquina.setText("Máquina");
        lblTu.setText("Tú -");
        btnSeleccionarCarta.setEnabled(false);
        btnSeleccionarCarta.addActionListener(event -> playSelectedCard());
        btnDuelo.setText("Duelo");
        btnDuelo.addActionListener(event -> startDuel());
        appendLog("Presiona Duelo para cargar tres cartas aleatorias por jugador.");
    }

    /** Carga seis cartas en segundo plano y solo habilita el juego cuando todas llegan. */
    private void startDuel() {
        btnDuelo.setEnabled(false);
        btnSeleccionarCarta.setEnabled(false);
        playerSelectors.forEach(selector -> selector.setEnabled(false));
        attackSelectors.forEach(selector -> selector.setEnabled(false));
        defenseSelectors.forEach(selector -> selector.setEnabled(false));
        int thisLoad = ++loadNumber;
        appendLog("\n¡¡HORA DEL DUELO!!");

        loadSixCardsOneAtATime().whenComplete((allCards, error) -> SwingUtilities.invokeLater(() -> {
                    if (thisLoad != loadNumber) return;
                    if (error != null) {
                        appendLog("No se pudieron cargar las cartas. Detalle: " + describeError(error));
                        btnDuelo.setEnabled(true);
                        return;
                    }
                    playerCards.clear();
                    machineCards.clear();
                    playerCards.addAll(allCards.subList(0, 3));
                    machineCards.addAll(allCards.subList(3, 6));
                    duel = new Duel(playerCards, machineCards, this);
                    showCards(thisLoad);
                    lblMarcadorPlayer.setText("0");
                    lblMarcadorMaquina.setText("0");
                    playerSelectors.forEach(selector -> selector.setEnabled(true));
                    for (int i = 0; i < 3; i++) {
                        attackSelectors.get(i).setSelected(true);
                        attackSelectors.get(i).setEnabled(true);
                        defenseSelectors.get(i).setEnabled(true);
                    }
                    seleccionarCarta1.setSelected(true);
                    btnSeleccionarCarta.setEnabled(true);
                    btnDuelo.setEnabled(true);
                    appendLog("Cartas listas. Selecciona la posicion de tus cartas y pulsa Seleccionar.");
                    appendLog(duel.isPlayerTurn() ? "La iniciativa inicial es del jugador." : "La iniciativa inicial es de la máquina.");
                }));
    }

    /** Hace las seis consultas en secuencia para evitar ráfagas contra la API. */
    private CompletableFuture<List<Card>> loadSixCardsOneAtATime() {
        CompletableFuture<List<Card>> result = CompletableFuture.completedFuture(new ArrayList<>());
        for (int i = 0; i < 6; i++) {
            result = result.thenCompose(cards -> fetchMonster(0).thenApply(card -> {
                cards.add(card);
                return cards;
            }));
        }
        return result;
    }

    private String describeError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        String message = cause.getMessage();
        return cause.getClass().getSimpleName() + (message == null || message.isEmpty() ? "" : ": " + message);
    }

    private void showCards(int thisLoad) {
        for (int i = 0; i < 3; i++) {
            Card playerCard = playerCards.get(i);
            playerStats.get(i).setText("<html><center><b>" + escapeHtml(playerCard.getName())
                    + "</b><br>ATK: " + playerCard.getAtk() + " &nbsp; DEF: " + playerCard.getDef() + "</center></html>");
            playerImages.get(i).setText("Cargando imagen...");
            playerImages.get(i).setIcon(null);
            playerSelectors.get(i).setToolTipText("Seleccionar " + playerCard.getName());

            Card machineCard = machineCards.get(i);
            machineImages.get(i).setText("Cargando imagen...\n" + machineCard.getName());
            machineImages.get(i).setIcon(null);
            machineImages.get(i).setToolTipText(machineCard.getName() + " - ATK " + machineCard.getAtk()
                    + " / DEF " + machineCard.getDef());

            loadCardImage(playerCard, playerImages.get(i), false, thisLoad);
            loadCardImage(machineCard, machineImages.get(i), true, thisLoad);
        }
    }

    private void loadCardImage(Card card, JLabel target, boolean showNameWithImage, int thisLoad) {
        imageExecutor.submit(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(card.getImageUrl()))
                        .timeout(Duration.ofSeconds(20)).GET().build();
                byte[] bytes = http.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
                Image image = new ImageIcon(bytes).getImage().getScaledInstance(150, 190, Image.SCALE_SMOOTH);
                SwingUtilities.invokeLater(() -> {
                    if (thisLoad != loadNumber) return;
                    target.setIcon(new ImageIcon(image));
                    target.setText(showNameWithImage ? "<html><center>" + escapeHtml(card.getName())
                            + "<br>ATK " + card.getAtk() + " / DEF " + card.getDef() + "</center></html>" : "");
                });
            } catch (Exception error) {
                SwingUtilities.invokeLater(() -> {
                    if (thisLoad == loadNumber) target.setText("Imagen no disponible");
                });
            }
        });
    }

    /** El radio button decide cuál carta del jugador se lanza; la máquina responde al azar. */
    private void playSelectedCard() {
        if (duel == null) {
            appendLog("Primero pulsa Duelo para cargar las cartas.");
            return;
        }
        int selectedIndex = -1;
        for (int i = 0; i < playerSelectors.size(); i++) {
            if (playerSelectors.get(i).isSelected()) selectedIndex = i;
        }
        if (selectedIndex < 0) {
            appendLog("Selecciona una carta con su radio button.");
            return;
        }
        Card selectedCard = playerCards.get(selectedIndex);
        if (!duel.getPlayerHand().contains(selectedCard)) {
            appendLog("Esa carta ya fue jugada. Selecciona otra.");
            return;
        }
        btnSeleccionarCarta.setEnabled(false);
        playerSelectors.forEach(selector -> selector.setEnabled(false));
        Duel.Position selectedPosition = defenseSelectors.get(selectedIndex).isSelected()
                ? Duel.Position.DEFENSE : Duel.Position.ATTACK;
        duel.play(selectedCard, selectedPosition);
        updateUsedCards();
        if (duel.getPlayerScore() < 2 && duel.getAiScore() < 2
                && !duel.getPlayerHand().isEmpty() && !duel.getAiHand().isEmpty()) {
            playerSelectors.forEach(selector -> selector.setEnabled(true));
            btnSeleccionarCarta.setEnabled(true);
            appendLog("Elige otra carta para la siguiente ronda.");
        }
    }

    private void updateUsedCards() {
        for (int i = 0; i < 3; i++) {
            if (!duel.getPlayerHand().contains(playerCards.get(i))) {
                playerSelectors.get(i).setEnabled(false);
                attackSelectors.get(i).setEnabled(false);
                defenseSelectors.get(i).setEnabled(false);
                playerStats.get(i).setText("<html><center><b>JUGADA</b><br>" + escapeHtml(playerCards.get(i).getName())
                        + "</center></html>");
            }
            if (!duel.getAiHand().contains(machineCards.get(i))) {
                machineImages.get(i).setText("<html><center>JUGADA<br>" + escapeHtml(machineCards.get(i).getName())
                        + "</center></html>");
                machineImages.get(i).setIcon(null);
            }
        }
    }

    /** Petición HTTP asíncrona. Se vuelve a consultar si la respuesta no es un monstruo. */
    private CompletableFuture<Card> fetchMonster(int attempts) {
        if (attempts >= 10) return failedFuture(new IllegalStateException("No se encontró una carta Monster."));
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL)).timeout(Duration.ofSeconds(20)).GET().build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() != 200) {
                        String body = response.body();
                        if (body.length() > 250) body = body.substring(0, 250) + "...";
                        String destination = response.headers().firstValue("Location")
                                .map(location -> " Destino: " + location + ".").orElse("");
                        return failedFuture(new IllegalStateException("HTTP " + response.statusCode() + "."
                                + destination + " Respuesta: " + body));
                    }
                    try {
                        JSONArray data = new JSONObject(response.body()).getJSONArray("data");
                        if (data.length() == 0) return failedFuture(new IllegalStateException("La API no devolvió cartas."));
                        JSONObject jsonCard = data.getJSONObject(0);
                        if (!jsonCard.optString("type").contains("Monster")) return fetchMonster(attempts + 1);
                        JSONArray images = jsonCard.getJSONArray("card_images");
                        String imageUrl = images.getJSONObject(0).getString("image_url");
                        return CompletableFuture.completedFuture(new Card(jsonCard.getString("name"),
                                jsonCard.optInt("atk"), jsonCard.optInt("def"), imageUrl));
                    } catch (Exception error) {
                        return failedFuture(error);
                    }
                });
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable error) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return future;
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public void onTurn(String playerCard, String aiCard, String winner) {
        appendLog("Tú lanzaste: " + playerCard + ".");
        appendLog("La máquina lanzó al azar: " + aiCard + ".");
        appendLog(winner.equals("Empate") ? "La ronda terminó en empate." : "Ganó la ronda: " + winner + ".");
    }

    @Override
    public void onScoreChanged(int playerScore, int aiScore) {
        lblMarcadorPlayer.setText(String.valueOf(playerScore));
        lblMarcadorMaquina.setText(String.valueOf(aiScore));
        appendLog("Marcador: Tú " + playerScore + " - " + aiScore + " Máquina.");
    }

    @Override
    public void onDuelEnded(String winner) {
        appendLog(winner.equals("Empate") ? "El duelo terminó empatado." : "¡" + winner + " ganó el duelo!");
        btnSeleccionarCarta.setEnabled(false);
        playerSelectors.forEach(selector -> selector.setEnabled(false));
        attackSelectors.forEach(selector -> selector.setEnabled(false));
        defenseSelectors.forEach(selector -> selector.setEnabled(false));
    }

    private void appendLog(String message) {
        textArea1.append(message + "\n");
        textArea1.setCaretPosition(textArea1.getDocument().getLength());
    }
}
