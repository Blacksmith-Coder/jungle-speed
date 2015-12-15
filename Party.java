//En cour de finition

import java.util.*;

class Party {

    private static Random loto = new Random(Calendar.getInstance().getTimeInMillis());

    public final static int PARTY_WAITING = 0; // Attendre que le nombre suffisant de joueur aient rejoint la partie.
    public final static int PARTY_ONGOING = 1; // Partie commencée sans etat spécial
    public final static int PARTY_MUSTPLAY = 2; // Les joueurs doivent jouer
    public final static int PARTY_END = 3;

    public final static int RES_ERROR = -2; // Erreur d'un joueur
    public final static int RES_LOST = -1; // représente le fait qu'un joueur ait perdu (sauf erreur) dans le tou courant
    public final static int RES_NULL = 0; // Représente le fait que rien n'arrive à un joueur pendant le tour actuel
    public final static int RES_WIN = 1; // Représente le fait qu'un joueur ai gagné le tour actuel

    String name; //Le nom de la partie
    Player creator; // Le créateur
    int nbJoueursNecessaire; // Le nombre de joueurs convenu pour la partie
    ArrayList<Player> players; // La liste des joueurs
    OutputStreamPool pool; // Une classe qui gère les flux
    int nbrJoueurs; // Nombre de joueurs


    Semaphore commenceTour; // Barrière de synchronisation du thred au début du tour.
    /* NOTE :
      Quatres états possibles :
         0 = Attendre des joueurs,
         1 = en cours,
         2 = Joueurs doivent choisirs
         3 = fin de la partie
     */
    int state;
    CardPacket allCards; // Le packet avec toutes les cartes.
    List<Card> underTotem; // Liste des cartes sous le totem (actuelle).

    int nbPlayerInTurn; // Nombre de joueurs en début de tour.
    Player currentPlayer; // Le joueur en cour.
    Player playerOfNextTurn; // le joueur au prochain tour.
    Card lastRevealedCard; // La carte révélé par le jouer durant le tour.
    boolean totemTaken; // devient true des que le totem est pris durant le tour.
    boolean totemHand; // devient true des qu'un joueur est le premier à mettre la main sur le totem.
    List<Player> played; // Liste des joueurs ayant déjà joué.
    List<Integer> result; // Le classement.
    String resultMsg; // Message envoyé à la fin du tour.

    /**
     * Constructeur de Partie
     * @param name
     * @param creator
     * @param nbJoueursNecessaire
     */
    public Party(String name, Player creator, int nbJoueursNecessaire) {
        this.name = name;
        this.creator = creator;
        this.nbJoueursNecessaire = nbJoueursNecessaire;
        allCards = new CardPacket(nbJoueursNecessaire);
        players = new ArrayList<Player>();
        players.add(creator); // Le créateur de la partie est le premier joueur de la partie
        nbrJoueurs = 1;
        List<Card> heap = allCards.takeXFirst(12);
        creator.joinParty(1,heap);
        underTotem = new ArrayList<Card>();
        played = new ArrayList<Player>();
        result = new ArrayList<Integer>();
        pool = new OutputStreamPool();
        state = PARTY_WAITING;
        currentPlayer = null;
        playerOfNextTurn = null;
        commenceTour = new Semaphore(0);
    }

    //Terminée.
    public synchronized void addPlayer(Player other) {
        /*
         si p n'est pas déjà dans cette partie & nbdejoueurs < nbjoueursnécessaires :
         alors  ajouter other à players
          */
        for (Player test :  players) {
            if (!test.equals(other) && this.nbrJoueurs < nbJoueursNecessaire) {

                this.nbrJoueurs++; //On incrémente le nbr de joueur de la partie

                // obtenir 12 cartes de allCards
                Card pioche;
                ArrayList<Card> distri = new ArrayList<Card>();
                for (int i = 0; i <= 12; i++) {
                    pioche = allCards.removeFirst();
                    distri.add(pioche);
                }

                // Affectation au joueur de son id et de son paquet.
                other.joinParty(loto.nextInt(),distri);
                players.add(other);

            } else if (nbrJoueurs == nbJoueursNecessaire) {

                // joueur du prochain tour  = premier joueur de la liste.
                playerOfNextTurn = players.get(0);
                // nb de joueur dans le tour = 0
                nbPlayerInTurn = 0;
                // Initialiser un nouveau tour.
                this.initNewTurn();
                //Etat partie "en cours"
                this.setCurrentState(1);
                //On réveille tous les threads.
                notifyAll();
            }
        }
    }

