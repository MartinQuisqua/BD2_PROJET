import java.sql.*;
import java.util.Scanner;

public class ApplicationProfesseur {

	private String url = "jdbc:postgresql://172.24.2.6:5432/dbnicolasheymans";
	private Connection connection = null;
	private PreparedStatement encoderEtudiant = null;
	private PreparedStatement encoderEntreprise = null;
	private PreparedStatement encoderMotClef = null;
	private PreparedStatement offresStageNV = null;
	private PreparedStatement validerStage = null;
	private PreparedStatement offresStageVA = null;
	private PreparedStatement etudiantsSansStage = null;
	private PreparedStatement offresStageAT = null;
	private Scanner scanner = new Scanner(System.in);

	public void ProgrammePrincipal() {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("Driver PostgreSQL manquant !");
			System.exit(1);
		}

		try {
			connection = DriverManager.getConnection(url, "nicolasheymans", "4N0ZKYWCU");
		} catch (SQLException e) {
			System.out.println("Impossible de joindre le server !");
			System.exit(1);
		}

		try {
			encoderEtudiant = connection.prepareStatement("SELECT Projet_BD2.encoderEtudiant(?, ?, ?, ?, ?);");
			encoderEntreprise = connection.prepareStatement("SELECT  Projet_BD2.encoderEntreprise(?, ?, ?, ?, ?);");
			encoderMotClef = connection.prepareStatement("SELECT Projet_BD2.encoderMotClef(?);");
			offresStageNV = connection.prepareStatement("SELECT * FROM Projet_BD2.offresStageNV;");
			validerStage = connection.prepareStatement("SELECT  Projet_BD2.validerStage(?);");
			offresStageVA = connection.prepareStatement("SELECT * FROM Projet_BD2.offresStageVA;");
			etudiantsSansStage = connection.prepareStatement("SELECT * FROM Projet_BD2.etudiantsSansStage;");
			offresStageAT = connection.prepareStatement("SELECT * FROM Projet_BD2.offresStageAT;");
		} catch (SQLException e) {
			System.out.println("Impossible de préparer la requête !");
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		ApplicationProfesseur appProf = new ApplicationProfesseur();
		appProf.ProgrammePrincipal();
		appProf.applicationCentrale();
	}

	public void applicationCentrale() {
		System.out.println("***************** application Professeur *****************");
		System.out.println("1 : Encoder un étudiant");
		System.out.println("2 : Encoder une entreprise");
		System.out.println("3 : Encoder un mot-clé");
		System.out.println("4 : Voir les offres de stage dans l’état « non validée »");
		System.out.println("5 : Valider une offre de stage");
		System.out.println("6 : Voir les offres de stage dans l’état « validée »");
		System.out.println("7 : Voir les étudiants qui n’ont pas de stage");
		System.out.println("8 : Voir les offres de stage dans l’état « attribuée »");
		System.out.println("autre : quitter le programme");

		int choix = scanner.nextInt();
		switch (choix) {
			case 1:
				encoderEtudiant();
				break;
			case 2:
				encoderEntreprise();
				break;
			case 3:
				encoderMotClef();
				break;
			case 4:
				offresStageNV();
				break;
			case 5:
				validerStage();
				break;
			case 6:
				offresStageVA();
				break;
			case 7:
				etudiantsSansStage();
				break;
			case 8:
				offresStageAT();
				break;
			default:
				quitterProgramme();
				break;
		}
	}

	private void encoderEtudiant() {
		System.out.println("***************** Encoder un étudiant *****************");
		String sel = BCrypt.gensalt();
		try {
			System.out.println("Veuillez entrez le nom de l'etudiant : ");
			encoderEtudiant.setString(1, scanner.next());

			System.out.println("Veuillez entrez le prenom de l'etudiant : ");
			encoderEtudiant.setString(2, scanner.next());

			System.out.println("Veuillez entrez le e-mail de l'etudiant : ");
			encoderEtudiant.setString(3, scanner.next());

			System.out.println("Veuillez entrez le mot de passe de l'etudiant : ");
			encoderEtudiant.setString(4, BCrypt.hashpw(scanner.next(), sel));

			System.out.println("Veuillez entrez le semestre de l'etudiant : ");
			encoderEtudiant.setString(5, scanner.next());

			encoderEtudiant.execute();
			ResultSet rs = encoderEtudiant.getResultSet();

			while (rs.next()) {
				System.out.println("id nouvelle etudiant creer : " + rs.getString(1));
			}
		} catch (SQLException se) {
			System.out.println("Erreur lors de l’insertion !" + se.getMessage());
		}
		applicationCentrale();
	}

