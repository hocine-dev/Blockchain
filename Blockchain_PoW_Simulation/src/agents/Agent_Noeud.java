package agents;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import com.anylogic.engine.Agent;

/**
 * Agent_Noeud (Le Mineur / Validateur).
 * <p>
 * C'est l'entité centrale de la simulation. Chaque instance de cet agent représente un nœud du réseau
 * blockchain qui maintient une copie locale du registre, participe au consensus (Proof-of-Work)
 * et relaie les blocs validés aux voisins (Gossip Protocol).
 */
public class Agent_Noeud extends Agent {

    // --- 1. VARIABLES D'ÉTAT & MÉMOIRE ---
    public ArrayList<Block> blockchain = new ArrayList<>();
    public Block blockInProgress;
    public int currentNonce = 0;
    
    // --- 2. VARIABLES VISUELLES & UI ---
    public ShapeRectangle voyant_rect;      // Le rectangle qui flashe (Vert/Bleu/Orange)
    public Text indexText;                  // Affiche "noeud X"
    public Text chainLengthText;            // Affiche "Blocks: X"
    
    // --- 3. VARIABLES DE CONTRÔLE ---
    public boolean visuelDejaRecu = false;  // Protection anti-écho pour la visualisation


    // ========================================================================
    // [SECTION: Agent Actions -> On Startup]
    // Initialisation de la topologie et de la blockchain au lancement.
    // ========================================================================
    public void onStartup() {
        // --- TOPOLOGIE P2P ---
        // On veut que chaque nœud ait au moins 2 connexions pour la sécurité
        int connectionsTarget = 2; 
        int totalNodes = main.noeuds.size();

        // Tant que je n'ai pas assez d'amis...
        while (getConnections().size() < connectionsTarget) {
            
            // 1. Je choisis un nœud au hasard dans la liste
            int randomIndex = uniform_discr(0, totalNodes - 1);
            Agent potentialNeighbor = main.noeuds.get(randomIndex);
            
            // 2. Je vérifie :
            // - Que ce n'est pas moi-même
            // - Que je ne suis pas déjà connecté à lui
            if (potentialNeighbor != this && !isConnectedTo(potentialNeighbor)) {
                // je me connecte à lui
                connectTo(potentialNeighbor);
            }
        }

        // Récupère l'index de cet agent dans la population (0, 1, 2, ou 3)
        int index = getIndex(); 

        // Met à jour l'objet 'indexText' avec le bon numéro
        indexText.setText("noeud " + index);


        // 1. Créer le Bloc Genesis 
        Block genesisBlock = new Block("Bloc Genesis (Initial)", "0");
        genesisBlock.hash = "0000000000000000000000000000000000000000000000000000000000000001";

        // 2. L'ajouter à notre chaîne
        blockchain.add(genesisBlock);

        // 3. Mettre à jour l'affichage
        updateVisuals(); 
    }


    // ========================================================================
    // [SECTION: Statechart -> Transition 'Mining' -> Action]
    // Boucle de travail principale (Minage non-bloquant).
    // ========================================================================
    public void miningLoopAction() {
        // 1. Avons-nous un bloc sur lequel travailler ?
        if (blockInProgress == null) {
            // Non, alors créons-en un nouveau
            Block lastBlock = blockchain.get(blockchain.size() - 1);
            blockInProgress = new Block("Transactions du bloc " + (blockchain.size()), lastBlock.hash);
            currentNonce = 0; // Réinitialiser le compteur de tentatives
        }

        // 2. Faisons UNE tentative de minage en appelant notre nouvelle fonction
        String winningHash = blockInProgress.mineBlock(currentNonce); // On appelle mineBlock

        // 3. Vérifions si on a gagné (si le résultat n'est pas 'null')
        if (winningHash != null) {
            
            // --- ON A GAGNÉ ! ---
            traceln("Noeud " + getIndex() + ": J'AI MINÉ LE BLOC " + blockchain.size() + " !");
            
            flash(Color.GREEN);
            
            // b. L'ajouter à notre chaîne
            blockchain.add(blockInProgress);
            
            visuelDejaRecu = true; 
            create_EventResetVisuel(5, SECOND);
            
            
            // c. Le diffuser aux autres (logique conditionnelle)
            if (main.chk_simulerFork.isSelected()) {
                // CAS 1 : La case EST cochée - Envoyer avec délai random pour chaque nœud
                for (Agent a : getConnections()) {
                    noeud voisin = (noeud) a;
                        
                    // 1. Création (Cible = Voisin, Source = Moi)
                    Messager leMessager = main.add_messagers(voisin, this);
                    
                    // 2. Placement & Vitesse
                    leMessager.jumpTo(this.getX(), this.getY());
                    leMessager.setSpeed(150); 
                    
                    // 3. Départ
                    leMessager.moveTo(voisin);

                    // Délai aléatoire entre 0.5 et 3 secondes
                    double delaiAleatoire = uniform(1.0, 2.0);

                    // Planifie l’envoi avec délai
                    create_SendWithDelayEvent(
                        delaiAleatoire,      // délai
                        SECOND,              // unité
                        blockInProgress,     // paramètre message
                        a                    // paramètre destinataire
                    );
                }
            } else {
                // CAS 2 : La case N'EST PAS cochée - Envoyer à tous les nœuds sans delay
                for (Agent a : getConnections()) {
                    noeud voisin = (noeud) a;
                        
                    // 1. Création (Cible = Voisin, Source = Moi)
                    Messager leMessager = main.add_messagers(voisin, this);
                    
                    // 2. Placement & Vitesse
                    leMessager.jumpTo(this.getX(), this.getY());
                    leMessager.setSpeed(150); 
                    
                    // 3. Départ
                    leMessager.moveTo(voisin);
                    send(blockInProgress, a); // Envoi immédiat sans délai
                }
            }
            
            // d. Mettre à jour notre affichage
            updateVisuals();
            
            // e. Réinitialiser le travail (prêt pour le bloc suivant)
            blockInProgress = null;
            currentNonce = 0;

        } else {
            // --- ON A PERDU ---
            // On augmente juste le compteur pour la prochaine tentative
            currentNonce++;
        }
    }


