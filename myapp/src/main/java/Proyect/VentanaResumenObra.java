package Proyect;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bson.Document;
import java.util.List;

public class VentanaResumenObra {

    public static void mostrar(Document obra) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Resumen — Obra " + obra.getString("obra"));
        stage.setMinWidth(680);
        stage.setResizable(true);

        List<Document> mats = obra.getList("materiales", Document.class);
        if (mats == null) mats = new java.util.ArrayList<>();

        // ── Calcular métricas globales ────────────────────────────────────────
        int total = mats.size();
        int preparados = 0, pendientes = 0, enCompras = 0;

        for (Document m : mats) {
            int salida    = m.getInteger("salidaUnidad", 0);
            int prep      = parseEntero(m.getOrDefault("preparado", "0").toString());
            int pedido2   = m.getInteger("pedidoCompleto2", 0);
            int falta     = Math.max(0, salida - prep);

            if (falta == 0 && salida > 0) {
                preparados++;
            } else if (pedido2 > 0 && falta > 0) {
                enCompras++;
            } else if (falta > 0) {
                pendientes++;
            } else {
                preparados++; // salida 0 → contamos como ok
            }
        }
        double pct = total > 0 ? (double) preparados / total : 0;

        // ════════════════════════════════════════════════════════════════════
        // CABECERA
        // ════════════════════════════════════════════════════════════════════
        VBox cabecera = new VBox(3);
        cabecera.setStyle("-fx-background-color: #3a3a3a; -fx-padding: 16 22;");
        Label lblTit = new Label("Obra " + obra.getString("obra") + " — " + nvl(obra, "cliente"));
        lblTit.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label lblSub = new Label(nvl(obra, "proyecto") + "  |  Responsable: " + nvl(obra, "responsable"));
        lblSub.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 12px;");
        cabecera.getChildren().addAll(lblTit, lblSub);

        // ════════════════════════════════════════════════════════════════════
        // META (2x2 grid)
        // ════════════════════════════════════════════════════════════════════
        GridPane meta = new GridPane();
        meta.setStyle("-fx-border-color: #dfe4ea; -fx-border-width: 0 0 1 0;");
        meta.add(metaItem("Cliente",          nvl(obra, "cliente")),     0, 0);
        meta.add(metaItem("Fecha de entrega", nvl(obra, "entrega")),     1, 0);
        meta.add(metaItem("Responsable",      nvl(obra, "responsable")), 0, 1);
        meta.add(metaItem("Referencia",       nvl(obra, "obra")),        1, 1);
        for (int c = 0; c < 2; c++)
            meta.getColumnConstraints().add(colConst(50));

        // ════════════════════════════════════════════════════════════════════
        // MÉTRICAS (4 tarjetas)
        // ════════════════════════════════════════════════════════════════════
        HBox metrics = new HBox(0);
        metrics.setStyle("-fx-border-color: #dfe4ea; -fx-border-width: 0 0 1 0;");
        metrics.getChildren().addAll(
            metricCard("Total",        total,      "#185fa5"),
            metricCard("Preparados",   preparados, "#3b6d11"),
            metricCard("Pendientes",   pendientes, "#a32d2d"),
            metricCard("En compras",   enCompras,  "#854f0b")
        );
        
        // ════════════════════════════════════════════════════════════════════
        // BARRA PROGRESO GLOBAL
        // ════════════════════════════════════════════════════════════════════
        VBox progSection = new VBox(6);
        progSection.setPadding(new Insets(14, 22, 14, 22));
        progSection.setStyle("-fx-border-color: #dfe4ea; -fx-border-width: 0 0 1 0;");

        Label lblProgLbl = new Label("Progreso global de preparación");
        lblProgLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #57606f;");

        ProgressBar pb = new ProgressBar(pct);
        pb.setPrefWidth(Double.MAX_VALUE);
        pb.setPrefHeight(14);
        String colorBarra = pct >= 1.0 ? "#27ae60" : pct >= 0.5 ? "#f39c12" : "#e74c3c";
        pb.setStyle("-fx-accent: " + colorBarra + ";");

