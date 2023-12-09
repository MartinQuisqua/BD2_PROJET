import java.sql.*;
import java.util.Scanner;

public class ApplicationEntreprise {
	//private String url = "jdbc:postgresql://172.24.2.6:5432/dbnicolasheymans";
	private String url = "jdbc:postgresql://localhost:5432/postgres";

	private Connection connection = null;
	private PreparedStatement connexionEntrepriseSql = null;
	private PreparedStatement encoderOffreStage = null;
	private PreparedStatement affichageMotClefs = null;
	private PreparedStatement ajouterMotClefs = null;
	private PreparedStatement voirSesOffres = null;
	private PreparedStatement voirSesCandidature = null;
	private PreparedStatement selectionnerCandidat = null;
	private PreparedStatement annulerCandidature = null;
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
			//connection = DriverManager.getConnection(url, "martinquisquater", "SJBD0H4YR");
			connection = DriverManager.getConnection(url, "postgres", "Mama@0202");
		} catch (SQLException e) {
			System.out.println("Impossible de joindre le server !");
			System.exit(1);
		}

		try {
			connexionEntrepriseSql = connection.prepareStatement("SELECT mdp_hash, code FROM Projet_BD2.entreprises WHERE email = ?;");
			encoderOffreStage = connection.prepareStatement("SELECT Projet_BD2.ajouterStage(?,?,?);");
			affichageMotClefs = connection.prepareStatement("SELECT * FROM Projet_BD2.affichageMotsClefs;");
			ajouterMotClefs = connection.prepareStatement("SELECT Projet_BD2.ajouterMotClef(?,?,?);");
			voirSesOffres = connection.prepareStatement("SELECT * FROM Projet_BD2.voirOffresDeStages WHERE code_entreprise = ?;");
			voirSesCandidature = connection.prepareStatement(" SELECT * FROM Projet_BD2.voirCandidatures WHERE code_stage = ?;");
			selectionnerCandidat = connection.prepareStatement("SELECT Projet_BD2.selectionnerEtudiant(?,?,?);");
			annulerCandidature = connection.prepareStatement("SELECT Projet_BD2.annulerOffreStage(?,?);");
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
		//String email = "Entreprise3@gmail.test";
		String email = scanner.nextLine();
		System.out.println("Veuillez entrer votre mot de passe");
		String passwordUser = scanner.nextLine();
		// String passwordUser = "Entreprisetest";

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
		System.out.println("***************** Application entreprise de " + idEntreprise + " *****************");
		System.out.println("1 :Encoder une offre de stage");
		System.out.println("2 :Voir les mots-clés disponibles pour une offre de stage");
		System.out.println("3 :Ajouter un mot-clé à une de ses offres de stage");
		System.out.println("4 :Voir ses offres de stages");
		System.out.println("5 :Voir les candidatures pour une de ses offres de stages");
		System.out.println("6 :Sélectionner un étudiant pour une de ses offres de stage");
		System.out.println("7 :Annuler une offre de stage en donnant son code");
		System.out.println("Autre : quitter le programme");
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
			default:
				quitterProgramme();
				break;
		}
	}


	private void encoderOffre() {
		System.out.println("***************** Encoder une offre de stage *****************");
		System.out.println("Veuillez entrer la description de l'offre de stage :");
		String description = scanner.nextLine();
		System.out.println("Veuillez entrer le semestre de l'offre de stage");
		String semestre = scanner.nextLine();
		try {
			encoderOffreStage.setString(1, idEntreprise);
			encoderOffreStage.setString(2, description);
			encoderOffreStage.setString(3, semestre);
			encoderOffreStage.executeQuery();
			System.out.println("Offre de stage ajoutée !");
		} catch (SQLException se) {
			System.out.println("Erreur lors de l’insertion !" + se.getMessage());
		}
		applicationCentrale();
	}

	private void voirLesMotsCLefs() {
		try {
			affichageMotClefs.execute();
			ResultSet rs = affichageMotClefs.getResultSet(); // Utilisez executeQuery au lieu de getResultSet
			System.out.println("Mot clefs disponibles");
			System.out.println("_______________");
			while (rs.next()) {
				System.out.printf("|  %-10s  | \n", rs.getString("mot")); // Utilisez le nom de la colonne "mot"
			}
			System.out.println("_______________");
		} catch (SQLException se) {
			System.out.println("Aucun mot clef disponible");
		}
		applicationCentrale();
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
			ajouterMotClefs.execute();
			System.out.println("Mot clé ajouté !");
		} catch (SQLException se) {
			System.out.println("Erreur lors de l’insertion !" + se.getMessage());
		}
		applicationCentrale();
	}

	private void voirSesOffres() {
		try {
			voirSesOffres.setString(1, idEntreprise);
			voirSesOffres.execute();
			ResultSet rs = voirSesOffres.getResultSet();
			System.out.println("  Offre de stage");
			// code stage |  Description   |Semestre|    etat     |nombre candidature en attentre |     nom
			System.out.println("_______________________________________________________________________________________________________|");
			System.out.printf(" | %-10s | %-20s | %-10s | %-11s | %-20s | %-10s |\n", "code stage", "Description", "Semestre", "etat", "nombre candidature en attentre", "nom");
			System.out.println(" Offre de stage");
			System.out.println("_______________________________________________________________________________________________________|");
			System.out.println("  | Code stage |  Description   |Semestre|    Etat     |Nombre candidature en attentre |     Nom       |");
			System.out.println("_______________________________________________________________________________________________________|");
			while (rs.next()) {
				System.out.printf(" | %-10s | %-20s | %-10s | %-11s | %-20s | %-10s |\n", rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6), rs.getString(7));
			}
			System.out.println("_______________________________________________________________________________________________________|");
		} catch (SQLException se) {
			System.out.println("Erreur lors de l’insertion !" + se.getMessage());
		}
		applicationCentrale();
	}

	private void voirSesCandidature() {
		System.out.println("***************** Voir les candidatures pour une de ses offres de stages *****************");
		System.out.println("Veuillez entrer le code de l'offre de stage :");
		String code = scanner.nextLine();
		try {
			voirSesCandidature.setString(1, code);
			voirSesCandidature.execute();
			ResultSet rs = voirSesCandidature.getResultSet();
			if (!rs.next()) {
				System.out.println("Il n'y a pas de candidature pour cette offre ou vous n'avez pas d'offre ayant ce code");
				applicationCentrale();
			}
			// etat, nom, prenom, email, motivations
			System.out.println("_______________________________________________________________________________________________________");
			System.out.printf(" | %-10s | %-10s | %-10s | %-20s | %-20s |\n", "etat", "nom", "prenom", "email", "motivations");
			System.out.println("_______________________________________________________");
			do {
				System.out.printf(" | %-10s | %-10s | %-10s | %-20s | %-20s |\n", rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8));
			} while (rs.next());
			System.out.println("_______________________________________________________________________________________________________");
		} catch (SQLException se) {
			System.out.println("Aucune candidature disponible");
		}
		applicationCentrale();
	}

	private void selectionnerEtudiant() {
		System.out.println("***************** Sélectionner un étudiant pour une de ses offres de stage *****************");
		System.out.println("Veuillez entrer le code de l'offre de stage :");
		String code = scanner.nextLine();
		System.out.println("Veuillez entrer l'email de l'étudiant :");
		String email = scanner.nextLine();
		try {
			selectionnerCandidat.setString(1, code);
			selectionnerCandidat.setString(2, email);
			selectionnerCandidat.setString(3, idEntreprise);
			selectionnerCandidat.execute();
			System.out.println("Etudiant sélectionné !");
		} catch (SQLException se) {
			System.out.println("Erreur lors de l’insertion !" + se.getMessage());
		}
		applicationCentrale();
	}

	private void annulerOffreDeStage() {
		System.out.println("***************** Annuler une offre de stage *****************");
		System.out.println("Veuillez entrer le code de l'offre de stage :");
		String code = scanner.nextLine();
		try {
			annulerCandidature.setString(1, code);
			annulerCandidature.setString(2, idEntreprise);
			annulerCandidature.execute();
			System.out.println("Offre de stage annulée !");
		} catch (SQLException se) {
			System.out.println("Erreur lors de l’insertion !" + se.getMessage());
		}
		applicationCentrale();
	}

	private void quitterProgramme() {
		System.out.println("Merci d'avoir utilisé notre programme !");
		try {
			voirSesOffres.close();
			voirSesCandidature.close();
			selectionnerCandidat.close();
			annulerCandidature.close();
			ajouterMotClefs.close();
			scanner.close();
			connection.close();
		} catch (SQLException se) {
			System.out.println("Erreur lors de la fermeture de la connexion !" + se.getMessage());
		}
		System.exit(1);
	}
}
