package agents;

import com.anylogic.engine.Agent;

/**
 * Agent_Messager (Le Facteur Visuel).
 * <p>
 * Agent mobile à durée de vie courte. Il représente visuellement un paquet de données (un Bloc)
 * transitant d'un nœud à un autre sur le réseau. Il permet de visualiser la latence
 * et la propagation en "vague".
 */
public class Agent_Messager extends Agent {

    // --- PARAMÈTRES ---
    
    /** Le nœud destinataire vers lequel le messager se déplace. */
    public Agent cible;
    
    /** Le nœud expéditeur (pour référence). */
    public noeud source;
    
    /** La charge utile (le bloc transporté). */
    public Block contenu;

    // ========================================================================
    // [SECTION: Agent Actions -> On arrival to target location]
    // Action exécutée lorsque le messager atteint physiquement sa destination.
    // ========================================================================
    public void onArrival() {
        // 1. Livraison du message
        // Le facteur déclenche la réception du message chez le destinataire.
        // C'est ce qui synchronise l'événement visuel (contact) avec l'événement logique.
        send(contenu, cible);

        // 2. Suppression
        // Une fois la mission accomplie, l'agent est détruit pour libérer la mémoire.
        main.remove_messagers(this);
    }
}