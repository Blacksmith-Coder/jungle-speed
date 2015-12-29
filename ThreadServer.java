//FINI

import java.util.*;
import java.io.*;
import java.net.*;

class ThreadServer extends Thread {


    Socket comm;
    Game game;
    Player player;
    Party currentParty; // Classe partagée : si null connection impossible.
    ObjectOutputStream oos;
    ObjectInputStream ois;

    /**
     * Le contructeur du ThreadServeur : prend en paramètre
     * une instance de classe jeu
     * une instance de socket en provenance de JungleServer
     *
     * @param game
     * @param comm
     */
    public ThreadServer(Game game, Socket comm) {
        this.game = game;
        this.comm = comm;
        currentParty = null; // Pour l'instant on ne lance pas de partie.
    }

    public void run() {

        String pseudo;
        boolean ok = false;

        System.out.println("Connection acceptée avec : " + comm.getRemoteSocketAddress().toString());
        try {
            ois = new ObjectInputStream(comm.getInputStream());
            oos = new ObjectOutputStream(comm.getOutputStream());

            while (!ok) {

                //Tant que c'est pas OK
                pseudo = (String) ois.readObject(); //On cherche à récupérer le pseudo

                //Si c'est le créateur...
                if (game.players.size() == 0) {
                    //C'est ok d'emblée
                    ok = true;
                } else {
                    // On vérifie que pas de doublon pseudo.
                    for (Player p : game.players) {
                        if (p.name.equals(pseudo)) {
                            ok = false;
                            break;
                        } else {
                            ok = true;
                        }
                    }
                }

                //On envoie au client ce qu'il en est de la décision
                if (ok) {

                    //On crée un joueur si il n'existe pas.
                    player = new Player(pseudo);
                    game.players.add(player);
                    //On prévient le client que c'est OK (true)
                    oos.writeBoolean(true);
                    ok = true;
                } else {
                    //Sinon on renvoie false au client
                    oos.writeBoolean(false);
                    ok = true;
                }
                oos.flush();

            }

        } catch (IOException e) {
            System.err.println("Problème de connection (IO)");
            return;
        } catch (ClassNotFoundException e) {
            System.err.println("Problème de requête client/serveur (ClassNotFound)");
            return;
        }

        try {
            while (true) {

                initLoop();
                // signaux de synchro de sorte que le threadclient n'envoie pas rapidement une demande
                oos.writeBoolean(true);
                oos.flush();

                try {
                    partyLoop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // supprimer le flux sortant associé à player du pool de la partie courante
                currentParty.pool.removeStream(player.id);

                // ret = supprimer player de la partie courante
                boolean ret = currentParty.removePlayer(player);

                // si ret est vrai (dernier joueur de la partie) supprimer la partie
                if (ret) {
                    if (currentParty.nbrJoueurs == 0) {
                        game.removeParty(currentParty);
                    }
                }

            }
        } catch (IllegalRequestException e) {
            System.err.println("Le client à envoyé une requête illégale: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Problème de connection avec le Client : " + e.getMessage());
        }
        // Ici, il y a eu déconnexion ou erreur de requête


        // si partie courante != null (i.e. le joueur s'est déconnnecté en pleine partie)
        if (currentParty != null) {

            // si l'état partie != en attente, etat partie = fin
            if (currentParty.getCurrentState() != Party.PARTY_WAITING) {
                currentParty.state = Party.PARTY_END;
            }

            // supprimer le flux sortant associé à player du pool de la partie courante
            currentParty.pool.removeStream(player.id);

            // ret = supprimer player de la partie courante
            boolean ret = currentParty.removePlayer(player);

            // si ret est vrai (dernier joueur de la partie) supprimer la partie
            if (ret) {
                game.removeParty(currentParty);
            }
        }

        // supprimer le joueur du jeu.
        game.removePlayer(player);

    }

    public void initLoop() throws IllegalRequestException, IOException {

        int idReq;
        boolean stop = false; // devient true en cas de requête CREATE ou JOIN réussie
        while (!stop) {

            // recevoir n° requete
            idReq = ois.readInt();

            // si n° correspond appeler la méthode correspondante :
            switch (idReq) {
                case JungleServer.REQ_LISTPARTY:
                    requestListParty();
                    stop = false;
                    break;
                case JungleServer.REQ_CREATEPARTY:
                    requestCreateParty();
                    stop = true;
                    break;
                case JungleServer.REQ_JOINPARTY:
                    requestJoinParty();
                    stop = true;
                    break;
                default:
                    throw new IllegalRequestException("Requete illegale");
            }
            // sinon générer une exception IllegalRequest
        }
    }

    public void partyLoop() throws IllegalRequestException, IOException, InterruptedException {

        int idReq;

        while (true) {

            // si etat partie == fin, retour
            System.out.println("passe");
            if (currentParty.state == 3) return;

            System.out.println("passe2");
            // recevoir n° requete
            idReq = ois.readInt();

            // si etat partie == fin, retour
            if (currentParty.state == 3) {
                System.out.println("retour");
                return;
            }

            System.out.println("passe3");
            // si n° req correspond appeler la méthode correspondante
            switch (idReq) {
                case JungleServer.REQ_WAITPARTYSTARTS:
                    System.out.println("Attente de démarre partie");
                    requestWaitPartyStarts();
                    break;
                case JungleServer.REQ_WAITTURNSTARTS:
                    System.out.println("Attendre tour demarre");
                    requestWaitTurnStarts();
                    break;
                case JungleServer.REQ_PLAY:
                    System.out.println("Joue");
                    requestPlay();
                    break;
                default:
                    // sinon générer une exception IllegalRequest
                    throw new IllegalRequestException("Requete illegale");
            }
        }
    }

    public void requestListParty() throws IOException, IllegalRequestException {
        String nomParty;
        try {
            System.out.println(game.parties.size());
            if (game.parties.size() == 0) {
                nomParty = "Pas de parties";
            } else {
                List<Party> parties = game.parties;
                nomParty = "";

                for (int i = 0, partiesSize = parties.size(); i < partiesSize; i++) {
                    Party p = parties.get(i);
                    nomParty += "Partie n° " + i + p.name + " createur " + p.creator.name + "\n";

                }
            }
            oos.writeObject(nomParty);
            oos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean requestCreateParty() throws IOException, IllegalRequestException {

        boolean rep = false; // mis a true si la requête permet effectivement de créer une nouvelle partie.
        try {
            String nomParty = (String) ois.readObject();
            int nbJoueurs = ois.readInt();
            Party party = game.createParty(nomParty, player, nbJoueurs);
            System.out.println(party.getClass().toString());
            //On ajoute le flux oos à la partie crée :
            party.pool.addStream(player.id, oos);
            currentParty = party;
            rep = true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return rep;
    }

    public boolean requestJoinParty() throws IOException, IllegalRequestException {

        boolean rep = false; // mis a true si la requête permet effectivement de rejoindre une partie existante

        // traiter requete JOIN PARTY (sans oublier les cas d'erreur)
        int numParty = ois.readInt();
        Party party = game.parties.get(numParty);

        // On ajoute le flux oos au pool de la partie rejointe
        if (game.playerJoinParty(player, game.parties.get(numParty))) {
            currentParty = party;
            party.pool.addStream(player.id, oos);
            rep = true;
            oos.writeInt(player.id);
        }

        return rep;
    }

    public void requestWaitPartyStarts() throws IOException, IllegalRequestException, InterruptedException {

        currentParty.waitForPartyStarts();
        oos.writeInt(player.id);

    }

    public void requestWaitTurnStarts() throws IOException, IllegalRequestException, InterruptedException {

        // traitement des cas d'erreur

        Player currentPlayer;

        // attendre début tour
        currentParty.waitForTurnStarts();

        // récupérer le joueur courant dans le tour -> currentPlayer
        currentPlayer = currentParty.currentPlayer;

        // si etat partie == fin, envoyer -1 (LOST) au client sinon envoyer id joueur courant
        if (currentParty.state == Party.PARTY_END) {
            oos.writeInt(Party.RES_LOST);
        } else {
            oos.writeInt(currentPlayer.id);
        }

        // Si this est le thread associé au joueur courant
        if (currentParty.currentPlayer == player) {

            // Attendre 3s
            wait(3000);

            //Montrer une carte
            currentParty.revealCard();

            // obtenir la liste des cartes visibles
            Object visibles = currentParty.getCurrentCards();

            // mettre état partie à "joueur doivent jouer".
            currentParty.setCurrentState(Party.PARTY_MUSTPLAY);

            // envoyer cette liste à tous les clients (grâce au pool)
            currentParty.pool.sendToAll(visibles);
        }
    }

    public void requestPlay() throws IOException, IllegalRequestException {

        String action = "";
        int idAction = -1;
        boolean lastPlayed = false;

        // traitement des cas d'erreur

        try {
            // recevoir la String qui indique l'ordre envoyé par le client
            String ordreClient = (String) ois.readObject();

            // en fonction de cette String, initialiser idAction
            // à ACT_TAKETOTEM ou ACT_HANDTOTEM ou ACT_NOP ou ACT_INCORRECT
            switch (ordreClient) {
                case "TT":
                    idAction = JungleServer.ACT_TAKETOTEM;
                    break;
                case "TH":
                    idAction = JungleServer.ACT_HANDTOTEM;
                    break;
                case "":
                    idAction = JungleServer.ACT_NOP;
                    break;
                default:
                    idAction = JungleServer.ACT_INCORRECT;
                    break;
            }

            // lastPlayed <- intégrer l'ordre donné par le joueur (cf. integratePlayerOrder())
            lastPlayed = currentParty.integratePlayerOrder(player, idAction);

            // si lastPLayer est vrai
            if (lastPlayed) {

                // si etat partie == fin
                if (currentParty.state == Party.PARTY_END) {
                    // envoyer un message à tous les clients
                    currentParty.pool.sendToAll("Partie finie");
                    // envoyer true (= fin de partie)
                    oos.writeBoolean(true);
                    return;
                }

                // analyser les résultats
                currentParty.analyseResults();
                String resultMsg = currentParty.resultMsg;
                // envoyer resultMsg de la partie courante à tous les clients
                currentParty.pool.sendToAll(resultMsg);

                // si etat partie == fin, envoyer true, sinon envoyer false
                if (currentParty.state == Party.PARTY_END) {
                    oos.writeBoolean(true);
                } else {
                    oos.writeBoolean(false);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
