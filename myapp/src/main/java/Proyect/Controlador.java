package Proyect;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bson.Document;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

public class Controlador {

    private final Vista vista;
    private final MongoCollection<Document> coleccion;

    // ── Estado del último resultado mostrado (para filtro en tiempo real) ─────
    private List<Document> ultimosDatosOriginales = null;
    private String[]       ultimasColumnas        = null;
    private String[]       ultimasLlaves          = null;
    private String         ultimoTitulo           = null;

    public Controlador(Vista vista, MongoCollection<Document> coleccion) {
        this.vista     = vista;
        this.coleccion = coleccion;
        registrarEventos();
        actualizarMetricasDashboard();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  REGISTRO DE EVENTOS
    // ═════════════════════════════════════════════════════════════════════════
    private void registrarEventos() {
        vista.getBtnImportar()     .setOnAction(e -> onImportar());
        vista.getBtnListarObras()  .setOnAction(e -> onListarObras());
        vista.getBtnBuscarObra()   .setOnAction(e -> onBuscarObra(null));
        vista.getBtnAgregarFila()  .setOnAction(e -> onAgregarFila());
        vista.getBtnEliminarFila() .setOnAction(e -> onEliminarFila());
        vista.getBtnActualizar()   .setOnAction(e -> onActualizar());
        vista.getBtnEliminarObra() .setOnAction(e -> onEliminarObra());
        vista.getBtnBuscarCliente().setOnAction(e -> onBuscarCliente());
        vista.getBtnBuscarRef()    .setOnAction(e -> onBuscarReferencia());
        vista.getBtnOrdenarA3()    .setOnAction(e -> onOrdenarA3());
        vista.getBtnLimpiarInput() .setOnAction(e -> onLimpiarInput());
        vista.getBtnExportar()     .setOnAction(e -> onExportar());
        //vista.getBtnSalir()        .setOnAction(e -> onSalir());
        vista.getBtnEtiquetas()    .setOnAction(e -> onImprimirEtiquetas());
        vista.setOnInputChange((obs, oldVal, newVal) -> onFiltrarEnTiempoReal(newVal));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPER: TextInputDialog con icono
    // ═════════════════════════════════════════════════════════════════════════
    private String pedirTextoConIcono(String titulo, String mensaje) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(titulo);
        dialog.setHeaderText(mensaje);
        dialog.getDialogPane().getScene().getWindow()
              .addEventHandler(javafx.stage.WindowEvent.WINDOW_SHOWN, event -> {
                  Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
                  try {
                      stage.getIcons().add(new javafx.scene.image.Image(
                              getClass().getResource("/logo.jpg").toExternalForm()));
                  } catch (Exception ignored) {
                      stage.setTitle("⚠ " + titulo);
                  }
              });
        return dialog.showAndWait().orElse("").trim();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  1. IMPORTAR EXCEL
    // ═════════════════════════════════════════════════════════════════════════
    private void onImportar() {
        File archivo = Modelo.seleccionarArchivo();
        if (archivo == null) {
            vista.setEstado("No se seleccionó archivo.");
            return;
        }
        vista.setEstado("Importando...");
        vista.limpiar();
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws Exception {
                Modelo.importarExcelAMongo(archivo, coleccion);
                return null;
            }
        };
        tarea.setOnSucceeded(e -> {
            vista.setEstado("✅ Excel importado correctamente.");
            onListarObras();
        });
        tarea.setOnFailed(e -> Platform.runLater(() ->
                vista.setEstado("❌ Error: " + tarea.getException().getMessage())));
        new Thread(tarea).start();
        actualizarMetricasDashboard();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  2. LISTAR TODAS LAS OBRAS
    // ═════════════════════════════════════════════════════════════════════════
    private void onListarObras() {
        List<Document> obras = new java.util.ArrayList<>();
        try (MongoCursor<Document> cursor = coleccion.find().iterator()) {
            while (cursor.hasNext()) obras.add(cursor.next());
        }
        String[] cols = { "REF. OBRA", "CLIENTE", "PROYECTO", "ENTREGA", "RESPONSABLE" };
        String[] keys = { "obra", "cliente", "proyecto", "entrega", "responsable" };
        mostrarTablaResultados("Listado General de Obras", obras, cols, keys);
        vista.setEstado("Total obras: " + obras.size());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  3. BUSCAR OBRA POR REFERENCIA
    // ═════════════════════════════════════════════════════════════════════════
    private void onBuscarObra(String refForzada) {
        String ref = (refForzada != null && !refForzada.isEmpty()) ? refForzada : vista.getInput();
        if (ref.isEmpty())
            ref = pedirTextoConIcono("Buscar obra", "Introduce la referencia de obra:");
        if (ref == null || ref.isEmpty()) return;

        Document doc = coleccion.find(eq("obra", ref)).first();
        if (doc == null) {
            vista.mostrarAlerta("Error", "No se encontró la obra: " + ref);
            return;
        }
        List<Document> materiales = doc.getList("materiales", Document.class);

        String[] cols = { "A3", "MARCA", "REFERENCIA", "DESCRIPCIÓN", "SALIDA",
                          "PREPARADO", "FALTA", "VALIDACIÓN", "PEDIDO COMPLETO",
                          "N° PEDIDO", "FECHA MODIF.", "OBSERVACIONES" };
        String[] keys = { "A3", "marca", "referencia", "descripcion", "salidaUnidad",
                          "preparado", "falta", "validacion", "pedidoCompleto",
                          "numeroPedido", "fechaPedido", "observaciones" };
        mostrarTablaResultados(
                "Detalle Obra: " + ref + " | Cliente: " + doc.getString("cliente"),
                materiales, cols, keys);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  5. AGREGAR FILA
    // ═════════════════════════════════════════════════════════════════════════
    private void onAgregarFila() {
        String ref = pedirObraConIcono("Agregar fila");
        if (ref == null) return;
        Document obra = coleccion.find(eq("obra", ref)).first();
        if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }

        Dialog<Document> dialog = new Dialog<>();
        dialog.setTitle("Gestión de Materiales");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(500);

        VBox cabecera = new VBox(2);
        cabecera.setStyle("-fx-background-color: #2f3542; -fx-padding: 20;");
        Label lblTitulo = new Label("Nueva Fila de Material");
        lblTitulo.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label lblSub = new Label("Añadiendo a la obra: " + ref);
        lblSub.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 13px;");
        cabecera.getChildren().addAll(lblTitulo, lblSub);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(12);
        grid.setPadding(new Insets(25));
        grid.setStyle("-fx-background-color: white;");

        TextField tfA3           = styledField("",    200); tfA3.setPromptText("Ej: 101");
        TextField tfMarca        = styledField("",    200);
        TextField tfReferencia   = styledField("",    200);
        TextField tfDescripcion  = styledField("",    250);
        TextField tfSalida       = styledField("0",   100);
        TextField tfServir       = styledField("0",   100);
        TextField tfPreparado    = styledField("0",   100);
        TextField tfObs          = styledField("",    250);
        TextField tfNumeroPedido = styledField("",    200);
        tfNumeroPedido.setPromptText("Ej: PED-2024-001");
        CheckBox cbVal = styledCheckbox(false, "Validación");
        CheckBox cbPed = styledCheckbox(false, "Pedido Completo");

        int row = 0;
        addFormRow(grid, "Número A3:",       tfA3,           row++);
        addFormRow(grid, "Marca:",           tfMarca,        row++);
        addFormRow(grid, "Referencia:",      tfReferencia,   row++);
        addFormRow(grid, "Descripción:",     tfDescripcion,  row++);
        grid.add(new Separator(), 0, row++, 2, 1);
        addFormRow(grid, "Unidades Salida:", tfSalida,       row++);
        addFormRow(grid, "Unidades Servir:", tfServir,       row++);
        addFormRow(grid, "Ya Preparado:",    tfPreparado,    row++);
        grid.add(new Label("Estados:"), 0, row);
        grid.add(new HBox(15, cbVal, cbPed), 1, row++);
        addFormRow(grid, "Número de Pedido:", tfNumeroPedido, row++);
        addFormRow(grid, "Observaciones:",   tfObs,          row++);

        dialog.getDialogPane().setContent(new VBox(cabecera, grid));
        Platform.runLater(tfA3::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            try {
                int a3     = Integer.parseInt(tfA3.getText().trim());
                int salida = parseEntero(tfSalida.getText());
                int prep   = parseEntero(tfPreparado.getText());
                return new Document()
                        .append("A3",            a3)
                        .append("marca",         tfMarca.getText().trim())
                        .append("referencia",    tfReferencia.getText().trim())
                        .append("descripcion",   tfDescripcion.getText().trim())
                        .append("salidaUnidad",  salida)
                        .append("servirUnidad",  parseEntero(tfServir.getText()))
                        .append("validacion",    cbVal.isSelected() ? "✔" : "✘")
                        .append("preparado",     prep)
                        .append("falta",         Math.max(0, salida - prep))
                        .append("pedidoCompleto",cbPed.isSelected() ? "✔" : "✘")
                        .append("numeroPedido",  tfNumeroPedido.getText().trim())
                        .append("fechaPedido",   new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()))
                        .append("observaciones", tfObs.getText().trim());
            } catch (Exception e) { return null; }
        });

        final String refFinal = ref;
        dialog.showAndWait().ifPresent(nuevaFila -> {
            int a3 = nuevaFila.getInteger("A3");
            List<Document> mats = obra.getList("materiales", Document.class);
            if (mats != null && Modelo.existeA3(mats, a3)) {
                vista.mostrarAlerta("Error", "El A3 " + a3 + " ya existe en esta obra.");
                return;
            }
            coleccion.updateOne(eq("obra", refFinal),
                    new Document("$push", new Document("materiales", nuevaFila)));
            vista.setEstado("✅ Fila A3=" + a3 + " añadida correctamente.");
            onBuscarObra(refFinal);
        });
        actualizarMetricasDashboard();
    }

    private void addFormRow(GridPane grid, String labelText, javafx.scene.Node field, int row) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606f;");
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  6. ELIMINAR FILA
    // ═════════════════════════════════════════════════════════════════════════
    private void onEliminarFila() {
        String ref = pedirObraConIcono("Eliminar fila");
        if (ref == null) return;
        Document obra = coleccion.find(eq("obra", ref)).first();
        if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }

        String a3Str = pedirTextoConIcono("Eliminar fila", "Número A3 a eliminar:");
        if (a3Str.isEmpty()) return;
        int a3;
        try { a3 = Integer.parseInt(a3Str); }
        catch (NumberFormatException e) { vista.mostrarAlerta("Error", "A3 debe ser un número."); return; }

        List<Document> mats = (List<Document>) obra.get("materiales");
        if (mats.stream().noneMatch(m -> m.getInteger("A3").equals(a3))) {
            vista.mostrarAlerta("Error", "El A3 no existe en esa obra."); return;
        }
        if (vista.confirmar("¿Eliminar la fila A3=" + a3 + " de la obra " + ref + "?")) {
            coleccion.updateOne(eq("obra", ref),
                    new Document("$pull", new Document("materiales", new Document("A3", a3))));
            vista.setEstado("🗑 Fila A3=" + a3 + " eliminada.");
            onBuscarObra(ref);
        }
        actualizarMetricasDashboard();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  7. ACTUALIZAR / EDITAR OBRA
    // ═════════════════════════════════════════════════════════════════════════
    private void onActualizar() {
        String ref = pedirObraConIcono("Editar obra");
        if (ref == null) return;

        Document obra = coleccion.find(eq("obra", ref)).first();
        if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }

        List<Document> mats = obra.getList("materiales", Document.class);
        if (mats == null || mats.isEmpty()) { vista.mostrarAlerta("Info", "La obra no tiene materiales."); return; }

        Dialog<List<Document>> dialog = new Dialog<>();
        dialog.setTitle("Editor Maestro de Obras");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(1280);
        dialog.getDialogPane().setPrefHeight(650);

        VBox cabeceraDialogo = new VBox(2);
        cabeceraDialogo.setStyle("-fx-background-color: #2f3542; -fx-padding: 15;");
        Label lblTitulo = new Label("Editando Obra: " + ref);
        lblTitulo.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label lblSub = new Label("Proyecto: " + obra.getString("proyecto"));
        lblSub.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 13px;");
        cabeceraDialogo.getChildren().addAll(lblTitulo, lblSub);

        double[] colWidths = { 50, 85, 85, 95, 75, 95, 95, 120, 145, 200 };

        GridPane gridHeader = new GridPane();
        gridHeader.setHgap(10);
        gridHeader.setPadding(new Insets(12, 15, 12, 15));
        gridHeader.setStyle("-fx-background-color: #f1f2f6; -fx-border-color: #dfe4ea; -fx-border-width: 0 0 1 0;");

        String[] headers = { "A3", "SALIDA 🔒", "SERVIR", "PREPARADO", "FALTA",
                             "VALIDAR", "PED. COMPL.", "N° PEDIDO", "FECHA MODIF.", "OBSERVACIONES" };
        for (int i = 0; i < headers.length; i++) {
            Label l = new Label(headers[i]);
            l.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606f; -fx-font-size: 11px;");
            l.setMinWidth(colWidths[i]); l.setMaxWidth(colWidths[i]);
            gridHeader.add(l, i, 0);
        }

        GridPane gridDatos = new GridPane();
        gridDatos.setHgap(10); gridDatos.setVgap(8);
        gridDatos.setPadding(new Insets(10, 15, 10, 15));

        List<Object[]> filaControles = new java.util.ArrayList<>();

        for (int i = 0; i < mats.size(); i++) {
            Document m = mats.get(i);
            boolean pedidoCompletado = "✔".equals(m.getString("pedidoCompleto"));

            TextField tfSalida = styledField(String.valueOf(m.getInteger("salidaUnidad", 0)), colWidths[1]);
            tfSalida.setEditable(false);
            tfSalida.setStyle(tfSalida.getStyle() + "-fx-background-color: #f1f2f6; -fx-text-fill: #7f8c8d;");

            TextField tfServir  = styledField(String.valueOf(m.getInteger("servirUnidad", 0)), colWidths[2]);
            TextField tfPrep    = styledField(String.valueOf(m.getOrDefault("preparado", 0)),  colWidths[3]);
            String numPedActual = m.getString("numeroPedido") != null ? m.getString("numeroPedido") : "";
            TextField tfNumPed  = styledField(numPedActual, colWidths[7]);
            tfNumPed.setPromptText("Ej: PED-001");
            TextField tfObs = styledField(
                    m.getString("observaciones") != null ? m.getString("observaciones") : "", colWidths[9]);

            Label lblA3    = new Label(String.valueOf(m.getInteger("A3")));
            lblA3.setStyle("-fx-font-weight: bold; -fx-min-width: " + colWidths[0] + ";");
            Label lblFalta = new Label(); lblFalta.setMinWidth(colWidths[4]);

            CheckBox cbVal = styledCheckbox("✔".equals(m.getString("validacion")), "Ok");
            cbVal.setMinWidth(colWidths[5]);
            CheckBox cbPed = styledCheckbox(pedidoCompletado, "Ok");
            cbPed.setMinWidth(colWidths[6]);

            if (pedidoCompletado) bloquearFila(tfServir, tfPrep, tfNumPed, tfObs, cbVal, cbPed);

            String fechaActual = m.getString("fechaPedido");
            Label lblFecha = new Label(fechaActual != null && !fechaActual.isEmpty() ? fechaActual : "—");
            lblFecha.setMinWidth(colWidths[8]);
            lblFecha.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");

            Runnable calcular = () -> {
                int s = parseEntero(tfSalida.getText());
                int p = parseEntero(tfPrep.getText());
                int f = Math.max(0, s - p);
                lblFalta.setText(String.valueOf(f));
                lblFalta.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (f > 0 ? "#e74c3c" : "#27ae60") + ";");
            };

            boolean[] modificado = { false };
            Runnable alCambiar = () -> {
                calcular.run();
                if (!modificado[0]) {
                    modificado[0] = true;
                    lblFecha.setText("✎ se actualizará");
                    lblFecha.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 10px;");
                }
            };

            if (!pedidoCompletado) {
                tfPrep .textProperty().addListener((o, ov, nv) -> alCambiar.run());
                tfServir.textProperty().addListener((o, ov, nv) -> alCambiar.run());
                tfObs  .textProperty().addListener((o, ov, nv) -> alCambiar.run());
                tfNumPed.textProperty().addListener((o, ov, nv) -> alCambiar.run());
                cbVal.selectedProperty().addListener((o, ov, nv) -> alCambiar.run());
                cbPed.selectedProperty().addListener((o, ov, nv) -> alCambiar.run());
            }
            calcular.run();

            gridDatos.add(lblA3,    0, i); gridDatos.add(tfSalida, 1, i);
            gridDatos.add(tfServir, 2, i); gridDatos.add(tfPrep,   3, i);
            gridDatos.add(lblFalta, 4, i); gridDatos.add(cbVal,    5, i);
            gridDatos.add(cbPed,    6, i); gridDatos.add(tfNumPed, 7, i);
            gridDatos.add(lblFecha, 8, i); gridDatos.add(tfObs,    9, i);

            filaControles.add(new Object[]{ m, tfSalida, tfServir, tfPrep, cbVal, cbPed, tfObs, modificado, tfNumPed });
        }

