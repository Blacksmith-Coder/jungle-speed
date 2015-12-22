//FINI

import java.util.*;

class Party {

    private static Random loto = new Random(Calendar.getInstance().getTimeInMillis());

    public final static int PARTY_WAITING = 0; // Attendre que le nombre suffisant de joueur aient rejoint la partie.
    public final static int PARTY_ONGOING = 1; // Partie commencée sans etat spécial
    public final static int PARTY_MUSTPLAY = 2; // Les joueurs doivent jouer
    public final static int PARTY_END = 3;  // Etat de fin de la partie

    public final static int RES_ERROR = -2; // Erreur d'un joueur
    public final static int RES_LOST = -1; // représente le fait qu'un joueur ait perdu (sauf erreur) dans le tou courant
    public final static int RES_NULL = 0; // Représente le fait que rien n'arrive à un joueur pendant le tour actuel
    public final static int RES_WIN = 1; // Représente le fait qu'un joueur ait gagné le tour actuel


    String name; //Le nom de la partie
    Player creator; // Le créateur
    int nbJoueursNecessaire; // Le nombre de joueurs convenu pour la partie
    ArrayList<Player> players; // La liste des joueurs
    OutputStreamPool pool; // Une classe qui gère les flux
    int nbrJoueurs; // Nombre de joueurs


    Semaphore commenceTour; // Barrière de synchronisation du thread au début du tour.

    int state;
    /*
      Quatres états possibles :
         0 = Attendre des joueurs,
         1 = en cours,
         2 = Joueurs doivent choisirs
         3 = fin de la partie
     */

    CardPacket allCards; // Le packet avec toutes les cartes.
    List<Card> underTotem; // Liste des cartes sous le totem (actuelle).

    int nbPlayerInTurn; // Nombre de joueurs en début de tour.
    Player currentPlayer; // Le joueur en cour.
    Player playerOfNextTurn; // le joueur au prochain tour.
    Card lastRevealedCard; // La carte révélé par le joueur durant le tour.
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
         si other n'est pas déjà dans cette partie & nbdejoueurs < nbjoueursnécessaires :
         alors  ajouter other à players
          */

            List<Card> distri = new ArrayList<>();
            if (!players.contains(other) && this.nbrJoueurs < nbJoueursNecessaire) {

                this.nbrJoueurs++; //On incrémente le nbr de joueur de la partie

                // obtenir 12 cartes de allCards
                 distri = allCards.takeXFirst(12);

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
            commenceTour.put(1); //semaphore
        }

