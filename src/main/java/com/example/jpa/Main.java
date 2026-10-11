package com.example.jpa;

import com.example.jpa.entities.Salle;
import com.example.jpa.entities.Utilisateur;
import com.example.jpa.service.ServiceGestion;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== DEMARRAGE DU TP JPA / HIBERNATE ===");

        ServiceGestion service = new ServiceGestion();

        try {
            // 1. Insertion de salles
            System.out.println("\n--- 1. Insertion des salles ---");
            Salle amphi = new Salle("Amphithéâtre Turing", 150);
            Salle labo = new Salle("Laboratoire Réseau", 25);
            Salle reunion = new Salle("Salle Réunion A", 8);

            service.ajouterSalle(amphi);
            service.ajouterSalle(labo);
            service.ajouterSalle(reunion);

            System.out.println("Salles créées :");
            for (Salle s : service.listerSalles()) {
                System.out.println("  -> " + s);
            }

            // 2. Insertion des utilisateurs avec rattachement
            System.out.println("\n--- 2. Insertion des utilisateurs ---");
            Utilisateur u1 = new Utilisateur("Jean Dupont", "jean.dupont@univ.fr", amphi);
            Utilisateur u2 = new Utilisateur("Alice Martin", "alice.martin@univ.fr", amphi);
            Utilisateur u3 = new Utilisateur("Karim Benzema", "karim.benzema@univ.fr", labo);

            service.ajouterUtilisateur(u1);
            service.ajouterUtilisateur(u2);
            service.ajouterUtilisateur(u3);

            System.out.println("Utilisateurs enregistrés :");
            for (Utilisateur u : service.listerUtilisateurs()) {
                System.out.println("  -> " + u);
            }

            // 3. Filtrage JPQL par capacité minimale
            System.out.println("\n--- 3. Requête JPQL : Salles avec capacité >= 20 ---");
            List<Salle> grandesSalles = service.sallesParCapaciteMin(20);
            for (Salle s : grandesSalles) {
                System.out.println("  -> " + s.getNom() + " (Capacité : " + s.getCapacite() + " places)");
            }

            // 4. Filtrage JPQL des utilisateurs par salle
            System.out.println("\n--- 4. Requête JPQL : Utilisateurs de l'Amphithéâtre Turing ---");
            List<Utilisateur> inscritsTuring = service.utilisateursParSalle("Amphithéâtre Turing");
            for (Utilisateur u : inscritsTuring) {
                System.out.println("  -> " + u.getNom() + " (" + u.getEmail() + ")");
            }

            // 5. Recherche par motif LIKE
            System.out.println("\n--- 5. Requête JPQL : Recherche d'utilisateurs contenant 'Martin' ---");
            List<Utilisateur> resultatLike = service.rechercherUtilisateursParNom("Martin");
            for (Utilisateur u : resultatLike) {
                System.out.println("  -> Trouvé : " + u.getNom());
            }

            // 6. Test d'erreur contrôlé (unicité d'email)
            System.out.println("\n--- 6. Test de gestion d'erreur (unicité email) ---");
            try {
                Utilisateur doublon = new Utilisateur("Doublon", "jean.dupont@univ.fr", reunion);
                service.ajouterUtilisateur(doublon);
            } catch (Exception ex) {
                System.out.println(">> Succès du test : L'exception a bien été interceptée et la transaction a été annulée (rollback).");
            }

        } finally {
            // Nettoyage et libération des ressources
            service.fermer();
            System.out.println("\n=== FIN DE L'EXECUTION - RESSOURCES FERMEES ===");
        }
    }
}
