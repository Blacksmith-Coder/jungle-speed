/* NOTE :
  Cette classe est incomplète : les commentaires sont là pour vous guider
 */

import java.util.*;

class Player {

    String name;
    CardPacket hiddenCards;
    CardPacket revealedCards;
    int id; // player id in the current Party, -1 if he didn't join a party.

    public Player(String name) {
        this.name = name;
        id = -1;
        hiddenCards = null; //will be set when joining party
        revealedCards = null; //will be set when joining party
    }

    public void joinParty(int id, List<Card> heap) {
        this.id = id;
        hiddenCards = new CardPacket(heap);
        revealedCards = new CardPacket();
    }

    public Card revealCard() {
        Card c = hiddenCards.get(0);
        hiddenCards.removeFirst();
        revealedCards.addFirst(c);
        return c;
        // enlever la première carte du tas caché
        // mettre cette carte en premier dans le tas révélé
        // renvoyer cette carte
    }

    public Card currentCard() {
        if (revealedCards.isEmpty()){
            return null;
        } else {
            return revealedCards.get(0);
        }
        // si le tas révélé est vide renovyer null
        // sinon renvoyer la première carte du tas révélé
    }

    public void takeCards(List<Card> heap) {
        hiddenCards.addCards(heap);
        hiddenCards.addCards(revealedCards);
        revealedCards.clear();
        hiddenCards.shuffle();
        // ajouter heap au tas caché
        // ajouter les cartes du tas révélé au tas caché
        // vider le tas révélé
        // mélanger le tas caché
    }

    public List<Card> giveRevealedCards() {
        List<Card> cards = new ArrayList<Card>();
        for (Card c: revealedCards.cards){
            cards.add(c);
        }
        revealedCards.clear();
        return cards;
        // mettre toutes les cartes du tas révélé dans cards
        // vider le tas révélé
        // renvoyer cards
    }

    public boolean hasWon() {
        if (revealedCards.isEmpty() && hiddenCards.isEmpty()){
            return true;
        } else {
            return false;
        }
        // renvoie true si tas révélé et caché sont vide, sinon false
    }
}
