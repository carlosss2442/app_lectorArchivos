package Proyect;
import com.mongodb.client.*;

import org.bson.Document;

public class Main {

    public static void main(String[] args) {

        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");

        MongoDatabase database = mongoClient.getDatabase("Listados");

        MongoCollection<Document> coleccion =
                database.getCollection("refObras");

        Vista vista = new Vista();

        new Controlador(vista, coleccion);

        vista.setVisible(true);
    }
}
