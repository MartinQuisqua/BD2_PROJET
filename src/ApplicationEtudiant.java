import java.sql.*;
import java.util.Scanner;

public class ApplicationEtudiant {
    private String url = "jdbc:postgresql://localhost:5432/";
    private Connection connection = null;
    private PreparedStatement connexionEtudiant = null;
    private PreparedStatement chercherOffresStageValidees = null;
    private PreparedStatement chercherOffresStageMotCle = null;
    private PreparedStatement poserCandidature = null;
    private PreparedStatement chercherOffresCandidature = null;
    private PreparedStatement annulerCandidature = null;
    private int idEtudiant;
    private Scanner scanner = new Scanner(System.in);

    public void ProgrammePrincipal() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver PostgreSQL manquant !");
            System.exit(1);
        }
        System.out.println("Veuillez introduire votre mot de passe : ");
        String mdpPostgres = scanner.nextLine();

        try {
            connection = DriverManager.getConnection(url, "postgres", mdpPostgres);
        } catch (
                SQLException e) {
            System.out.println("Impossible de joindre le server !");
            System.exit(1);
        }

        try {
            chercherOffresStageValidees = connection.prepareStatement("SELECT * FROM Projet_BD2.afficherOffresStageValidees WHERE id_etudiant = ?;");
            chercherOffresStageMotCle = connection.prepareStatement("SELECT * FROM Projet_BD2.rechercheOffresStageMotCle WHERE id_etudiant = ? AND mot_cle = ?;");
            poserCandidature = connection.prepareStatement("SELECT Projet_BD2.poser_candidature(?,?,?)");
            chercherOffresCandidature = connection.prepareStatement("SELECT * FROM Projet_BD2.offresCandidature WHERE etudiant = ?;");
            annulerCandidature = connection.prepareStatement("SELECT Projet_BD2.annuler_candidature(?,?)");
            connexionEtudiant = connection.prepareStatement("SELECT mdp_hash, id_etudiant FROM Projet_BD2.etudiants WHERE email = ?;");
        } catch (SQLException e) {
            System.out.println("Impossible de prÃ©parer la requÃªte !");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        ApplicationEtudiant app = new ApplicationEtudiant();
        app.ProgrammePrincipal();
        app.connexionEtudiants();
    }

    public void connexionEtudiants() {
        System.out.println("***************** Connexion Ã©tudiant *****************");
        System.out.println("Veuillez entrer votre nom d'utilisateur");
        String email = scanner.nextLine();
        System.out.println("Veuillez entrer votre mot de passe");
        String passwordUser = scanner.nextLine();
        //String passwordUser = "EtudiantTest";

        try {
            connexionEtudiant.setString(1, email);
            connexionEtudiant.execute();
            ResultSet rs = connexionEtudiant.getResultSet();

            if (rs.next()) {
                String hashedPasswordFromDB = rs.getString(1);

                if (BCrypt.checkpw(passwordUser, hashedPasswordFromDB)) {
                    idEtudiant = rs.getInt(2);
                    System.out.println("Connexion rÃ©ussie !");
                    System.out.println();
                    applicationCentrale();
                } else {
                    System.out.println("Email ou mot de passe incorrect ! Veuillez rÃ©essayer.");
                    connexionEtudiants();
                }
            } else {
                System.out.println("Email ou mot de passe incorrect ! Veuillez rÃ©essayer.");
                connexionEtudiants();
            }

        } catch (SQLException se) {
            System.out.println("Erreur lors de la connexion !");
            se.printStackTrace();
            connexionEtudiants();
        }
    }

    public void applicationCentrale() {
        System.out.println("idEtudiant : " + idEtudiant);
        System.out.println("***************** Application Etudiant *****************");
        System.out.println("1 :Voir offres de stage validees");
        System.out.println("2 :Rechercher une offre de stage par mot clÃ©");
        System.out.println("3 :Poser sa candidature pour un stage");
        System.out.println("4 :Voir les offres de stage pour lesquels lâ€™Ã©tudiant a posÃ© sa candidature");
        System.out.println("5 :Annuler une candidature en prÃ©cisant le code de lâ€™offre de stage");
        int choix = scanner.nextInt();
        scanner.nextLine();
        switch (choix) {
            case 1:
                voirOffresStageValidees();
                break;
            case 2:
                chercherOffresStageMotCle();
                break;
            case 3:
                poserCandidature();
                break;
            case 4:
                voirOffresCandidature();
                break;
            case 5:
                annulerCandidature();
                break;
            default:
                quitterProgramme();
                break;
        }
    }

    private void voirOffresStageValidees() {
        try {
            chercherOffresStageValidees.setInt(1, idEtudiant);
            chercherOffresStageValidees.execute();
            ResultSet rs = chercherOffresStageValidees.getResultSet(); // Utilisez executeQuery au lieu de getResultSet
            System.out.println("Offres de stage validees");
            System.out.println("_______________");
            while (rs.next()) {
                System.out.println("IdStage : " + rs.getInt(1) + " | nomEntreprise :  " + rs.getString(2) + " | adresseEntreprise : " + rs.getString(3) + " | description : " + rs.getString(5) + " | motsClefs : " + rs.getString(6));
            }
            System.out.println("_______________");
            applicationCentrale();
        } catch (SQLException se) {
            se.printStackTrace();
            System.out.println("Impossible de voir les offres de stage validées !");
        }
    }

    private void chercherOffresStageMotCle() {
        try {
            System.out.println("Veuillez introduire le mot clÃ© : ");
            String motCle = scanner.nextLine();

            chercherOffresStageMotCle.setInt(1, idEtudiant);
            chercherOffresStageMotCle.setString(2, motCle);

            chercherOffresStageMotCle.execute();
            ResultSet rs = chercherOffresStageMotCle.getResultSet(); // Utilisez executeQuery au lieu de getResultSet
            System.out.println("Offres de stage par mot clÃ©");
            System.out.println("_______________");
            while (rs.next()) {
                System.out.println("IdStage : " + rs.getInt(1) + " | nomEntreprise :  " + rs.getString(2) + " | adresseEntreprise : " + rs.getString(3) + " | description : " + rs.getString(5) + " | motsClefs : " + rs.getString(6));
            }
            System.out.println("_______________");
            applicationCentrale();
        } catch (SQLException se) {
            se.printStackTrace();
            System.out.println("Impossible de voir les offres de stage validées par mot clé!");
        }
    }

    private void poserCandidature() {
        try {
            System.out.println("Veuillez introduire le code de l'offre de stage pour lequel vous posez votre candidature : ");
            String codeStage = scanner.nextLine();
            System.out.println("Veuillez introduire vos motivations : ");
            String motivations = scanner.nextLine();

            poserCandidature.setString(1, codeStage);
            poserCandidature.setString(2, motivations);

            poserCandidature.execute();
            ResultSet rs = poserCandidature.getResultSet();

            while (rs.next()) {
                System.out.println("Inscrit pour le stage dont l'ID est : " + rs.getString(1));
            }
        } catch (SQLException se) {
            se.printStackTrace();
            System.out.println("Impossible de poser la candidature !");
        }
    }

    private void voirOffresCandidature() {
        try {
            chercherOffresStageValidees.setInt(1, idEtudiant);
            chercherOffresStageValidees.execute();
            ResultSet rs = chercherOffresStageValidees.getResultSet(); // Utilisez executeQuery au lieu de getResultSet
            System.out.println("Offres de stage pour lesquelles l'étudiant est candidat");
            System.out.println("_______________");
            while (rs.next()) {
                System.out.println("Code de l'offre de stage : " + rs.getInt(1) + " | nomEntreprise :  " + rs.getString(2) + " | etatCandidature : " + rs.getString(3));
            }
            System.out.println("_______________");
            applicationCentrale();
        } catch (SQLException se) {
            se.printStackTrace();
            System.out.println("Impossible de voir les offres de stage pour lesquelles l'étudiant est candidat !");
        }
    }

    private void annulerCandidature() {
        try {
            System.out.println("Veuillez introduire le code de l'offre de stage pour laquelle vous voulez annuler votre candidature : ");
            String codeStage = scanner.nextLine();

            annulerCandidature.setInt(1, idEtudiant);
            annulerCandidature.setString(2, codeStage);

            poserCandidature.execute();
            ResultSet rs = poserCandidature.getResultSet();

            while (rs.next()) {
                System.out.println("Candidature annulée pour le stage dont l'ID est : " + rs.getString(1));
            }
        } catch (SQLException se) {
            se.printStackTrace();
            System.out.println("Impossible d'annuler la candidature !");
        }
    }

    private void quitterProgramme() {
        System.out.println("Fermeture du programme");
        System.exit(1);
    }
}


