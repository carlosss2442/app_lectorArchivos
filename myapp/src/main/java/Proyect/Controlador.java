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
import javafx.scene.layout.VBox;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import static com.mongodb.client.model.Filters.*;

public class Controlador {

	private final Vista vista;
	private final MongoCollection<Document> coleccion;

	public Controlador(Vista vista, MongoCollection<Document> coleccion) {
		this.vista = vista;
		this.coleccion = coleccion;
		registrarEventos();
	}

	private void registrarEventos() {
		vista.getBtnImportar().setOnAction(e -> onImportar());
		vista.getBtnListarObras().setOnAction(e -> onListarObras());
		vista.getBtnBuscarObra().setOnAction(e -> onBuscarObra());
		vista.getBtnEstadisticas().setOnAction(e -> onEstadisticas());
		vista.getBtnAgregarFila().setOnAction(e -> onAgregarFila());
		vista.getBtnEliminarFila().setOnAction(e -> onEliminarFila());
		vista.getBtnActualizar().setOnAction(e -> onActualizar());
		vista.getBtnEliminarObra().setOnAction(e -> onEliminarObra());
		vista.getBtnBuscarCliente().setOnAction(e -> onBuscarCliente());
		vista.getBtnBuscarRef().setOnAction(e -> onBuscarReferencia());
		vista.getBtnOrdenarA3().setOnAction(e -> onOrdenarA3());
		vista.getBtnContarObras().setOnAction(e -> onContarObras());
		vista.getBtnLimpiarInput().setOnAction(e -> onLimpiarInput());
		vista.getBtnExportar().setOnAction(e -> onExportar());
	}

	// ── 1. Importar Excel ─────────────────────────────────────────────────────
	private void onImportar() {
		File archivo = Modelo.seleccionarArchivo();
		if (archivo == null) { vista.setEstado("No se seleccionó archivo."); return; }
		vista.setEstado("Importando...");
		vista.limpiar();
		Task<Void> tarea = new Task<>() {
			@Override protected Void call() throws Exception {
				Modelo.importarExcelAMongo(archivo, coleccion);
				return null;
			}
		};
		tarea.setOnSucceeded(e -> { vista.setEstado("✅ Excel importado correctamente."); onListarObras(); });
		tarea.setOnFailed(e -> Platform.runLater(() -> vista.setEstado("❌ Error: " + tarea.getException().getMessage())));
		new Thread(tarea).start();
	}

	// ── 2. Listar todas las obras ─────────────────────────────────────────────
	private void onListarObras() {
		List<Document> obras = new java.util.ArrayList<>();
		try (MongoCursor<Document> cursor = coleccion.find().iterator()) {
			while (cursor.hasNext()) obras.add(cursor.next());
		}
		String[] cols = {"REF. OBRA", "CLIENTE", "PROYECTO", "ENTREGA", "RESPONSABLE"};
		String[] keys = {"obra", "cliente", "proyecto", "entrega", "responsable"};
		mostrarTablaResultados("Listado General de Obras", obras, cols, keys);
		vista.setEstado("Total obras: " + obras.size());
	}

