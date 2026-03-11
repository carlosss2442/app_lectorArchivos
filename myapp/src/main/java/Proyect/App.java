package Proyect;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.bson.Document;

public class App extends Application {

    private MongoClient mongoClient;

    @Override
    public void start(Stage stage) {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("Listados");
        MongoCollection<Document> coleccion = database.getCollection("refObras");

        Vista vista = new Vista();
        new Controlador(vista, coleccion);

        stage.setScene(vista.construirEscena());
        stage.setTitle("Gestión de Obras");
        Image icon = new Image(getClass().getResourceAsStream("/logo.jpg"));
        stage.getIcons().add(icon);
        stage.show();
     // ebced5431fa7ca4b25f6ec99f926adefda63d274d6aad8153b7a84f080716cc3	   
    }

    @Override
    public void stop() {
        if (mongoClient != null) mongoClient.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}