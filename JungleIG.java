/* NOTE :
  Cette classe est incomplète : les commentaires sont là pour vous guider
 */


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;


class JungleIG extends JFrame implements ActionListener {

    /* attributes for communications */
    private Socket comm;
    public ObjectOutputStream oos;
    public ObjectInputStream ois;

    /* widgets for connection panel */
    protected JPanel panConn;
    protected JTextField textServerIP;
    protected JTextField textPseudo;
    protected JButton butConnect;

    /* widgets for "before party" panel */
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

    /* others attributes */
    public boolean orderSent; // set to false every time IG unblocks the paly button. set to true if play button is clicked while unlocked

    public JungleIG() {

        createWidget();
        pack();
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }


    public void createWidget() {

        panConn = createPanelConnect();
        panInit = createPanelInit();
        panParty = createPanelPlay();

        setContentPane(panConn);
    }

    private JPanel createPanelConnect() {

        JPanel panAll = new JPanel(new BorderLayout());

        JPanel panPseudo = new JPanel();
        textPseudo = new JTextField("",20);
        textPseudo.setMaximumSize(textPseudo.getPreferredSize());
        panPseudo.add(new JLabel("Pseudo: "));
        panPseudo.add(textPseudo);

        JPanel panConn = new JPanel();
        textServerIP = new JTextField("127.0.0.1",15);
        panConn.add(new JLabel("Server IP: "));
        panConn.add(textServerIP);

        butConnect = new JButton("Connect");
        butConnect.addActionListener(this);

        panAll.add(panPseudo,BorderLayout.NORTH);
        panAll.add(panConn,BorderLayout.CENTER);
        panAll.add(butConnect, BorderLayout.SOUTH);

        return panAll;
    }