        // prendre un jeton dans le sémaphore
        commenceTour.get();
    }

    private synchronized void initNewTurn() {
        played.clear();
        result.clear();
        totemTaken = false;
        totemHand = false;
        resultMsg = "";
    }

    public synchronized Card revealCard() {

        // demande au joueur actuel de révéler une carte.
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
        // quelque soit etat partie, si newState = fin -> etat = fin
        if (newState == PARTY_END) {
            state = PARTY_END;
        }

        //    si etat partie = en attente, alors seul newState = en cours est valide
        if (state == PARTY_WAITING && newState == PARTY_ONGOING) {
            state = PARTY_ONGOING;
        }

        //    si etat partie = en cours, alors seul newState = joueur doit jouer est valide
        if (state == PARTY_ONGOING && newState == PARTY_MUSTPLAY) {
            state = PARTY_MUSTPLAY;
        }

        //    si etat partie = joueur doit jouer, alors seul newState = en cours est valide
        if (state == PARTY_MUSTPLAY && newState == PARTY_ONGOING) {
            state = PARTY_ONGOING;
        }

        // toute autre combinaison est invalide et ne fait rien
    }

    public synchronized Object getCurrentCards() {
        // Récupération de toutes les cartes visibles autour de la table.
        CardPacket list = getAllRevealedCards();
        List<Card> unelist = list.getAll();
        String chaine = "";
        for (Card carte :  unelist) {
            chaine += carte.card;
        }
        Object s = chaine;
        return s;
    }

    /**
     * Methode qui récupère toutes les cartes visibles et les
     * renvoie sous forme d'un paquet de cartes.
     * @return
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

    /**
     * Méthode pour controler si des joueurs on la même carte visible.
     * @param p
     * @return
     */
    private boolean checkSameCards(Player p) {
        boolean same = false;
        // si p != null
        if (p != null) {
            for (Player other :  played) {
                // same = true si la carte visible de p est la même qu'un autre joueur et false sinon
                if (p.revealCard().equals(other.revealCard())) {
                    same = true;
                } else {
                    same = false;
                }
            }
        }
        return same;
    }

    /*
        Les valeurs de résultats sont:
        RES_ERROR: un joueur fait une erreur (voir ci-dessous)
        RES_LOST: un joueur a perdu parce qu'il n'a pas pris le totem alors qu'il devrait
        RES_NULL: un joueur a pris la bonne décision, mais n'est pas le plus rapide
        RES_WIN: un joueur a gagné le tour

       Les cas d'erreurs sont les suivants:
        - Un joueur ne fait rien alors que la dernière carte révélée est «la main sur totem
        - Un joueur prend le totem tandis que la dernière carte révélée est «la main sur totem
        - Un joueur pose sa main sur le totem tandis que la dernière carte révélée est «prendre totem
        - Un joueur prend le totem alors qu'il n'a pas la même carte à un autre joueur et le dernier est révélé = 'H' ou 'T'!

       Signification des ordres
       ACT_NOP: ne rien faire
       ACT_TAKETOTEM: prendre totem
       ACT_HANDTOTEM: main sur le totem
       ACT_INCORRECT : acte invalide

       retrun true si dernier thread à effectuer integratePlayerOrder
     */

    public synchronized boolean integratePlayerOrder(Player player, int order) {

        played.add(player);

        /*
           Quelque soit la valeur de order, il faut déterminer le résultat
           de l'action de player et ajouter celui-ci dans la list result.
           Ainsi, comme played et result sont remplis en même temps,
           on peut retrouver faiclement le résultat de chaque joueur.
        */

        // si dernière carte révélée == H
        if (lastRevealedCard.card == 'H') {

            // si ordre == ACT_NOP -> le joueur a fait une erreur

            if (order == JungleServer.ACT_NOP ) {
                result.add(RES_ERROR);
            }

            // sinon si ordre == ACT_TAKETOTEM -> le joueur a fait une erreur
            else if (order == JungleServer.ACT_TAKETOTEM) {
                result.add(RES_ERROR);
            }

            // sinon si ordre == ACT_HANDTOTEM
            else if (order == JungleServer.ACT_HANDTOTEM) {
                // si le totem n'a pas encore de main posé dessus
                if (!totemHand) {
                    result.add(RES_WIN); // Le joueur gagne
                    totemHand = true; // Mise à jour totemHand
                } else {
                    // si this == dernier à jouer
                    if (played.size() == nbrJoueurs) {
                        result.add(RES_LOST);
                    } else {
                        result.add(RES_NULL);
                    }
                }
            } else {
                // sinon le joueur a fait une erreur
                result.add(JungleServer.ACT_INCORRECT); // ordre invalide
            }
        }

        // sinon si dernière carte révélée == T
        else if (lastRevealedCard.card == 'T') {

            // si ordre == ACT_NOP -> le joueur a perdu
            if (order == JungleServer.ACT_NOP) {
                result.add(RES_LOST);
            }

            // sinon si ordre == ACT_TAKETOTEM
            else if (order == JungleServer.ACT_TAKETOTEM) {

                // si le totem n'est pas encore pris
                if (!totemTaken) {
                    result.add(RES_WIN);
                    totemTaken = true;
                }

                // sinon rien n'arrive au joueur
                else {
                    result.add(RES_NULL);
                }
            }

            // sinon si ordre == ACT_HANDTOTEM -> le joueur a fait une erreur
            else if (order == JungleServer.ACT_HANDTOTEM) {
                result.add(RES_ERROR);
            }

            // sinon le joueur a fait une erreur
            else {
                result.add(JungleServer.ACT_INCORRECT); // ordre invalide
            }

        } else {

            // si ordre == ACT_NOP
            if (order == JungleServer.ACT_NOP) {

                // si le joueur a même carte que d'autres -> le joueur a perdu
                if (checkSameCards(player)) {
                    result.add(RES_LOST);
                } else {
                    result.add(RES_NULL);
                }
            }

            // sinon si ordre == ACT_TAKETOTEM
            else if (order == JungleServer.ACT_TAKETOTEM) {

                // si le joueur a même carte que d'autres
                if (checkSameCards(player)) {

                    // si le totem n'est pas encore pris
                    if (!totemTaken) {
                        result.add(RES_WIN); // le joueur est gagnant
                        totemTaken = true; // mise à jour
                    } else {
                        result.add(RES_LOST);
                    }
                } else {
                   // sinon le joueur a fait une erreur
                    result.add(RES_ERROR);
                }
            }

            // sinon si ordre == ACT_HANDTOTEM -> le joueur a fait une erreur
            else if (order == JungleServer.ACT_HANDTOTEM) {
                result.add(RES_ERROR);
            }

            // sinon le joueur a fait une erreur
            else {
                result.add(JungleServer.ACT_INCORRECT); // ordre invalide
            }
        }

        if (played.size() >= nbrJoueurs) {
            setCurrentState(PARTY_ONGOING);
            return true;
        }
        return false;
    }

    public synchronized void analyseResults() {

        List<Player> lstErrors = new ArrayList<Player>(); // liste des joueurs qui ont fait une erreur ce tour
        List<Player> lstLoosers = new ArrayList<Player>(); // liste des joueurs qui ont perdu ce tour
        Player turnWinner = null;

        for(int i=0;i< nbrJoueurs;i++) {
            if (result.get(i) == RES_ERROR) {
                lstErrors.add(played.get(i));
            }
            if (result.get(i) == RES_LOST) {
                lstLoosers.add(played.get(i));
            }
        }

        // Si des joueurs ont fait une erreur.
        if (!lstErrors.isEmpty()) {
            /*
            * Quel que soit le cas, les joueurs qui ont fait une erreur sont les perdants ultimes:
            *   - Collecter toutes les cartes révélées
            *   - Ajouter ceux sous le totem
            *   - Répartir entre loosers
            */
            CardPacket errorPack = getAllRevealedCards();

            int nb = (errorPack.size()+1) / lstErrors.size();
            for(int i=0;i<lstErrors.size();i++) {
                Player p = lstErrors.get(i);
                if (i < lstErrors.size()-1) {
                    p.takeCards(errorPack.takeXFirst(nb));
                    resultMsg = resultMsg + p.name + " a fait une erreur, il prend " + nb + " cartes à tous les autres joueurs.\n";
                }
                else {
                    resultMsg = resultMsg + p.name + " a fait une erreur, il prend " + errorPack.size() + " cartes à tous les autres joueurs.\n";
                    p.takeCards(errorPack.getAll());
                }
            }
            // verifier si quelqu'un a gagné.
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
        // sinon si aucun joueur n'a fait d'erreur
        else {

            int indexWinner = -1;
            for(Integer r : result) {
                if (r == RES_WIN) {
                    indexWinner = r;
                    break;
                }
            }
            // Si personne ne gagne lors de ce tour
            if (indexWinner == -1) {
                resultMsg = resultMsg + "Nobody won this turn\n";
                playerOfNextTurn = players.get(currentPlayer.id % nbrJoueurs);
                resultMsg = resultMsg + "Next player: "+ playerOfNextTurn.name;
            }

            // Autrement si un joueur est le gagnant : le résultat dépend des dernières cartes révélées
            else {
                turnWinner = players.get(indexWinner);
                resultMsg = resultMsg + turnWinner.name + " won the turn.\n";

                // Si le gagnant remporte en prenant un totem
                if (lastRevealedCard.card == 'T') {
                    resultMsg = resultMsg + "He puts his cards under the totem.\n";
                    underTotem.addAll(turnWinner.revealedCards.getAll());
                    turnWinner.revealedCards.clear();
                }
                // le gagnant gagne avec main sur le totem
                else if (lastRevealedCard.card == 'H') {
                    Player looser = lstLoosers.get(0); // normally there should be a single player in lstLoosers list
                    resultMsg = resultMsg + "He gives his cards and those under totem to "+looser.name+".\n";
                    CardPacket winnerPack = getWinnerRevealedCards(turnWinner);
                    looser.takeCards(winnerPack.getAll());
                }
                // si le gagnant gagne parce qu'il a la même carte que certains autres joueurs
                else {
                    // distribuer la carte révélée du gagnant au perdant
                    CardPacket winnerPack = getWinnerRevealedCards(turnWinner);
                    int nb = (winnerPack.size()+1) / lstLoosers.size();
                    for(int i=0;i<lstLoosers.size();i++) {
                        Player p = players.get(i);
                        if (i < lstLoosers.size()-1) {
                            p.takeCards(winnerPack.takeXFirst(nb));
                            resultMsg = resultMsg + p.name + " perd le duel contre "+turnWinner.name+". Il prend " + nb + "cartes.\n";
                        }
                        else {
                            p.takeCards(winnerPack.getAll());
                            resultMsg = resultMsg + p.name + " perd le duel contre  "+turnWinner.name+". Il prend " + winnerPack.size() + "cartes.\n";
                        }
                    }
                }
                // Verifier si quelqu'un à gagné
                for(Player p :players) {
                    if (p.hasWon()) {
                        resultMsg = resultMsg + p.name + " gagne la partie";
                        setCurrentState(PARTY_END);
                        return;
                    }
                }
                playerOfNextTurn = lstLoosers.get(loto.nextInt(lstLoosers.size()));
                resultMsg = resultMsg + "Joueur suivant : "+ playerOfNextTurn.name;
            }
        }
        System.out.println("-------------------------------------------------------------------");
        for(Player p : players) {
            System.out.println(p.name+" has "+p.revealedCards.size()+" revealed cards and "+p.hiddenCards.size()+ " hidden cards");
        }
        System.out.println("-------------------------------------------------------------------");
    }

}
