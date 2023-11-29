import java.sql.*;
import java.util.Scanner;

public class ApplicationEntreprise {
    private String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=Mama@0202";
    private Connection connection = null;
    private PreparedStatement connexionEntrepriseSql = null;
    private PreparedStatement encoderOffreStage = null;
    private PreparedStatement affichageMotClefs = null;
    private PreparedStatement ajouterMotClefs = null;
    private PreparedStatement voirSesOffres = null;
    private Scanner scanner = new Scanner(System.in);
    private String idEntreprise;

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
            encoderOffreStage = connection.prepareStatement("SELECT Projet_BD2.ajouterStage(?,?,?);");
            affichageMotClefs = connection.prepareStatement("SELECT * FROM Projet_BD2.affichage_mot_clefs;");
            ajouterMotClefs = connection.prepareStatement("SELECT Projet_BD2.ajouterMotClef(?,?,?);");
            voirSesOffres = connection.prepareStatement("SELECT * FROM Projet_BD2.voir_offres_des_stages WHERE code_entreprise = ?;");
        } catch (SQLException e) {
            System.out.println("Impossible de préparer la requête !");
            System.exit(1);
        }

    }

    public static void main(String[] args) {
        ApplicationEntreprise app = new ApplicationEntreprise();
        app.ProgrammePrincipal();
        app.connexionEntreprises();
    }
    public void connexionEntreprises() {
        System.out.println("***************** Connexion entreprise *****************");
        System.out.println("Veuillez entrer votre nom d'utilisateur");
        String email = "Entreprise3@gmail.test"; //scanner.nextLine(); // TODO: 07/05/2021 "
        System.out.println("Veuillez entrer votre mot de passe");
        // String passwordUser = scanner.nextLine();
        String passwordUser = "Entreprisetest"; // TODO: 07/05/2021

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

    public void applicationCentrale() {
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
        scanner.nextLine();
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




    private void encoderOffre() {
        System.out.println("***************** Encoder une offre de stage *****************");
        System.out.println("Veuillez entrer une description de l'offre de stage :");
        String description = scanner.nextLine();
        System.out.println("description : " + description);
        System.out.println("Veuillez entrer le semestre de l'offre de stage");
        String semestre = scanner.nextLine();
        try {
            encoderOffreStage.setString(1, idEntreprise);
            encoderOffreStage.setString(2, description);
            encoderOffreStage.setString(3, semestre);
            encoderOffreStage.executeQuery();
            System.out.println("Offre de stage ajoutée !");
            applicationCentrale();
        } catch (SQLException se) {
            System.out.println("Erreur lors de l’insertion !");
            se.printStackTrace();
            System.exit(1);
        }
    }
    private void voirLesMotsCLefs() {
        try {
            affichageMotClefs.execute();
            ResultSet rs = affichageMotClefs.getResultSet(); // Utilisez executeQuery au lieu de getResultSet
            System.out.println("  mot clefs disponible");
            System.out.println("_______________");
            while (rs.next()) {
                System.out.printf("|  %-10s  | \n", rs.getString("mot")); // Utilisez le nom de la colonne "mot"
            }
            System.out.println("_______________");
            applicationCentrale();
        } catch (SQLException se) {
            se.printStackTrace();
            System.out.println("Aucun cours disponible");
        }
    }

    private void ajouterMotClefs() {
        System.out.println("***************** Ajouter un mot-clé à une de ses offres de stage *****************");
        System.out.println("Veuillez entrer le code de l'offre de stage :");
        String code = scanner.nextLine();
        System.out.println("Veuillez entrer le mot clé à ajouter :");
        String motClef = scanner.nextLine();
        try {
            ajouterMotClefs.setString(1, code);
            ajouterMotClefs.setString(2, motClef);
            ajouterMotClefs.setString(3, idEntreprise);
            ajouterMotClefs.executeQuery();
            System.out.println("Mot clé ajouté !");
            applicationCentrale();
        } catch (SQLException se) {
            System.out.println("Erreur lors de l’insertion !");
            se.printStackTrace();
            System.exit(1);
        }
    }
    private void voirSesOffres() {
        try {
            voirSesOffres.setString(1, idEntreprise);
            voirSesOffres.execute();
            ResultSet rs = voirSesOffres.getResultSet();
            System.out.println("  Offre de stage");
            System.out.println("_______________________________________________________________________________________________________|");
            System.out.println("  | code stage |  Description   |Semestre|    etat     |nombre candidature en attentre |     nom       |");
            System.out.println("_______________________________________________________________________________________________________|");
            while (rs.next()) {
                System.out.printf("  |   %-7s  |   %-10s   |  %-5s | %-10s | %-10s                    | %-10s |\n", rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6));
            }
            System.out.println("____________________________");
            applicationCentrale();
        } catch (SQLException se) {
            se.printStackTrace();
            System.out.println("Aucun cours disponible");
        }
    }
    private void voirSesCandidature() {
    }
    private void selectionnerEtudiant() {
    }
    private void annulerOffreDeStage() {
    }
}