	// ── 3. Buscar obra por referencia ─────────────────────────────────────────
	private void onBuscarObra() {
		String ref = vista.getInput();
		if (ref.isEmpty()) ref = vista.pedirTexto("Buscar obra", "Referencia de obra:");
		if (ref == null || ref.isEmpty()) return;
		Document doc = coleccion.find(eq("obra", ref)).first();
		if (doc == null) { vista.mostrarAlerta("Error", "No se encontró la obra: " + ref); return; }
		List<Document> materiales = doc.getList("materiales", Document.class);
		String[] cols = {"A3", "MARCA", "REFERENCIA", "DESCRIPCIÓN", "SALIDA", "PREPARADO", "FALTA", "VALIDACIÓN", "PEDIDO COMPLETO", "FECHA MODIF.", "OBSERVACIONES"};
		String[] keys = {"A3", "marca", "referencia", "descripcion", "salidaUnidad", "preparado", "falta", "validacion", "pedidoCompleto", "fechaPedido", "observaciones"};
		mostrarTablaResultados("Detalle Obra: " + ref + " | Cliente: " + doc.getString("cliente"), materiales, cols, keys);
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
			totalFalta  += m.getInteger("falta", 0);
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
	private void onAgregarFila() {
		String ref = pedirObra("Agregar fila");
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

		TextField tfA3          = styledField("", 200);   tfA3.setPromptText("Ej: 101");
		TextField tfMarca       = styledField("", 200);
		TextField tfReferencia  = styledField("", 200);
		TextField tfDescripcion = styledField("", 250);
		TextField tfSalida      = styledField("0", 100);
		TextField tfServir      = styledField("0", 100);
		TextField tfPreparado   = styledField("0", 100);
		TextField tfObs         = styledField("", 250);
		CheckBox  cbVal         = styledCheckbox(false, "Validación");
		CheckBox  cbPed         = styledCheckbox(false, "Pedido Completo");

		int row = 0;
		addFormRow(grid, "Número A3:",       tfA3,          row++);
		addFormRow(grid, "Marca:",           tfMarca,       row++);
		addFormRow(grid, "Referencia:",      tfReferencia,  row++);
		addFormRow(grid, "Descripción:",     tfDescripcion, row++);
		grid.add(new Separator(), 0, row++, 2, 1);
		addFormRow(grid, "Unidades Salida:", tfSalida,      row++);
		addFormRow(grid, "Unidades Servir:", tfServir,      row++);
		addFormRow(grid, "Ya Preparado:",    tfPreparado,   row++);
		grid.add(new Label("Estados:"), 0, row);
		grid.add(new HBox(15, cbVal, cbPed), 1, row++);
		addFormRow(grid, "Observaciones:",   tfObs,         row++);

		dialog.getDialogPane().setContent(new VBox(cabecera, grid));
		Platform.runLater(tfA3::requestFocus);

		dialog.setResultConverter(btn -> {
			if (btn != ButtonType.OK) return null;
			try {
				int a3     = Integer.parseInt(tfA3.getText().trim());
				int salida = parseEntero(tfSalida.getText());
				int prep   = parseEntero(tfPreparado.getText());
				return new Document()
					.append("A3",             a3)
					.append("marca",          tfMarca.getText().trim())
					.append("referencia",     tfReferencia.getText().trim())
					.append("descripcion",    tfDescripcion.getText().trim())
					.append("salidaUnidad",   salida)
					.append("servirUnidad",   parseEntero(tfServir.getText()))
					.append("validacion",     cbVal.isSelected() ? "✔" : "✘")
					.append("preparado",      prep)
					.append("falta",          Math.max(0, salida - prep))
					.append("pedidoCompleto", cbPed.isSelected() ? "✔" : "✘")
					.append("fechaPedido",    new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()))
					.append("observaciones",  tfObs.getText().trim());
			} catch (Exception e) { return null; }
		});

		dialog.showAndWait().ifPresent(nuevaFila -> {
			int a3 = nuevaFila.getInteger("A3");
			List<Document> mats = obra.getList("materiales", Document.class);
			if (mats != null && Modelo.existeA3(mats, a3)) {
				vista.mostrarAlerta("Error", "El A3 " + a3 + " ya existe en esta obra.");
				return;
			}
			coleccion.updateOne(eq("obra", ref), new Document("$push", new Document("materiales", nuevaFila)));
			vista.setEstado("✅ Fila A3=" + a3 + " añadida correctamente.");
			onBuscarObra();
		});
	}

