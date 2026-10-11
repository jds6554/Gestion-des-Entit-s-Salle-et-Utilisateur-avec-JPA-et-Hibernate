package com.example.jpa.service;

import com.example.jpa.dao.SalleDao;
import com.example.jpa.dao.UtilisateurDao;
import com.example.jpa.entities.Salle;
import com.example.jpa.entities.Utilisateur;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;

public class ServiceGestion {

    private EntityManagerFactory emf;
    private EntityManager em;
    private SalleDao salleDao;
    private UtilisateurDao utilisateurDao;

    public ServiceGestion() {
        emf = Persistence.createEntityManagerFactory("gestionPU");
        em = emf.createEntityManager();
        salleDao = new SalleDao(em);
        utilisateurDao = new UtilisateurDao(em);
    }

    public void fermer() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    public void ajouterSalle(Salle salle) {
        salleDao.create(salle);
    }

    public void ajouterUtilisateur(Utilisateur utilisateur) {
        utilisateurDao.create(utilisateur);
    }

    public List<Salle> listerSalles() {
        return salleDao.findAll();
    }

    public List<Utilisateur> listerUtilisateurs() {
        return utilisateurDao.findAll();
    }

    public List<Utilisateur> utilisateursParSalle(String nomSalle) {
        return utilisateurDao.findBySalleNom(nomSalle);
    }

    public List<Salle> sallesParCapaciteMin(int capaciteMin) {
        return salleDao.findByCapaciteMin(capaciteMin);
    }

    public List<Utilisateur> rechercherUtilisateursParNom(String pattern) {
        return utilisateurDao.findByNomContient(pattern);
    }

    public Salle trouverSalle(Long id) {
        return salleDao.find(id);
    }

    public Utilisateur trouverUtilisateur(Long id) {
        return utilisateurDao.find(id);
    }

    public void modifierSalle(Salle salle) {
        salleDao.update(salle);
    }

    public void supprimerSalle(Salle salle) {
        salleDao.delete(salle);
    }
}
