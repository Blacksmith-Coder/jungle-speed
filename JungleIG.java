//FINI

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Classe JungleIG : Interface graphique du jeu
 */
class JungleIG extends JFrame implements ActionListener {

    /* attributs de communication */
    private Socket comm;
    public ObjectOutputStream oos;
    public ObjectInputStream ois;

    /* panneau connection */
    protected JPanel panConn;
    protected JTextField textServerIP;
    protected JTextField textPseudo;
    protected JButton butConnect;

    /* panneau avant partie */
    protected JPanel panInit;
    public JTextArea textInfoInit;
    protected JButton butListParty;
    protected JButton butCreateParty;
    protected JSpinner spinNbPlayer;
    protected JTextField textCreate;
    protected JButton butJoinParty;
    protected JTextField textJoin;

    /* widgets for party panel */
    protected JPanel panParty;
    public JTextArea textInfoParty;
    protected JTextField textPlay;
    protected JButton butPlay;
    protected JButton butQuit;

    /* Autres attributs */
    // valeur false à chaque fois IG débloque le bouton play.
    // la valeur true si le bouton de lecture est cliqué tout déverrouillé
    public boolean orderSent;

    public JungleIG() {

        createWidget();
        pack();
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * methode creatWidget
     * dimmensionne panelConnect et panelInit
     */
    public void createWidget() {

        panConn = createPanelConnect();
        panConn.setSize(new Dimension(350, 100));
        panInit = createPanelInit();
        panInit.setPreferredSize(new Dimension(1200, 300));
        panParty = createPanelPlay();

        setContentPane(panConn);
    }

    /**
     * Méthode de création du panel de Connection
     * @return
     */
    private JPanel createPanelConnect() {

        JPanel panAll = new JPanel(new BorderLayout());

        JPanel panPseudo = new JPanel();
        textPseudo = new JTextField("", 20);
        textPseudo.setMaximumSize(textPseudo.getPreferredSize());
        panPseudo.add(new JLabel("Pseudo: "));
        panPseudo.add(textPseudo);

        JPanel panConn = new JPanel();
        textServerIP = new JTextField("127.0.0.1", 15);
        panConn.add(new JLabel("Server IP: "));
        panConn.add(textServerIP);

        butConnect = new JButton("Connect");
        butConnect.addActionListener(this);

        panAll.add(panPseudo, BorderLayout.NORTH);
        panAll.add(panConn, BorderLayout.CENTER);
        panAll.add(butConnect, BorderLayout.SOUTH);

        return panAll;
    }

    /**
     * Méthode de création du panelInit
     * @return
     */
    private JPanel createPanelInit() {

        JPanel panRight = new JPanel();
        panRight.setLayout(new BoxLayout(panRight, BoxLayout.Y_AXIS));

        butListParty = new JButton("Liste des parties");
        butListParty.addActionListener(this);

        JPanel panCreate = new JPanel();
        panCreate.setLayout(new BoxLayout(panCreate, BoxLayout.X_AXIS));
        textCreate = new JTextField("", 40);
        textCreate.setMaximumSize(textCreate.getPreferredSize());
        butCreateParty = new JButton("Creation de parties ");
        butCreateParty.addActionListener(this);
        SpinnerModel model = new SpinnerNumberModel(3, 2, 8, 1);
        spinNbPlayer = new JSpinner(model);
        spinNbPlayer.setMaximumSize(spinNbPlayer.getPreferredSize());
        panCreate.add(new JLabel("Nouveau nom de partie : "));
        panCreate.add(textCreate);
        panCreate.add(Box.createHorizontalStrut(20));
        panCreate.add(new JLabel("Nombres de joueurs : "));
        panCreate.add(spinNbPlayer);
        panCreate.add(butCreateParty);

        JPanel panJoin = new JPanel();
        panJoin.setLayout(new BoxLayout(panJoin, BoxLayout.X_AXIS));
        textJoin = new JTextField("", 2);
        textJoin.setMaximumSize(textJoin.getPreferredSize());
        butJoinParty = new JButton("Rejoindre la partie ");
        butJoinParty.addActionListener(this);
        panJoin.add(new JLabel("Num Partie : "));
        panJoin.add(textJoin);
        panJoin.add(butJoinParty);

        panRight.add(butListParty);
        panRight.add(panCreate);
        panRight.add(panJoin);
        panRight.add(Box.createVerticalGlue());


        textInfoInit = new JTextArea(20, 100);
        textInfoInit.setLineWrap(true);

        JScrollPane scroll = new JScrollPane(textInfoInit, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        JPanel panAll = new JPanel();
        panAll.setLayout(new BoxLayout(panAll, BoxLayout.X_AXIS));
        panAll.add(scroll);
        panAll.add(panRight);

        return panAll;
    }

    /**
     * Méthode de création du panel de Jeu
     * @return
     */
    private JPanel createPanelPlay() {

        JPanel panAll = new JPanel(new BorderLayout());

        textInfoParty = new JTextArea(20, 100);
        textInfoParty.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(textInfoParty, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        JPanel panPlay = new JPanel();
        textPlay = new JTextField("", 5);
        textPlay.setMaximumSize(textPlay.getPreferredSize());
        butPlay = new JButton("Jouer");
        butPlay.addActionListener(this);
        panPlay.add(new JLabel("Ordre :  "));
        panPlay.add(textPlay);
        enableOrder(false);
        JPanel panRight = new JPanel(new BorderLayout());
        panRight.add(panPlay, BorderLayout.CENTER);
        panRight.add(butPlay, BorderLayout.SOUTH);

        JPanel panMain = new JPanel();
        panMain.add(scroll);
        panMain.add(panRight);

        butQuit = new JButton("Quitter");
        butQuit.addActionListener(this);

        panAll.add(panMain, BorderLayout.CENTER);
        panAll.add(butQuit, BorderLayout.SOUTH);

        return panAll;
    }

    public void setConnectionPanel() {
        setContentPane(panConn);
        pack();
    }

    public void setInitPanel() {
        setContentPane(panInit);
        pack();
    }

    public void setPartyPanel() {
        setContentPane(panParty);
        pack();
    }

    public void enableOrder(boolean state) {
        textPlay.setEnabled(state);
        butPlay.setEnabled(state);

        if (state) {
            orderSent = false;
        }
    }

    /**
     * Methode de connection avec le serveur
     * @return
     * @throws IOException
     */
    public boolean serverConnection() throws IOException {

        boolean ok = false;
        // créer la connexion au serveur, ainsi que les flux uniquement si elle n'est pas active
        if (comm == null) {
            try {
                //Récupération de l'adresse du serveur
                comm = new Socket(textServerIP.getText(), 12345);

                oos = new ObjectOutputStream(comm.getOutputStream());
                ois = new ObjectInputStream(comm.getInputStream());

                // envoyer le pseudo du joueur
                oos.writeObject(textPseudo.getText());
                oos.flush();
                // lire un booléen -> ok
                ok = true;
            } catch (IOException e) {
                System.out.println("problème de connexion au serveur : (JungleIG)" + e.getMessage());
                System.exit(1);
            }
        }
        return ok;
    }


    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == butConnect) {
            try {
                boolean ok = serverConnection();
                if (ok) {
                    setInitPanel();
                } else {
                    System.out.println("Le pseudo existe déjà, choisissez en un autre.");
                }
            } catch (IOException err) {
                System.err.println("Problème de connection avec le serveur: " + err.getMessage() + "\n.Arrêt...");
                System.exit(1);
            }
        } else if (e.getSource() == butListParty) {
            try {
                // envoyer requête LIST PARTY
                oos.writeInt(JungleServer.REQ_LISTPARTY);
                oos.flush();

                // recevoir résultat et l'afficher dans textInfoInit
                System.out.println("flush");
                boolean pret = ois.readBoolean();
                System.out.println(pret);
                if (pret) {
                    String nomParty = (String) ois.readObject();
                    System.out.println("apres read");
                    textInfoInit.append(nomParty+" ");
                }

            } catch (ClassNotFoundException err) {
            } catch (IOException err) {
                System.err.println("Problème de connection avec le serveur: " + err.getMessage() + "\n.Arrêt...");
                System.exit(1);
            }

        } else if (e.getSource() == butCreateParty) {
            try {
                boolean ok;
                // envoyer requête CREATE PARTY (paramètres : nom partie et nb joueurs nécessaires)
                oos.writeInt(JungleServer.REQ_CREATEPARTY);
                oos.writeObject(textCreate.getText());
                int nbJoueurs = (Integer) spinNbPlayer.getValue();
                oos.writeInt(nbJoueurs);
                oos.flush();
                // recevoir résultat -> ok
                ok = ois.readBoolean();
                System.out.println(ok);
                // si ok == true :
                if (ok) {
                    // mettre le panneau party au centre
                    setPartyPanel();
                    // afficher un message dans textInfoParty comme quoi il faut attendre le début de partie
                    textInfoParty.append("Attendre le début de la partie");
                    // créer un ThreadClient et lancer son exécution
                    ThreadClient threadClient = new ThreadClient(this);
                    threadClient.start();
                }
            } catch (IOException err) {
                System.err.println("probleme de connection serveur : " + err.getMessage() + "\n.Aborting...");
                System.exit(1);
            }
        } else if (e.getSource() == butJoinParty) {

            try {
                int idPlayer;
                // envoyer requête JOIN PARTY (paramètres : numero partie)
                oos.writeInt(JungleServer.REQ_JOINPARTY);
                oos.flush();
                System.out.println("requete envoyée");
                int numPartie = Integer.parseInt(textJoin.getText());
                oos.writeInt(numPartie);
                oos.flush();
                System.out.println("numPartie flushé");
                // recevoir résultat -> idPlayer
                idPlayer = ois.readInt();
                System.out.println("idplayer reçu : " + idPlayer);
                // si idPlayer >= 1 :
                if (idPlayer >= 1) {
                    // mettre le panneau party au centre
                    setPartyPanel();
                    // afficher un message dans textInfoParty comme quoi il faut attendre le début de partie
                    textInfoParty.append("Attendre la début de la partie");
                    // créer un ThreadClient et lancer son exécution
                    System.out.println("avant démarrage du thread");
                    ThreadClient threadClient = new ThreadClient(this);
                    threadClient.start();
                    System.out.println("Apres le thread");
                }
            } catch (IOException err) {
                System.err.println("Problème de connection serveur: " + err.getMessage() + "\n.Arrêt...");
                System.exit(1);
            }
        } else if (e.getSource() == butPlay) {
            try {
                // envoyer requête PLAY (paramètre : contenu de textPlay)
                oos.writeInt(JungleServer.REQ_PLAY);
                oos.writeObject(textPlay.getText());
                oos.flush();
                orderSent = true;
                butPlay.setEnabled(false);
                textPlay.setEnabled(false);

                // mettre orderSent à true
                // bloquer le bouton play et le textfiled associé
            } catch (IOException err) {
                System.err.println("Problème connection serveur: " + err.getMessage() + "\n.Arrêt...");
                System.exit(1);
            }
        } else if (e.getSource() == butQuit) {
            try {
                oos.close();
                ois.close();
                setConnectionPanel();
                comm = null;
            } catch (IOException err) {
                System.err.println("Problème connection serveur: " + err.getMessage() + "\n.Arrêt...");
                System.exit(1);
            }
        }
    }
}