    public synchronized boolean removePlayer(Player other) {

        // supprimer other de players
        players.remove(other);

        // décrémenter nb joueur dans la partie
        this.nbrJoueurs--;

        // remettre id de player à -1 (= pas dans une partie)
        currentPlayer.id = -1;

        // mettre des jetons dans le sémaphore (au cas où des threads soient
        // bloqués dans la barrière de début de tour)
        commenceTour.put(nbrJoueurs);

        // si nb joueurs dans partie == 0, renvoyer vrai sinon renvoyer false
        if (nbrJoueurs == 0) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized void waitForPartyStarts() {

        // tant que état partie == en attente ET nb joueurs dans la partie != nb joueurs nécessaires
        while(this.getCurrentState() == PARTY_WAITING && nbrJoueurs != nbJoueursNecessaire) {
          this.setCurrentState(PARTY_WAITING);
        }
        this.setCurrentState(PARTY_ONGOING);
    }

    /**
     * Attendre pour demarrer le tour :
     * Est basé sur un sémaphore pour mettre en œuvre une barrière synchrone.
     * Le seamphore est créé avec 0 jeton. Ainsi, chaque thread essayant d'obtenir un jeton
     * devra attendre jusqu'à qu'un autre jeton soit mis dans le sémaphore.
     */
    public void waitForTurnStarts() {
        // incrementer nb joueurs dans le tour
        nbPlayerInTurn++;

        // si nb joueurs dans le tour == nb joueurs dans partie
        //    remettre nb joueurs dans le tour à 0
        //    initialiser joueurs courant avec valeur joueur du prochain tour
        //    initialiser nouveau tour
        //    mettre des jetons dans le semaphore
        if (nbPlayerInTurn == nbrJoueurs) {
            nbPlayerInTurn = 0;
            currentPlayer = playerOfNextTurn;
            initNewTurn();
            commenceTour.put(nbrJoueurs);
        }

        // prendre un jeton dans le sémaphore
        commenceTour.put(1);
    }

    private synchronized void initNewTurn() {
        played.clear();
        result.clear();
        totemTaken = false;
        totemHand = false;
        resultMsg = "";
    }

    public synchronized Card revealCard() {

        // update lastRevealedCard by asking current player to reveal a card
        lastRevealedCard = currentPlayer.revealCard();
        return lastRevealedCard;
    }

    public synchronized Player getCurrentPlayer() {
        return currentPlayer;
    }

    public synchronized int getCurrentState() {
        return state;
    }

    public synchronized void setCurrentState(int newState) {
        // les règles à tester dans l'ordre pour gérer l'état de la partie :
        //    qq soit etat partie, si newState = fin -> etat = fin
        //    si etat partie = en attente, alors seul newState = en cours est valide
        //    si etat partie = en cours, alors seul newState = joueur doit jouer est valide
        //    si etat partie = joueur doit jouer, alors seul newState = en cours est valide
        // toute autre combinaison est invalide et ne fait rien
    }

    public synchronized Object getCurrentCards() {
        Object s;
        // récupérer toutes les cartes actuellement visibles autour de la table et en faire un objet
        // NB : à vous de décider la classe de cette objet et comment il est construit.
        // Vous pouvez par exemple simplement créer une chaîne de caractères

        return s;
    }

    /* getAllRevealedCards() ;
       In case of players are in error, we must collect all visible cards plus those under the totem
       and distribute them among these players. This method does the first part
     */
    private CardPacket getAllRevealedCards() {

        CardPacket packet = new CardPacket();
        for(Player p : players) {
            packet.addCards(p.giveRevealedCards());
        }
        packet.addCards(underTotem);
        underTotem.clear();

        return packet;
    }

    /* getWinnerRevealedCards() ; =>
       Dans le cas où un joueur a gagné le tour, on doit recueillir ses cartes visibles en
       plus de celles sous le totem et les distribuer parmi les perdants.
       La méthode fait la première partie :
     */
    private CardPacket getWinnerRevealedCards(Player turnWinner) {

        CardPacket packet = new CardPacket();
        packet.addCards(turnWinner.giveRevealedCards());
        packet.addCards(underTotem);
        underTotem.clear();

        return packet;
    }

    private boolean checkSameCards(Player p) {
        boolean same = false;
        // si p != null
        //   same = true si la carte visible de p est la même qu'un autre joueur et false sinon
        //   ATTENTION : certains joueurs (dont p) n'ont peut être pas de carte visible

        return false;
    }

    /* NB : result values are :
       RES_ERROR: a player made an error (see below)
       RES_LOST: a player lost because he didn't take the totem while he should
       RES_NULL: a player took the good decision but is not the faster
       RES_WIN: a player has win the turn

       error cases are the following :
       - a player does nothing while the last revealed card is 'hand on totem'
       - a player takes the totem while the last revealed card is 'hand on totem'
       - a player puts his hand on the totem while the last revealed card is 'take totem'
       - a player takes the totem while he hasn't the same card than another player and the last revealed is != 'H' or 'T'

       NB: order values are :
       ACT_NOP: do nothing
       ACT_TAKETOTEM: take totem
       ACT_HANDTOTEM: hand on totem

       reutrned value : true if i'am the last thread to integrate the player order
     */

    public synchronized boolean integratePlayerOrder(Player player, int order) {

        played.add(player);

        /* NOTE : qq soit la valeur de order, il faut déterminer le résultat de l'action
           de player et ajouter celui-ci dans la list result.
           Ainsi, comme played et result sont remplis en même temps, on peut retrouver faiclement le résultat de chaque joueur.
        */

        // si dernière carte révélée == H

        //    si ordre == ACT_NOP -> le joueur a fait une erreur
        //    sinon si ordre == ACT_TAKETOTEM -> le joueur a fait une erreur
        //    sinon si ordre == ACT_HANDTOTEM
        //       si le totem n'a pas encore de main posé dessus
        //          le joueur est gagnant
        //          mettre à jour totemHand
        //       sinon
        //          si je suis le dernier à jouer (i.e. taille de played == nb joueurs dans partie)
        //             joueur a perdu
        //          sinon rien n'arrive au joueur
        //       fsi
        //    sinon le joueur a fait une erreur (i.e. un ordre invalide)

        // sinon si dernière carte révélée == T
        //    si ordre == ACT_NOP -> le joueur a perdu
        //    sinon si ordre == ACT_TAKETOTEM
        //       si le totem n'est pas encore pris
        //          le joueur est gagnant
        //          mettre à jour totemTaken
        //       sinon rien n'arrive au joueur
        //       fsi
        //    sinon si ordre == ACT_HANDTOTEM -> le joueur a fait une erreur
        //    sinon le joueur a fait une erreur (i.e. un ordre invalide)

        // sinon (.ie. dernière carte révélée est normale)
        //    si ordre == ACT_NOP
        //       si le joueur a même carte que d'autres -> le joueur a perdu
        //       sinon rien n'arrive au joueur
        //    sinon si ordre == ACT_TAKETOTEM
        //       si le joueur a même carte que d'autres
        //          si le totem n'est pas encore pris
        //             le joueur est gagnant
        //             mettre à jour totemTaken
        //          sinon le joueur est perdant
        //       sinon le joueur a fait une erreur
        //    sinon si ordre == ACT_HANDTOTEM -> le joueur a fait une erreur
        //    sinon le joueur a fait une erreur (i.e. un ordre invalide)
        // fsi

        if (played.size() >= nbrJoueurs) {
            setCurrentState(PARTY_ONGOING);
            return true;
        }
        return false;
    }

    public synchronized void analyseResults() {

        List<Player> lstErrors = new ArrayList<Player>(); // list of players that made an error this turn
        List<Player> lstLoosers = new ArrayList<Player>(); // list of players that lost (not an error) this turn
        Player turnWinner = null;

        for(int i=0;i< nbrJoueurs;i++) {
            if (result.get(i) == RES_ERROR) {
                lstErrors.add(played.get(i));
            }
            if (result.get(i) == RES_LOST) {
                lstLoosers.add(played.get(i));
            }
        }

        // if some players made an error
        if (!lstErrors.isEmpty()) {
            /* whatever the case, players that made an error are the ultimate loosers:
	           - collect all revealed cards
	           - add those under the totem
	           - distribute them among loosers
            */
            CardPacket errorPack = getAllRevealedCards();

            int nb = (errorPack.size()+1) / lstErrors.size();
            for(int i=0;i<lstErrors.size();i++) {
                Player p = lstErrors.get(i);
                if (i < lstErrors.size()-1) {
                    p.takeCards(errorPack.takeXFirst(nb));
                    resultMsg = resultMsg + p.name + " made an error: he takes " + nb + "cards from all players.\n";
                }
                else {
                    resultMsg = resultMsg + p.name + " made an error: he takes " + errorPack.size() + "cards from all players.\n";
                    p.takeCards(errorPack.getAll());
                }
            }
            // now check if someone has won
            for(Player p :players) {
                if (p.hasWon()) {
                    resultMsg = resultMsg + p.name + " wins the party";
                    setCurrentState(PARTY_END);
                    return;
                }
            }
            playerOfNextTurn = lstErrors.get(loto.nextInt(lstErrors.size()));
            resultMsg = resultMsg + "Next player: "+ playerOfNextTurn.name;
        }
        // else if no player made an error
        else {

            int indexWinner = -1;
            for(Integer r : result) {
                if (r == RES_WIN) {
                    indexWinner = r;
                    break;
                }
            }
            // if nobody wins this turn
            if (indexWinner == -1) {
                resultMsg = resultMsg + "Nobody won this turn\n";
                playerOfNextTurn = players.get(currentPlayer.id % nbrJoueurs);
                resultMsg = resultMsg + "Next player: "+ playerOfNextTurn.name;
            }
            // else if a player is the winner : result depends on the last revealed cards
            else {
                turnWinner = players.get(indexWinner);
                resultMsg = resultMsg + turnWinner.name + " won the turn.\n";

                // if winner wins on a take totem
                if (lastRevealedCard.card == 'T') {
                    resultMsg = resultMsg + "He puts his cards under the totem.\n";
                    underTotem.addAll(turnWinner.revealedCards.getAll());
                    turnWinner.revealedCards.clear();
                }
                // else if winner wins on a hand on totem
                else if (lastRevealedCard.card == 'H') {
                    Player looser = lstLoosers.get(0); // normally there should be a single player in lstLoosers list
                    resultMsg = resultMsg + "He gives his cards and those under totem to "+looser.name+".\n";
                    CardPacket winnerPack = getWinnerRevealedCards(turnWinner);
                    looser.takeCards(winnerPack.getAll());
                }
                // if winner wins because he has the same card than some other players
                else {
                    // distribute winner's revealed card to loosers
                    CardPacket winnerPack = getWinnerRevealedCards(turnWinner);
                    int nb = (winnerPack.size()+1) / lstLoosers.size();
                    for(int i=0;i<lstLoosers.size();i++) {
                        Player p = players.get(i);
                        if (i < lstLoosers.size()-1) {
                            p.takeCards(winnerPack.takeXFirst(nb));
                            resultMsg = resultMsg + p.name + " lost his a duel with "+turnWinner.name+". He takes " + nb + "cards.\n";
                        }
                        else {
                            p.takeCards(winnerPack.getAll());
                            resultMsg = resultMsg + p.name + " lost his a duel with "+turnWinner.name+". He takes " + winnerPack.size() + "cards.\n";
                        }
                    }
                }
                // now check if someone has won
                for(Player p :players) {
                    if (p.hasWon()) {
                        resultMsg = resultMsg + p.name + " wins the party";
                        setCurrentState(PARTY_END);
                        return;
                    }
                }
                playerOfNextTurn = lstLoosers.get(loto.nextInt(lstLoosers.size()));
                resultMsg = resultMsg + "Next player: "+ playerOfNextTurn.name;
            }
        }
        System.out.println("-------------------------------------------------------------------");
        for(Player p : players) {
            System.out.println(p.name+" has "+p.revealedCards.size()+" revealed cards and "+p.hiddenCards.size()+ " hidden cards");
        }
        System.out.println("-------------------------------------------------------------------");
    }

}
