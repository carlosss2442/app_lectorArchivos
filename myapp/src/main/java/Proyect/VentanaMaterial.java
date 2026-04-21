package Proyect;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bson.Document;

public class VentanaMaterial {

    public static void mostrar(Document material, String refObra, String cliente) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Material A3: " + material.getInteger("A3") + " — " + refObra);
        stage.setMinWidth(520);
        stage.setResizable(false);

        // ── Cabecera ──────────────────────────────────────────────────────────
        VBox cabecera = new VBox(3);
        cabecera.setStyle("-fx-background-color: #2f3542; -fx-padding: 14 20;");
        Label lblTit = new Label("Material — A3: " + material.getInteger("A3"));
        lblTit.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label lblSub = new Label("Obra: " + refObra + "  |  " + (cliente != null ? cliente : "—"));
        lblSub.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 12px;");
        cabecera.getChildren().addAll(lblTit, lblSub);

        // ── Badges de estado ──────────────────────────────────────────────────
        int salida   = material.getInteger("salidaUnidad", 0);
        int preparado = parseEntero(material.getOrDefault("preparado", "0").toString());
        int pedido2  = material.getInteger("pedidoCompleto2", 0);
        int falta    = Math.max(0, salida - preparado);
        boolean completo = (falta == 0 && salida > 0);

        HBox badges = new HBox(8);
        badges.setPadding(new Insets(10, 20, 10, 20));
        badges.setStyle("-fx-background-color: #f1f2f6; -fx-border-color: #dfe4ea; -fx-border-width: 0 0 1 0;");
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(
            badge("A3: " + material.getInteger("A3"), "#e6f1fb", "#185fa5"),
            badge("REF: " + str(material, "referencia"), "#f1f2f6", "#57606f"),
            badge(material.getString("marca") != null ? material.getString("marca") : "—", "#f1f2f6", "#57606f"),
            badge(completo ? "✔ Completo" : "✘ Pendiente",
                  completo ? "#eaf3de" : "#fcebeb",
                  completo ? "#3b6d11"  : "#a32d2d")
        );

        // ── Grid de tarjetas ──────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 20, 16, 20));
        grid.setStyle("-fx-background-color: white;");

        // Descripción — ancho completo
        grid.add(tarjeta("Descripción", str(material, "descripcion"), null, true), 0, 0, 2, 1);

        // Fila de números
        grid.add(tarjeta("Salida de Unidades", String.valueOf(salida), null, false), 0, 1);
        grid.add(tarjeta("Preparado", String.valueOf(preparado), "#3b6d11", false), 1, 1);
        grid.add(tarjeta("A pedir", String.valueOf(falta), falta > 0 ? "#a32d2d" : "#3b6d11", false), 0, 2);
        grid.add(tarjeta("Cantidad pedido (compras)", String.valueOf(pedido2), "#185fa5", false), 1, 2);

        // Barra de progreso
        double pct = salida > 0 ? (double) preparado / salida : 0;
        VBox barraBox = new VBox(6);
        barraBox.setPadding(new Insets(10, 12, 10, 12));
        barraBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        Label lblBarra = new Label("Progreso de preparación");
        lblBarra.setStyle("-fx-font-size: 11px; -fx-text-fill: #57606f;");
        ProgressBar pb = new ProgressBar(pct);
        pb.setPrefWidth(Double.MAX_VALUE);
        pb.setPrefHeight(14);
        String colorBarra = pct >= 1.0 ? "#27ae60" : pct >= 0.5 ? "#f39c12" : "#e74c3c";
        pb.setStyle("-fx-accent: " + colorBarra + ";");
        Label lblPct = new Label(preparado + " de " + salida + " unidades (" + String.format("%.0f%%", pct * 100) + ")");
        lblPct.setStyle("-fx-font-size: 11px; -fx-text-fill: #57606f;");
        barraBox.getChildren().addAll(lblBarra, pb, lblPct);
        grid.add(barraBox, 0, 3, 2, 1);

        // Fecha y validación
        String fecha = material.getString("fechaPedido") != null ? material.getString("fechaPedido") : "—";
        grid.add(tarjeta("Fecha de pedido", fecha, null, false), 0, 4);

        String val = material.getString("validacion");
        boolean validado = "✔".equals(val);
        grid.add(tarjeta("Validación", validado ? "✔ Validado" : "✘ Sin validar",
                         validado ? "#3b6d11" : "#a32d2d", false), 1, 4);

        // Observaciones
        String obs = material.getString("observaciones");
        if (obs != null && !obs.isBlank()) {
            grid.add(tarjeta("Observaciones", obs, "#57606f", true), 0, 5, 2, 1);
        }

        // ── Pie de botones ────────────────────────────────────────────────────
        HBox pie = new HBox(8);
        pie.setPadding(new Insets(12, 20, 12, 20));
        pie.setAlignment(Pos.CENTER_RIGHT);
        pie.setStyle("-fx-border-color: #dfe4ea; -fx-border-width: 1 0 0 0;");

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: white; -fx-border-color: #dfe4ea; " +
                           "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 16; -fx-cursor: hand;");
        btnCerrar.setOnAction(e -> stage.close());

        pie.getChildren().add(btnCerrar);

        // ── Ensamblado ────────────────────────────────────────────────────────
        VBox root = new VBox(cabecera, badges, grid, pie);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Label badge(String texto, String bg, String color) {
        Label l = new Label(texto);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; " +
                   "-fx-padding: 3 10; -fx-background-radius: 5; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private static VBox tarjeta(String etiqueta, String valor, String colorValor, boolean ancho) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        if (ancho) card.setPrefWidth(Double.MAX_VALUE);

        Label lbl = new Label(etiqueta);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #57606f;");

        Label val = new Label(valor != null ? valor : "—");
        val.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " +
                     (colorValor != null ? colorValor : "#2c3e50") + ";");
        val.setWrapText(true);

        card.getChildren().addAll(lbl, val);
        return card;
    }

    private static String str(Document doc, String key) {
        String v = doc.getString(key);
        return (v != null && !v.isBlank()) ? v : "—";
    }

    private static int parseEntero(String valor) {
        try { return (int) Double.parseDouble(valor.replace(",", ".")); }
        catch (Exception e) { return 0; }
    }
}