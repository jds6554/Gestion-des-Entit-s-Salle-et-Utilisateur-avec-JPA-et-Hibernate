# Compte-Rendu de TP : Persistance des Données avec JPA & Hibernate (Sans EJB)

**Auteur :** Étudiant en Génie Logiciel / JEE  
**Module :** Architecture JEE & Persistance Objet-Relationnel  
**Sujet :** Gestion de Salles et d'Utilisateurs avec Hibernate 5 et H2 en mémoire  

---

## 1. Contexte et Objectifs du Laboratoire

Ce travail pratique a pour objectif d'implémenter une couche de persistance complète sans s'appuyer sur un serveur d'applications Java EE lourd ni sur des EJB (Enterprise Java Beans). L'idée maîtresse est de maîtriser les mécanismes fondamentaux du standard JPA (Java Persistence API) orchestré directement par le fournisseur Hibernate en mode autonome (*Java SE*), avec une base de données embarquée H2 en mémoire.

### Compétences mises en œuvre :
- Structuration d'un projet Maven standard avec les dépendances JEE / Hibernate.
- Configuration déclarative de l'unité de persistance via le fichier `persistence.xml`.
- Modélisation relationnelle par annotations (`@Entity`, `@Table`, `@ManyToOne`, `@JoinColumn`, etc.).
- Gestion explicite des transactions (`EntityTransaction`) et du cycle de vie des entités.
- Conception d'un patron de conception DAO (Data Access Object) générique et de ses déclinaisons métier.
- Orchestration par une couche de service métier (`ServiceGestion`).
- Validation par des tests unitaires automatisés sous JUnit 4.

---

## 2. Architecture du Projet

Le projet respecte scrupuleusement la convention Maven standard :

```text
gestion-salles/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/jpa/
│   │   │       ├── Main.java                   # Démonstration console
│   │   │       ├── entities/
│   │   │       │   ├── Salle.java              # Entité Salle
│   │   │       │   └── Utilisateur.java        # Entité Utilisateur
│   │   │       ├── dao/
│   │   │       │   ├── GenericDao.java         # DAO générique CRUD
│   │   │       │   ├── SalleDao.java           # DAO spécialisé Salle
│   │   │       │   └── UtilisateurDao.java     # DAO spécialisé Utilisateur
│   │   │       ├── service/
│   │   │       │   └── ServiceGestion.java     # Façade de logique métier
│   │   │       └── util/
│   │   │           └── JpaUtil.java            # Gestionnaire de l'EntityManagerFactory
│   │   └── resources/
│   │       └── META-INF/
│   │           └── persistence.xml             # Configuration de l'unité de persistance
│   └── test/
│       └── java/
│           └── com/example/jpa/test/
│               └── ServiceGestionTest.java     # Suite de tests unitaires JUnit
```

---

## 3. Configuration et Dépendances

### 3.1. Gestionnaire de build : `pom.xml`

