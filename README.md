# Gestion des Entités Salle et Utilisateur avec JPA et Hibernate

Projet Java / JEE démontrant la modélisation et la gestion d'entités avec JPA 2.2, Hibernate 5.6 et une base H2 en mémoire sans conteneur EJB.

## Structure du projet

- `src/main/java/com/example/jpa/entities/` : Entités `Salle` et `Utilisateur`
- `src/main/java/com/example/jpa/dao/` : DAO générique et spécialisés (`SalleDao`, `UtilisateurDao`)
- `src/main/java/com/example/jpa/service/` : Service métier (`ServiceGestion`)
- `src/main/java/com/example/jpa/util/` : Utilitaire JPA (`JpaUtil`)
- `src/main/java/com/example/jpa/Main.java` : Application de démonstration en console
- `src/main/resources/META-INF/persistence.xml` : Configuration JPA / Hibernate / H2
- `src/test/java/` : Tests unitaires JUnit 4
- `COMPTE_RENDU_TP.md` : Compte-rendu détaillé du TP

## Prérequis

- Java 11 ou version supérieure
- Apache Maven

## Compilation et exécution

Exécuter les tests unitaires :
```bash
mvn test
```

Lancer l'application de démonstration :
```bash
mvn compile exec:java -Dexec.mainClass="com.example.jpa.Main"
```