	private void encoderEntreprise() {
		System.out.println("***************** Encoder une entreprise *****************");
		String sel = BCrypt.gensalt();
		try {
			System.out.println("Veuillez entrez le nom de l'entreprise : ");
			encoderEntreprise.setString(1, scanner.next());

			System.out.println("Veuillez entrez le code de l'entreprise : ");
			encoderEntreprise.setString(2, scanner.next());

			System.out.println("Veuillez entrez le e-mail de l'entreprise : ");
			encoderEntreprise.setString(3, scanner.next());

			System.out.println("Veuillez entrez le mot de passe de l'entreprise : ");
			encoderEntreprise.setString(4, BCrypt.hashpw(scanner.next(), sel));

			System.out.println("Veuillez entrez l'adressee de l'entreprise : ");
			encoderEntreprise.setString(5, scanner.next());

			encoderEntreprise.execute();
			ResultSet rs = encoderEntreprise.getResultSet();

			while (rs.next()) {
				System.out.println("Code nouvelle entreprise creer : " + rs.getString(1));
			}

		} catch (SQLException se) {
			System.out.println("Imposible d'encoder l'entreprise " + se.getMessage());
		}
		applicationCentrale();
	}

	private void encoderMotClef() {
		System.out.println("***************** Encoder un mot-clef *****************");
		try {
			System.out.println("Veuillez entrez le nouveau mot-celf : ");
			encoderMotClef.setString(1, scanner.next());

			encoderMotClef.execute();
			ResultSet rs = encoderMotClef.getResultSet();

			while (rs.next()) {
				System.out.println("Code nouveau mot clef creer : " + rs.getString(1));
			}

		} catch (SQLException se) {
			System.out.println("impossible d'encoder le mot de passe!" + se.getMessage());
		}
		applicationCentrale();
	}

	private void offresStageNV() {
		System.out.println("***************** Voir les offres de stage dans l’état « non validée » *****************");
		try {

			offresStageNV.execute();
			ResultSet rs = offresStageNV.getResultSet();

			while (rs.next()) {
				System.out.println(rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3) + " | " + rs.getString(4) + " | " + rs.getString(5));
			}
		} catch (SQLException e) {
			System.out.println("Impossible d'afficher les offres de stage non valider !" + e.getMessage());
		}
		applicationCentrale();
	}

	private void validerStage() {
		System.out.println("***************** Valider un stage *****************");
		try {
			System.out.println("Veuillez entrez le code du stage : ");
			validerStage.setString(1, scanner.next());

			validerStage.execute();
			ResultSet rs = validerStage.getResultSet();

			while (rs.next()) {
				System.out.println("Id stage valider : " + rs.getString(1));
			}

		} catch (SQLException se) {
			System.out.println("Impossible d'encoder un entreprise !" + se.getMessage());
		}
		applicationCentrale();
	}

	private void offresStageVA() {
		System.out.println("***************** Voir les offres de stage dans l’état « validée » *****************");
		try {

			offresStageVA.execute();
			ResultSet rs = offresStageVA.getResultSet();

			while (rs.next()) {
				System.out.println(rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3) + " | " + rs.getString(4) + " | " + rs.getString(5));
			}
		} catch (SQLException e) {
			System.out.println("Impossible d'afficher les offres de stage valider !");
		}
		applicationCentrale();
	}

	private void etudiantsSansStage() {
		System.out.println("***************** Voir les étudiants sans stage *****************");
		try {

			etudiantsSansStage.execute();
			ResultSet rs = etudiantsSansStage.getResultSet();

			while (rs.next()) {
				System.out.println(rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3) + " | " + rs.getString(4) + " | " + rs.getString(5) + " | " + rs.getString(6));
			}
		} catch (SQLException e) {
			System.out.println("Impossible d'afficher les etudiant sans stage !" + e.getMessage());
		}
		applicationCentrale();
	}

	private void offresStageAT() {
		System.out.println("***************** Voir les offres de stage attribuées *****************");
		try {

			offresStageAT.execute();
			ResultSet rs = offresStageAT.getResultSet();

			while (rs.next()) {
				System.out.println(rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3) + " | " + rs.getString(4) + " | " + rs.getString(5) + " | " + rs.getString(6));
			}
		} catch (SQLException e) {
			System.out.println("Impossible d'afficher les offres de stage attribuer !" + e.getMessage());
		}
		applicationCentrale();
	}

	private void quitterProgramme() {
		System.out.println("Au revoir !");
		try {
			encoderEtudiant.close();
			encoderEntreprise.close();
			encoderMotClef.close();
			offresStageNV.close();
			validerStage.close();
			offresStageVA.close();
			etudiantsSansStage.close();
			offresStageAT.close();
			scanner.close();
			connection.close();
		} catch (SQLException e) {
			System.out.println("Impossible de fermer la connexion !" + e.getMessage());
		}
		System.exit(1);
	}
}
