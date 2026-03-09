package Proyect;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;

public class Controlador {

    private final Vista vista;
    private final MongoCollection<Document> coleccion;

    public Controlador(Vista vista, MongoCollection<Document> coleccion) {
        this.vista      = vista;
        this.coleccion  = coleccion;
        registrarEventos();
    }

    private void registrarEventos() {
        vista.getBtnImportar()     .setOnAction(e -> onImportar());
        vista.getBtnListarObras()  .setOnAction(e -> onListarObras());
        vista.getBtnBuscarObra()   .setOnAction(e -> onBuscarObra());
        vista.getBtnEstadisticas() .setOnAction(e -> onEstadisticas());
        vista.getBtnAgregarFila()  .setOnAction(e -> onAgregarFila());
        vista.getBtnEliminarFila() .setOnAction(e -> onEliminarFila());
        vista.getBtnActualizar()   .setOnAction(e -> onActualizar());
        vista.getBtnEliminarObra() .setOnAction(e -> onEliminarObra());
        vista.getBtnBuscarCliente().setOnAction(e -> onBuscarCliente());
        vista.getBtnBuscarRef()    .setOnAction(e -> onBuscarReferencia());
        vista.getBtnOrdenarA3()    .setOnAction(e -> onOrdenarA3());
        vista.getBtnContarObras()  .setOnAction(e -> onContarObras());
    }

