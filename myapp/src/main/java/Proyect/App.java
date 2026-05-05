package Proyect;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import javafx.application.Application;
import javafx.stage.Stage;
import java.net.URISyntaxException;
import org.bson.Document;

public class App extends Application {

	private MongoClient mongoClient;
	private Controlador controlador; // ← añade este campo

	@Override
	public void start(Stage stage) throws URISyntaxException {
		String conexionAtlas = "mongodb+srv://tecnomatDB:tecnomatAppCat@cluster0.0lj80bm.mongodb.net/?appName=Cluster0";
		mongoClient = MongoClients.create(conexionAtlas);

		MongoDatabase database = mongoClient.getDatabase("Listados");
		MongoCollection<Document> coleccion = database.getCollection("refObras");
		// mongodb+srv://tecnomatDB:tecnomatAppCat@cluster0.0lj80bm.mongodb.net/?appName=Cluster0
		Vista vista = new Vista();
		controlador = new Controlador(vista, coleccion);

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