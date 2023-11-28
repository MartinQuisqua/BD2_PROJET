import java.sql.*;
import java.util.Scanner;

public class ApplicationEntreprise {
    private String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=Mama@0202";
    private Connection connection = null;
    static PreparedStatement connexionEntrepriseSql = null;
    static PreparedStatement encoderOffreStage = null;
    static Scanner scanner = new Scanner(System.in);
    static String idEntreprise;

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
            connexionEntrepriseSql = connection.prepareStatement("SELECT mdp_hash, code FROM Projet_BD2.entreprises WHERE email = ?;");
        } catch (SQLException e) {
            System.out.println("Impossible de préparer la requête !");
            System.exit(1);
        }
        try {
            encoderOffreStage = connection.prepareStatement("Projet_BD2.ajouterStage(?,?,?);");
        } catch (SQLException e) {
            System.out.println("Impossible de préparer la requête !");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        ApplicationEntreprise app = new ApplicationEntreprise();
        app.ProgrammePrincipal();
        connexionEntreprises();
    }
    public static void connexionEntreprises() {
        System.out.println("***************** Connexion entreprise *****************");
        System.out.println("Veuillez entrer votre nom d'utilisateur");
        String email = "Entreprise1.test@Entreprise.test"; //scanner.nextLine(); // TODO: 07/05/2021 "
        System.out.println("Veuillez entrer votre mot de passe");
        String passwordUser = scanner.nextLine();

        try {
            connexionEntrepriseSql.setString(1, email);
            connexionEntrepriseSql.execute();
            ResultSet rs = connexionEntrepriseSql.getResultSet();

            if (rs.next()) {
                String hashedPasswordFromDB = rs.getString(1);

                if (BCrypt.checkpw(passwordUser, hashedPasswordFromDB)) {
                    idEntreprise = rs.getString(2);
                    System.out.println("Connexion réussie !");
                    System.out.println();
                    applicationCentrale();
                } else {
                    System.out.println("Email ou mot de passe incorrect ! Veuillez réessayer.");
                    connexionEntreprises();
                }
            } else {
                System.out.println("Email ou mot de passe incorrect ! Veuillez réessayer.");
                connexionEntreprises();
            }

        } catch (SQLException se) {
            System.out.println("Erreur lors de la connexion !");
            se.printStackTrace();
            connexionEntreprises();
        }
    }

    public static void applicationCentrale() {
        System.out.println("idEntreprise : " + idEntreprise);
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
