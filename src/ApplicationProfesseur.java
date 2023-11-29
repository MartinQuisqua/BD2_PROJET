import java.sql.*;
import java.util.Scanner;

public class ApplicationProfesseur {

	private String url = "jdbc:postgresql://localhost:5432/";
	private Connection connection = null;
	private PreparedStatement encoderEtudiant = null;
	private PreparedStatement encoderEntreprise = null;
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
		System.out.println("Entrez votre mot de passe postgres");
		String mdpPostgres = scanner.nextLine();

		try {
			connection = DriverManager.getConnection(url, "postgres", mdpPostgres);
		} catch (SQLException e) {
			System.out.println("Impossible de joindre le server !");
			System.exit(1);
		}

		try {
			encoderEtudiant = connection.prepareStatement("SELECT Projet_BD2.encoderEtudiant(?, ?, ?, ?, ?)");
			encoderEntreprise = connection.prepareStatement("SELECT  Projet_BD2.encoderEntreprise(?, ?, ?, ?, ?)");
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
		System.out.println("8 : Voir les étudiants qui n’ont pas de stage");
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
				offres_stage_nv();
				break;
			case 5:
				valider_stage();
				break;
			case 6:
				offres_stage_va();
				break;
			case 7:
				etudiants_sans_stage();
				break;
			case 8:
				offres_stage_at();
				break;
			default:
				quitterProgramme();
				break;
		}
	}

	private void encoderEtudiant() {
		String sel = BCrypt.gensalt();
		try {
			System.out.println("Veuillez entrez le nom de l'etudiant : ");
			encoderEtudiant.setString(1, scanner.next());

			System.out.println("Veuillez entrez le prenom de l'etudiant : ");
			encoderEtudiant.setString(2, scanner.next());

			System.out.println("Veuillez entrez le mot de passe de l'etudiant : ");
			encoderEtudiant.setString(3, BCrypt.hashpw(scanner.next(), sel));

			System.out.println("Veuillez entrez le e-mail de l'etudiant : ");
			encoderEtudiant.setString(4, scanner.next());

			System.out.println("Veuillez entrez le semestre de l'etudiant : ");
			encoderEtudiant.setString(5, scanner.next());

			encoderEtudiant.execute();
			ResultSet rs = encoderEtudiant.getResultSet();

			while (rs.next()) {
				System.out.println("id nouvelle etudiant creer : " + rs.getString(1));
			}

		} catch (SQLException e) {
			System.out.println("Impossible d'encoder un étudiant !");
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void encoderEntreprise() {
		String sel = BCrypt.gensalt();
		try {
			System.out.println("Veuillez entrez le code de l'entreprise : ");
			encoderEntreprise.setString(1, scanner.next());

			System.out.println("Veuillez entrez le nom de l'entreprise : ");
			encoderEntreprise.setString(2, scanner.next());

			System.out.println("Veuillez entrez le e-mail de l'entreprise : ");
			encoderEntreprise.setString(3, BCrypt.hashpw(scanner.next(), sel));

			System.out.println("Veuillez entrez le mot de passe de l'entreprise : ");
			encoderEntreprise.setString(4, scanner.next());

			System.out.println("Veuillez entrez l'adressee de l'entreprise : ");
			encoderEntreprise.setString(5, scanner.next());

			encoderEntreprise.execute();
			ResultSet rs = encoderEntreprise.getResultSet();

			while (rs.next()) {
				System.out.println("Code nouvelle entreprise creer : " + rs.getString(1));
			}

		} catch (SQLException e) {
			System.out.println("Impossible d'encoder un entreprise !");
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void encoderMotClef() {

	}

	private void offres_stage_nv() {

	}

	private void valider_stage() {

	}

	private void offres_stage_va() {

	}

	private void etudiants_sans_stage() {

	}

	private void offres_stage_at() {

	}

	private void quitterProgramme() {
		System.out.println("merci d'avoir tt fais maintenant bare toi !");
		System.exit(1);
	}
}