    // ========================================================================
    // [SECTION: Communication -> On message received]
    // Cerveau du consensus : Validation, Acceptation et Relais.
    // ========================================================================
    public void onMessageReceived(Object msg, Agent sender) {
        if (msg instanceof Block) {
            Block receivedBlock = (Block) msg;

            // --- 1. PROTECTION ANTI-BOUCLE ---
            // Si j'ai déjà ce bloc dans ma chaîne, je l'ignore.
            // Cela empêche le message de tourner en rond dans l'anneau.
            for (Block b : blockchain) {
                if (b.hash.equals(receivedBlock.hash)) {
                    return; // STOP ! Je connais déjà.
                }
            }

            noeud expediteur = (noeud) sender;
            Block myLastBlock = blockchain.get(blockchain.size() - 1);

            // Recalcule le hash pour être sûr (validation)
            String calculatedHash = receivedBlock.calculateHash();
            boolean isValid = calculatedHash.equals(receivedBlock.hash) &&
                              receivedBlock.hash.substring(0, Block.difficulty).equals(Block.difficultyTarget);
            
            if (!isValid) {
                traceln("Noeud " + getIndex() + ": Bloc reçu de la part de Noeud " + expediteur.getIndex() + " est INVALIDE. REJETÉ.");
                flash(Color.ORANGE);
                return;
            }
            
            // CAS A : Le bloc s'ajoute parfaitement
            if (receivedBlock.previousHash.equals(myLastBlock.hash)) {
                blockchain.add(receivedBlock);
                traceln("Noeud " + getIndex() + ": Bloc " + (blockchain.size()-1) + " reçu de la part de Noeud " + expediteur.getIndex() + " et ACCEPTÉ.");
                updateVisuals();
                
                flash(Color.BLUE);
                
                // Abandonner notre travail actuel
                blockInProgress = null;
                currentNonce = 0;

                // --- 2. PROPAGATION (Le Relais) ---
                // Je transmets aux voisins sauf à celui qui me l'a envoyé
                for (Agent voisin : getConnections()) {
                    if (voisin != sender) {
                        if (main.chk_simulerFork.isSelected()) {
                            // Mode Latence : On utilise votre Événement Dynamique
                            double delai = uniform(0.5, 3.0);
                            create_SendWithDelayEvent(delai, SECOND, receivedBlock, voisin);
                        } else {
                            // Mode Instantané
                            send(receivedBlock, voisin);
                        }
                    }
                }
            
            // CAS B : C'est un FORK
            } else if (expediteur.blockchain.size() > this.blockchain.size()) {
                traceln("Noeud " + getIndex() + ": FORK DÉTECTÉ ! Chaîne de " + expediteur.getIndex() + " adoptée.");
                this.blockchain = new ArrayList<Block>(expediteur.blockchain);
                updateVisuals();
                flash(Color.YELLOW);
                
                // Abandonner notre travail actuel
                blockInProgress = null;
                currentNonce = 0;

                // --- 2. PROPAGATION DU FORK ---
                // Je transmets la nouvelle chaîne (le dernier bloc) aux voisins
                for (Agent voisin : getConnections()) {
                    if (voisin != sender) {
                        if (main.chk_simulerFork.isSelected()) {
                            // Mode Latence
                            double delai = uniform(0.5, 3.0);
                            create_SendWithDelayEvent(delai, SECOND, receivedBlock, voisin);
                        } else {
                            // Mode Instantané
                            send(receivedBlock, voisin);
                        }
                    }
                }
            }
        }
    }


    // ========================================================================
    // [SECTION: Fonctions Visuelles]
    // ========================================================================

    /** Met à jour l'affichage de la longueur de la chaîne sur le nœud. */
    void updateVisuals() {
        // 1. Met à jour le texte (code existant)
        int chainSize = blockchain.size();
        chainLengthText.setText("Blocks: " + chainSize);
    }

    /** Allume le voyant du nœud avec une couleur spécifique pendant 1 seconde. */
    void flash(Color c) {
        // 1. Allume le voyant avec la couleur donnée
        voyant_rect.setFillColor(c);

        // 2. Planifie de l'éteindre
        revertColorEvent.restart(1, SECOND);
    }
    
    /** Action exécutée par l'événement revertColorEvent */
    void revertColorAction() {
        // Remet le voyant en couleur "éteint" (gris foncé)
        voyant_rect.setFillColor(Color.DARK_GRAY);
    }
}