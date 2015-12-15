//FINI

import java.util.*;

class CardPacket {

    private static Random loto = new Random(Calendar.getInstance().getTimeInMillis());

    List<Card> cards;

    // create a void packet for a single player
    public CardPacket() {
        cards = new ArrayList<Card>();
    }

    // create a packet for a single player from a list of cards
    public CardPacket(List<Card> heap) {
        cards = new ArrayList<Card>();
        cards.addAll(heap);
    }

    // create the whole packet for all players
    public CardPacket(int nbPlayers) {
        Card c;
        cards = new ArrayList<Card>();
        for(int i=0;i<nbPlayers;i++) {
            c = new Card('O');
            cards.add(c);
            c = new Card('Q');
            cards.add(c);
            c = new Card('B');
            cards.add(c);
            c = new Card('P');
            cards.add(c);
            c = new Card('E');
            cards.add(c);
            c = new Card('F');
            cards.add(c);
            c = new Card('I');
            cards.add(c);
            c = new Card('J');
            cards.add(c);
            c = new Card('C');
            cards.add(c);
            c = new Card('G');
            cards.add(c);
            c = new Card('T');
            cards.add(c);
            c = new Card('H');
            cards.add(c);
        }
        shuffle();
    }

    public int size() {
        return cards.size();
    }

    public void addCards(List<Card> heap) {
        cards.addAll(heap);
    }

    public void addCards(CardPacket heap) {
        cards.addAll(heap.cards);
    }

    public void clear() {
        cards.clear();
    }

    public void addFirst(Card c) {
        cards.add(0,c);
    }

    public Card removeFirst() {
        return cards.remove(0);
    }

    public void shuffle() {
        int index;
        for(int i=0;i<200;i++) {
            index = loto.nextInt(cards.size());
            Card c = cards.remove(index);
            cards.add(c);
        }
    }

    public List<Card> takeXFirst(int nb) {
        List<Card> heap = new ArrayList<Card>();
        Card c;
        for(int i=0;i<nb;i++) {
            c = cards.remove(0);
            heap.add(c);
        }
        return heap;
    }

    public Card get(int index) {
        if ((index <0) || (index >= cards.size())) return null;

        return cards.get(index);
    }

    public List<Card> getAll() {
        return cards;
    }

    public boolean isEmpty() {
        if (cards.isEmpty()) return true;
        return false;
    }
}
