package com.example.jpa.test;

import com.example.jpa.entities.Salle;
import com.example.jpa.entities.Utilisateur;
import com.example.jpa.service.ServiceGestion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ServiceGestionTest {

    private ServiceGestion service;

    @Before
    public void setUp() {
        service = new ServiceGestion();
    }

    @After
    public void tearDown() {
        if (service != null) {
            service.fermer();
        }
    }

    @Test
    public void testCreationEtRechercheSalle() {
        Salle salle = new Salle("Salle Test", 20);
        service.ajouterSalle(salle);

        List<Salle> salles = service.listerSalles();
        assertFalse(salles.isEmpty());
        // On vérifie que la salle insérée est bien présente
        boolean trouve = salles.stream().anyMatch(s -> "Salle Test".equals(s.getNom()));
        assertTrue("La salle créée doit être présente dans la liste", trouve);
    }

    @Test
    public void testCreationUtilisateurAvecSalle() {
        Salle salle = new Salle("Salle B", 15);
        service.ajouterSalle(salle);

        Utilisateur utilisateur = new Utilisateur("Alice", "alice@example.com", salle);
        service.ajouterUtilisateur(utilisateur);

        List<Utilisateur> utilisateurs = service.listerUtilisateurs();
        assertFalse(utilisateurs.isEmpty());

        Utilisateur trouve = utilisateurs.stream()
                .filter(u -> "alice@example.com".equals(u.getEmail()))
                .findFirst()
                .orElse(null);

        assertNotNull("L'utilisateur Alice doit exister", trouve);
        assertEquals("Alice", trouve.getNom());
        assertNotNull("L'utilisateur doit être lié à une salle", trouve.getSalle());
        assertEquals("Salle B", trouve.getSalle().getNom());
    }

    @Test
    public void testRechercheUtilisateursParSalle() {
        Salle salle = new Salle("Salle C", 30);
        service.ajouterSalle(salle);

        Utilisateur u1 = new Utilisateur("Bob", "bob@example.com", salle);
        Utilisateur u2 = new Utilisateur("Carol", "carol@example.com", salle);
        service.ajouterUtilisateur(u1);
        service.ajouterUtilisateur(u2);

        List<Utilisateur> utilisateurs = service.utilisateursParSalle("Salle C");
        assertEquals(2, utilisateurs.size());
    }

    @Test
    public void testFiltreCapaciteMin() {
        Salle petite = new Salle("Petite Réunion", 6);
        Salle grande = new Salle("Grand Amphi", 120);
        service.ajouterSalle(petite);
        service.ajouterSalle(grande);

        List<Salle> grandesSalles = service.sallesParCapaciteMin(50);
        assertTrue("L'amphi doit ressortir dans le filtre capacite >= 50",
                grandesSalles.stream().anyMatch(s -> "Grand Amphi".equals(s.getNom())));
        assertFalse("La petite salle ne doit pas figurer dans le filtre capacite >= 50",
                grandesSalles.stream().anyMatch(s -> "Petite Réunion".equals(s.getNom())));
    }

    @Test
    public void testRechercheUtilisateurParPatternNom() {
        Salle salle = new Salle("Salle D", 10);
        service.ajouterSalle(salle);

        Utilisateur u1 = new Utilisateur("Jean Dupont", "jean.dupont@test.fr", salle);
        Utilisateur u2 = new Utilisateur("Claire Martin", "claire.martin@test.fr", salle);
        service.ajouterUtilisateur(u1);
        service.ajouterUtilisateur(u2);

        List<Utilisateur> resultats = service.rechercherUtilisateursParNom("Jean");
        assertEquals(1, resultats.size());
        assertEquals("Jean Dupont", resultats.get(0).getNom());
    }

    @Test(expected = Exception.class)
    public void testContrainteUniciteEmailEtRollback() {
        Salle salle = new Salle("Salle E", 8);
        service.ajouterSalle(salle);

        Utilisateur u1 = new Utilisateur("Marc", "doublon@test.fr", salle);
        service.ajouterUtilisateur(u1);

        // Cette deuxième insertion viole la contrainte unique sur l'email
        // Elle doit déclencher une exception et annuler la transaction (rollback)
        Utilisateur u2 = new Utilisateur("Autre Marc", "doublon@test.fr", salle);
        service.ajouterUtilisateur(u2);
    }
}
