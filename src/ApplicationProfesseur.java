import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

public class ApplicationProfesseur {

	private String url = "jdbc:postgresql://localhost:5432/";
	private Connection connection = null;
	private static PreparedStatement connexionEntrepriseSql = null;
	private static PreparedStatement encoderOffreStage = null;
	private static Scanner scanner = new Scanner(System.in);
	private static String idEntreprise;

	public void ProgrammePrincipal() {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("Driver PostgreSQL manquant !");
			System.exit(1);
		}
		System.out.println("Entrez votre user name postgres");
		String usernamePostgresse = scanner.nextLine();

		System.out.println("Entrez votre mot de passe postgres");
		String mdpPostgresse = scanner.nextLine();

		try {
			connection = DriverManager.getConnection(url, usernamePostgresse, mdpPostgresse);
		} catch (SQLException e) {
			System.out.println("Impossible de joindre le server !");
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		ApplicationProfesseur app = new ApplicationProfesseur();
		app.ProgrammePrincipal();
		applicationCentrale();
	}

	public static void applicationCentrale() {
		System.out.println("idEntreprise : " + idEntreprise);
		System.out.println("***************** application entreprise *****************");
		System.out.println("1 : Encoder un étudiant");
		System.out.println("2 : Encoder une entreprise");
		System.out.println("3 : Encoder un mot-clé");
		System.out.println("4 : Voir les offres de stage dans l’état « non validée »");
		System.out.println("5 : Valider une offre de stage");
		System.out.println("6 : Voir les offres de stage dans l’état « validée »");
		System.out.println("7 : Voir les étudiants qui n’ont pas de stage");
		System.out.println("8 : Voir les étudiants qui n’ont pas de stage");

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
		}
	}

	private static void encoderEtudiant(){
		
	}

	private static void encoderEntreprise(){

	}

	private static void encoderMotClef(){

	}

	private static void offres_stage_nv(){

	}

	private static void valider_stage(){

	}

	private static void offres_stage_va(){

	}

	private static void etudiants_sans_stage(){

	}

	private static void offres_stage_at(){

	}
}
