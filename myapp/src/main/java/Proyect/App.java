package Proyect;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URISyntaxException;

import org.bson.Document;

public class App extends Application {

	private MongoClient mongoClient;

	@Override
	public void start(Stage stage) throws URISyntaxException {
		String uri = "mongodb://localhost:27017";
		String miIp = "mongodb://192.168.1.57:27017";
		String conexionAtlas = "mongodb+srv://tecnomatDB:tecnomatAppCat@cluster0.0lj80bm.mongodb.net/?appName=Cluster0";
		mongoClient = MongoClients.create(conexionAtlas);
		// mongoClient = MongoClients.create("mongodb://localhost:27017");
		MongoDatabase database = mongoClient.getDatabase("Listados");
		MongoCollection<Document> coleccion = database.getCollection("refObras");
		// mongodb+srv://tecnomatDB:tecnomatAppCat@cluster0.0lj80bm.mongodb.net/?appName=Cluster0
		Vista vista = new Vista();
		new Controlador(vista, coleccion);

		stage.setScene(vista.construirEscena());
		stage.setTitle("GESTIÓN DE MATERIALES ");
		// stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
		stage.setMaximized(true);
		stage.show();
		/*
		 * //System.out.println(App.class.getResource("/logo.jpg")); String url =
		 * App.class.getResource("/logo.jpg").toURI().toString(); Image icon = new
		 * Image(url); stage.getIcons().add(icon);
		 */

	}

	@Override
	public void stop() {
		if (mongoClient != null)
			mongoClient.close();
	}

	public static void main(String[] args) {
		launch(args);
	}
}