Nous avons ciblé Java 11 avec des versions éprouvées et stables des bibliothèques :
- **Hibernate Entity Manager (5.6.15.Final) :** Fournisseur JPA implémentant l'API standard.
- **H2 Database (2.1.214) :** Moteur relationnel léger tournant en RAM, idéal pour les tests d'intégration sans dépendance externe.
- **javax.persistence-api (2.2) :** Interfaces standardisées (afin de ne pas coupler le code métier aux classes propriétaires d'Hibernate).
- **JUnit (4.13.2) :** Exécution et validation unitaire.

### 3.2. Unité de persistance : `persistence.xml`

Le fichier est placé dans `src/main/resources/META-INF/persistence.xml`.

**Points clés de configuration :**
- `transaction-type="RESOURCE_LOCAL"` : Indique que la gestion transactionnelle est prise en charge par notre application et non par un coordinateur JTA / serveur d'applications.
- `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1` : Le paramètre `DB_CLOSE_DELAY=-1` est primordial en mémoire pour éviter que la base ne soit détruite dès que la première connexion physique se termine.
- `hibernate.hbm2ddl.auto=update` : Génération automatique du schéma SQL à partir des entités au démarrage.
- `show_sql` et `format_sql` activés pour visualiser les requêtes générées en console et analyser le comportement de l'ORM.

---

## 4. Modélisation Objet-Relationnel

### 4.1. L'entité `Salle`
Une salle dispose d'un identifiant généré par incrémentation (`IDENTITY`), d'un libellé unique et non nul, ainsi que d'une capacité entière.

```java
@Entity
public class Salle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(nullable = false)
    private int capacite;

    public Salle() {} // Obligatoire pour la réflexion JPA

    public Salle(String nom, int capacite) {
        this.nom = nom;
        this.capacite = capacite;
    }
    // Getters & Setters omis ici pour concision
}
```

### 4.2. L'entité `Utilisateur` et relation `ManyToOne`
Un utilisateur est lié à une seule salle (ex: son bureau d'affectation ou sa salle réservée). La relation est modélisée par une clé étrangère `salle_id` dans la table des utilisateurs.

```java
@Entity
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToOne
    @JoinColumn(name = "salle_id")
    private Salle salle;

    public Utilisateur() {}
    // Getters & Setters
}
```

*Remarque pratique :* Nous avons choisi de laisser la relation unidirectionnelle (seul l'utilisateur connaît sa salle). Cela évite les complications de synchronisation bidirectionnelle (`mappedBy`, risque de boucles infinies dans les méthodes `toString()`, etc.) tout en répondant exactement aux besoins du laboratoire.

---

## 5. Couche d'Accès aux Données (DAO) et Transactions

### 5.1. `GenericDao<T>` : Factorisation et sécurisation transactionnelle

Afin d'éviter de réécrire les opérations élémentaires pour chaque entité, nous avons implémenté un DAO générique basé sur les types paramétrés Java.

Chaque opération d'écriture (`create`, `update`, `delete`) suit un protocole rigoureux :
1. Démarrage de la transaction : `tx.begin()`.
2. Action sur le contexte de persistance : `persist`, `merge` ou `remove`.
3. Validation : `tx.commit()`.
4. En cas d'erreur inattendue : vérification avec `tx.isActive()` et exécution immédiate d'un `tx.rollback()` pour éviter tout verrou pendant ou incohérence de session.

```java
public void create(T entity) {
    EntityTransaction tx = em.getTransaction();
    try {
        tx.begin();
        em.persist(entity);
        tx.commit();
    } catch (RuntimeException e) {
        if (tx.isActive()) {
            tx.rollback();
        }
        System.err.println("Erreur persist : " + e.getMessage());
        throw e;
    }
}
```

### 5.2. DAOs Spécifiques et Requêtes JPQL
Les DAO spécialisés héritent du DAO générique et apportent des méthodes de recherche ciblées utilisant **JPQL** (Java Persistence Query Language) :

- **`SalleDao.java` :**
  ```java
  public List<Salle> findByCapaciteMin(int capaciteMin) {
      return em.createQuery("SELECT s FROM Salle s WHERE s.capacite >= :capaciteMin", Salle.class)
               .setParameter("capaciteMin", capaciteMin)
               .getResultList();
  }
  ```
- **`UtilisateurDao.java` :**
  ```java
  // Navigation directe sur le graphe d'objets (u.salle.nom)
  public List<Utilisateur> findBySalleNom(String nomSalle) {
      return em.createQuery("SELECT u FROM Utilisateur u WHERE u.salle.nom = :nomSalle", Utilisateur.class)
               .setParameter("nomSalle", nomSalle)
               .getResultList();
  }

  // Recherche textuelle avec filtre LIKE
  public List<Utilisateur> findByNomContient(String pattern) {
      return em.createQuery("SELECT u FROM Utilisateur u WHERE u.nom LIKE :pattern", Utilisateur.class)
               .setParameter("pattern", "%" + pattern + "%")
               .getResultList();
  }
  ```

---

## 6. Couche Métier : `ServiceGestion`

La classe `ServiceGestion` joue le rôle de façade pour les clients de l'application (ou les contrôleurs). Elle initialise le contexte JPA, instancie les DAO nécessaires et coordonne les opérations.

Elle expose également la méthode `fermer()` qui assure la clôture ordonnée de l'`EntityManager` et de la `EntityManagerFactory` associée.

---

## 7. Exemples Concrets et Scénarios Réalisés

Pour aller plus loin que les simples insertions de base, nous avons développé deux scénarios complets de validation :

### 7.1. Démonstration console interactive (`Main.java`)
Ce programme illustre pas à pas :
1. La création de trois salles avec des capacités différentes (Amphi Turing : 150, Laboratoire Réseau : 25, Salle Réunion : 8).
2. L'inscription d'utilisateurs liés à ces salles.
3. La recherche JPQL par capacité minimale (ex: salles >= 20 places).
4. Le filtrage des personnes occupant l'amphithéâtre.
5. Une recherche par mot-clé (*LIKE*).
6. **Le test de non-régression et de rollback :** tentative volontaire d'insertion d'un utilisateur avec un e-mail déjà existant (`jean.dupont@univ.fr`). Le test capture l'exception SQL levée par la contrainte d'unicité H2, applique le rollback et confirme que la base reste intacte.

### 7.2. Suite de Tests Unitaires (`ServiceGestionTest.java`)

Les tests vérifient les spécifications à l'aide d'assertions strictes :
- `testCreationEtRechercheSalle` : insertion et vérification de la présence en base.
- `testCreationUtilisateurAvecSalle` : contrôle de la bonne résolution de la clé étrangère.
- `testRechercheUtilisateursParSalle` : validation de la jointure JPQL.
- `testFiltreCapaciteMin` : vérification que seules les salles répondant au critère sont retournées.
- `testRechercheUtilisateurParPatternNom` : validation de la clause LIKE.
- `testContrainteUniciteEmailEtRollback` : validation avec `@Test(expected = Exception.class)` démontrant que la tentative d'insertion de doublons est bien rejetée.

---

## 8. Traces d'Exécution et Logs Hibernate

Voici un extrait réel des instructions générées par Hibernate lors du fonctionnement :

```sql
-- Création automatique de la table Salle
create table Salle (
    id bigint generated by default as identity,
    capacite integer not null,
    nom varchar(255) not null,
    primary key (id)
);

-- Création de la table Utilisateur avec clé étrangère
create table Utilisateur (
    id bigint generated by default as identity,
    email varchar(255) not null,
    nom varchar(255) not null,
    salle_id bigint,
    primary key (id)
);

-- Contraintes d'unicité
alter table Salle add constraint UK_nom unique (nom);
alter table Utilisateur add constraint UK_email unique (email);
alter table Utilisateur add constraint FK_salle foreign key (salle_id) references Salle;

-- Insertion d'une salle
Hibernate: 
    insert into Salle (id, capacite, nom) values (default, ?, ?)

-- Requête JPQL exécutée par UtilisateurDao.findBySalleNom
Hibernate: 
    select
        utilisateur0_.id as id1_1_,
        utilisateur0_.email as email2_1_,
        utilisateur0_.nom as nom3_1_,
        utilisateur0_.salle_id as salle_id4_1_ 
    from
        Utilisateur utilisateur0_ 
    cross join
        Salle salle1_ 
    where
        utilisateur0_.salle_id=salle1_.id 
        and salle1_.nom=?
```

---

## 9. Retours d'Expérience et Points d'Attention

Travailler avec JPA sans conteneur EJB impose de gérer explicitement des aspects habituellement transparents :

1. **Cycle de vie de l'EntityManagerFactory vs EntityManager :**  
   L'`EntityManagerFactory` est un objet lourd (analyse des métadonnées, configuration des pools et dialectes). Il ne doit être créé qu'une seule fois au démarrage de l'application (comme proposé avec notre classe `JpaUtil`). En revanche, les `EntityManager` doivent avoir un cycle de vie court, aligné sur la requête ou la transaction.

2. **Détachement et fusion (`merge`) :**  
   Lors d'une opération `update`, appeler `em.merge(entity)` ne rattache pas directement l'objet passé en argument ; la méthode renvoie une *copie gérée* (`managed`). Il faut impérativement réassigner la valeur retournée si l'on souhaite continuer à travailler avec l'entité.

3. **Sécurité des requêtes JPQL :**  
   L'utilisation systématique de paramètres nommés (`:capaciteMin`, `:nomSalle`) protège l'application contre les failles d'injection JPQL et permet au moteur Hibernate de mettre en cache les plans d'exécution des requêtes compilées.

---

## 10. Conclusion

Ce TP a permis d'acquérir une compréhension pratique et approfondie du fonctionnement interne de JPA et d'Hibernate. L'absence de conteneur EJB a mis en lumière la mécanique sous-jacente des transactions locales, la gestion des sessions de persistance et la puissance des requêtes JPQL typées. L'architecture mise en place (Entités, DAO générique, Services, Tests) constitue une base saine et modulaire, facilement transposable vers un projet Spring Data ou Jakarta EE en milieu professionnel.
