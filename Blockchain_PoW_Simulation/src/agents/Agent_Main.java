package agents;

import com.anylogic.engine.Agent;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

/**
 * Agent Main (L'Environnement).
 * <p>
 * C'est l'agent racine qui contient le réseau de nœuds, la population de messagers
 * et l'interface utilisateur (UI). Il joue le rôle d'orchestrateur.
 */
public class Agent_Main extends Agent {

    // --- POPULATIONS ---
    /** Liste de tous les mineurs du réseau. */
    public ArrayList<noeud> noeuds = new ArrayList<>();
    
    /** Liste des agents visuels (facteurs) en transit. */
    public ArrayList<Messager> messagers = new ArrayList<>();


    // --- INTERFACE UTILISATEUR (UI) ---
    
    /** Interrupteur pour activer/désactiver la latence réseau (simulation de Fork). */
    public CheckBox chk_simulerFork;
    
    /** Bouton pour déclencher manuellement une attaque de fraude. */
    public Button btn_attaque;


    // ========================================================================
    // [SECTION: Contrôles -> Button 'Diffuser Bloc Invalide' -> Action]
    // ========================================================================
    
    /**
     * Action déclenchée par le clic sur le bouton d'attaque.
     * Simule un "Mineur Paresseux" ou malveillant qui diffuse un bloc
     * sans avoir effectué le travail de hachage nécessaire.
     */
    public void onClick_BtnAttaque() {
        
        // 0. Créer un générateur de nombres aléatoires
        java.util.Random rand = new java.util.Random();

        // 1. On vérifie qu'il y a des nœuds dans le réseau
        if (noeuds.isEmpty()) return;

        // 2. On choisit un attaquant au HASARD
        int indexAttaquant = rand.nextInt(noeuds.size());
        noeud attaquant = (noeud) noeuds.get(indexAttaquant);

        // 3. Feedback Visuel : L'attaquant flashe en ROUGE
        attaquant.flash(Color.RED); 

        // 4. On prépare un faux bloc (hash invalide)
        // On se base sur le dernier bloc valide pour que la fraude soit subtile
        Block lastBlock = attaquant.blockchain.get(attaquant.blockchain.size() - 1);
        Block fakeBlock = new Block("FAUSSES TRANSACTIONS", lastBlock.hash);
        
        // LA FRAUDE : On force un hash qui ne correspond pas mathématiquement aux données + nonce
        fakeBlock.hash = "0000aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        // 5. L'ATTAQUE (Logique uniquement)
        traceln("--- ATTAQUE : Noeud " + indexAttaquant + " diffuse un FAUX BLOC ! ---");

        // On boucle sur les voisins de l'attaquant pour diffuser la fraude
        for (Agent a : attaquant.getConnections()) {
            
            noeud voisin = (noeud) a;

            // LOGIQUE D'ENVOI
            // On utilise le système d'événement dynamique pour gérer le délai d'envoi
            if (chk_simulerFork.isSelected()) {
                // Mode Latence (Réseau réaliste)
                double delai = uniform(1.0, 2.0);
                attaquant.create_SendWithDelayEvent(delai, SECOND, fakeBlock, voisin);
            } else {
                // Mode Instantané (simulation avec délai infime)
                attaquant.create_SendWithDelayEvent(0.001, SECOND, fakeBlock, voisin);
            }
        }
    }
}