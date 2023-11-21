import javax.swing.*;
import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class ApplicationEntreprise {
    private String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=Mama@0202";
    private Connection connection = null;
    static PreparedStatement connexionEntrepriseSql = null;
    static Scanner scanner = new Scanner(System.in);

    public void ProgrammePrincipal() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver PostgreSQL manquant !");
            System.exit(1);
        }
        try {
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println("Impossible de joindre le server !");
            System.exit(1);
        }
        try {
            connexionEntrepriseSql = connection.prepareStatement("SELECT * FROM Projet_BD2.connexionEntreprise(?,?);");
        } catch (SQLException e) {
            System.out.println("Impossible de préparer la requête !");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        ApplicationEntreprise app = new ApplicationEntreprise();
        app.ProgrammePrincipal();
    }

    public static boolean connexionEntreprises(){
        System.out.println("***************** connexion entreprise *****************");
        System.out.println("Veuillez entrer votre nom d'utilisateur");
        String email = scanner.nextLine();
        System.out.println("Veuillez entrer votre mot de passe");
        String password = scanner.nextLine();
        try {
            connexionEntrepriseSql.setString(1, email);
            connexionEntrepriseSql.setString(2, password);
            connexionEntrepriseSql.execute();

        } catch (SQLException se) {
            System.out.println("Erreur lors de l’insertion !");
            se.printStackTrace();
            applicationCentrale();
        }
        try {
            if (connexionEntrepriseSql.getResultSet().next()) {
                System.out.println("Connexion réussie");
                applicationCentrale();
            } else {
                System.out.println("Connexion échouée");
                connexionEntreprises();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void applicationCentrale() {
        System.out.println("***************** application entreprise *****************");
        System.out.println("1 :Encoder une offre de stage");
        System.out.println("2 :Voir les mots-clés disponibles pour une offre de stage");
        System.out.println("3 :Ajouter un mot-clé à une de ses offres de stage");
        System.out.println("4 :Voir ses offres de stages");
        System.out.println("5 :Voir les candidatures pour une de ses offres de stages");
        System.out.println("6 :Sélectionner un étudiant pour une de ses offres de stage");
        System.out.println("7 :Annuler une offre de stage en donnant son code");
        int choix = scanner.nextInt();
        switch (choix) {
            case 1:
                encoderOffre();
                break;
            case 2:
                voirLesMotsCLefs();
                break;
            case 3:
                ajouterMotClefs();
                break;
            case 4:
                voirSesOffres();
                break;
            case 5:
                voirSesCandidature();
                break;
            case 6:
                selectionnerEtudiant();
                break;
            case 7:
                annulerOffreDeStage();
                break;
        }


    }




    private static void encoderOffre() {
    }
    private static void ajouterMotClefs() {
    }
    private static void voirLesMotsCLefs() {
    }
    private static void voirSesOffres() {
    }
    private static void voirSesCandidature() {
    }
    private static void selectionnerEtudiant() {
    }
    private static void annulerOffreDeStage() {
    }
}
