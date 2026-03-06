package Proyect; // Asegúrate de que tu carpeta se llame exactamente "Proyect"

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class VistaPrueba extends Application {

    @Override
    public void start(Stage stage) {
        // 1. Datos de ejemplo (Esto vendría de tu ServicioDatos)
        List<Zapato> listaZapatos = new ArrayList<>();
        listaZapatos.add(new Zapato("Air Max 2026", 129.99, "Rojo"));
        listaZapatos.add(new Zapato("Classic Runner", 85.50, "Azul"));
        listaZapatos.add(new Zapato("Urban Street", 60.00, "Negro"));
        listaZapatos.add(new Zapato("Sport Pro", 110.00, "Blanco"));

        // 2. Contenedor principal de los zapatos
        VBox contenedorZapatos = new VBox(15); 
        contenedorZapatos.setPadding(new Insets(20));
        contenedorZapatos.setStyle("-fx-background-color: #f4f4f4;");

        // 3. Crear una "Tarjeta" para cada zapato
        for (Zapato z : listaZapatos) {
            HBox tarjeta = crearTarjetaZapato(z);
            contenedorZapatos.getChildren().add(tarjeta);
        }

        // 4. Hacer que se pueda hacer Scroll (por si hay muchos)
        ScrollPane scroll = new ScrollPane(contenedorZapatos);
        scroll.setFitToWidth(true);

        // Titulo de la App
        Label titulo = new Label("👟 Mi Tienda de Zapatos");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 10;");
        
        VBox layoutPrincipal = new VBox(titulo, scroll);
        
        Scene scene = new Scene(layoutPrincipal, 450, 600);
        stage.setTitle("Catálogo Intuitivo");
        stage.setScene(scene);
        stage.show();
    }

    // Método para crear el diseño de cada fila/tarjeta
    private HBox crearTarjetaZapato(Zapato z) {
        HBox hbox = new HBox(20);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(15));
        hbox.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                      "-fx-border-color: #ddd; -fx-border-radius: 10;");

        // Icono o placeholder de imagen (puedes poner una ImageView aquí)
        Label icono = new Label("👟");
        icono.setStyle("-fx-font-size: 30px;");

        VBox info = new VBox(5);
        Label lblModelo = new Label(z.getModelo());
        lblModelo.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        
        Label lblDetalle = new Label("Color: " + z.getColor() + " | Precio: " + z.getPrecio() + "€");
        lblDetalle.setStyle("-fx-text-fill: #666;");
        
        info.getChildren().addAll(lblModelo, lblDetalle);

        Button btnComprar = new Button("Detalles");
        btnComprar.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;");
        
        // Espaciador para empujar el botón a la derecha
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hbox.getChildren().addAll(icono, info, spacer, btnComprar);

        // Efecto visual al pasar el ratón (Hover)
        hbox.setOnMouseEntered(e -> hbox.setStyle(hbox.getStyle() + "-fx-background-color: #e3f2fd;"));
        hbox.setOnMouseExited(e -> hbox.setStyle(hbox.getStyle().replace("-fx-background-color: #e3f2fd;", "-fx-background-color: white;")));

        return hbox;
    }

    public static void main(String[] args) {
        launch(args);
    }
}