	private void addFormRow(GridPane grid, String labelText, javafx.scene.Node field, int row) {
		Label label = new Label(labelText);
		label.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606f;");
		grid.add(label, 0, row);
		grid.add(field, 1, row);
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
			onBuscarObra();
		}
	}

	// ── 7. Actualizar / Editar obra ───────────────────────────────────────────
	private void onActualizar() {
		String ref = pedirObra("Editar obra");
		if (ref == null) return;

		Document obra = coleccion.find(eq("obra", ref)).first();
		if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }

		List<Document> mats = obra.getList("materiales", Document.class);
		if (mats == null || mats.isEmpty()) { vista.mostrarAlerta("Info", "La obra no tiene materiales."); return; }

		Dialog<List<Document>> dialog = new Dialog<>();
		dialog.setTitle("Editor Maestro de Obras");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		dialog.getDialogPane().setPrefWidth(1100);
		dialog.getDialogPane().setPrefHeight(650);

		VBox cabeceraDialogo = new VBox(2);
		cabeceraDialogo.setStyle("-fx-background-color: #2f3542; -fx-padding: 15;");
		Label lblTitulo = new Label("Editando Obra: " + ref);
		lblTitulo.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
		Label lblSub = new Label("Proyecto: " + obra.getString("proyecto"));
		lblSub.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 13px;");
		cabeceraDialogo.getChildren().addAll(lblTitulo, lblSub);

		// ── 9 columnas visibles ───────────────────────────────────────────────
		double[] colWidths = { 50, 85, 85, 95, 75, 95, 95, 145, 200 };

		GridPane gridHeader = new GridPane();
		gridHeader.setHgap(10);
		gridHeader.setPadding(new Insets(12, 15, 12, 15));
		gridHeader.setStyle("-fx-background-color: #f1f2f6; -fx-border-color: #dfe4ea; -fx-border-width: 0 0 1 0;");

		String[] headers = { "A3", "SALIDA 🔒", "SERVIR", "PREPARADO", "FALTA", "VALIDAR", "PED. COMPL.", "FECHA MODIF.", "OBSERVACIONES" };
		for (int i = 0; i < headers.length; i++) {
			Label l = new Label(headers[i]);
			l.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606f; -fx-font-size: 11px;");
			l.setMinWidth(colWidths[i]);
			l.setMaxWidth(colWidths[i]);
			gridHeader.add(l, i, 0);
		}

		GridPane gridDatos = new GridPane();
		gridDatos.setHgap(10);
		gridDatos.setVgap(8);
		gridDatos.setPadding(new Insets(10, 15, 10, 15));

		List<Object[]> filaControles = new java.util.ArrayList<>();

		for (int i = 0; i < mats.size(); i++) {
			Document m = mats.get(i);

			// Col 1 - Salida (bloqueado)
			TextField tfSalida = styledField(String.valueOf(m.getInteger("salidaUnidad", 0)), colWidths[1]);
			tfSalida.setEditable(false);
			tfSalida.setStyle(tfSalida.getStyle() + "-fx-background-color: #f1f2f6; -fx-text-fill: #7f8c8d;");

			// Col 2-3
			TextField tfServir = styledField(String.valueOf(m.getInteger("servirUnidad", 0)), colWidths[2]);
			TextField tfPrep   = styledField(String.valueOf(m.getOrDefault("preparado", 0)), colWidths[3]);

			// Col 7 - Observaciones
			TextField tfObs = styledField(m.getString("observaciones") != null ? m.getString("observaciones") : "", colWidths[8]);

			// Col 0 - A3
			Label lblA3 = new Label(String.valueOf(m.getInteger("A3")));
			lblA3.setStyle("-fx-font-weight: bold; -fx-min-width: " + colWidths[0] + ";");

			// Col 4 - Falta
			Label lblFalta = new Label();
			lblFalta.setMinWidth(colWidths[4]);

			// Col 5 - Validar
			CheckBox cbVal = styledCheckbox("✔".equals(m.getString("validacion")), "Ok");
			cbVal.setMinWidth(colWidths[5]);

			// Col 6 - Pedido Completo
			CheckBox cbPed = styledCheckbox("✔".equals(m.getString("pedidoCompleto")), "Ok");
			cbPed.setMinWidth(colWidths[6]);

			// Col 7 - Fecha modificación: muestra la fecha guardada en BD
			String fechaActual = m.getString("fechaPedido");
			Label lblFecha = new Label(fechaActual != null && !fechaActual.isEmpty() ? fechaActual : "—");
			lblFecha.setMinWidth(colWidths[7]);
			lblFecha.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");

			// Cálculo de falta
			Runnable calcular = () -> {
				int s = parseEntero(tfSalida.getText());
				int p = parseEntero(tfPrep.getText());
				int f = Math.max(0, s - p);
				lblFalta.setText(String.valueOf(f));
				lblFalta.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (f > 0 ? "#e74c3c" : "#27ae60") + ";");
			};

			// Al modificar cualquier campo: aviso en la columna de fecha
			boolean[] modificado = { false };
			Runnable alCambiar = () -> {
				calcular.run();
				if (!modificado[0]) {
					modificado[0] = true;
					lblFecha.setText("✎ se actualizará");
					lblFecha.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 10px;");
				}
			};

			tfPrep.textProperty().addListener((o, ov, nv)    -> alCambiar.run());
			tfServir.textProperty().addListener((o, ov, nv)  -> alCambiar.run());
			tfObs.textProperty().addListener((o, ov, nv)     -> alCambiar.run());
			cbVal.selectedProperty().addListener((o, ov, nv) -> alCambiar.run());
			cbPed.selectedProperty().addListener((o, ov, nv) -> alCambiar.run());

			calcular.run();

			// 9 columnas en el grid (0..8)
			gridDatos.add(lblA3,    0, i);
			gridDatos.add(tfSalida, 1, i);
			gridDatos.add(tfServir, 2, i);
			gridDatos.add(tfPrep,   3, i);
			gridDatos.add(lblFalta, 4, i);
			gridDatos.add(cbVal,    5, i);
			gridDatos.add(cbPed,    6, i);
			gridDatos.add(lblFecha, 7, i);
			gridDatos.add(tfObs,    8, i);

			// ctrl: [0]=mOrig [1]=tfSalida [2]=tfServir [3]=tfPrep
			//       [4]=cbVal  [5]=cbPed   [6]=tfObs    [7]=modificado
			filaControles.add(new Object[]{ m, tfSalida, tfServir, tfPrep, cbVal, cbPed, tfObs, modificado });
		}

		ScrollPane scroll = new ScrollPane(gridDatos);
		scroll.setFitToWidth(true);
		scroll.setPrefHeight(500);
		scroll.setStyle("-fx-background-color:transparent;");

		dialog.getDialogPane().setContent(new VBox(cabeceraDialogo, gridHeader, scroll));

		dialog.setResultConverter(btn -> {
			if (btn != ButtonType.OK) return null;
			List<Document> nuevaLista = new java.util.ArrayList<>();
			String ahora = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());

			for (Object[] ctrl : filaControles) {
				Document mOrig = (Document) ctrl[0];
				int s   = parseEntero(((TextField) ctrl[1]).getText());
				int p   = parseEntero(((TextField) ctrl[3]).getText());
				boolean mod = ((boolean[]) ctrl[7])[0];

				Document d = new Document(mOrig)
					.append("salidaUnidad",   s)
					.append("servirUnidad",   parseEntero(((TextField) ctrl[2]).getText()))
					.append("preparado",      p)
					.append("falta",          Math.max(0, s - p))
					.append("validacion",     ((CheckBox) ctrl[4]).isSelected() ? "✔" : "✘")
					.append("pedidoCompleto", ((CheckBox) ctrl[5]).isSelected() ? "✔" : "✘")
					.append("observaciones",  ((TextField) ctrl[6]).getText().trim());

				// Solo actualiza la fecha si se tocó algo en esa fila
				if (mod) d.append("fechaPedido", ahora);

				nuevaLista.add(d);
			}
			return nuevaLista;
		});

		dialog.showAndWait().ifPresent(lista -> {
			coleccion.updateOne(eq("obra", ref), new Document("$set", new Document("materiales", lista)));
			vista.setEstado("✅ Obra " + ref + " actualizada.");
			onBuscarObra();
		});
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
		if (cliente == null || cliente.isEmpty()) return;
		List<Document> resultados = new java.util.ArrayList<>();
		try (MongoCursor<Document> cursor = coleccion.find(regex("cliente", cliente, "i")).iterator()) {
			while (cursor.hasNext()) resultados.add(cursor.next());
		}
		String[] cols = {"OBRA", "CLIENTE", "PROYECTO", "ENTREGA", "RESPONSABLE"};
		String[] keys = {"obra", "cliente", "proyecto", "entrega", "responsable"};
		mostrarTablaResultados("Resultados Cliente: " + cliente, resultados, cols, keys);
	}

	// ── 10. Buscar por referencia de material ─────────────────────────────────
	private void onBuscarReferencia() {
		String refBusqueda = vista.getInput();
		if (refBusqueda.isEmpty()) refBusqueda = vista.pedirTexto("Buscar referencia", "Referencia del material:");
		if (refBusqueda == null || refBusqueda.isEmpty()) return;
		List<Document> filas = new java.util.ArrayList<>();
		try (MongoCursor<Document> cursor = coleccion.find(eq("materiales.referencia", refBusqueda)).iterator()) {
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
		String[] cols = {"OBRA", "A3", "REFERENCIA", "MARCA", "DESCRIPCIÓN", "FALTA", "FECHA MODIF."};
		String[] keys = {"obraRef", "A3", "referencia", "marca", "descripcion", "falta", "fechaPedido"};
		mostrarTablaResultados("Búsqueda de Material: " + refBusqueda, filas, cols, keys);
	}

	// ── 11. Ordenar por A3 ────────────────────────────────────────────────────
	private void onOrdenarA3() {
		String ref = pedirObra("Ordenar Obra");
		if (ref == null) return;
		Document doc = coleccion.find(eq("obra", ref)).first();
		if (doc == null) return;
		List<Document> mats = doc.getList("materiales", Document.class);
		mats.sort((a, b) -> Integer.compare(a.getInteger("A3", 0), b.getInteger("A3", 0)));
		String[] cols = {"A3", "MARCA", "REFERENCIA", "DESCRIPCIÓN", "FALTA"};
		String[] keys = {"A3", "marca", "referencia", "descripcion", "falta"};
		mostrarTablaResultados("Obra " + ref + " (Ordenada por A3)", mats, cols, keys);
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
			return (int) Double.parseDouble(valor.replace(",", "."));
		} catch (NumberFormatException e) { return 0; }
	}

	private void onLimpiarInput() {
		vista.limpiarInput();
		vista.setEstado("Input limpiado.");
	}

	private void onExportar() {
		String ref = pedirObra("Exportar a Excel");
		if (ref == null) return;
		Document obra = coleccion.find(eq("obra", ref)).first();
		if (obra == null) { vista.mostrarAlerta("Error", "Obra no encontrada."); return; }
		javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
		fc.setTitle("Guardar Excel");
		fc.setInitialFileName(ref + "_materiales.xlsx");
		fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel", "*.xlsx"));
		File archivo = fc.showSaveDialog(null);
		if (archivo == null) return;
		Task<Void> tarea = new Task<>() {
			@Override protected Void call() throws Exception {
				Modelo.exportar(obra, archivo);
				return null;
			}
		};
		tarea.setOnSucceeded(e -> vista.setEstado("✅ Excel exportado: " + archivo.getName()));
		tarea.setOnFailed(e  -> vista.setEstado("❌ Error al exportar: " + tarea.getException().getMessage()));
		new Thread(tarea).start();
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

	@SuppressWarnings("deprecation")
	private void mostrarTablaResultados(String titulo, List<Document> datos, String[] columnas, String[] llaves) {
		if (datos == null || datos.isEmpty()) {
			vista.mostrarAlerta("Información", "No se encontraron resultados.");
			return;
		}
		Dialog<Void> dialog = new Dialog<>();
		dialog.setTitle(titulo);
		dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
		dialog.getDialogPane().setPrefWidth(1100);
		dialog.getDialogPane().setPrefHeight(650);

		VBox header = new VBox(5);
		header.setStyle("-fx-background-color: #2f3542; -fx-padding: 15;");
		Label lblTit = new Label(titulo.toUpperCase());
		lblTit.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
		Label lblHint = new Label("💡 Tip: Selecciona celdas y usa Ctrl+C para copiar a Excel");
		lblHint.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 11px;");
		header.getChildren().addAll(lblTit, lblHint);

		TableView<Document> tabla = new TableView<>();
		tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		tabla.getSelectionModel().setCellSelectionEnabled(true);
		tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		for (int i = 0; i < columnas.length; i++) {
			final String llave = llaves[i];
			TableColumn<Document, Object> col = new TableColumn<>(columnas[i]);
			col.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().get(llave)));
			if (llave.equals("falta")) {
				col.setCellFactory(column -> new TableCell<>() {
					@Override protected void updateItem(Object item, boolean empty) {
						super.updateItem(item, empty);
						if (empty || item == null) { setText(null); setStyle(""); }
						else {
							setText(item.toString());
							int f = Integer.parseInt(item.toString());
							setStyle("-fx-text-fill: " + (f > 0 ? "#e74c3c" : "#27ae60") + "; -fx-font-weight: bold;");
						}
					}
				});
			}
			tabla.getColumns().add(col);
		}

		tabla.getItems().addAll(datos);
		tabla.setOnKeyPressed(e -> {
			if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.C) copiarSeleccionAlPortapapeles(tabla);
		});
		ContextMenu menu = new ContextMenu();
		MenuItem itemCopiar = new MenuItem("Copiar selección");
		itemCopiar.setOnAction(e -> copiarSeleccionAlPortapapeles(tabla));
		menu.getItems().add(itemCopiar);
		tabla.setContextMenu(menu);

		VBox layout = new VBox(header, tabla);
		VBox.setVgrow(tabla, Priority.ALWAYS);
		dialog.getDialogPane().setContent(layout);
		dialog.showAndWait();
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
		final ClipboardContent contenido = new ClipboardContent();
		contenido.putString(sb.toString());
		Clipboard.getSystemClipboard().setContent(contenido);
	}
}