    private JPanel createPanelInit() {

        JPanel panRight = new JPanel();
        panRight.setLayout(new BoxLayout(panRight, BoxLayout.Y_AXIS));

        butListParty = new JButton("List parties");
        butListParty.addActionListener(this);

        JPanel panCreate = new JPanel();
        panCreate.setLayout(new BoxLayout(panCreate, BoxLayout.X_AXIS));
        textCreate = new JTextField("",40);
        textCreate.setMaximumSize(textCreate.getPreferredSize());
        butCreateParty = new JButton("Create party");
        butCreateParty.addActionListener(this);
        SpinnerModel model = new SpinnerNumberModel(3, 2, 8 , 1);
        spinNbPlayer = new JSpinner(model);
        spinNbPlayer.setMaximumSize(spinNbPlayer.getPreferredSize());
        panCreate.add(new JLabel("new party name: "));
        panCreate.add(textCreate);
        panCreate.add(Box.createHorizontalStrut(20));
        panCreate.add(new JLabel("number of players: "));
        panCreate.add(spinNbPlayer);
        panCreate.add(butCreateParty);

        JPanel panJoin = new JPanel();
        panJoin.setLayout(new BoxLayout(panJoin, BoxLayout.X_AXIS));
        textJoin = new JTextField("",2);
        textJoin.setMaximumSize(textJoin.getPreferredSize());
        butJoinParty = new JButton("Join party");
        butJoinParty.addActionListener(this);
        panJoin.add(new JLabel("party number: "));
        panJoin.add(textJoin);
        panJoin.add(butJoinParty);

        panRight.add(butListParty);
        panRight.add(panCreate);
        panRight.add(panJoin);
        panRight.add(Box.createVerticalGlue());


        textInfoInit = new JTextArea(20,100);
        textInfoInit.setLineWrap(true);

        JScrollPane scroll = new JScrollPane (textInfoInit,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        JPanel panAll = new JPanel();
        panAll.setLayout(new BoxLayout(panAll, BoxLayout.X_AXIS));
        panAll.add(scroll);
        panAll.add(panRight);

        return panAll;
    }

    private JPanel createPanelPlay() {

        JPanel panAll = new JPanel(new BorderLayout());

        textInfoParty = new JTextArea(20,100);
        textInfoParty.setLineWrap(true);
        JScrollPane scroll = new JScrollPane (textInfoParty,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        JPanel panPlay = new JPanel();
        textPlay = new JTextField("",5);
        textPlay.setMaximumSize(textPlay.getPreferredSize());
        butPlay = new JButton("Play");
        butPlay.addActionListener(this);
        panPlay.add(new JLabel("order: "));
        panPlay.add(textPlay);
        enableOrder(false);
        JPanel panRight = new JPanel(new BorderLayout());
        panRight.add(panPlay,BorderLayout.CENTER);
        panRight.add(butPlay, BorderLayout.SOUTH);

        JPanel panMain = new JPanel();
        panMain.add(scroll);
        panMain.add(panRight);

        butQuit = new JButton("quit");
        butQuit.addActionListener(this);

        panAll.add(panMain,BorderLayout.CENTER);
        panAll.add(butQuit,BorderLayout.SOUTH);

        return panAll;
    }

    /* setConnectionPanel() : set the central widget of the window with connection panel */
    public void setConnectionPanel() {
        setContentPane(panConn);
        pack();
    }

    /* setConnectionPanel() : set the central widget of the window with "before party" panel */
    public void setInitPanel() {
        setContentPane(panInit);
        pack();
    }

    /* setConnectionPanel() : set the central widget of the window with party panel */
    public void setPartyPanel() {
        setContentPane(panParty);
        pack();
    }

    /* enableOrder() : enable/disable the play button and associated textfield */
    public void enableOrder(boolean state) {
        textPlay.setEnabled(state);
        butPlay.setEnabled(state);

        if (state == true) {
            orderSent = false;
        }
    }

    public boolean serverConnection() throws IOException {

        boolean ok = false;
        // créer la connexion au serveur, ainsi que les flux uniquement si elle n'est pas active (i.e. comm != null)
        // envoyer le pseudo du joueur
        // lire un booléen -> ok
        return ok;
    }


    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == butConnect) {
            try {
                boolean ok = serverConnection();
                if (ok) {
                    setInitPanel();
                }
                else {
                    System.out.println("pseudo is already taken. Choose another one and try again.");
                }
            }
            catch(IOException err) {
                System.err.println("pb with the connection to server: "+err.getMessage()+"\n.Aborting...");
                System.exit(1);
            }
        }
        else if (e.getSource() == butListParty) {
            try {

                // envoyer requête LIST PARTY
                // recevoir résultat et l'afficher dans textInfoInit

            }
            catch(ClassNotFoundException err ) {}
            catch(IOException err) {
                System.err.println("pb with the connection to server: "+err.getMessage()+"\n.Aborting...");
                System.exit(1);
            }

        }
        else if (e.getSource() == butCreateParty) {
            try {
                boolean ok;
                // envoyer requête CREATE PARTY (paramètres : nom partie et nb joueurs nécessaires)
                // recevoir résultat -> ok
                // si ok == true :
                //    mettre le panneau party au centre
                //    afficher un message dans textInfoParty comme quoi il faut attendre le début de partie
                //    créer un ThreadClient et lancer son exécution
            }
            catch(IOException err) {
                System.err.println("pb with the connection to server: "+err.getMessage()+"\n.Aborting...");
                System.exit(1);
            }
        }
        else if (e.getSource() == butJoinParty) {

            try {
                int idPlayer;
                // envoyer requête JOIN PARTY (paramètres : numero partie)
                // recevoir résultat -> idPlayer
                // si idPlayer >= 1 :
                //    mettre le panneau party au centre
                //    afficher un message dans textInfoParty comme quoi il faut attendre le début de partie
                //    créer un ThreadClient et lancer son exécution
            }
            catch(IOException err) {
                System.err.println("pb with the connection to server: "+err.getMessage()+"\n.Aborting...");
                System.exit(1);
            }
        }
        else if (e.getSource() == butPlay) {
            try {
                // envoyer requête PLAY (paramètre : contenu de textPlay)
                // mettre orderSent à true
                // bloquer le bouton play et le textfiled associé
            }
            catch(IOException err) {
                System.err.println("pb with the connection to server: "+err.getMessage()+"\n.Aborting...");
                System.exit(1);
            }
        }
        else if (e.getSource() == butQuit) {
            try {
                oos.close();
                ois.close();
                setConnectionPanel();
                comm = null;
            }
            catch(IOException err) {
                System.err.println("pb with the connection to server: "+err.getMessage()+"\n.Aborting...");
                System.exit(1);
            }
        }
    }
}
