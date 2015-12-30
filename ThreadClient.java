//FINI

import java.util.*;
import java.io.*;
import javax.swing.*;

class ThreadClient extends Thread {


    JungleIG ig;
    ObjectInputStream ois;
    ObjectOutputStream oos;

    public ThreadClient(JungleIG ig) {
        this.ig = ig;
        ois = ig.ois;
        oos = ig.oos;
    }

    public void run() {

        boolean stop = false;

        try {

            // recevoir booléen qui signale que le serveur est prêt
            stop = ois.readBoolean();
            System.out.println("Bool reçu " + stop);

            //Envoyer requête attendre départ de la partie
            oos.writeInt(JungleServer.REQ_WAITPARTYSTARTS);
            System.out.println("requête envoyée");
            // recevoir l'id pour la partie
            int idJoueur = ois.readInt();
            System.out.println("id reçue " + idJoueur);


            while (!stop) {

                // envoyer requête "attendre début tour"
                oos.writeInt(JungleServer.REQ_WAITTURNSTARTS);

                // recevoir id joueur courant
                int idJoueurCourant = ois.readInt();

                // si id joueur courant < 0, arreter thread
                if (idJoueurCourant < 0) {
                    interrupt();


                }
                // sinon si id joueur courant == mon id : afficher message dans ig
                else if (idJoueurCourant == idJoueur) {
                    ig.textInfoParty.add(new JLabel("C'est votre tour"));
                } else {
                    ig.textInfoParty.add(new JLabel("C'est au tour de " + idJoueur));
                }

                // recevoir la liste des cartes visibles et les afficher dans l'ig
                String visibles = (String) ois.readObject();
                ig.textInfoParty.add(new JLabel(visibles));

                // débloquer le champ de saisie + bouton jouer
                ig.textPlay.setEnabled(true);
                ig.butPlay.setEnabled(true);

                // attendre 3s
                wait(3000);


                boolean ordre = ig.orderSent;

                // bloquer le champ de saisie+bouton jouer
                ig.textPlay.setEnabled(false);
                ig.butPlay.setEnabled(false);

                // si pas d'odre envoyé pdt les 3s
                if (!ordre) {

                    // envoyer requête PLAY avec comme paramètre chaîne vide
                    oos.writeInt(JungleServer.REQ_PLAY);
                    oos.writeObject("");
                }

                // recevoir résultat du tour et l'afficher dans l'IG
                String resultTour = (String) ois.readObject();
                ig.textInfoParty.append(resultTour);

                // recevoir booléen = true si partie finie, false sinon
                stop = ois.readBoolean();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(null, "Partie terminée, retour au menu.");
        ig.setInitPanel();
    }
}
