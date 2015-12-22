//FINI

import java.util.*;

class Player {

    String name;
    CardPacket hiddenCards;
    CardPacket revealedCards;
    int id; // Id joueur dans la partie courante, -1 si il n'a pas rejoins de partie.

    public Player(String name) {
        this.name = name;
        id = -1;
        hiddenCards = null; //Sera définie lors qu'une partie sera rejointe
        revealedCards = null; //Sera définie ultérieurement aussi.
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
        // si le tas révélé est vide renovyer null
        if (revealedCards.isEmpty()){
            return null;
        }
        // sinon renvoyer la première carte du tas révélé
        else {
            return revealedCards.get(0);
        }


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
        // mettre toutes les cartes du tas révélé dans cards
        for (Card c: revealedCards.cards){
            cards.add(c);
        }
        // vider le tas révélé
        revealedCards.clear();
        // renvoyer cards
        return cards;



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
