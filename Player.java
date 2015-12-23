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
        // enlever la première carte du tas caché
        Card c = hiddenCards.get(0);
        hiddenCards.removeFirst();
        // mettre cette carte en premier dans le tas révélé
        revealedCards.addFirst(c);
        // renvoyer cette carte
        return c;



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
        // ajouter heap au tas caché
        hiddenCards.addCards(heap);
        // ajouter les cartes du tas révélé au tas caché
        hiddenCards.addCards(revealedCards);
        // vider le tas révélé
        revealedCards.clear();
        // mélanger le tas caché
        hiddenCards.shuffle();
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
