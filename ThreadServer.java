//Terminer les exceptions

import java.util.*;
import java.io.*;
import java.net.*;

class ThreadServer extends Thread {

    private static Random loto = new Random(Calendar.getInstance().getTimeInMillis());

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

            while (!ok){ //Tant que c'est pas OK
                pseudo = (String) ois.readObject(); //On cherche à récupérer le pseudo

                // On vérifie que pas de doublon pseudo.
                for (Player p : game.players){
                    if (p.name.equals(pseudo)){
                        ok = false;
                        break;
                    } else {
                        ok = true;
                    }
                }

                //On envoie au client ce qu'il en est de la décision
                if (ok){

                    //On crée un joueur si il n'existe pas.
                    player = new Player(pseudo);
                    //On prévient le client que c'est OK (true)
                    oos.writeBoolean(true);
                } else {
                    //Sinon on renvoie false au client
                    oos.writeBoolean(false);
                }
                oos.flush();

            }

        }
        catch(IOException e) {
            System.err.println("Problème de connection (IO)");
            return;
        }
        catch(ClassNotFoundException e) {
            System.err.println("Problème de requête client/serveur (ClassNotFound)");
            return;
        }

        try {
            while (true) {
                initLoop();
                oos.writeBoolean(true); // synchro signals so that thread client does not sends to quickly a request
                oos.flush();
                partyLoop();
                currentParty.pool.removeStream(player.id);
                boolean ret = currentParty.removePlayer(player);
                if (ret){
                    if (currentParty.nbrJoueurs == 0){
                        game.removeParty(currentParty);
                    }
                }
                // supprimer le flux sortant associé à player du pool de la partie courante
                // ret = supprimer player de la partie courante
                // si ret == true (i.e. dernier joueur de la partie) supprimer la partie
            }
        }
        catch(IllegalRequestException e) {
            System.err.println("Le client à envoyé une requête illégale: "+e.getMessage());
        }
        catch(IOException e) {
            System.err.println("Problème de connection avec le Client : "+e.getMessage());
        }
        // NB : si on arrive ici, c'est qu'il y a eu déconnexion ou erreur de requête


        if (currentParty != null){
            if (currentParty.getCurrentState() != 0){
                currentParty.state = 3;
            }
            currentParty.pool.removeStream(player.id);
            boolean ret = currentParty.removePlayer(player);
            if (ret){
                game.removeParty(currentParty);
            }
        }
        game.removePlayer(player);

        // si partie courante != null (i.e. le joueur s'est déconnnecté en pleine partie)
        //    si l'état partie != en attente, etat partie = fin
        //    supprimer le flux sortant associé à player du pool de la partie courante
        //    ret = supprimer player de la partie courante
        //    si ret == true (i.e. dernier joueur de la partie) supprimer la partie
        // fsi
        // supprimer le joueur de game
    }

    public void initLoop() throws IllegalRequestException,IOException  {

        int idReq;
        boolean stop = false; // devient true en cas de requête CREATE ou JOIN réussie
        while (!stop) {
             idReq = ois.readInt();
            switch (idReq){
                case 1:
                    requestListParty();
                    break;
                case 2:
                    requestCreateParty();
                    break;
                case 3: requestJoinParty();
                    break;
                default:
                    throw new IllegalRequestException("Requete illegale");
            }
            // recevoir n° requete
            // si n° correspond à LIST PARTY, CREATE PARTY ou JOIN PARTY appeler la méthode correspondante
            // sinon générer une exception IllegalRequest
        }
    }

    public void partyLoop() throws IllegalRequestException,IOException  {

        int idReq;

        while (true) {

            if (currentParty.state == 3){
                return;
            }
            idReq = ois.readInt();
            if (currentParty.state == 3){
                return;
            }
            switch (idReq){
                case 4:
                    requestWaitPartyStarts();
                    break;
                case 5:
                    requestWaitTurnStarts();
                    break;
                case 6:
                    requestPlay();
                    break;
                default:
                    throw new IllegalRequestException("Requete illegale");
            }
            // si etat partie == fin, retour
            // recevoir n° requete
            // si etat partie == fin, retour
            // si n° req correpsond à WAIT PARTY STARTS, WAIT TURN STARTS, PLAY, appeler la méthode correspondante
            // sinon générer une exception IllegalRequest
        }
    }

    public void requestListParty() throws IOException,IllegalRequestException {
        try {
            List<Party> parties = game.parties;
            String nomParty = "";
            for (Party p : parties){
                nomParty += "-" + p.name + "\n";
            }
            oos.writeObject(nomParty);
            oos.flush();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public boolean requestCreateParty() throws IOException,IllegalRequestException {

        boolean rep = false; // mis a true si la requête permet effectivement de créer une nouvelle partie.
        try {
            String nomParty = (String) ois.readObject();
            int nbJoueurs = ois.readInt();
            Party party = game.createParty(nomParty ,player, nbJoueurs);

            //On ajoute le flux oos à la partie crée :
            party.pool.addStream(player.id, oos);
            rep = true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return rep;
    }

    public boolean requestJoinParty() throws IOException,IllegalRequestException {

        boolean rep = false; // mis a true si la requête permet effectivement de rejoindre une partie existante

        // traiter requete JOIN PARTY (sans oublier les cas d'erreur)
        int numParty = (int) ois.readInt();
        Party party = game.parties.get(numParty);
        game.playerJoinParty(player, game.parties.get(numParty));

        // On ajoute le flux oos au pool de la partie rejointe
        party.pool.addStream(player.id, oos);

        return rep;
    }

    public void requestWaitPartyStarts() throws IOException,IllegalRequestException {

        // traiter requete WAIT PARY STARTS (sans oublier les cas d'erreur)

    }

    public void requestWaitTurnStarts() throws IOException,IllegalRequestException {

        Player currentPlayer;
        // traiter cas d'erreur
        // attendre début tour
        // récupérer le joueur courant dans le tour -> currentPlayer
        // si etat partie == fin, envoyer -1 au client sinon envoyer id joueur courant
        // si je suis le thread associé au joueur courant
        //    faire dodo entre 1 et 3s
        //    révéler une carte
        //    obtenir la liste des cartes visibles
        //    mettre état partie à "joueur doivent jouer".
        //    envoyer cette liste à tous les clients (grâce au pool)
        // fsi
    }

    public void requestPlay() throws IOException,IllegalRequestException {

        String action = "";
        int idAction = -1;
        boolean lastPlayed = false;

        // traiter cas d'erreur

        // recevoir la String qui indique l'ordre envoyé par le client
        // en fonction de cette String, initialiser idAction à ACT_TAKETOTEM ou ACT_HANDTOTEM ou ACT_NOP ou ACT_INCORRECT
        // lastPlayed <- intégrer l'ordre donné par le joueur (cf. integratePlayerOrder() )
        // si lastPLayer vaut true
        //    si etat partie == fin
        //       envoyer un message du style "partie finie" à tous les client
        //       envoyer true (= fin de partie) puyis retour
        //    fsi
        //    analyser les résultats
        //    envoyer resultMsg de la partie courante à tous les clients
        //    si etat partie == fin, envoyer true, sinon envoyer false
        // fsi
    }
}
