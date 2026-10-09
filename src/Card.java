/** Datos de una carta Monster recibidos desde YGOProDeck. */
public class Card {
    private final String name;
    private final int atk;
    private final int def;
    private final String imageUrl;

    public Card(String name, int atk, int def, String imageUrl) {
        this.name = name;
        this.atk = atk;
        this.def = def;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public int getAtk() { return atk; }
    public int getDef() { return def; }
    public String getImageUrl() { return imageUrl; }

    @Override
    public String toString() {
        return name + " (ATK " + atk + " / DEF " + def + ")";
    }
}