        HBox progRow = new HBox();
        Label lblLeft  = new Label(preparados + " de " + total + " materiales preparados");
        lblLeft.setStyle("-fx-font-size: 11px; -fx-text-fill: #57606f;");
        Label lblRight = new Label(String.format("%.0f%%", pct * 100));
        lblRight.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + colorBarra + ";");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        progRow.getChildren().addAll(lblLeft, spacer, lblRight);

        progSection.getChildren().addAll(lblProgLbl, pb, progRow);

        // ════════════════════════════════════════════════════════════════════
        // LISTA DE MATERIALES (top pendientes primero)
        // ════════════════════════════════════════════════════════════════════
        VBox matsSection = new VBox(0);
        matsSection.setPadding(new Insets(14, 22, 4, 22));
        Label lblMatsTitle = new Label("Estado por material");
        lblMatsTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #57606f; -fx-padding: 0 0 8 0;");
        matsSection.getChildren().add(lblMatsTitle);

        // Ordenar: pendientes y en compras primero
        List<Document> ordenados = new java.util.ArrayList<>(mats);
        ordenados.sort((a, b) -> {
            return Integer.compare(estadoOrden(a), estadoOrden(b));
        });

        for (Document m : ordenados) {
            int sal  = m.getInteger("salidaUnidad", 0);
            int prep = parseEntero(m.getOrDefault("preparado", "0").toString());
            int ped2 = m.getInteger("pedidoCompleto2", 0);
            int falt = Math.max(0, sal - prep);
            double pctM = sal > 0 ? (double) prep / sal : 1.0;

            HBox fila = new HBox(10);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setPadding(new Insets(6, 0, 6, 0));
            fila.setStyle("-fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0;");

            Label lblA3 = new Label("A3·" + m.getInteger("A3", 0));
            lblA3.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #57606f; -fx-min-width: 38px;");

            Label lblDesc = new Label(str(m, "descripcion"));
            lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50;");
            lblDesc.setMaxWidth(280);
            HBox.setHgrow(lblDesc, Priority.ALWAYS);

            // Mini barra
            ProgressBar pbM = new ProgressBar(pctM);
            pbM.setPrefWidth(80);
            pbM.setPrefHeight(7);
            String cM = pctM >= 1.0 ? "#27ae60" : pctM > 0 ? "#f39c12" : "#e74c3c";
            pbM.setStyle("-fx-accent: " + cM + ";");

            Label lblPct = new Label(String.format("%.0f%%", pctM * 100));
            lblPct.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 34px; -fx-alignment: center-right; -fx-text-fill: " + cM + ";");

            Label pill = pilEstado(falt, ped2, sal);

            fila.getChildren().addAll(lblA3, lblDesc, pbM, lblPct, pill);
            matsSection.getChildren().add(fila);
        }

        // ════════════════════════════════════════════════════════════════════
        // PIE
        // ════════════════════════════════════════════════════════════════════
        HBox pie = new HBox(8);
        pie.setPadding(new Insets(12, 22, 12, 22));
        pie.setAlignment(Pos.CENTER_RIGHT);
        pie.setStyle("-fx-border-color: #dfe4ea; -fx-border-width: 1 0 0 0;");

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: white; -fx-border-color: #dfe4ea; " +
                           "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 16; -fx-cursor: hand;");
        btnCerrar.setOnAction(e -> stage.close());
        pie.getChildren().add(btnCerrar);

        // ── Scroll para la lista de materiales ────────────────────────────────
        ScrollPane scroll = new ScrollPane(matsSection);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(280);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: #dfe4ea; -fx-border-width: 0 0 1 0;");

        VBox root = new VBox(cabecera, meta, metrics, progSection, scroll, pie);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────────

    private static VBox metaItem(String etiqueta, String valor) {
        VBox box = new VBox(3);
        box.setPadding(new Insets(10, 22, 10, 22));
        box.setStyle("-fx-border-color: #dfe4ea; -fx-border-width: 0 1 0 0;");
        Label lbl = new Label(etiqueta);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #57606f;");
        Label val = new Label(valor);
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private static VBox metricCard(String lbl, int num, String color) {
        VBox card = new VBox(3);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 0, 14, 0));
        card.setStyle("-fx-border-color: #dfe4ea; -fx-border-width: 0 1 0 0;");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label lblNum = new Label(String.valueOf(num));
        lblNum.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label lblTxt = new Label(lbl);
        lblTxt.setStyle("-fx-font-size: 11px; -fx-text-fill: #57606f;");
        card.getChildren().addAll(lblNum, lblTxt);
        return card;
    }

    private static Label pilEstado(int falta, int pedido2, int salida) {
        String txt, bg, fg;
        if (falta == 0 && salida > 0)    { txt = "Completo";   bg = "#eaf3de"; fg = "#3b6d11"; }
        else if (pedido2 > 0)             { txt = "En compras"; bg = "#faeeda"; fg = "#633806"; }
        else if (falta > 0)               { txt = "Pendiente";  bg = "#fcebeb"; fg = "#a32d2d"; }
        else                              { txt = "—";           bg = "#f1f2f6"; fg = "#57606f"; }

        Label l = new Label(txt);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                   "-fx-padding: 2 8; -fx-background-radius: 99; -fx-font-size: 10px; -fx-font-weight: bold;");
        return l;
    }

    private static int estadoOrden(Document m) {
        int sal  = m.getInteger("salidaUnidad", 0);
        int prep = parseEntero(m.getOrDefault("preparado", "0").toString());
        int ped2 = m.getInteger("pedidoCompleto2", 0);
        int falt = Math.max(0, sal - prep);
        if (falt > 0 && ped2 == 0) return 0;  // pendiente → primero
        if (falt > 0 && ped2 > 0)  return 1;  // en compras
        return 2;                               // completo → al final
    }

    private static ColumnConstraints colConst(double pct) {
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(pct);
        return cc;
    }

    private static String nvl(Document doc, String key) {
        String v = doc.getString(key);
        return (v != null && !v.isBlank()) ? v : "—";
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