        ScrollPane scroll = new ScrollPane(gridDatos);
        scroll.setFitToWidth(true); scroll.setPrefHeight(500);
        scroll.setStyle("-fx-background-color:transparent;");
        dialog.getDialogPane().setContent(new VBox(cabeceraDialogo, gridHeader, scroll));

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            List<Document> nuevaLista = new java.util.ArrayList<>();
            String ahora = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            for (Object[] ctrl : filaControles) {
                Document mOrig   = (Document) ctrl[0];
                int s            = parseEntero(((TextField) ctrl[1]).getText());
                int p            = parseEntero(((TextField) ctrl[3]).getText());
                boolean mod      = ((boolean[]) ctrl[7])[0];
                CheckBox cbPedCtrl = (CheckBox) ctrl[5];
                Document d = new Document(mOrig)
                        .append("salidaUnidad",  s)
                        .append("servirUnidad",  parseEntero(((TextField) ctrl[2]).getText()))
                        .append("preparado",     p)
                        .append("falta",         Math.max(0, s - p))
                        .append("validacion",    ((CheckBox) ctrl[4]).isSelected() ? "✔" : "✘")
                        .append("pedidoCompleto",cbPedCtrl.isSelected()            ? "✔" : "✘")
                        .append("observaciones", ((TextField) ctrl[6]).getText().trim())
                        .append("numeroPedido",  ((TextField) ctrl[8]).getText().trim());
                if (mod) d.append("fechaPedido", ahora);
                nuevaLista.add(d);
            }
            return nuevaLista;
        });

        final String refFinal = ref;
        dialog.showAndWait().ifPresent(lista -> {
            coleccion.updateOne(eq("obra", refFinal),
                    new Document("$set", new Document("materiales", lista)));
            vista.setEstado("✅ Obra " + refFinal + " actualizada.");
            onBuscarObra(refFinal);
        });
        actualizarMetricasDashboard();
    }

    private void bloquearFila(TextField tfServir, TextField tfPrep, TextField tfNumPed,
                               TextField tfObs, CheckBox cbVal, CheckBox cbPed) {
        String estiloLocked = "-fx-background-color: #f1f2f6; -fx-text-fill: #7f8c8d;";
        tfServir.setEditable(false); tfServir.setStyle(tfServir.getStyle() + estiloLocked);
        tfPrep  .setEditable(false); tfPrep  .setStyle(tfPrep  .getStyle() + estiloLocked);
        tfNumPed.setEditable(false); tfNumPed.setStyle(tfNumPed.getStyle() + estiloLocked);
        tfObs   .setEditable(false); tfObs   .setStyle(tfObs   .getStyle() + estiloLocked);
        cbVal.setDisable(true); cbVal.setOpacity(1.0);
        cbPed.setDisable(true); cbPed.setOpacity(1.0);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  8. ELIMINAR OBRA COMPLETA
    // ═════════════════════════════════════════════════════════════════════════
    private void onEliminarObra() {
        String ref = pedirObraConIcono("Eliminar obra");
        if (ref == null) return;
        Document obra = coleccion.find(eq("obra", ref)).first();
        if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }
        if (vista.confirmar("⚠ ¿Eliminar TODA la obra " + ref + "? Esta acción no se puede deshacer.")) {
            coleccion.deleteOne(eq("obra", ref));
            vista.limpiar();
            vista.setEstado("🗑 Obra " + ref + " eliminada.");
            onListarObras();
        }
        actualizarMetricasDashboard();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  9. BUSCAR POR CLIENTE
    // ═════════════════════════════════════════════════════════════════════════
    private void onBuscarCliente() {
        String cliente = vista.getInput();
        if (cliente.isEmpty())
            cliente = pedirTextoConIcono("Buscar cliente", "Introduce el nombre del cliente:");
        if (cliente == null || cliente.isEmpty()) return;
        List<Document> resultados = new java.util.ArrayList<>();
        try (MongoCursor<Document> cursor = coleccion.find(regex("cliente", cliente, "i")).iterator()) {
            while (cursor.hasNext()) resultados.add(cursor.next());
        }
        String[] cols = { "OBRA", "CLIENTE", "PROYECTO", "ENTREGA", "RESPONSABLE" };
        String[] keys = { "obra", "cliente", "proyecto", "entrega", "responsable" };
        mostrarTablaResultados("Resultados Cliente: " + cliente, resultados, cols, keys);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  10. BUSCAR POR REFERENCIA DE MATERIAL
    // ═════════════════════════════════════════════════════════════════════════
    private void onBuscarReferencia() {
        String refBusqueda = vista.getInput();
        if (refBusqueda.isEmpty())
            refBusqueda = pedirTextoConIcono("Buscar referencia", "Introduce la referencia del material:");
        if (refBusqueda == null || refBusqueda.isEmpty()) return;
        List<Document> filas = new java.util.ArrayList<>();
        try (MongoCursor<Document> cursor =
                     coleccion.find(eq("materiales.referencia", refBusqueda)).iterator()) {
            while (cursor.hasNext()) {
                Document obraDoc = cursor.next();
                List<Document> mats = obraDoc.getList("materiales", Document.class);
                for (Document m : mats) {
                    if (refBusqueda.equalsIgnoreCase(m.getString("referencia"))) {
                        m.append("obraRef", obraDoc.getString("obra"));
                        filas.add(m);
                    }
                }
            }
        }
        String[] cols = { "OBRA", "A3", "REFERENCIA", "MARCA", "DESCRIPCIÓN", "FALTA", "FECHA MODIF." };
        String[] keys = { "obraRef", "A3", "referencia", "marca", "descripcion", "falta", "fechaPedido" };
        mostrarTablaResultados("Búsqueda de Material: " + refBusqueda, filas, cols, keys);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  11. ORDENAR POR A3
    // ═════════════════════════════════════════════════════════════════════════
    private void onOrdenarA3() {
        String ref = pedirObraConIcono("Ordenar Obra");
        if (ref == null) return;
        Document doc = coleccion.find(eq("obra", ref)).first();
        if (doc == null) return;
        List<Document> mats = doc.getList("materiales", Document.class);
        mats.sort((a, b) -> Integer.compare(a.getInteger("A3", 0), b.getInteger("A3", 0)));
        String[] cols = { "A3", "MARCA", "REFERENCIA", "DESCRIPCIÓN", "FALTA", "N° PEDIDO" };
        String[] keys = { "A3", "marca", "referencia", "descripcion", "falta", "numeroPedido" };
        mostrarTablaResultados("Obra " + ref + " (Ordenada por A3)", mats, cols, keys);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  EXPORTAR (EXCEL O PDF)
    // ═════════════════════════════════════════════════════════════════════════
    private void onExportar() {
        String ref = pedirObraConIcono("Exportar obra");
        if (ref == null) return;

        Document obra = coleccion.find(eq("obra", ref)).first();
        if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }

        // ── Diálogo de elección de formato ───────────────────────────────────
        Dialog<String> fmtDialog = new Dialog<>();
        fmtDialog.setTitle("Exportar obra: " + ref);
        fmtDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox cab = new VBox(2);
        cab.setStyle("-fx-background-color: #2f3542; -fx-padding: 18;");
        Label lt = new Label("Selecciona el formato de exportación");
        lt.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label ls = new Label("Obra: " + ref + "  |  " + obra.getString("cliente"));
        ls.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 12px;");
        cab.getChildren().addAll(lt, ls);

        ToggleGroup tg = new ToggleGroup();
        RadioButton rbExcel = new RadioButton(); rbExcel.setToggleGroup(tg); rbExcel.setSelected(true);
        RadioButton rbPDF   = new RadioButton(); rbPDF  .setToggleGroup(tg);

        VBox cardExcel = crearTarjetaFormato(
                "📊  Excel (.xlsx)",
                "Hoja de cálculo completa con todos los materiales,\ntotales y formato corporativo.",
                "#1D6F42", rbExcel);
        VBox cardPDF = crearTarjetaFormato(
                "📄  PDF (.pdf)",
                "Documento listo para imprimir. Ficha por material\ncon colores de estado y datos de la obra.",
                "#C0392B", rbPDF);

        HBox opciones = new HBox(20, cardExcel, cardPDF);
        opciones.setPadding(new Insets(25));
        opciones.setAlignment(javafx.geometry.Pos.CENTER);

        fmtDialog.getDialogPane().setContent(new VBox(cab, opciones));
        fmtDialog.getDialogPane().setPrefWidth(540);
        fmtDialog.setResultConverter(btn ->
                btn == ButtonType.OK ? (rbExcel.isSelected() ? "xlsx" : "pdf") : null);

        fmtDialog.showAndWait().ifPresent(formato -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Guardar " + formato.toUpperCase());
            if ("xlsx".equals(formato)) {
                fc.setInitialFileName(ref + "_materiales.xlsx");
                fc.getExtensionFilters().add(
                        new javafx.stage.FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
            } else {
                fc.setInitialFileName(ref + "_materiales.pdf");
                fc.getExtensionFilters().add(
                        new javafx.stage.FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
            }

            File archivo = fc.showSaveDialog(null);
            if (archivo == null) return;

            vista.setEstado("⏳ Exportando a " + formato.toUpperCase() + "...");
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() throws Exception {
                    if ("xlsx".equals(formato)) {
                        Modelo.exportar(obra, archivo);
                    } else {
                        exportarObrasAPDF(obra, archivo);
                    }
                    return null;
                }
            };
            tarea.setOnSucceeded(e -> vista.setEstado("✅ Exportado: " + archivo.getName()));
            tarea.setOnFailed(e ->
                    vista.setEstado("❌ Error: " + tarea.getException().getMessage()));
            new Thread(tarea).start();
        });
    }

    /** Exporta todos los materiales de la obra en un único PDF con tabla continua */
    private void exportarObrasAPDF(Document obra, File destino) throws Exception {
        List<Document> mats = obra.getList("materiales", Document.class);
        if (mats == null || mats.isEmpty()) {
            Platform.runLater(() -> vista.mostrarAlerta("Info",
                    "La obra no tiene materiales para exportar."));
            return;
        }
        FichaPDF.generarObraCompleta(obra, destino);
    }
    /** Tarjeta visual para el diálogo de selección de formato */
    private VBox crearTarjetaFormato(String titulo, String desc,
                                      String color, RadioButton rb) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setPrefWidth(210);
        card.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        card.setStyle(
                "-fx-background-color: white;"
              + "-fx-background-radius: 12;"
              + "-fx-border-color: " + color + ";"
              + "-fx-border-width: 0 0 5 0;"
              + "-fx-border-radius: 12;"
              + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.10), 10, 0, 0, 4);"
              + "-fx-cursor: hand;");

        Label lblTit  = new Label(titulo);
        lblTit.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-text-fill: #57606f; -fx-font-size: 11px;");
        lblDesc.setWrapText(true);
        rb.setStyle("-fx-font-size: 11px; -fx-text-fill: #2f3542;");

        card.getChildren().addAll(lblTit, lblDesc, rb);
        card.setOnMouseClicked(e -> rb.setSelected(true));
        rb.selectedProperty().addListener((o, ov, nv) -> {
            if (nv) {
                card.setStyle(card.getStyle().replace("-fx-border-width: 0 0 5 0;", "-fx-border-width: 3;"));
            } else {
                card.setStyle(card.getStyle().replace("-fx-border-width: 3;", "-fx-border-width: 0 0 5 0;"));
            }
        });
        return card;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  IMPRIMIR ETIQUETAS / QR
    // ═════════════════════════════════════════════════════════════════════════
    private void onImprimirEtiquetas() {
        String ref = pedirObraConIcono("Imprimir Etiquetas / QR");
        if (ref == null) return;

        Document obra = coleccion.find(eq("obra", ref)).first();
        if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada: " + ref); return; }

        List<Document> mats = obra.getList("materiales", Document.class);
        if (mats == null || mats.isEmpty()) { vista.mostrarAlerta("Info", "La obra no tiene materiales."); return; }

        Dialog<String> opcionDialog = new Dialog<>();
        opcionDialog.setTitle("Imprimir Etiquetas QR");
        opcionDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox cabOpc = new VBox(2);
        cabOpc.setStyle("-fx-background-color: #2f3542; -fx-padding: 15;");
        Label ltOpc = new Label("Generación de Etiquetas QR");
        ltOpc.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label lsOpc = new Label("Obra: " + ref + "  |  " + obra.getString("cliente"));
        lsOpc.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 12px;");
        cabOpc.getChildren().addAll(ltOpc, lsOpc);

        GridPane gp = new GridPane();
        gp.setHgap(15); gp.setVgap(12);
        gp.setPadding(new Insets(25));

        ToggleGroup tg    = new ToggleGroup();
        RadioButton rbTodos = new RadioButton("Hoja completa (todos los materiales: " + mats.size() + ")");
        RadioButton rbUno   = new RadioButton("Solo un material (por número A3)");
        rbTodos.setToggleGroup(tg); rbTodos.setSelected(true);
        rbUno  .setToggleGroup(tg);

        TextField tfA3sel = new TextField();
        tfA3sel.setPromptText("Número A3..."); tfA3sel.setDisable(true); tfA3sel.setPrefWidth(120);
        rbUno.selectedProperty().addListener((o, ov, nv) -> tfA3sel.setDisable(!nv));

        gp.add(rbTodos, 0, 0, 2, 1);
        gp.add(rbUno,   0, 1);
        gp.add(tfA3sel, 1, 1);
        opcionDialog.getDialogPane().setContent(new VBox(cabOpc, gp));
        opcionDialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            return rbUno.isSelected() ? tfA3sel.getText().trim() : "TODOS";
        });

        opcionDialog.showAndWait().ifPresent(seleccion -> {
            if (seleccion == null) return;
            List<Document> aImprimir;
            if ("TODOS".equals(seleccion)) {
                aImprimir = mats;
            } else {
                int a3buscado;
                try { a3buscado = Integer.parseInt(seleccion); }
                catch (NumberFormatException ex) { vista.mostrarAlerta("Error", "A3 debe ser un número."); return; }
                final int a3f = a3buscado;
                aImprimir = mats.stream()
                        .filter(m -> m.getInteger("A3", -1) == a3f)
                        .collect(java.util.stream.Collectors.toList());
                if (aImprimir.isEmpty()) { vista.mostrarAlerta("Error", "No se encontró A3=" + seleccion); return; }
            }

            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Guardar hoja de etiquetas");
            fc.setInitialFileName(ref + "_etiquetas_QR.png");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
            File archivo = fc.showSaveDialog(null);
            if (archivo == null) return;

            final String        cliente   = obra.getString("cliente") != null ? obra.getString("cliente") : "";
            final List<Document> listaFinal = aImprimir;
            final String        refFinal  = ref;

            vista.setEstado("⏳ Generando etiquetas QR...");
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() throws Exception {
                    EtiquetaQR.generarHojaEtiquetas(refFinal, cliente, listaFinal, archivo);
                    return null;
                }
            };
            tarea.setOnSucceeded(e -> {
                vista.setEstado("✅ Etiquetas guardadas: " + archivo.getName());
                try {
                    if (java.awt.Desktop.isDesktopSupported())
                        java.awt.Desktop.getDesktop().open(archivo);
                } catch (Exception ignored) {}
            });
            tarea.setOnFailed(e -> vista.setEstado("❌ Error: " + tarea.getException().getMessage()));
            new Thread(tarea).start();
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SALIR
    // ═════════════════════════════════════════════════════════════════════════
    private void onSalir() {
        if (vista.confirmar("¿Estás seguro de que deseas cerrar el programa?")) {
            javafx.application.Platform.exit();
            System.exit(0);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  MOSTRAR TABLA DE RESULTADOS
    //  *** CORREGIDO: ahora guarda ultimosDatosOriginales para el filtro ***
    // ═════════════════════════════════════════════════════════════════════════
    @SuppressWarnings("deprecation")
    private void mostrarTablaResultados(String titulo, List<Document> datos,
                                         String[] columnas, String[] llaves) {
        if (datos == null || datos.isEmpty()) {
            vista.mostrarAlerta("Información", "No se encontraron resultados.");
            return;
        }

        // ── GUARDAR ESTADO PARA EL FILTRO EN TIEMPO REAL ─────────────────────
        ultimosDatosOriginales = new java.util.ArrayList<>(datos);
        ultimasColumnas        = columnas;
        ultimasLlaves          = llaves;
        ultimoTitulo           = titulo;
        // ─────────────────────────────────────────────────────────────────────

        vista.setContenidoCentral(construirTablaVBox(titulo, datos, columnas, llaves, false));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  FILTRO EN TIEMPO REAL
    // ═════════════════════════════════════════════════════════════════════════
    private void onFiltrarEnTiempoReal(String texto) {
        if (ultimosDatosOriginales == null || ultimasColumnas == null) return;

        String filtro = texto == null ? "" : texto.trim().toLowerCase();

        if (filtro.isEmpty()) {
            vista.setInputFiltrandoEstilo(false);
            // Reconstruir tabla original sin tocar ultimosDatosOriginales
            vista.setContenidoCentral(construirTablaVBox(
                    ultimoTitulo, ultimosDatosOriginales, ultimasColumnas, ultimasLlaves, false));
            vista.setEstado("📋 Mostrando " + ultimosDatosOriginales.size() + " resultados.");
            return;
        }

        List<Document> filtrados = ultimosDatosOriginales.stream()
                .filter(doc -> {
                    for (String llave : ultimasLlaves) {
                        Object valor = doc.get(llave);
                        if (valor != null && valor.toString().toLowerCase().contains(filtro)) return true;
                    }
                    return false;
                })
                .collect(java.util.stream.Collectors.toList());

        vista.setInputFiltrandoEstilo(!filtrados.isEmpty());

        if (filtrados.isEmpty()) {
            vista.setEstado("🔍 Sin resultados para: \"" + texto.trim() + "\"");
            // Mostrar tarjeta vacía
            VBox contenedor = new VBox();
            contenedor.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 8);");
            HBox cabecera = new HBox();
            cabecera.setPadding(new Insets(15, 25, 15, 25));
            cabecera.setStyle("-fx-background-color: linear-gradient(to right, #2c3e50, #4b6584); " +
                    "-fx-background-radius: 15 15 0 0;");
            Label lblTit = new Label(("Sin resultados para: " + texto.trim()).toUpperCase());
            lblTit.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 15px;");
            cabecera.getChildren().add(lblTit);
            Label lblVacio = new Label("😕  No se encontraron resultados para ese término");
            lblVacio.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 16px; -fx-padding: 50;");
            lblVacio.setMaxWidth(Double.MAX_VALUE);
            lblVacio.setAlignment(javafx.geometry.Pos.CENTER);
            contenedor.getChildren().addAll(cabecera, lblVacio);
            vista.setContenidoCentral(contenedor);
            return;
        }

        String tituloFiltrado = ultimoTitulo + "  🔍 \"" + texto.trim() + "\"  ("
                + filtrados.size() + " resultado" + (filtrados.size() == 1 ? "" : "s") + ")";
        // Usar cabecera verde para resultados filtrados
        vista.setContenidoCentral(construirTablaVBox(
                tituloFiltrado, filtrados, ultimasColumnas, ultimasLlaves, true));
        vista.setEstado("🔍 " + filtrados.size() + " resultado(s) para: \"" + texto.trim() + "\"");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR INTERNO DE TABLA (evita duplicar código entre los dos métodos)
    // ═════════════════════════════════════════════════════════════════════════
    @SuppressWarnings("deprecation")
    private VBox construirTablaVBox(String titulo, List<Document> datos,
                                     String[] columnas, String[] llaves,
                                     boolean esFiltrada) {
        VBox contenedorPrincipal = new VBox(0);
        contenedorPrincipal.setStyle("-fx-background-color: white; -fx-background-radius: 15; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 8);");

        HBox cabeceraColor = new HBox();
        cabeceraColor.setPadding(new Insets(15, 25, 15, 25));
        // Verde si es resultado de filtrado, azul-oscuro si es listado normal
        String gradiente = esFiltrada
                ? "-fx-background-color: linear-gradient(to right, #1a6e4c, #27ae60);"
                : "-fx-background-color: linear-gradient(to right, #2c3e50, #4b6584);";
        cabeceraColor.setStyle(gradiente + "-fx-background-radius: 15 15 0 0;");

        Label lblTit = new Label(titulo.toUpperCase());
        lblTit.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 15px; -fx-letter-spacing: 1px;");
        cabeceraColor.getChildren().add(lblTit);

        TableView<Document> tabla = new TableView<>();
        tabla.setColumnResizePolicy(columnas.length < 8
                ? TableView.CONSTRAINED_RESIZE_POLICY
                : TableView.UNCONSTRAINED_RESIZE_POLICY);
        tabla.setMinHeight(550);
        tabla.getSelectionModel().setCellSelectionEnabled(true);
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.C)
                copiarSeleccionAlPortapapeles(tabla);
        });

        ContextMenu menu = new ContextMenu();
        MenuItem itemCopiar = new MenuItem("📋 Copiar para Excel");
        itemCopiar.setStyle("-fx-font-weight: bold;");
        itemCopiar.setOnAction(e -> copiarSeleccionAlPortapapeles(tabla));
        menu.getItems().add(itemCopiar);
        tabla.setContextMenu(menu);

        for (int i = 0; i < columnas.length; i++) {
            final String llave = llaves[i];
            TableColumn<Document, Object> col = new TableColumn<>();
            Label lblHeader = new Label(columnas[i]);
            lblHeader.setStyle("-fx-text-fill: #000000; -fx-font-weight: 900; -fx-font-size: 11px;");
            col.setGraphic(lblHeader);

            switch (llave) {
                case "A3"                              -> col.setPrefWidth(60);
                case "salidaUnidad","preparado","falta"-> col.setPrefWidth(75);
                case "descripcion"                     -> col.setPrefWidth(400);
                case "observaciones"                   -> col.setPrefWidth(250);
                case "obra","obraRef"                  -> col.setPrefWidth(130);
                default                                -> col.setPrefWidth(150);
            }

            col.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleObjectProperty<>(d.getValue().get(llave)));
            col.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); }
                    else {
                        setText(item.toString());
                        if (llave.equals("falta")) {
                            int f = parseEntero(item.toString());
                            setStyle("-fx-text-fill: " + (f > 0 ? "#e74c3c" : "#27ae60") + "; -fx-font-weight: 900;");
                        } else if (llave.equals("descripcion") || llave.equals("A3")
                                || llave.equals("obra") || llave.equals("obraRef")) {
                            setStyle("-fx-text-fill: #000000; -fx-font-weight: 900;");
                        } else {
                            setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
                        }
                    }
                }
            });
            tabla.getColumns().add(col);
        }

        tabla.getItems().addAll(datos);
        contenedorPrincipal.getChildren().addAll(cabeceraColor, tabla);
        return contenedorPrincipal;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS PRIVADOS
    // ═════════════════════════════════════════════════════════════════════════
    private String pedirObraConIcono(String titulo) {
        String ref = vista.getInput();
        if (ref.isEmpty()) ref = pedirTextoConIcono(titulo, "Introduce la referencia de obra:");
        return ref.isEmpty() ? null : ref;
    }

    private static int parseEntero(String valor) {
        try {
            if (valor == null || valor.trim().isEmpty()) return 0;
            return (int) Double.parseDouble(valor.replace(",", "."));
        } catch (NumberFormatException e) { return 0; }
    }

    private void onLimpiarInput() {
        vista.limpiarInput();
        vista.setEstado("Input limpiado.");
    }

    private TextField styledField(String value, double width) {
        TextField tf = new TextField(value != null ? value : "");
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");
        return tf;
    }

    private CheckBox styledCheckbox(boolean selected, String text) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        return cb;
    }

    private void copiarSeleccionAlPortapapeles(TableView<Document> tabla) {
        StringBuilder sb = new StringBuilder();
        var celdas = tabla.getSelectionModel().getSelectedCells();
        int filaActual = -1;
        for (TablePosition pos : celdas) {
            int fila = pos.getRow();
            int col  = pos.getColumn();
            if (filaActual != -1 && filaActual != fila) sb.append("\n");
            else if (filaActual == fila) sb.append("\t");
            Object valor = tabla.getColumns().get(col).getCellData(fila);
            sb.append(valor == null ? "" : valor.toString());
            filaActual = fila;
        }
        javafx.scene.input.ClipboardContent contenido = new javafx.scene.input.ClipboardContent();
        contenido.putString(sb.toString());
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(contenido);
        vista.setEstado("📋 Copiado al portapapeles.");
    }

    private void actualizarMetricasDashboard() {
        Task<int[]> tarea = new Task<>() {
            @Override protected int[] call() {
                return new int[]{
                    (int) coleccion.countDocuments(),
                    Modelo.contarTodosLosMateriales(coleccion),
                    Modelo.contarAlertasFalta(coleccion)
                };
            }
        };
        tarea.setOnSucceeded(e ->
                vista.actualizarDashboard(tarea.getValue()[0], tarea.getValue()[1], tarea.getValue()[2]));
        new Thread(tarea).start();
    }
}