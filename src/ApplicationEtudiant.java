import java.sql.*;
import java.util.Scanner;

public class ApplicationEtudiant {
	//private String url = "jdbc:postgresql://localhost:5432/postgres";
	private String url = "jdbc:postgresql://172.24.2.6:5432/dbnicolasheymans";
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

		try {
			//connection = DriverManager.getConnection(url, "postgres", "");
			connection = DriverManager.getConnection(url, "gauthiercollard", "MIV4S2DP6");
		} catch (
				SQLException e) {
			System.out.println("Impossible de joindre le server !");
			System.exit(1);
		}

		try {
			chercherOffresStageValidees = connection.prepareStatement("SELECT * FROM Projet_BD2.offresStageValideesParEtudiant WHERE id_etudiant = ?;");
			chercherOffresStageMotCle = connection.prepareStatement("SELECT * FROM Projet_BD2.rechercheOffresStageMotClef WHERE id_etudiant = ? AND mot_clef = ?;");
			poserCandidature = connection.prepareStatement("SELECT Projet_BD2.poserCandidature(?,?,?);");
			chercherOffresCandidature = connection.prepareStatement("SELECT * FROM Projet_BD2.offresCandidature WHERE etudiant = ?;");
			annulerCandidature = connection.prepareStatement("SELECT Projet_BD2.annulerCandidature(?,?);");
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

					System.out.println("idEtudiant : " + idEtudiant);
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
		System.out.println("***************** Application Etudiant *****************");
		System.out.println("1 :Voir offres de stage validees");
		System.out.println("2 :Rechercher une offre de stage par mot clÃ©");
		System.out.println("3 :Poser sa candidature pour un stage");
		System.out.println("4 :Voir les offres de stage pour lesquelles l'etudiant a pose sa candidature");
		System.out.println("5 :Annuler une candidature en precisant le code de l'offre de stage");
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
		System.out.println("***************** Voir offres de stage validees *****************");
		try {
			chercherOffresStageValidees.setInt(1, idEtudiant);
			chercherOffresStageValidees.execute();
			ResultSet rs = chercherOffresStageValidees.getResultSet(); // Utilisez executeQuery au lieu de getResultSet
			System.out.println("___________________________________________________________________________________________________________");
			System.out.printf(" | %-10s | %-20s | %-20s | %-20s | %-20s |\n", "Code stage", "Nom de l'entreprise", "Adresse", "Description", "Mots-clefs");
			System.out.println("___________________________________________________________________________________________________________");
			while (rs.next()) {
				System.out.printf(" | %-10s | %-20s | %-20s | %-20s | %-20s |\n", rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6));
			}
			System.out.println("___________________________________________________________________________________________________________");

		} catch (SQLException se) {
			System.out.println("Impossible de voir les offres de stage validées !");
		}
		applicationCentrale();
	}

	private void chercherOffresStageMotCle() {
		System.out.println("***************** Rechercher une offre de stage par mot clÃ© *****************");
		try {
			System.out.println("Veuillez introduire le mot clÃ© : ");
			String motCle = scanner.nextLine();

			chercherOffresStageMotCle.setInt(1, idEtudiant);
			chercherOffresStageMotCle.setString(2, motCle);

			chercherOffresStageMotCle.execute();
			ResultSet rs = chercherOffresStageMotCle.getResultSet(); // Utilisez executeQuery au lieu de getResultSet
			System.out.println("___________________________________________________________________________________________________________");
			System.out.printf(" | %-10s | %-20s | %-20s | %-20s | %-20s |\n", "Code stage", "Nom de l'entreprise", "Adresse", "Description", "Mots-clefs");
			System.out.println("___________________________________________________________________________________________________________");
			while (rs.next()) {
				System.out.printf(" | %-10s | %-20s | %-20s | %-20s | %-20s |\n", rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6));
			}
			System.out.println("___________________________________________________________________________________________________________");

		} catch (SQLException se) {
			System.out.println("Impossible de voir les offres de stage validées par mot clé!");
		}
		applicationCentrale();
	}

	private void poserCandidature() {
		System.out.println("***************** Poser sa candidature pour un stage *****************");
		try {
			System.out.println("Veuillez introduire le code de l'offre de stage pour lequel vous posez votre candidature : ");
			String codeStage = scanner.nextLine();
			System.out.println("Veuillez introduire vos motivations : ");
			String motivations = scanner.nextLine();

			poserCandidature.setString(1, codeStage);
			poserCandidature.setString(2, motivations);
			poserCandidature.setInt(3, idEtudiant);

			poserCandidature.execute();
			ResultSet rs = poserCandidature.getResultSet();

			while (rs.next()) {
				System.out.println("Inscrit pour le stage dont l'ID est : " + rs.getString(1));
			}
		} catch (SQLException se) {
			System.out.println("Erreur lors de l’insertion !" + se.getMessage());
		}
		applicationCentrale();
	}

	private void voirOffresCandidature() {
		System.out.println("***************** Voir les offres de stage pour lesquelles l'etudiant a pose sa candidature *****************");
		try {
			chercherOffresCandidature.setInt(1, idEtudiant);
			chercherOffresCandidature.execute();
			ResultSet rs = chercherOffresCandidature.getResultSet(); // Utilisez executeQuery au lieu de getResultSet
			System.out.println("__________________________________________________________________");
			System.out.printf(" | %-15s | %-20s | %-20s |\n", "Code stage", "Nom de l'entreprise", "Etat candidature");
			System.out.println("__________________________________________________________________");
			while (rs.next()) {
				System.out.printf(" | %-15s | %-20s | %-20s |\n", rs.getString(3), rs.getString(4), rs.getString(5));
			}
			System.out.println("__________________________________________________________________");

		} catch (SQLException se) {
			System.out.println("Impossible de voir les offres de stage pour lesquelles l'étudiant est candidat !");
		}
		applicationCentrale();
	}

	private void annulerCandidature() {
		System.out.println("***************** Annuler une candidature *****************");
		try {
			System.out.println("Veuillez introduire le code de l'offre de stage pour laquelle vous voulez annuler votre candidature : ");
			String codeStage = scanner.nextLine();

			annulerCandidature.setString(1, codeStage);
			annulerCandidature.setInt(2, idEtudiant);

			annulerCandidature.execute();
			ResultSet rs = annulerCandidature.getResultSet();

			while (rs.next()) {
				System.out.println("Candidature annulée pour le stage dont l'ID est : " + rs.getString(1));
			}
		} catch (SQLException se) {
			System.out.println("Impossible d'annuler le stage " + se.getMessage());
		}
		applicationCentrale();
	}

	private void quitterProgramme() {
		System.out.println("Fermeture du programme");
		try {
			chercherOffresCandidature.close();
			chercherOffresStageMotCle.close();
			chercherOffresStageValidees.close();
			poserCandidature.close();
			annulerCandidature.close();
			scanner.close();
			connection.close();
		} catch (SQLException se) {
			System.out.println("Impossible de fermer la connexion !" + se.getMessage());
		}
		System.exit(1);
	}
}


