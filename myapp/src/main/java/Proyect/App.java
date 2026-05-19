package Proyect;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.io.InputStream;
import java.net.URISyntaxException;
import org.bson.Document;

public class App extends Application {

	private MongoClient mongoClient;
	private Controlador controlador;

	@Override
	public void start(Stage stage) throws URISyntaxException {
		String conexionAtlas = "localhost:27017";
		mongoClient = MongoClients.create(conexionAtlas);
		MongoDatabase database = mongoClient.getDatabase("Listados");

		MongoCollection<Document> coleccion = database.getCollection("refObras");
		MongoCollection<Document> coleccionOrdenes = database.getCollection("numero_ordenes_de_trabajos");

		Vista vista = new Vista();
		controlador = new Controlador(vista, coleccion, coleccionOrdenes);

		try (InputStream is = getClass().getResourceAsStream("/logo.jpg")) {
			if (is != null) {
				Image icon = new Image(is);
				stage.getIcons().add(icon);
			} else {
				System.err.println(
						"Warning: /logo.jpg not found in resources; application will run without custom icon.");
			}
		} catch (Exception e) {
			System.err.println("Failed to load application icon: " + e.getMessage());
		}

		stage.setScene(vista.construirEscena());
		stage.setTitle("GESTIÓN DE MATERIALES");
		stage.setMaximized(true);
		stage.show();
	}

	@Override
	public void stop() {
		if (controlador != null)
			controlador.detenerRefresco();
		if (mongoClient != null)
			mongoClient.close();
	}

	public static void main(String[] args) {
		launch(args);
	}
}