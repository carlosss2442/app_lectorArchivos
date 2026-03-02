package MyApp;

import javax.swing.JFrame;
import javax.swing.UIManager;

public class Lector_ficheros  {

	public static void main(String[] args) {
	    try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	        Modelo m = new Modelo();
	        Vista v = new Vista();
	        new Controlador(m, v);
	    }
	}
	
	/*
	 * import java.io.*; import java.nio.file.*;
	 * 
	 * public class LectorSincronizado { public static void main(String[] args) { //
	 * Obtenemos la ruta del usuario actual (ej: C:\Users\NombreUsuario) String
	 * userHome = System.getProperty("user.home");
	 * 
	 * // Construimos la ruta hacia la carpeta de Dropbox o OneDrive // Nota:
	 * Asegúrate de que el nombre de la carpeta sea el correcto Path rutaArchivo =
	 * Paths.get(userHome, "Dropbox", "mi_app", "datos.txt");
	 * 
	 * // Leer y modificar try { if (Files.exists(rutaArchivo)) { String contenido =
	 * Files.readString(rutaArchivo); System.out.println("Contenido actual: " +
	 * contenido);
	 * 
	 * // Modificación simple String nuevoContenido = contenido +
	 * "\nEditado desde: " + System.getProperty("user.name");
	 * Files.writeString(rutaArchivo, nuevoContenido);
	 * 
	 * System.out.println("Archivo actualizado y listo para sincronizar."); } else {
	 * System.out.println("El archivo no existe en la carpeta de sincronización.");
	 * } } catch (IOException e) { e.printStackTrace(); } } }
	 */