    // ── 1. Importar Excel ─────────────────────────────────────────────────────
    private void onImportar() {
        // Paso 1: FileChooser AQUÍ, en hilo JavaFX ✅
        File archivo = Modelo.seleccionarArchivo();
        if (archivo == null) {
            vista.setEstado("No se seleccionó archivo.");
            return;
        }

        // Paso 2: procesamiento en hilo secundario
        vista.setEstado("Importando...");
        vista.limpiar();

        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws Exception {
                Modelo.importarExcelAMongo(archivo, coleccion); // pasa el File
                return null;
            }
        };
        tarea.setOnSucceeded(e -> {
            vista.setEstado("✅ Excel importado correctamente.");
            onListarObras();
        });
        tarea.setOnFailed(e ->
            Platform.runLater(() -> vista.setEstado("❌ Error: " + tarea.getException().getMessage()))
        );
        new Thread(tarea).start();
    }

    // ── 2. Listar todas las obras ─────────────────────────────────────────────
    private void onListarObras() {
        vista.limpiar();
        StringBuilder sb = new StringBuilder();
        String fmt = "%-5s %-15s %-25s %-25s %-20s%n";
        sb.append(String.format(fmt, "#", "REF. OBRA", "CLIENTE", "PROYECTO", "ENTREGA"));
        sb.append("─".repeat(95)).append("\n");

        int i = 0;
        try (MongoCursor<Document> cursor = coleccion.find().iterator()) {
            while (cursor.hasNext()) {
                i++;
                JSONObject j = new JSONObject(cursor.next().toJson());
                sb.append(String.format(fmt, i,
                    j.optString("obra",     "N/A"),
                    j.optString("cliente",  "N/A"),
                    j.optString("proyecto", "N/A"),
                    j.optString("entrega",  "N/A")));
            }
        }
        vista.mostrarTexto(i == 0 ? "No hay obras registradas." : sb.toString());
        vista.setEstado("Total obras: " + i);
    }

    // ── 3. Buscar obra por referencia ─────────────────────────────────────────
    private void onBuscarObra() {
        String ref = vista.getInput();
        if (ref.isEmpty()) ref = vista.pedirTexto("Buscar obra", "Referencia de obra:");
        if (ref.isEmpty()) return;

        Document doc = coleccion.find(eq("obra", ref)).first();
        if (doc == null) {
            vista.mostrarTexto("❌ No se encontró la obra: " + ref);
            vista.setEstado("Obra no encontrada.");
            return;
        }

        JSONObject json = new JSONObject(doc.toJson());
        StringBuilder sb = new StringBuilder();

        sb.append("═".repeat(160)).append("\n");
        sb.append("  ").append(json.optString("titulo", "SIN TÍTULO").toUpperCase()).append("\n");
        sb.append("═".repeat(160)).append("\n");
        sb.append(String.format("  %-15s %s%n", "REF. OBRA:",   json.optString("obra",        "N/A")));
        sb.append(String.format("  %-15s %s%n", "CLIENTE:",     json.optString("cliente",      "N/A")));
        sb.append(String.format("  %-15s %s%n", "RESPONSABLE:", json.optString("responsable",  "N/A")));
        sb.append(String.format("  %-15s %s%n", "PROYECTO:",    json.optString("proyecto",     "N/A")));
        sb.append(String.format("  %-15s %s%n", "IMPRESIÓN:",   json.optString("impresion",    "N/A")));
        sb.append(String.format("  %-15s %s%n", "ENTREGA:",     json.optString("entrega",      "N/A")));
        sb.append("─".repeat(160)).append("\n");

        String fmt = "| %-6s | %-12s | %-15s | %-30s | %-6s | %-6s | %-10s | %-10s | %-5s | %-15s | %-14s | %-15s |%n";
        sb.append(String.format(fmt,
            "A3","MARCA","REFERENCIA","DESCRIPCIÓN",
            "SALIDA","SERVIR","VALIDACIÓN","PREPARADO",
            "FALTA","PEDIDO COMP.","FECHA PED.","OBSERVACIONES"));
        sb.append("─".repeat(160)).append("\n");

        JSONArray mats = json.optJSONArray("materiales");
        if (mats != null) {
            for (int i = 0; i < mats.length(); i++) {
                JSONObject m = mats.getJSONObject(i);
                String desc = m.optString("descripcion","");
                if (desc.length() > 30) desc = desc.substring(0,27) + "...";
                sb.append(String.format(fmt,
                    m.optInt("A3"), m.optString("marca",""), m.optString("referencia",""), desc,
                    m.optInt("salidaUnidad"), m.optInt("servirUnidad"),
                    m.optString("validacion",""), m.optString("preparado",""),
                    m.optInt("falta"), m.optString("pedidoCompleto",""),
                    m.optString("fechaPedido",""), m.optString("observaciones","")));
            }
        }
        sb.append("─".repeat(160)).append("\n");
        sb.append(String.format("  Total materiales: %d%n", mats != null ? mats.length() : 0));

        vista.mostrarTexto(sb.toString());
        vista.setEstado("Obra cargada: " + ref);
    }

    // ── 4. Estadísticas ───────────────────────────────────────────────────────
    private void onEstadisticas() {
        String ref = vista.getInput();
        if (ref.isEmpty()) ref = vista.pedirTexto("Estadísticas", "Referencia de obra:");
        if (ref.isEmpty()) return;

        Document obra = coleccion.find(eq("obra", ref)).first();
        if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada: " + ref); return; }

        List<Document> mats = (List<Document>) obra.get("materiales");
        int totalSalida = 0, totalServir = 0, totalFalta = 0;
        for (Document m : mats) {
            totalSalida += m.getInteger("salidaUnidad", 0);
            totalServir += m.getInteger("servirUnidad", 0);
            totalFalta  += m.getInteger("falta",        0);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═".repeat(45)).append("\n");
        sb.append(String.format("  ESTADÍSTICAS OBRA: %s%n", ref));
        sb.append("═".repeat(45)).append("\n");
        sb.append(String.format("  %-22s %d%n", "Total materiales:", mats.size()));
        sb.append(String.format("  %-22s %d%n", "Total salida:",     totalSalida));
        sb.append(String.format("  %-22s %d%n", "Total servir:",     totalServir));
        sb.append(String.format("  %-22s %d%n", "Total falta:",      totalFalta));
        sb.append("═".repeat(45)).append("\n");

        vista.mostrarTexto(sb.toString());
        vista.setEstado("Estadísticas de: " + ref);
    }

    // ── 5. Agregar fila ───────────────────────────────────────────────────────
    // falta y fechaPedido NO se piden → falta = salidaUnidad (nada preparado aún)
    // validacion y pedidoCompleto → checkboxes
    private void onAgregarFila() {
        String ref = pedirObra("Agregar fila");
        if (ref == null) return;

        Document obra = coleccion.find(eq("obra", ref)).first();
        if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }

        // ── Diálogo personalizado con todos los campos ────────────────────────
        Dialog<Document> dialog = new Dialog<>();
        dialog.setTitle("Agregar nueva fila");
        dialog.setHeaderText("Obra: " + ref);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        // Campos de texto
        TextField tfA3          = new TextField();
        TextField tfMarca       = new TextField();
        TextField tfReferencia  = new TextField();
        TextField tfDescripcion = new TextField(); tfDescripcion.setPrefWidth(250);
        TextField tfSalida      = new TextField();
        TextField tfServir      = new TextField();
        TextField tfPreparado   = new TextField();
        TextField tfObservaciones = new TextField(); tfObservaciones.setPrefWidth(250);

        // Checkboxes
        CheckBox cbValidacion     = new CheckBox("Validado");
        CheckBox cbPedidoCompleto = new CheckBox("Pedido completo");

        int row = 0;
        grid.add(new Label("A3:"),           0, row); grid.add(tfA3,            1, row++);
        grid.add(new Label("Marca:"),         0, row); grid.add(tfMarca,         1, row++);
        grid.add(new Label("Referencia:"),    0, row); grid.add(tfReferencia,    1, row++);
        grid.add(new Label("Descripción:"),   0, row); grid.add(tfDescripcion,   1, row++);
        grid.add(new Label("Salida unidad:"), 0, row); grid.add(tfSalida,        1, row++);
        grid.add(new Label("Servir unidad:"), 0, row); grid.add(tfServir,        1, row++);
        grid.add(new Label("Preparado:"),     0, row); grid.add(tfPreparado,     1, row++);
        grid.add(cbValidacion,                0, row, 2, 1); row++;
        grid.add(cbPedidoCompleto,            0, row, 2, 1); row++;
        grid.add(new Label("Observaciones:"), 0, row); grid.add(tfObservaciones, 1, row++);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(tfA3::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;

            int a3;
            try { a3 = Integer.parseInt(tfA3.getText().trim()); }
            catch (NumberFormatException e) { return null; }

            int salida    = parseEntero(tfSalida.getText());
            int preparado = parseEntero(tfPreparado.getText());
            int falta     = Math.max(0, salida - preparado); // calculado automáticamente

            return new Document()
                .append("A3",            a3)
                .append("marca",         tfMarca.getText().trim())
                .append("referencia",    tfReferencia.getText().trim())
                .append("descripcion",   tfDescripcion.getText().trim())
                .append("salidaUnidad",  salida)
                .append("servirUnidad",  parseEntero(tfServir.getText()))
                .append("validacion",    cbValidacion.isSelected() ? "✔" : "✘")
                .append("preparado",     preparado)
                .append("falta",         falta)                          // automático
                .append("pedidoCompleto",cbPedidoCompleto.isSelected() ? "✔" : "✘")
                .append("fechaPedido",   new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()))
                .append("observaciones", tfObservaciones.getText().trim());
        });

        Optional<Document> resultado = dialog.showAndWait();
        if (resultado.isEmpty()) return;
        Document nuevaFila = resultado.get();

        int a3 = nuevaFila.getInteger("A3");
        List<Document> mats = obra.getList("materiales", Document.class);
        if (mats != null && Modelo.existeA3(mats, a3)) {
            vista.mostrarAlerta("Error", "El A3 " + a3 + " ya existe en esta obra."); return;
        }

        coleccion.updateOne(eq("obra", ref), new Document("$push", new Document("materiales", nuevaFila)));
        vista.setEstado("✅ Fila A3=" + a3 + " agregada. Falta calculada: " + nuevaFila.getInteger("falta"));
        onBuscarObra();
    }

    // ── 6. Eliminar fila ──────────────────────────────────────────────────────
    private void onEliminarFila() {
        String ref = pedirObra("Eliminar fila");
        if (ref == null) return;

        Document obra = coleccion.find(eq("obra", ref)).first();
        if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }

        String a3Str = vista.pedirTexto("Eliminar fila", "Número A3 a eliminar:");
        if (a3Str.isEmpty()) return;
        int a3;
        try { a3 = Integer.parseInt(a3Str); } catch (NumberFormatException e) {
            vista.mostrarAlerta("Error", "A3 debe ser un número."); return;
        }

        List<Document> mats = (List<Document>) obra.get("materiales");
        if (mats.stream().noneMatch(m -> m.getInteger("A3").equals(a3))) {
            vista.mostrarAlerta("Error", "El A3 no existe en esa obra."); return;
        }

        if (vista.confirmar("¿Eliminar la fila A3=" + a3 + " de la obra " + ref + "?")) {
            coleccion.updateOne(eq("obra", ref),
                new Document("$pull", new Document("materiales", new Document("A3", a3))));
            vista.setEstado("🗑 Fila A3=" + a3 + " eliminada.");
            onBuscarObra();
        }
    }

    // ── 7. Actualizar campo ───────────────────────────────────────────────────
    private void onActualizar() {
        String ref = pedirObra("Actualizar campo");
        if (ref == null) return;

        Document obra = coleccion.find(eq("obra", ref)).first();
        if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }

        String a3Str = vista.pedirTexto("Actualizar", "Número A3:");
        if (a3Str.isEmpty()) return;
        int a3;
        try { a3 = Integer.parseInt(a3Str); } catch (NumberFormatException e) {
            vista.mostrarAlerta("Error", "A3 debe ser un número."); return;
        }

        List<Document> mats = (List<Document>) obra.get("materiales");
        if (mats.stream().noneMatch(m -> m.getInteger("A3").equals(a3))) {
            vista.mostrarAlerta("Error", "El A3 no existe."); return;
        }

        String[] nombresOpciones = {
            "1. Marca","2. Referencia","3. Descripción",
            "4. Salida","5. Servir","6. Validación",
            "7. Preparado","8. Falta","9. Pedido Completo",
            "10. Fecha Pedido","11. Observaciones"
        };
        String[] camposMongo = {
            "marca","referencia","descripcion",
            "salidaUnidad","servirUnidad","validacion",
            "preparado","falta","pedidoCompleto",
            "fechaPedido","observaciones"
        };

        String elegido = vista.elegirOpcion("Actualizar campo", "¿Qué campo?", nombresOpciones);
        if (elegido.isEmpty()) return;

        int idx = -1;
        for (int i = 0; i < nombresOpciones.length; i++) {
            if (nombresOpciones[i].equals(elegido)) { idx = i; break; }
        }
        if (idx == -1) return;

        String nuevoValor;

        // validacion (idx=5) y pedidoCompleto (idx=8) → checkbox
        if (idx == 5 || idx == 8) {
            String campo = idx == 5 ? "Validación" : "Pedido Completo";
            CheckBox cb = new CheckBox(campo);

            // Precargamos el estado actual
            Document matActual = mats.stream()
                .filter(m -> m.getInteger("A3").equals(a3))
                .findFirst().orElse(null);
            if (matActual != null) {
                String valActual = matActual.getString(camposMongo[idx]);
                cb.setSelected(valActual != null && valActual.equals("✔"));
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Actualizar " + campo);
            alert.setHeaderText(null);
            VBox content = new VBox(10, new Label("Marca el estado:"), cb);
            content.setPadding(new Insets(10));
            alert.getDialogPane().setContent(content);
            alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> res = alert.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) return;
            nuevoValor = cb.isSelected() ? "✔" : "✘";
        } else {
            nuevoValor = vista.pedirTexto("Nuevo valor", "Ingresa el valor para '" + elegido + "':");
        }

        if (!vista.confirmar("¿Confirmar actualización?")) { vista.setEstado("Cancelado."); return; }

        String fechaHoy = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());

        Document setFields = new Document("materiales.$." + camposMongo[idx], nuevoValor)
                                .append("materiales.$.fechaPedido", fechaHoy);
        coleccion.updateOne(and(eq("obra", ref), eq("materiales.A3", a3)), new Document("$set", setFields));

        // Si se actualizó PREPARADO → recalcular FALTA
        if (idx == 6) {
            Document obraAct = coleccion.find(eq("obra", ref)).first();
            List<Document> matsAct = (List<Document>) obraAct.get("materiales");
            for (Document m : matsAct) {
                if (m.getInteger("A3").equals(a3)) {
                    int salida    = m.getInteger("salidaUnidad", 0);
                    int preparado = parseEntero(m.getOrDefault("preparado", "0").toString());
                    int falta     = Math.max(0, salida - preparado);
                    coleccion.updateOne(and(eq("obra", ref), eq("materiales.A3", a3)),
                        new Document("$set", new Document("materiales.$.falta", falta)));
                    vista.setEstado("✅ Preparado actualizado → Falta: " + falta + " | 📅 " + fechaHoy);
                    break;
                }
            }
        } else {
            vista.setEstado("✅ '" + elegido + "' actualizado | 📅 " + fechaHoy);
        }
        onBuscarObra();
    }

    // ── 8. Eliminar obra completa ─────────────────────────────────────────────
    private void onEliminarObra() {
        String ref = pedirObra("Eliminar obra");
        if (ref == null) return;

        Document obra = coleccion.find(eq("obra", ref)).first();
        if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }

        if (vista.confirmar("⚠ ¿Eliminar TODA la obra " + ref + "? Esta acción no se puede deshacer.")) {
            coleccion.deleteOne(eq("obra", ref));
            vista.limpiar();
            vista.setEstado("🗑 Obra " + ref + " eliminada.");
            onListarObras();
        }
    }

    // ── 9. Buscar por cliente ─────────────────────────────────────────────────
    private void onBuscarCliente() {
        String cliente = vista.getInput();
        if (cliente.isEmpty()) cliente = vista.pedirTexto("Buscar cliente", "Nombre del cliente:");
        if (cliente.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s %-25s %-20s%n", "REF. OBRA", "CLIENTE", "PROYECTO"));
        sb.append("─".repeat(65)).append("\n");

        boolean encontrado = false;
        final String cf = cliente;
        try (MongoCursor<Document> cursor = coleccion.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String c = doc.getString("cliente");
                if (c != null && c.toLowerCase().contains(cf.toLowerCase())) {
                    sb.append(String.format("%-15s %-25s %-20s%n",
                        doc.getOrDefault("obra","N/A"), c, doc.getOrDefault("proyecto","N/A")));
                    encontrado = true;
                }
            }
        }
        vista.mostrarTexto(encontrado ? sb.toString() : "No se encontró el cliente: " + cliente);
        vista.setEstado(encontrado ? "Resultados para: " + cliente : "Sin resultados.");
    }

    // ── 10. Buscar por referencia de material ─────────────────────────────────
    private void onBuscarReferencia() {
        String ref = vista.getInput();
        if (ref.isEmpty()) ref = vista.pedirTexto("Buscar referencia", "Referencia del material:");
        if (ref.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s %-12s %-15s %-35s%n", "OBRA","MARCA","REFERENCIA","DESCRIPCIÓN"));
        sb.append("─".repeat(80)).append("\n");

        boolean encontrado = false;
        final String rf = ref;
        try (MongoCursor<Document> cursor = coleccion.find().iterator()) {
            while (cursor.hasNext()) {
                JSONObject jsonO = new JSONObject(cursor.next().toJson());
                String obra = jsonO.optString("obra","?");
                JSONArray mats = jsonO.optJSONArray("materiales");
                if (mats == null) continue;
                for (int i = 0; i < mats.length(); i++) {
                    JSONObject m = mats.getJSONObject(i);
                    if (m.optString("referencia").equalsIgnoreCase(rf)) {
                        sb.append(String.format("%-15s %-12s %-15s %-35s%n",
                            obra, m.optString("marca",""), m.optString("referencia",""), m.optString("descripcion","")));
                        encontrado = true;
                    }
                }
            }
        }
        vista.mostrarTexto(encontrado ? sb.toString() : "No hay materiales con esa referencia.");
        vista.setEstado(encontrado ? "Referencia: " + ref : "Sin resultados.");
    }

    // ── 11. Ordenar por A3 ────────────────────────────────────────────────────
    private void onOrdenarA3() {
        String ref = vista.getInput();
        if (ref.isEmpty()) ref = vista.pedirTexto("Ordenar por A3", "Referencia de obra:");
        if (ref.isEmpty()) return;

        Document obraDoc = coleccion.find(eq("obra", ref)).first();
        if (obraDoc == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }

        JSONArray mats = new JSONObject(obraDoc.toJson()).getJSONArray("materiales");
        java.util.List<JSONObject> lista = new java.util.ArrayList<>();
        for (int i = 0; i < mats.length(); i++) lista.add(mats.getJSONObject(i));
        lista.sort((m1, m2) -> Integer.compare(m1.getInt("A3"), m2.getInt("A3")));

        StringBuilder sb = new StringBuilder();
        String fmt = "| %-6s | %-12s | %-15s | %-30s | %-6s | %-6s | %-5s |%n";
        sb.append("─".repeat(100)).append("\n");
        sb.append(String.format(fmt,"A3","MARCA","REFERENCIA","DESCRIPCIÓN","SALIDA","SERVIR","FALTA"));
        sb.append("─".repeat(100)).append("\n");
        for (JSONObject m : lista) {
            String desc = m.optString("descripcion","");
            if (desc.length() > 30) desc = desc.substring(0,27)+"...";
            sb.append(String.format(fmt,
                m.optInt("A3"), m.optString("marca",""), m.optString("referencia",""),
                desc, m.optInt("salidaUnidad"), m.optInt("servirUnidad"), m.optInt("falta")));
        }
        sb.append("─".repeat(100)).append("\n");
        vista.mostrarTexto(sb.toString());
        vista.setEstado("Materiales ordenados por A3 — obra: " + ref);
    }

    // ── 12. Contar obras ──────────────────────────────────────────────────────
    private void onContarObras() {
        long total = coleccion.countDocuments();
        vista.mostrarTexto("Total de obras en la base de datos: " + total);
        vista.setEstado("Total: " + total + " obras.");
    }

    // ── Helpers privados ──────────────────────────────────────────────────────
    private String pedirObra(String titulo) {
        String ref = vista.getInput();
        if (ref.isEmpty()) ref = vista.pedirTexto(titulo, "Referencia de obra:");
        return ref.isEmpty() ? null : ref;
    }

    private static int parseEntero(String valor) {
        try {
            if (valor == null || valor.trim().isEmpty()) return 0;
            return (int) Double.parseDouble(valor.replace(",","."));
        } catch (NumberFormatException e) { return 0; }
    }
}