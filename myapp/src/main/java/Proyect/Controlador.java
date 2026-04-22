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
	private String[] ultimasColumnas = null;
	private String[] ultimasLlaves = null;
	private String ultimoTitulo = null;

	public Controlador(Vista vista, MongoCollection<Document> coleccion) {
		this.vista = vista;
		this.coleccion = coleccion;
		registrarEventos();
		actualizarMetricasDashboard();

	}

	// ═════════════════════════════════════════════════════════════════════════
	// REGISTRO DE EVENTOS
	// ═════════════════════════════════════════════════════════════════════════
	private void registrarEventos() {
		vista.getBtnImportar().setOnAction(e -> onImportar());
		vista.getBtnListarObras().setOnAction(e -> onListarObras());
		vista.getBtnBuscarObra().setOnAction(e -> onBuscarObra(null));
		vista.getBtnAgregarFila().setOnAction(e -> onAgregarFila());
		vista.getBtnEliminarFila().setOnAction(e -> onEliminarFila());
		vista.getBtnActualizar().setOnAction(e -> onActualizar());
		vista.getBtnEliminarObra().setOnAction(e -> onEliminarObra());
		vista.getBtnLimpiarInput().setOnAction(e -> onLimpiarInput());
		vista.getBtnExportar().setOnAction(e -> onExportar());
		vista.getBtnCompras().setOnAction(e -> onCompras());
		// vista.getBtnSalir() .setOnAction(e -> onSalir());
		// vista.getBtnEtiquetas().setOnAction(e -> onImprimirEtiquetas());
		vista.setOnInputChange((obs, oldVal, newVal) -> onFiltrarEnTiempoReal(newVal));
	}

	// ═════════════════════════════════════════════════════════════════════════
	// HELPER: TextInputDialog con icono
	// ═════════════════════════════════════════════════════════════════════════
	private String pedirTextoConIcono(String titulo, String mensaje) {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle(titulo);
		dialog.setHeaderText(mensaje);
		dialog.getDialogPane().getScene().getWindow().addEventHandler(javafx.stage.WindowEvent.WINDOW_SHOWN, event -> {
			Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
			try {
				stage.getIcons()
						.add(new javafx.scene.image.Image(getClass().getResource("/logo.jpg").toExternalForm()));
			} catch (Exception ignored) {
				stage.setTitle("⚠ " + titulo);
			}
		});
		return dialog.showAndWait().orElse("").trim();
	}

	// ═════════════════════════════════════════════════════════════════════════
	// 1. IMPORTAR EXCEL
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
			@Override
			protected Void call() throws Exception {
				Modelo.importarExcelAMongo(archivo, coleccion);
				return null;
			}
		};

		tarea.setOnSucceeded(e -> {
			vista.setEstado("✅ Excel importado correctamente.");
			onListarObras();
			actualizarMetricasDashboard(); // ← aquí, cuando ya terminó
		});

		tarea.setOnFailed(
				e -> Platform.runLater(() -> vista.setEstado("❌ Error: " + tarea.getException().getMessage())));
		new Thread(tarea).start();
		actualizarMetricasDashboard();
	}

	// ═════════════════════════════════════════════════════════════════════════
	// 2. LISTAR TODAS LAS OBRAS
	// ═════════════════════════════════════════════════════════════════════════
	private void onListarObras() {
		List<Document> obras = new java.util.ArrayList<>();
		try (MongoCursor<Document> cursor = coleccion.find().iterator()) {
			while (cursor.hasNext())
				obras.add(cursor.next());
		}
		String[] cols = { "REF. OBRA", "CLIENTE", "PROYECTO", "ENTREGA", "RESPONSABLE" };
		String[] keys = { "obra", "cliente", "proyecto", "entrega", "responsable" };
		mostrarTablaResultados("Listado General de Obras", obras, cols, keys);
		vista.setEstado("Total obras: " + obras.size());
	}

	// ═════════════════════════════════════════════════════════════════════════
	// 3. BUSCAR OBRA POR REFERENCIA
	// ═════════════════════════════════════════════════════════════════════════
	private void onBuscarObra(String refForzada) {
		// ── Si viene forzada (desde otro método) salta directo al detalle ────
		if (refForzada != null && !refForzada.isEmpty()) {
			mostrarDetalleObra(refForzada);
			return;
		}

		// ── Diálogo de tipo de búsqueda ───────────────────────────────────────
		Dialog<String[]> dialog = new Dialog<>();
		dialog.setTitle("Buscar Obra");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		dialog.getDialogPane().setPrefWidth(420);

		VBox cabecera = new VBox(2);
		cabecera.setStyle("-fx-background-color: #2f3542; -fx-padding: 18;");
		Label lblTit = new Label("Buscar Obra");
		lblTit.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
		Label lblSub = new Label("Selecciona el tipo de búsqueda e introduce el valor");
		lblSub.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 12px;");
		cabecera.getChildren().addAll(lblTit, lblSub);

		GridPane grid = new GridPane();
		grid.setHgap(15);
		grid.setVgap(15);
		grid.setPadding(new Insets(25));

		ToggleGroup tg = new ToggleGroup();

		RadioButton rbObra = new RadioButton("Referencia de obra");
		RadioButton rbCliente = new RadioButton("Cliente");
		RadioButton rbRef = new RadioButton("Referencia de material");
		RadioButton rbA3 = new RadioButton("Número A3");

		rbObra.setToggleGroup(tg);
		rbCliente.setToggleGroup(tg);
		rbRef.setToggleGroup(tg);
		rbA3.setToggleGroup(tg);
		rbObra.setSelected(true);

		for (RadioButton rb : new RadioButton[] { rbObra, rbCliente, rbRef, rbA3 })
			rb.setStyle("-fx-font-size: 13px;");

		TextField tfBusqueda = new TextField();
		tfBusqueda.setPromptText("Introduce la referencia de obra...");
		tfBusqueda.setPrefWidth(340);
		tfBusqueda.setStyle("-fx-background-radius: 6; -fx-border-color: #dfe4ea; "
				+ "-fx-border-radius: 6; -fx-font-size: 13px; -fx-padding: 6;");

		// Cambiar placeholder según selección
		tg.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal == rbObra)
				tfBusqueda.setPromptText("Introduce la referencia de obra...");
			if (newVal == rbCliente)
				tfBusqueda.setPromptText("Introduce el nombre del cliente...");
			if (newVal == rbRef)
				tfBusqueda.setPromptText("Introduce la referencia del material...");
			if (newVal == rbA3)
				tfBusqueda.setPromptText("Introduce el número A3...");
		});

		// Prellenar con el input si ya hay algo escrito
		String inputActual = vista.getInput();
		if (!inputActual.isEmpty())
			tfBusqueda.setText(inputActual);

		grid.add(rbObra, 0, 0);
		grid.add(rbCliente, 0, 1);
		grid.add(rbRef, 0, 2);
		grid.add(rbA3, 0, 3);
		grid.add(new Separator(), 0, 4);
		grid.add(tfBusqueda, 0, 5);

		dialog.getDialogPane().setContent(new VBox(cabecera, grid));
		Platform.runLater(tfBusqueda::requestFocus);

		dialog.setResultConverter(btn -> {
			if (btn != ButtonType.OK)
				return null;
			String tipo = rbObra.isSelected() ? "obra"
					: rbCliente.isSelected() ? "cliente" : rbRef.isSelected() ? "referencia" : "a3";
			return new String[] { tipo, tfBusqueda.getText().trim() };
		});

		dialog.showAndWait().ifPresent(resultado -> {
			String tipo = resultado[0];
			String valor = resultado[1];
			if (valor.isEmpty())
				return;

			switch (tipo) {
			case "obra" -> mostrarDetalleObra(valor);
			case "cliente" -> buscarPorCliente(valor);
			case "referencia" -> buscarPorReferencia(valor);
			case "a3" -> buscarPorA3(valor);
			}
		});
	}

	// ── Detalle completo de una obra ──────────────────────────────────────────
	// ═════════════════════════════════════════════════════════════════════════
	// MÉTODO CORREGIDO: mostrarDetalleObra
	// FIX: "falta" (A PEDIR) solo depende de salidaUnidad y preparado.
//	      pedidoCompleto2 y fechaPedido se muestran como columnas informativas
//	      pero NO alteran el cálculo de lo que queda por preparar.
	// ═════════════════════════════════════════════════════════════════════════
	private void mostrarDetalleObra(String ref) {
		Document doc = coleccion.find(eq("obra", ref)).first();
		if (doc == null) {
			vista.mostrarAlerta("Error", "No se encontró la obra: " + ref);
			return;
		}

		List<Document> materialesOriginales = doc.getList("materiales", Document.class);
		if (materialesOriginales == null) {
			vista.mostrarAlerta("Info", "La obra no tiene materiales.");
			return;
		}

		List<Document> materiales = new java.util.ArrayList<>();
		for (Document m : materialesOriginales) {
			Document copia = new Document(m);

			int salida = copia.getInteger("salidaUnidad", 0);
			int preparado = parseEntero(copia.getOrDefault("preparado", "0").toString());

			// ── FIX: falta = salida - preparado (sin restar pedidoCompleto2) ──
			// pedidoCompleto2 y fechaPedido se muestran como info, no modifican A PEDIR
			int falta = Math.max(0, salida - preparado);
			copia.put("falta", falta);
			copia.put("pedidoCompleto", (falta == 0 && salida > 0) ? "✔" : "✘");

			materiales.add(copia);
		}

		String[] cols = { "A3", "MARCA", "REFERENCIA", "DESCRIPCIÓN", "SALIDA", "A PEDIR", "PEDIDO COMPLETO",
				"CANTIDAD PEDIDO", "FECHA PEDIDO" };
		String[] keys = { "A3", "marca", "referencia", "descripcion", "salidaUnidad", "falta", "pedidoCompleto",
				"pedidoCompleto2", "fechaPedido" };
		mostrarTablaResultados("Detalle Obra: " + ref + " | Cliente: " + doc.getString("cliente"), materiales, cols,
				keys);
	}

	// ── Buscar por cliente ────────────────────────────────────────────────────
	private void buscarPorCliente(String cliente) {
		List<Document> resultados = new java.util.ArrayList<>();
		try (MongoCursor<Document> cursor = coleccion.find(regex("cliente", cliente, "i")).iterator()) {
			while (cursor.hasNext())
				resultados.add(cursor.next());
		}
		String[] cols = { "OBRA", "CLIENTE", "PROYECTO", "ENTREGA", "RESPONSABLE" };
		String[] keys = { "obra", "cliente", "proyecto", "entrega", "responsable" };
		mostrarTablaResultados("Resultados Cliente: " + cliente, resultados, cols, keys);
	}

	// ── Buscar por referencia de material ────────────────────────────────────
	private void buscarPorReferencia(String refBusqueda) {
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
		String[] cols = { "OBRA", "A3", "REFERENCIA", "MARCA", "DESCRIPCIÓN", "PREPARADO", "A PEDIR" };
		String[] keys = { "obraRef", "A3", "referencia", "marca", "descripcion", "preparado", "falta" };
		mostrarTablaResultados("Búsqueda de Material: " + refBusqueda, filas, cols, keys);
	}

	// ── Buscar por A3 ─────────────────────────────────────────────────────────
	private void buscarPorA3(String a3Str) {
		int a3;
		try {
			a3 = Integer.parseInt(a3Str);
		} catch (NumberFormatException e) {
			vista.mostrarAlerta("Error", "El A3 debe ser un número.");
			return;
		}
		List<Document> filas = new java.util.ArrayList<>();
		try (MongoCursor<Document> cursor = coleccion.find(eq("materiales.A3", a3)).iterator()) {
			while (cursor.hasNext()) {
				Document obraDoc = cursor.next();
				List<Document> mats = obraDoc.getList("materiales", Document.class);
				for (Document m : mats) {
					if (m.getInteger("A3", -1) == a3) {
						m.append("obraRef", obraDoc.getString("obra"));
						filas.add(m);
					}
				}
			}
		}
		String[] cols = { "OBRA", "A3", "REFERENCIA", "MARCA", "DESCRIPCIÓN", "PREPARADO", "A PEDIR" };
		String[] keys = { "obraRef", "A3", "referencia", "marca", "descripcion", "preparado", "falta" };
		mostrarTablaResultados("Búsqueda A3: " + a3, filas, cols, keys);
	}

	// ═════════════════════════════════════════════════════════════════════════
	// 5. AGREGAR FILA
	// Primero muestra la tabla actual de materiales de la obra, luego abre
	// el formulario de nueva fila con los campos ya conocidos.
	// ═════════════════════════════════════════════════════════════════════════
	private void onAgregarFila() {
		String ref = pedirObraConIcono("Agregar fila");
		if (ref == null)
			return;

		Document obra = coleccion.find(eq("obra", ref)).first();
		if (obra == null) {
			vista.mostrarAlerta("Error", "Obra no encontrada.");
			return;
		}

		// ── Mostrar tabla actual de materiales ────────────────────────────────
		List<Document> materialesOriginales = obra.getList("materiales", Document.class);
		List<Document> materialesVista = new java.util.ArrayList<>();
		if (materialesOriginales != null) {
			for (Document m : materialesOriginales) {
				Document copia = new Document(m);
				int salida = copia.getInteger("salidaUnidad", 0);
				int preparado = parseEntero(copia.getOrDefault("preparado", "0").toString());
				int falta = Math.max(0, salida - preparado);
				copia.put("falta", falta);
				copia.put("pedidoCompleto", (falta == 0 && salida > 0) ? "✔" : "✘");
				materialesVista.add(copia);
			}
		}

		String[] cols = { "A3", "MARCA", "REFERENCIA", "DESCRIPCIÓN", "SALIDA", "A PEDIR", "PEDIDO COMPLETO",
				"CANTIDAD PEDIDO", "FECHA PEDIDO" };
		String[] keys = { "A3", "marca", "referencia", "descripcion", "salidaUnidad", "falta", "pedidoCompleto",
				"pedidoCompleto2", "fechaPedido" };

		if (!materialesVista.isEmpty()) {
			mostrarTablaResultados("Materiales de Obra: " + ref + " | Cliente: " + obra.getString("cliente"),
					materialesVista, cols, keys);
			vista.setEstado("📋 Obra " + ref + " — " + materialesVista.size() + " materiales. Abriendo formulario...");
		}

		// ── Formulario de nueva fila ──────────────────────────────────────────
		Dialog<Document> dialog = new Dialog<>();
		dialog.setTitle("Agregar Fila — Obra: " + ref);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		dialog.getDialogPane().setPrefWidth(520);

		// Cabecera
		VBox cabecera = new VBox(4);
		cabecera.setStyle("-fx-background-color: #2f3542; -fx-padding: 20;");
		Label lblTitulo = new Label("Nueva Fila de Material");
		lblTitulo.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
		Label lblSub = new Label("Obra: " + ref + "  |  " + obra.getString("cliente") + "  |  " + materialesVista.size()
				+ " materiales existentes");
		lblSub.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 12px;");
		cabecera.getChildren().addAll(lblTitulo, lblSub);

		// Formulario
		GridPane grid = new GridPane();
		grid.setHgap(15);
		grid.setVgap(12);
		grid.setPadding(new Insets(20, 25, 20, 25));
		grid.setStyle("-fx-background-color: white;");

		TextField tfA3 = styledField("", 200);
		tfA3.setPromptText("Ej: 101");
		TextField tfMarca = styledField("", 200);
		tfMarca.setPromptText("Ej: Schneider");
		TextField tfReferencia = styledField("", 200);
		tfReferencia.setPromptText("Ej: A9F74210");
		TextField tfDescripcion = styledField("", 300);
		tfDescripcion.setPromptText("Descripción del material");
		TextField tfSalida = styledField("0", 100);

		// A PEDIR calculado en tiempo real (salida, sin preparado ni servir)
		Label lblAPedirVal = new Label("0");
		lblAPedirVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-font-size: 14px;");
		tfSalida.textProperty().addListener((o, ov, nv) -> {
			int ap = parseEntero(nv);
			lblAPedirVal.setText(String.valueOf(ap));
			lblAPedirVal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: "
					+ (ap > 0 ? "#e74c3c" : "#27ae60") + ";");
		});

		int row = 0;

		// Sección: Identificación
		Label secId = new Label("Identificación");
		secId.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2980b9; -fx-padding: 0 0 2 0;");
		grid.add(secId, 0, row++, 2, 1);
		addFormRow(grid, "A3 *", tfA3, row++);
		addFormRow(grid, "Marca", tfMarca, row++);
		addFormRow(grid, "Referencia", tfReferencia, row++);
		addFormRow(grid, "Descripción", tfDescripcion, row++);

		grid.add(new Separator(), 0, row++, 2, 1);

		// Sección: Cantidades
		Label secCant = new Label("Cantidades");
		secCant.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2980b9; -fx-padding: 4 0 2 0;");
		grid.add(secCant, 0, row++, 2, 1);
		addFormRow(grid, "Salida (SALIDA)", tfSalida, row++);

		// A PEDIR (solo lectura, calculado)
		Label lblAP = new Label("A PEDIR (calculado)");
		lblAP.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606f;");
		grid.add(lblAP, 0, row);
		grid.add(lblAPedirVal, 1, row++);

		dialog.getDialogPane().setContent(new VBox(0, cabecera, grid));
		Platform.runLater(tfA3::requestFocus);

		dialog.setResultConverter(btn -> {
			if (btn != ButtonType.OK)
				return null;
			try {
				int a3 = Integer.parseInt(tfA3.getText().trim());
				int salida = parseEntero(tfSalida.getText());
				return new Document().append("A3", a3).append("marca", tfMarca.getText().trim())
						.append("referencia", tfReferencia.getText().trim())
						.append("descripcion", tfDescripcion.getText().trim()).append("salidaUnidad", salida)
						.append("servirUnidad", 0).append("preparado", 0).append("pedidoCompleto2", 0)
						.append("fechaPedido", null);
			} catch (Exception e) {
				vista.mostrarAlerta("Error", "El campo A3 debe ser un número entero válido.");
				return null;
			}
		});

		final String refFinal = ref;
		dialog.showAndWait().ifPresent(nuevaFila -> {
			if (nuevaFila == null)
				return;
			int a3 = nuevaFila.getInteger("A3");
			Document obraActual = coleccion.find(eq("obra", refFinal)).first();
			List<Document> mats = obraActual != null ? obraActual.getList("materiales", Document.class) : null;
			if (mats != null && Modelo.existeA3(mats, a3)) {
				vista.mostrarAlerta("Error", "El A3 " + a3 + " ya existe en esta obra.");
				return;
			}
			coleccion.updateOne(eq("obra", refFinal), new Document("$push", new Document("materiales", nuevaFila)));
			vista.setEstado("✅ Fila A3=" + a3 + " añadida. Actualizando tabla...");
			mostrarDetalleObra(refFinal);
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
	// 6. ELIMINAR FILA
	// ═════════════════════════════════════════════════════════════════════════
	private void onEliminarFila() {
		String ref = pedirObraConIcono("Eliminar fila");
		if (ref == null)
			return;
		Document obra = coleccion.find(eq("obra", ref)).first();
		if (obra == null) {
			vista.mostrarAlerta("Error", "Obra no encontrada con el numero de ref.");
			return;
		}

		String a3Str = pedirTextoConIcono("Eliminar fila", "Número A3 a eliminar:");
		if (a3Str.isEmpty())
			return;
		int a3;
		try {
			a3 = Integer.parseInt(a3Str);
		} catch (NumberFormatException e) {
			vista.mostrarAlerta("Error", "A3 debe ser un número.");
			return;
		}

		List<Document> mats = (List<Document>) obra.get("materiales");
		if (mats.stream().noneMatch(m -> m.getInteger("A3").equals(a3))) {
			vista.mostrarAlerta("Error", "El A3 no existe en esa obra.");
			return;
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
	// 7. ACTUALIZAR / EDITAR OBRA *** MODIFICADO: muestra Referencia y Descripción
	// bloqueados, solo se pueden editar SERVIR y QUITAR, con validaciones en tiempo
	// real

	// MÉTODO CORREGIDO: onActualizar
	// ═════════════════════════════════════════════════════════════════════════
	private void onActualizar() {
		String ref = pedirObraConIcono("Editar obra");
		if (ref == null)
			return;

		Document obra = coleccion.find(eq("obra", ref)).first();
		if (obra == null) {
			vista.mostrarAlerta("Error", "Obra no encontrada.");
			return;
		}

		List<Document> mats = obra.getList("materiales", Document.class);
		if (mats == null || mats.isEmpty()) {
			vista.mostrarAlerta("Info", "La obra no tiene materiales.");
			return;
		}

		ButtonType btnTypeAplicar = new ButtonType("Guardar", ButtonBar.ButtonData.APPLY);

		Dialog<List<Document>> dialog = new Dialog<>();
		dialog.setTitle("Editor Maestro de Obras");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, btnTypeAplicar, ButtonType.CANCEL);
		dialog.getDialogPane().setPrefWidth(1400);
		dialog.getDialogPane().setPrefHeight(650);

		VBox cabeceraDialogo = new VBox(2);
		cabeceraDialogo.setStyle("-fx-background-color: #2f3542; -fx-padding: 15;");
		Label lblTitulo = new Label("Editando Obra: " + ref);
		lblTitulo.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
		Label lblSub = new Label("Proyecto: " + obra.getString("proyecto"));
		lblSub.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 13px;");
		cabeceraDialogo.getChildren().addAll(lblTitulo, lblSub);

		double[] colWidths = { 55, 130, 250, 80, 80, 90, 80, 80, 80, 90, 80 };

		GridPane gridHeader = new GridPane();
		gridHeader.setHgap(10);
		gridHeader.setPadding(new Insets(12, 15, 12, 15));
		gridHeader.setStyle("-fx-background-color: #f1f2f6; -fx-border-color: #dfe4ea; -fx-border-width: 0 0 1 0;");

		String[] headers = { "A3", "REFERENCIA 🔒", "DESCRIPCIÓN 🔒", "PEDIDO 🔒", "SERVIR", "VALIDAR", "PREPARADO 🔒",
				"QUITAR", "QUITAR BTN", "CANT. PEDIDO 🔒", "A PEDIR" };

		for (int i = 0; i < headers.length; i++) {
			Label l = new Label(headers[i]);
			l.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606f; -fx-font-size: 11px;");
			l.setMinWidth(colWidths[i]);
			l.setPrefWidth(colWidths[i]);
			l.setMaxWidth(colWidths[i]);
			l.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
			gridHeader.add(l, i, 0);
		}

		GridPane gridDatos = new GridPane();
		gridDatos.setHgap(10);
		gridDatos.setVgap(8);
		gridDatos.setPadding(new Insets(10, 15, 10, 15));

		List<Object[]> filaControles = new java.util.ArrayList<>();

		for (int i = 0; i < mats.size(); i++) {
			Document m = mats.get(i);

			Label lblA3 = new Label(String.valueOf(m.getInteger("A3")));
			lblA3.setMinWidth(colWidths[0]);
			lblA3.setPrefWidth(colWidths[0]);
			lblA3.setStyle("-fx-font-weight: bold;");

			Label lblRef = new Label(m.getString("referencia") != null ? m.getString("referencia") : "—");
			lblRef.setMinWidth(colWidths[1]);
			lblRef.setPrefWidth(colWidths[1]);
			lblRef.setMaxWidth(colWidths[1]);
			lblRef.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
			lblRef.setWrapText(false);

			Label lblDesc = new Label(m.getString("descripcion") != null ? m.getString("descripcion") : "—");
			lblDesc.setMinWidth(colWidths[2]);
			lblDesc.setPrefWidth(colWidths[2]);
			lblDesc.setMaxWidth(colWidths[2]);
			lblDesc.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 11px; -fx-font-weight: bold;");
			lblDesc.setWrapText(false);

			TextField tfSalida = styledField(String.valueOf(m.getInteger("salidaUnidad", 0)), colWidths[3]);
			tfSalida.setEditable(false);
			tfSalida.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
					+ "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50; -fx-alignment: center;");

			TextField tfServir = styledField("", colWidths[4]);

			TextField tfPrep = styledField(String.valueOf(m.getOrDefault("preparado", 0)), colWidths[6]);
			tfPrep.setEditable(false);
			tfPrep.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
					+ "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #27ae60; -fx-alignment: center;");

			// ── pedidoCompleto2: se recarga desde Mongo en calcular() ──────────
			int pedidoCompletado2Val = m.getInteger("pedidoCompleto2", 0);
			TextField tfPedidoComp2 = styledField(String.valueOf(pedidoCompletado2Val), 80);
			tfPedidoComp2.setEditable(false);
			tfPedidoComp2.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
					+ "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2980b9; -fx-alignment: center;");

			TextField tfAPedir = styledField("", colWidths[9]);
			tfAPedir.setEditable(false);
			tfAPedir.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
					+ "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #e74c3c; -fx-alignment: center;");

			TextField tfQuitar = styledField("", colWidths[7]);

			String fechaExistente = m.getString("fechaPedido") != null ? m.getString("fechaPedido") : "—";
			Label lblFechaPedido = new Label(fechaExistente);
			lblFechaPedido.setMinWidth(100);
			lblFechaPedido.setPrefWidth(100);
			lblFechaPedido.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-font-weight: bold;");

			Button btnValidar = new Button("Validar");
			btnValidar.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; "
					+ "-fx-font-weight: bold; -fx-font-size: 11px; "
					+ "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");

			Button btnQuitar = new Button("Quitar");
			btnQuitar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
					+ "-fx-font-weight: bold; -fx-font-size: 11px; "
					+ "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");

			final int indiceDeLaFila = i; // ← añade esta línea antes del Runnable
			final String refFinalCalc = ref;

			Runnable calcular = () -> {
				// ── Recargar por ÍNDICE, no por A3 ──────────────────────────────
				Document matFresco = recargarMaterialPorIndice(refFinalCalc, indiceDeLaFila);
				int comprado = matFresco != null ? matFresco.getInteger("pedidoCompleto2", 0) : 0;
				tfPedidoComp2.setText(String.valueOf(comprado));

				int s = parseEntero(tfSalida.getText());
				int p = parseEntero(tfPrep.getText());
				int aPedir = Math.max(0, s - p);

				tfAPedir.setText(String.valueOf(aPedir));
				tfAPedir.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
						+ "-fx-font-weight: bold; -fx-font-size: 13px; -fx-alignment: center; -fx-text-fill: "
						+ (aPedir > 0 ? "#e74c3c" : "#27ae60") + ";");
			};
			calcular.run();

			tfServir.textProperty().addListener((obs, oldVal, newVal) -> {
				if (!newVal.matches("\\d*")) {
					tfServir.setText(oldVal);
					return;
				}
				int pedido = parseEntero(tfSalida.getText());
				int servir = parseEntero(newVal);
				if (servir > pedido) {
					tfServir.setStyle("-fx-background-radius: 5; -fx-border-color: #e74c3c; "
							+ "-fx-border-radius: 5; -fx-border-width: 2;");
				} else {
					tfServir.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");
				}
			});

			tfQuitar.textProperty().addListener((obs, oldVal, newVal) -> {
				if (!newVal.matches("\\d*")) {
					tfQuitar.setText(oldVal);
					return;
				}
				int prep = parseEntero(tfPrep.getText());
				int quitar = parseEntero(newVal);
				if (quitar > prep) {
					tfQuitar.setStyle("-fx-background-radius: 5; -fx-border-color: #e74c3c; "
							+ "-fx-border-radius: 5; -fx-border-width: 2;");
				} else {
					tfQuitar.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");
				}
			});

			btnValidar.setOnAction(e -> {
				int servir = parseEntero(tfServir.getText());
				int prepActual = parseEntero(tfPrep.getText());
				int pedido = parseEntero(tfSalida.getText());

				String fechaHoy = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
				lblFechaPedido.setText(fechaHoy);
				lblFechaPedido.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px; -fx-font-weight: bold;");

				if (servir > pedido) {
					Alert alerta = new Alert(Alert.AlertType.ERROR);
					alerta.setTitle("Error de validación");
					alerta.setHeaderText("Cantidad de servir superada");
					alerta.setContentText(
							"La cantidad a servir (" + servir + ") no puede superar el pedido (" + pedido + ").");
					alerta.showAndWait();
					return;
				}

				int nuevoPrep = prepActual + servir;
				if (nuevoPrep > pedido) {
					Alert alerta = new Alert(Alert.AlertType.ERROR);
					alerta.setTitle("Error de validación");
					alerta.setHeaderText("Cantidad total superada");
					alerta.setContentText("Preparado (" + prepActual + ") + Servir (" + servir + ") = " + nuevoPrep
							+ ", supera el pedido de " + pedido + " unidades.");
					alerta.showAndWait();
					return;
				}

				tfPrep.setText(String.valueOf(nuevoPrep));
				tfServir.setText("");
				tfServir.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");
				calcular.run();
			});

			btnQuitar.setOnAction(e -> {
				int quitar = parseEntero(tfQuitar.getText());
				int prepActual = parseEntero(tfPrep.getText());
				int pedido = parseEntero(tfSalida.getText());

				if (quitar <= 0)
					return;

				if (quitar > prepActual) {
					Alert alerta = new Alert(Alert.AlertType.ERROR);
					alerta.setTitle("Error al quitar");
					alerta.setHeaderText("Cantidad a quitar superada");
					alerta.setContentText(
							"No puedes quitar (" + quitar + ") más de lo que hay preparado (" + prepActual + ").");
					alerta.showAndWait();
					return;
				}

				if (quitar > pedido) {
					Alert alerta = new Alert(Alert.AlertType.ERROR);
					alerta.setTitle("Error al quitar");
					alerta.setHeaderText("Cantidad supera el pedido");
					alerta.setContentText(
							"La cantidad a quitar (" + quitar + ") no puede superar el pedido (" + pedido + ").");
					alerta.showAndWait();
					return;
				}

				tfPrep.setText(String.valueOf(prepActual - quitar));
				tfQuitar.setText("");
				tfQuitar.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");
				calcular.run();
			});

			HBox hboxValidar = new HBox(btnValidar);
			hboxValidar.setMinWidth(colWidths[5]);
			hboxValidar.setPrefWidth(colWidths[5]);
			hboxValidar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

			HBox hboxQuitar = new HBox(btnQuitar);
			hboxQuitar.setMinWidth(colWidths[8]);
			hboxQuitar.setPrefWidth(colWidths[8]);
			hboxQuitar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

			gridDatos.add(lblA3, 0, i);
			gridDatos.add(lblRef, 1, i);
			gridDatos.add(lblDesc, 2, i);
			gridDatos.add(tfSalida, 3, i);
			gridDatos.add(tfServir, 4, i);
			gridDatos.add(hboxValidar, 5, i);
			gridDatos.add(tfPrep, 6, i);
			gridDatos.add(tfQuitar, 7, i);
			gridDatos.add(hboxQuitar, 8, i);
			gridDatos.add(tfPedidoComp2, 9, i);
			gridDatos.add(tfAPedir, 10, i);

			// ctrl[0]=doc ctrl[1]=tfPrep ctrl[2]=tfAPedir ctrl[3]=tfPedidoComp2
			// ctrl[4]=tfServir ctrl[5]=btnValidar ctrl[6]=tfQuitar ctrl[7]=btnQuitar
			// ctrl[8]=a3DeLaFila (int guardado como Integer) ctrl[9]=lblFechaPedido
			filaControles.add(new Object[] { m, tfPrep, tfAPedir, tfPedidoComp2, tfServir, btnValidar, tfQuitar,
					btnQuitar, i, lblFechaPedido });
		}

		// ── Bloquear filas ya completas al abrir ─────────────────────────────
		for (Object[] ctrl : filaControles) {
			Document mCtrl = (Document) ctrl[0];
			TextField tfPrepC = (TextField) ctrl[1];
			TextField tfServirC = (TextField) ctrl[4];
			Button btnValidarC = (Button) ctrl[5];
			TextField tfQuitarC = (TextField) ctrl[6];
			Button btnQuitarC = (Button) ctrl[7];
			int salidaC = mCtrl.getInteger("salidaUnidad", 0);
			int prepC = parseEntero(tfPrepC.getText());
			if (salidaC > 0 && prepC >= salidaC) {
				bloquearFila(tfServirC, tfPrepC, btnValidarC, tfQuitarC, btnQuitarC);
			}
		}

		List<TextField> camposServir = new java.util.ArrayList<>();
		List<Button> botonesValidar = new java.util.ArrayList<>();
		List<TextField> camposQuitar = new java.util.ArrayList<>();
		List<Button> botonesQuitar = new java.util.ArrayList<>();
		for (Object[] ctrl : filaControles) {
			camposServir.add((TextField) ctrl[4]);
			botonesValidar.add((Button) ctrl[5]);
			camposQuitar.add((TextField) ctrl[6]);
			botonesQuitar.add((Button) ctrl[7]);
		}

		for (int i = 0; i < camposServir.size(); i++) {
			final int idx = i;
			camposServir.get(i).setOnKeyPressed(ev -> {
				if (ev.getCode() == javafx.scene.input.KeyCode.ENTER) {
					botonesValidar.get(idx).fire();
					for (int j = idx + 1; j < camposServir.size(); j++) {
						if (camposServir.get(j).isEditable()) {
							camposServir.get(j).requestFocus();
							camposServir.get(j).selectAll();
							break;
						}
					}
					ev.consume();
				}
			});
		}

		for (int i = 0; i < camposQuitar.size(); i++) {
			final int idx = i;
			camposQuitar.get(i).setOnKeyPressed(ev -> {
				if (ev.getCode() == javafx.scene.input.KeyCode.ENTER) {
					botonesQuitar.get(idx).fire();
					for (int j = idx + 1; j < camposQuitar.size(); j++) {
						if (camposQuitar.get(j).isEditable()) {
							camposQuitar.get(j).requestFocus();
							camposQuitar.get(j).selectAll();
							break;
						}
					}
					ev.consume();
				}
			});
		}

		ScrollPane scroll = new ScrollPane(gridDatos);
		scroll.setFitToWidth(false);
		scroll.setPrefHeight(500);
		scroll.setStyle("-fx-background-color: transparent;");
		dialog.getDialogPane().setContent(new VBox(cabeceraDialogo, gridHeader, scroll));

		// ════════════════════════════════════════════════════════════════════
		// HELPER local: construye la lista leyendo pedidoCompleto2 FRESCO
		// ════════════════════════════════════════════════════════════════════
		final String refFinal = ref;

		java.util.function.Supplier<List<Document>> construirListaFresca = () -> {
		    List<Document> nuevaLista = new java.util.ArrayList<>();
		    for (Object[] ctrl : filaControles) {
		        Document mOrig    = (Document) ctrl[0];
		        int nuevoPrep     = parseEntero(((TextField) ctrl[1]).getText());
		        int indice        = (int) ctrl[8]; // ← índice de posición

		        // Recargar por índice — funciona aunque haya A3 duplicados
		        Document matFresco = recargarMaterialPorIndice(refFinal, indice);
		        int pedidoComp2Fresco   = matFresco != null ? matFresco.getInteger("pedidoCompleto2", 0) : 0;
		        String fechaPedidoFresca = matFresco != null ? matFresco.getString("fechaPedido") : null;

		        // Si el usuario validó en este diálogo, la fecha del label tiene prioridad
		        String fechaDelLabel = ((Label) ctrl[9]).getText();
		        if (fechaDelLabel != null && !fechaDelLabel.equals("—")) {
		            fechaPedidoFresca = fechaDelLabel;
		        }

		        Document actualizado = new Document(mOrig)
		                .append("preparado", nuevoPrep)
		                .append("pedidoCompleto2", pedidoComp2Fresco)
		                .append("fechaPedido", fechaPedidoFresca);
		        nuevaLista.add(actualizado);
		    }
		    return nuevaLista;
		};

		// ── Botón Aplicar (guarda sin cerrar) ────────────────────────────────
		Button botonAplicar = (Button) dialog.getDialogPane().lookupButton(btnTypeAplicar);
		botonAplicar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
			event.consume();

			// Disparar validaciones pendientes
			for (Object[] ctrl : filaControles) {
				TextField tfServirCtrl = (TextField) ctrl[4];
				Button btnValidarCtrl = (Button) ctrl[5];
				TextField tfQuitarCtrl = (TextField) ctrl[6];
				Button btnQuitarCtrl = (Button) ctrl[7];
				if (parseEntero(tfServirCtrl.getText()) > 0 && !btnValidarCtrl.isDisabled())
					btnValidarCtrl.fire();
				if (parseEntero(tfQuitarCtrl.getText()) > 0 && !btnQuitarCtrl.isDisabled())
					btnQuitarCtrl.fire();
			}

			// Guardar con pedidoCompleto2 FRESCO
			List<Document> nuevaLista = construirListaFresca.get();
			coleccion.updateOne(eq("obra", refFinal), new Document("$set", new Document("materiales", nuevaLista)));
			vista.setEstado("💾 Cambios guardados (sin cerrar).");
			onBuscarObra(refFinal);
		});

		// ── Botón OK (guarda y cierra) ────────────────────────────────────────
		dialog.setResultConverter(btn -> {
			if (btn != ButtonType.OK)
				return null;
			return construirListaFresca.get();
		});

		dialog.showAndWait().ifPresent(lista -> {
			coleccion.updateOne(eq("obra", refFinal), new Document("$set", new Document("materiales", lista)));
			vista.setEstado("✅ Obra " + refFinal + " actualizada.");
			onBuscarObra(refFinal);
		});

		actualizarMetricasDashboard();
	}

	// ═════════════════════════════════════════════════════════════════════════
	// 8. ELIMINAR OBRA COMPLETA
	// ═════════════════════════════════════════════════════════════════════════
	private void onEliminarObra() {
		String ref = pedirObraConIcono("Eliminar obra");
		if (ref == null)
			return;
		Document obra = coleccion.find(eq("obra", ref)).first();
		if (obra == null) {
			vista.mostrarAlerta("Error", "Obra no encontrada.");
			return;
		}
		if (vista.confirmar("⚠ ¿Eliminar TODA la obra " + ref + "? Esta acción no se puede deshacer.")) {
			coleccion.deleteOne(eq("obra", ref));
			vista.limpiar();
			vista.setEstado("🗑 Obra " + ref + " eliminada.");
			onListarObras();
		}
		actualizarMetricasDashboard();
	}

	// ═════════════════════════════════════════════════════════════════════════
	// 8. APARTADO DE COMPRAS: GESTIÓN DE PEDIDOS POR OBRA
	// ═════════════════════════════════════════════════════════════════════════
	private void onCompras() {
		String ref = pedirObraConIcono("Compras");
		if (ref == null)
			return;

		Document obra = coleccion.find(eq("obra", ref)).first();
		if (obra == null) {
			vista.mostrarAlerta("Error", "Obra no encontrada.");
			return;
		}

		List<Document> mats = obra.getList("materiales", Document.class);
		if (mats == null || mats.isEmpty()) {
			vista.mostrarAlerta("Info", "La obra no tiene materiales.");
			return;
		}

		Dialog<List<Document>> dialog = new Dialog<>();
		dialog.setTitle("Gestión de Compras");
		ButtonType btnTypeAplicar = new ButtonType("Guardar", ButtonBar.ButtonData.RIGHT);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, btnTypeAplicar, ButtonType.CANCEL);
		dialog.getDialogPane().setPrefWidth(1100);
		dialog.getDialogPane().setPrefHeight(650);

		VBox cabeceraDialogo = new VBox(2);
		cabeceraDialogo.setStyle("-fx-background-color: #2f3542; -fx-padding: 15;");
		Label lblTitulo = new Label("Compras: " + ref);
		lblTitulo.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
		Label lblSub = new Label(
				"Cliente: " + obra.getString("cliente") + "  |  Proyecto: " + obra.getString("proyecto"));
		lblSub.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 13px;");
		cabeceraDialogo.getChildren().addAll(lblTitulo, lblSub);

		double[] colWidths = { 55, 250, 90, 90, 90, 110, 90, 90, 110 };

		GridPane gridHeader = new GridPane();
		gridHeader.setHgap(10);
		gridHeader.setPadding(new Insets(12, 15, 12, 15));
		gridHeader.setStyle("-fx-background-color: #f1f2f6; -fx-border-color: #dfe4ea; -fx-border-width: 0 0 1 0;");

		String[] headers = { "A3", "DESCRIPCIÓN", "A PEDIR", "PEDIR", "VALIDAR", "PEDIDO COMPLETO 🔒", "QUITAR",
				"QUITAR BTN", "FECHA PEDIDO" };

		for (int i = 0; i < headers.length; i++) {
			Label l = new Label(headers[i]);
			l.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606f; -fx-font-size: 11px;");
			l.setMinWidth(colWidths[i]);
			l.setPrefWidth(colWidths[i]);
			l.setMaxWidth(colWidths[i]);
			l.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
			gridHeader.add(l, i, 0);
		}

		GridPane gridDatos = new GridPane();
		gridDatos.setHgap(10);
		gridDatos.setVgap(8);
		gridDatos.setPadding(new Insets(10, 15, 10, 15));

		List<Object[]> filaControles = new java.util.ArrayList<>();
		List<TextField> camposPedir = new java.util.ArrayList<>();
		List<Button> botonesValidar = new java.util.ArrayList<>();

		for (int i = 0; i < mats.size(); i++) {
			Document m = mats.get(i);

			int salidaVal = m.getInteger("salidaUnidad", 0);
			int prepVal = parseEntero(m.getOrDefault("preparado", "0").toString());
			int pedidoCompletoVal = m.getInteger("pedidoCompleto2", 0);

			// A PEDIR: salida - preparado - ya pedido (solo lectura, no se modifica aquí)
			int aPedirInicial = Math.max(0, salidaVal - prepVal);

			// ── A3 ────────────────────────────────────────────────────────────
			Label lblA3 = new Label(String.valueOf(m.getInteger("A3")));
			lblA3.setMinWidth(colWidths[0]);
			lblA3.setPrefWidth(colWidths[0]);
			lblA3.setStyle("-fx-font-weight: bold;");

			// ── Descripción ───────────────────────────────────────────────────
			Label lblDesc = new Label(m.getString("descripcion") != null ? m.getString("descripcion") : "—");
			lblDesc.setMinWidth(colWidths[1]);
			lblDesc.setPrefWidth(colWidths[1]);
			lblDesc.setMaxWidth(colWidths[1]);
			lblDesc.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 11px; -fx-font-weight: bold;");
			lblDesc.setWrapText(false);

			// ── A Pedir (solo lectura, se actualiza solo desde editar material) ──
			TextField tfAPedir = new TextField(String.valueOf(aPedirInicial));
			tfAPedir.setEditable(false);
			tfAPedir.setMinWidth(colWidths[2]);
			tfAPedir.setPrefWidth(colWidths[2]);
			tfAPedir.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
					+ "-fx-font-weight: bold; -fx-font-size: 13px; -fx-alignment: center; -fx-text-fill: "
					+ (aPedirInicial > 0 ? "#e74c3c" : "#27ae60") + ";");

			// ── Pedir (editable) ──────────────────────────────────────────────
			TextField tfPedir = styledField("", colWidths[3]);

			tfPedir.textProperty().addListener((obs, oldVal, newVal) -> {
				if (!newVal.matches("\\d*")) {
					tfPedir.setText(oldVal);
					return;
				}
				int max = parseEntero(tfAPedir.getText());
				int valor = parseEntero(newVal);
				if (valor > max) {
					tfPedir.setStyle("-fx-background-radius: 5; -fx-border-color: #e74c3c; "
							+ "-fx-border-radius: 5; -fx-border-width: 2;");
				} else {
					tfPedir.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");
				}
			});

			// ── Pedido Completo (solo lectura) ────────────────────────────────
			TextField tfPedidoCompleto = new TextField(String.valueOf(pedidoCompletoVal));
			tfPedidoCompleto.setEditable(false);
			tfPedidoCompleto.setMinWidth(colWidths[5]);
			tfPedidoCompleto.setPrefWidth(colWidths[5]);
			tfPedidoCompleto.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
					+ "-fx-font-weight: bold; -fx-font-size: 13px; -fx-alignment: center; -fx-text-fill: #2980b9;");

			// ── Quitar (editable) ─────────────────────────────────────────────
			TextField tfQuitar = styledField("", colWidths[6]);

			tfQuitar.textProperty().addListener((obs, oldVal, newVal) -> {
				if (!newVal.matches("\\d*")) {
					tfQuitar.setText(oldVal);
					return;
				}
				int max = parseEntero(tfPedidoCompleto.getText());
				int valor = parseEntero(newVal);
				if (valor > max) {
					tfQuitar.setStyle("-fx-background-radius: 5; -fx-border-color: #e74c3c; "
							+ "-fx-border-radius: 5; -fx-border-width: 2;");
				} else {
					tfQuitar.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");
				}
			});

			// ── Fecha pedido ──────────────────────────────────────────────────
			String fechaExistente = m.getString("fechaPedido") != null ? m.getString("fechaPedido") : "—";
			Label lblFechaPedido = new Label(fechaExistente);
			lblFechaPedido.setMinWidth(100);
			lblFechaPedido.setPrefWidth(100);
			lblFechaPedido.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-font-weight: bold;");

			// ── Botón Validar ─────────────────────────────────────────────────
			Button btnQuitar = new Button("Quitar");
			Button btnValidar = new Button("Validar");
			btnValidar.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; "
					+ "-fx-font-weight: bold; -fx-font-size: 11px; "
					+ "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");

			btnValidar.setOnAction(e -> {
				int pedir = parseEntero(tfPedir.getText());
				int pedidoActual = parseEntero(tfPedidoCompleto.getText());

				if (pedir <= 0)
					return;

				int nuevoPedidoCompleto = pedidoActual + pedir;
				tfPedidoCompleto.setText(String.valueOf(nuevoPedidoCompleto));
				tfPedir.setText("");
				tfPedir.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");
				tfPedidoCompleto.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
						+ "-fx-font-weight: bold; -fx-font-size: 13px; "
						+ "-fx-alignment: center; -fx-text-fill: #2980b9;");

				String fechaHoy = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
				lblFechaPedido.setText(fechaHoy);
				lblFechaPedido.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px; -fx-font-weight: bold;");

				// ── Bloquear si ya está todo pedido ──────────────────────────────────
				int salidaV = m.getInteger("salidaUnidad", 0);
				int prepV = parseEntero(m.getOrDefault("preparado", "0").toString());
				int maximoAPedir = Math.max(0, salidaV - prepV);
				if (nuevoPedidoCompleto > maximoAPedir && maximoAPedir > 0) {
					bloquearFilaCompras(tfPedir, btnValidar, tfQuitar, btnQuitar);
				}
			});

			// ── Botón Quitar ──────────────────────────────────────────────────

			btnQuitar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
					+ "-fx-font-weight: bold; -fx-font-size: 11px; "
					+ "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");

			btnQuitar.setOnAction(e -> {
				int quitar = parseEntero(tfQuitar.getText());
				int pedidoActual = parseEntero(tfPedidoCompleto.getText());

				if (quitar <= 0)
					return;

				if (quitar > pedidoActual) {
					Alert alerta = new Alert(Alert.AlertType.ERROR);
					alerta.setTitle("Error al quitar");
					alerta.setHeaderText("Cantidad a quitar superada");
					alerta.setContentText(
							"No puedes quitar (" + quitar + ") más de lo que hay pedido (" + pedidoActual + ").");
					alerta.showAndWait();
					return;
				}

				// ✅ Solo resta de pedido completo, no toca A Pedir
				int nuevoPedidoCompleto = pedidoActual - quitar;
				tfPedidoCompleto.setText(String.valueOf(nuevoPedidoCompleto));
				tfQuitar.setText("");
				tfQuitar.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");
				tfPedidoCompleto.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
						+ "-fx-font-weight: bold; -fx-font-size: 13px; -fx-alignment: center; -fx-text-fill: #2980b9;");

				// ── Desbloquear si vuelve a haber cantidad por pedir ─────────────────
				int salidaV = m.getInteger("salidaUnidad", 0);
				int prepV = parseEntero(m.getOrDefault("preparado", "0").toString());
				int maximoAPedir = Math.max(0, salidaV - prepV);
				if (nuevoPedidoCompleto < maximoAPedir) {
					desbloquearFilaCompras(tfPedir, btnValidar);
				}

			});

			HBox hboxValidar = new HBox(btnValidar);
			hboxValidar.setMinWidth(colWidths[4]);
			hboxValidar.setPrefWidth(colWidths[4]);
			hboxValidar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

			HBox hboxQuitar = new HBox(btnQuitar);
			hboxQuitar.setMinWidth(colWidths[7]);
			hboxQuitar.setPrefWidth(colWidths[7]);
			hboxQuitar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

			// ── Añadir al grid ────────────────────────────────────────────────
			gridDatos.add(lblA3, 0, i);
			gridDatos.add(lblDesc, 1, i);
			gridDatos.add(tfAPedir, 2, i);
			gridDatos.add(tfPedir, 3, i);
			gridDatos.add(hboxValidar, 4, i);
			gridDatos.add(tfPedidoCompleto, 5, i);
			gridDatos.add(tfQuitar, 6, i);
			gridDatos.add(hboxQuitar, 7, i);
			gridDatos.add(lblFechaPedido, 8, i);

			camposPedir.add(tfPedir);
			botonesValidar.add(btnValidar);
			filaControles.add(
					new Object[] { m, tfPedidoCompleto, lblFechaPedido, tfPedir, btnValidar, tfQuitar, btnQuitar });
		}

		// ── Listas para quitar (igual que pedir) ─────────────────────────────────
		List<TextField> camposQuitar = new java.util.ArrayList<>();
		List<Button> botonesQuitar = new java.util.ArrayList<>();
		for (Object[] ctrl : filaControles) {
			camposQuitar.add((TextField) ctrl[5]);
			botonesQuitar.add((Button) ctrl[6]);
		}

		// ── Enter en campos PEDIR: valida y mueve foco ────────────────────────────
		for (int i = 0; i < camposPedir.size(); i++) {
			final int idx = i;
			camposPedir.get(i).setOnKeyPressed(ev -> {
				if (ev.getCode() == javafx.scene.input.KeyCode.ENTER) {
					botonesValidar.get(idx).fire();
					for (int j = idx + 1; j < camposPedir.size(); j++) {
						if (camposPedir.get(j).isEditable()) {
							camposPedir.get(j).requestFocus();
							camposPedir.get(j).selectAll();
							break;
						}
					}
					ev.consume();
				}
			});
		}

		// ── Enter en campos QUITAR: ejecuta quitar y mueve foco ───────────────────
		for (int i = 0; i < camposQuitar.size(); i++) {
			final int idx = i;
			camposQuitar.get(i).setOnKeyPressed(ev -> {
				if (ev.getCode() == javafx.scene.input.KeyCode.ENTER) {
					botonesQuitar.get(idx).fire();
					for (int j = idx + 1; j < camposQuitar.size(); j++) {
						if (camposQuitar.get(j).isEditable()) {
							camposQuitar.get(j).requestFocus();
							camposQuitar.get(j).selectAll();
							break;
						}
					}
					ev.consume();
				}
			});
		}

		// ── Bloquear filas ya completas al abrir ──────────────────────────────────
		for (Object[] ctrl : filaControles) {
			int aPedirActual = parseEntero(((TextField) ctrl[1]).getText()); // pedidoCompleto2 actual
			// Recalcular aPedir real desde los valores originales
			Document mOrig = (Document) ctrl[0];
			int salidaV = mOrig.getInteger("salidaUnidad", 0);
			int prepV = parseEntero(mOrig.getOrDefault("preparado", "0").toString());
			int yaP = parseEntero(((TextField) ctrl[1]).getText());
			int aPedirReal = Math.max(0, salidaV - prepV - yaP);

			// Una fila se bloquea solo cuando ya pediste >= lo que falta preparar
			int maximoAPedir = Math.max(0, salidaV - prepV);
			if (yaP > maximoAPedir && maximoAPedir > 0) {
				bloquearFilaCompras((TextField) ctrl[3], (Button) ctrl[4], (TextField) ctrl[5], (Button) ctrl[6]);
			}
		}

		ScrollPane scroll = new ScrollPane(gridDatos);
		scroll.setFitToWidth(false);
		scroll.setPrefHeight(500);
		scroll.setStyle("-fx-background-color: transparent;");
		dialog.getDialogPane().setContent(new VBox(cabeceraDialogo, gridHeader, scroll));

		// ── Botón Aplicar (guarda sin cerrar) ─────────────────────────────────
		Button botonAplicar = (Button) dialog.getDialogPane().lookupButton(btnTypeAplicar);
		botonAplicar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
			event.consume();

			// ── Primero dispara todos los botones Validar y Quitar pendientes ──
			for (Object[] ctrl : filaControles) {
				TextField tfPedirCtrl = (TextField) ctrl[3];
				Button btnValidarCtrl = (Button) ctrl[4];
				TextField tfQuitarCtrl = (TextField) ctrl[5];
				Button btnQuitarCtrl = (Button) ctrl[6];

				if (parseEntero(tfPedirCtrl.getText()) > 0 && !btnValidarCtrl.isDisabled())
					btnValidarCtrl.fire();

				if (parseEntero(tfQuitarCtrl.getText()) > 0 && !btnQuitarCtrl.isDisabled())
					btnQuitarCtrl.fire();
			}
			// ── Luego guarda el estado actual en MongoDB ───────────────────────
			List<Document> nuevaLista = new java.util.ArrayList<>();
			for (Object[] ctrl : filaControles) {
				Document mOrig = (Document) ctrl[0];
				int pedidoCompleto = parseEntero(((TextField) ctrl[1]).getText());
				String fecha = ((Label) ctrl[2]).getText();
				Document actualizado = new Document(mOrig).append("pedidoCompleto2", pedidoCompleto)
						.append("fechaPedido", fecha);
				nuevaLista.add(actualizado);
			}
			coleccion.updateOne(eq("obra", ref), new Document("$set", new Document("materiales", nuevaLista)));
			vista.setEstado("💾 Compras guardadas.");
		});
		// ── Botón OK (guarda y cierra) ────────────────────────────────────────
		dialog.setResultConverter(btn -> {
			if (btn != ButtonType.OK)
				return null;
			List<Document> nuevaLista = new java.util.ArrayList<>();
			for (Object[] ctrl : filaControles) {
				Document mOrig = (Document) ctrl[0];
				int pedidoCompleto = parseEntero(((TextField) ctrl[1]).getText());
				String fecha = ((Label) ctrl[2]).getText();
				nuevaLista.add(
						new Document(mOrig).append("pedidoCompleto2", pedidoCompleto).append("fechaPedido", fecha));
			}
			return nuevaLista;
		});

		final String refFinal = ref;
		dialog.showAndWait().ifPresent(lista -> {
			coleccion.updateOne(eq("obra", refFinal), new Document("$set", new Document("materiales", lista)));
			vista.setEstado("✅ Compras de obra " + refFinal + " guardadas.");
			onBuscarObra(refFinal);
		});

	}

	// ═════════════════════════════════════════════════════════════════════════
	// EXPORTAR (EXCEL O PDF)
	// ═════════════════════════════════════════════════════════════════════════
	private void onExportar() {
		String ref = pedirObraConIcono("Exportar obra");
		if (ref == null)
			return;

		Document obra = coleccion.find(eq("obra", ref)).first();
		if (obra == null) {
			vista.mostrarAlerta("Error", "Obra no encontrada.");
			return;
		}

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
		RadioButton rbExcel = new RadioButton();
		rbExcel.setToggleGroup(tg);
		rbExcel.setSelected(true);
		RadioButton rbPDF = new RadioButton();
		rbPDF.setToggleGroup(tg);

		VBox cardExcel = crearTarjetaFormato("📊  Excel (.xlsx)",
				"Hoja de cálculo completa con todos los materiales,\ntotales y formato corporativo.", "#1D6F42",
				rbExcel);
		VBox cardPDF = crearTarjetaFormato("📄  PDF (.pdf)",
				"Documento listo para imprimir. Ficha por material\ncon colores de estado y datos de la obra.",
				"#C0392B", rbPDF);

		HBox opciones = new HBox(20, cardExcel, cardPDF);
		opciones.setPadding(new Insets(25));
		opciones.setAlignment(javafx.geometry.Pos.CENTER);

		fmtDialog.getDialogPane().setContent(new VBox(cab, opciones));
		fmtDialog.getDialogPane().setPrefWidth(540);
		fmtDialog.setResultConverter(btn -> btn == ButtonType.OK ? (rbExcel.isSelected() ? "xlsx" : "pdf") : null);

		fmtDialog.showAndWait().ifPresent(formato -> {
			javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
			fc.setTitle("Guardar " + formato.toUpperCase());
			if ("xlsx".equals(formato)) {
				fc.setInitialFileName(ref + "_materiales.xlsx");
				fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
			} else {
				fc.setInitialFileName(ref + "_materiales.pdf");
				fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
			}

			File archivo = fc.showSaveDialog(null);
			if (archivo == null)
				return;

			vista.setEstado("⏳ Exportando a " + formato.toUpperCase() + "...");
			Task<Void> tarea = new Task<>() {
				@Override
				protected Void call() throws Exception {
					if ("xlsx".equals(formato)) {
						Modelo.exportar(obra, archivo);
					} else {
						exportarObrasAPDF(obra, archivo);
					}
					return null;
				}
			};
			tarea.setOnSucceeded(e -> vista.setEstado("✅ Exportado: " + archivo.getName()));
			tarea.setOnFailed(e -> vista.setEstado("❌ Error: " + tarea.getException().getMessage()));
			new Thread(tarea).start();
		});
	}

	private void exportarObrasAPDF(Document obra, File destino) throws Exception {
		List<Document> mats = obra.getList("materiales", Document.class);
		if (mats == null || mats.isEmpty()) {
			Platform.runLater(() -> vista.mostrarAlerta("Info", "La obra no tiene materiales para exportar."));
			return;
		}
		FichaPDF.generarObraCompleta(obra, destino);
	}

	private VBox crearTarjetaFormato(String titulo, String desc, String color, RadioButton rb) {
		VBox card = new VBox(10);
		card.setPadding(new Insets(18));
		card.setPrefWidth(210);
		card.setAlignment(javafx.geometry.Pos.TOP_LEFT);
		card.setStyle("-fx-background-color: white;" + "-fx-background-radius: 12;" + "-fx-border-color: " + color + ";"
				+ "-fx-border-width: 0 0 5 0;" + "-fx-border-radius: 12;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.10), 10, 0, 0, 4);" + "-fx-cursor: hand;");

		Label lblTit = new Label(titulo);
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
	// IMPRIMIR ETIQUETAS / QR
	// ═════════════════════════════════════════════════════════════════════════
	private void onImprimirEtiquetas() {
		String ref = pedirObraConIcono("Imprimir Etiquetas / QR");
		if (ref == null)
			return;

		Document obra = coleccion.find(eq("obra", ref)).first();
		if (obra == null) {
			vista.mostrarAlerta("Error", "Obra no encontrada: " + ref);
			return;
		}

		List<Document> mats = obra.getList("materiales", Document.class);
		if (mats == null || mats.isEmpty()) {
			vista.mostrarAlerta("Info", "La obra no tiene materiales.");
			return;
		}

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
		gp.setHgap(15);
		gp.setVgap(12);
		gp.setPadding(new Insets(25));

		ToggleGroup tg = new ToggleGroup();
		RadioButton rbTodos = new RadioButton("Hoja completa (todos los materiales: " + mats.size() + ")");
		RadioButton rbUno = new RadioButton("Solo un material (por número A3)");
		rbTodos.setToggleGroup(tg);
		rbTodos.setSelected(true);
		rbUno.setToggleGroup(tg);

		TextField tfA3sel = new TextField();
		tfA3sel.setPromptText("Número A3...");
		tfA3sel.setDisable(true);
		tfA3sel.setPrefWidth(120);
		rbUno.selectedProperty().addListener((o, ov, nv) -> tfA3sel.setDisable(!nv));

		gp.add(rbTodos, 0, 0, 2, 1);
		gp.add(rbUno, 0, 1);
		gp.add(tfA3sel, 1, 1);
		opcionDialog.getDialogPane().setContent(new VBox(cabOpc, gp));
		opcionDialog.setResultConverter(btn -> {
			if (btn != ButtonType.OK)
				return null;
			return rbUno.isSelected() ? tfA3sel.getText().trim() : "TODOS";
		});

		opcionDialog.showAndWait().ifPresent(seleccion -> {
			if (seleccion == null)
				return;
			List<Document> aImprimir;
			if ("TODOS".equals(seleccion)) {
				aImprimir = mats;
			} else {
				int a3buscado;
				try {
					a3buscado = Integer.parseInt(seleccion);
				} catch (NumberFormatException ex) {
					vista.mostrarAlerta("Error", "A3 debe ser un número.");
					return;
				}
				final int a3f = a3buscado;
				aImprimir = mats.stream().filter(m -> m.getInteger("A3", -1) == a3f)
						.collect(java.util.stream.Collectors.toList());
				if (aImprimir.isEmpty()) {
					vista.mostrarAlerta("Error", "No se encontró A3=" + seleccion);
					return;
				}
			}

			javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
			fc.setTitle("Guardar hoja de etiquetas");
			fc.setInitialFileName(ref + "_etiquetas_QR.png");
			fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
			File archivo = fc.showSaveDialog(null);
			if (archivo == null)
				return;

			final String cliente = obra.getString("cliente") != null ? obra.getString("cliente") : "";
			final List<Document> listaFinal = aImprimir;
			final String refFinal = ref;

			vista.setEstado("⏳ Generando etiquetas QR...");
			Task<Void> tarea = new Task<>() {
				@Override
				protected Void call() throws Exception {
					EtiquetaQR.generarHojaEtiquetas(refFinal, cliente, listaFinal, archivo);
					return null;
				}
			};
			tarea.setOnSucceeded(e -> {
				vista.setEstado("✅ Etiquetas guardadas: " + archivo.getName());
				try {
					if (java.awt.Desktop.isDesktopSupported())
						java.awt.Desktop.getDesktop().open(archivo);
				} catch (Exception ignored) {
				}
			});
			tarea.setOnFailed(e -> vista.setEstado("❌ Error: " + tarea.getException().getMessage()));
			new Thread(tarea).start();
		});
	}

	// ═════════════════════════════════════════════════════════════════════════
	// SALIR
	// ═════════════════════════════════════════════════════════════════════════
	private void onSalir() {
		if (vista.confirmar("¿Estás seguro de que deseas cerrar el programa?")) {
			javafx.application.Platform.exit();
			System.exit(0);
		}
	}

	// ═════════════════════════════════════════════════════════════════════════
	// MOSTRAR TABLA DE RESULTADOS
	// ═════════════════════════════════════════════════════════════════════════
	@SuppressWarnings("deprecation")
	private void mostrarTablaResultados(String titulo, List<Document> datos, String[] columnas, String[] llaves) {
		if (datos == null || datos.isEmpty()) {
			vista.mostrarAlerta("Información", "No se encontraron resultados.");
			return;
		}

		ultimosDatosOriginales = new java.util.ArrayList<>(datos);
		ultimasColumnas = columnas;
		ultimasLlaves = llaves;
		ultimoTitulo = titulo;

		vista.setContenidoCentral(construirTablaVBox(titulo, datos, columnas, llaves, false));
	}

	// ═════════════════════════════════════════════════════════════════════════
	// FILTRO EN TIEMPO REAL
	// ═════════════════════════════════════════════════════════════════════════
	private void onFiltrarEnTiempoReal(String texto) {
		if (ultimosDatosOriginales == null || ultimasColumnas == null)
			return;

		String filtro = texto == null ? "" : texto.trim().toLowerCase();

		if (filtro.isEmpty()) {
			vista.setInputFiltrandoEstilo(false);
			vista.setContenidoCentral(
					construirTablaVBox(ultimoTitulo, ultimosDatosOriginales, ultimasColumnas, ultimasLlaves, false));
			vista.setEstado("📋 Mostrando " + ultimosDatosOriginales.size() + " resultados.");
			return;
		}

		List<Document> filtrados = ultimosDatosOriginales.stream().filter(doc -> {
			for (String llave : ultimasLlaves) {
				Object valor = doc.get(llave);
				if (valor != null && valor.toString().toLowerCase().contains(filtro))
					return true;
			}
			return false;
		}).collect(java.util.stream.Collectors.toList());

		vista.setInputFiltrandoEstilo(!filtrados.isEmpty());

		if (filtrados.isEmpty()) {
			vista.setEstado("🔍 Sin resultados para: \"" + texto.trim() + "\"");
			VBox contenedor = new VBox();
			contenedor.setStyle("-fx-background-color: white; -fx-background-radius: 15; "
					+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 8);");
			HBox cabecera = new HBox();
			cabecera.setPadding(new Insets(15, 25, 15, 25));
			cabecera.setStyle("-fx-background-color: linear-gradient(to right, #2c3e50, #4b6584); "
					+ "-fx-background-radius: 15 15 0 0;");
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

		String tituloFiltrado = ultimoTitulo + "  🔍 \"" + texto.trim() + "\"  (" + filtrados.size() + " resultado"
				+ (filtrados.size() == 1 ? "" : "s") + ")";
		vista.setContenidoCentral(construirTablaVBox(tituloFiltrado, filtrados, ultimasColumnas, ultimasLlaves, true));
		vista.setEstado("🔍 " + filtrados.size() + " resultado(s) para: \"" + texto.trim() + "\"");
	}

	// ═════════════════════════════════════════════════════════════════════════
	// CONSTRUCTOR INTERNO DE TABLA
	// ═════════════════════════════════════════════════════════════════════════
	@SuppressWarnings("deprecation")
	private VBox construirTablaVBox(String titulo, List<Document> datos, String[] columnas, String[] llaves,
			boolean esFiltrada) {
		VBox contenedorPrincipal = new VBox(0);
		contenedorPrincipal.setStyle("-fx-background-color: white; -fx-background-radius: 15; "
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 8);");

		HBox cabeceraColor = new HBox();
		cabeceraColor.setPadding(new Insets(15, 25, 15, 25));
		String gradiente = esFiltrada ? "-fx-background-color: linear-gradient(to right, #1a6e4c, #27ae60);"
				: "-fx-background-color: linear-gradient(to right, #2c3e50, #4b6584);";
		cabeceraColor.setStyle(gradiente + "-fx-background-radius: 15 15 0 0;");

		Label lblTit = new Label(titulo.toUpperCase());
		lblTit.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 15px; -fx-letter-spacing: 1px;");
		cabeceraColor.getChildren().add(lblTit);

		TableView<Document> tabla = new TableView<>();
		tabla.setColumnResizePolicy(
				columnas.length < 8 ? TableView.CONSTRAINED_RESIZE_POLICY : TableView.UNCONSTRAINED_RESIZE_POLICY);
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
			case "A3" -> col.setPrefWidth(60);
			case "salidaUnidad", "preparado", "falta" -> col.setPrefWidth(75);
			case "pedidoCompleto2" -> col.setPrefWidth(100); // ← AÑADIR
			case "descripcion" -> col.setPrefWidth(400);
			case "observaciones" -> col.setPrefWidth(250);
			case "obra", "obraRef" -> col.setPrefWidth(130);
			default -> col.setPrefWidth(150);
			}

			col.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().get(llave)));
			col.setCellFactory(column -> new TableCell<>() {
				@Override
				protected void updateItem(Object item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
						setStyle("");
					} else {
						// ── PEDIDO COMPLETO: se calcula desde "falta" de la misma fila ──
						if (llave.equals("pedidoCompleto")) {
							Object faltaVal = getTableView().getItems().get(getIndex()).get("falta");
							int falta = parseEntero(faltaVal != null ? faltaVal.toString() : "1");
							setText(falta == 0 ? "✔" : "✘");
							setStyle("-fx-font-weight: 900; -fx-alignment: center; -fx-text-fill: "
									+ (falta == 0 ? "#27ae60" : "#e74c3c") + ";");
						} else if (llave.equals("falta")) {
							int f = parseEntero(item.toString());
							setText(String.valueOf(f));
							setStyle("-fx-text-fill: " + (f > 0 ? "#e74c3c" : "#27ae60") + "; -fx-font-weight: 900;");
						} else if (llave.equals("descripcion") || llave.equals("A3") || llave.equals("obra")
								|| llave.equals("obraRef")) {
							setText(item.toString());
							setStyle("-fx-text-fill: #000000; -fx-font-weight: 900;");
						} else if (llave.equals("pedidoCompleto2")) {
							int v = parseEntero(item.toString());
							setText(String.valueOf(v));
							setStyle("-fx-text-fill: " + (v > 0 ? "#2980b9" : "#a4b0be")
									+ "; -fx-font-weight: 900; -fx-alignment: center;");
						} else {
							setText(item.toString());
							setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
						}
					}
				}
			});
			tabla.getColumns().add(col);
		}

		// Al final del bucle de columnas, añadir la columna de acción
		TableColumn<Document, Void> colAccion = new TableColumn<>();
		Label lblAccion = new Label("VER");
		lblAccion.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606f; -fx-font-size: 11px;");
		colAccion.setGraphic(lblAccion);
		colAccion.setPrefWidth(80);

		colAccion.setCellFactory(col -> new TableCell<>() {
			private final Button btn = new Button("Ver →");
			{
				btn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; "
						+ "-fx-font-size: 10px; -fx-font-weight: bold; "
						+ "-fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 3 8;");
				btn.setOnAction(e -> {
					Document doc = getTableView().getItems().get(getIndex());
					String ref = doc.getString("obra") != null ? doc.getString("obra") : doc.getString("obraRef");

					if (doc.containsKey("referencia") || doc.containsKey("A3")) {
						// Es un material → popup de material
						Document obraDoc = coleccion.find(eq("obra", ref)).first();
						String cliente = obraDoc != null ? obraDoc.getString("cliente") : null;
						VentanaMaterial.mostrar(doc, ref != null ? ref : "—", cliente);
					} else {
						// Es una obra → popup de resumen de obra
						Document obraDoc = coleccion.find(eq("obra", ref)).first();
						if (obraDoc != null)
							VentanaResumenObra.mostrar(obraDoc);
					}
				});
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(empty ? null : btn);
			}
		});

		tabla.getColumns().add(colAccion);

		tabla.getItems().addAll(datos);
		contenedorPrincipal.getChildren().addAll(cabeceraColor, tabla);
		return contenedorPrincipal;
	}

	// ═════════════════════════════════════════════════════════════════════════
	// HELPERS PRIVADOS
	// ═════════════════════════════════════════════════════════════════════════
	private String pedirObraConIcono(String titulo) {
		String ref = vista.getInput();
		if (ref.isEmpty())
			ref = pedirTextoConIcono(titulo, "Introduce la referencia de obra:");
		return ref.isEmpty() ? null : ref;
	}

	private static int parseEntero(String valor) {
		try {
			if (valor == null || valor.trim().isEmpty())
				return 0;
			return (int) Double.parseDouble(valor.replace(",", "."));
		} catch (NumberFormatException e) {
			return 0;
		}
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
			int col = pos.getColumn();
			if (filaActual != -1 && filaActual != fila)
				sb.append("\n");
			else if (filaActual == fila)
				sb.append("\t");
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
			@Override
			protected int[] call() {
				return new int[] { (int) coleccion.countDocuments(), Modelo.contarTodosLosMateriales(coleccion),
						Modelo.contarAlertasFalta(coleccion) };
			}
		};
		tarea.setOnSucceeded(
				e -> vista.actualizarDashboard(tarea.getValue()[0], tarea.getValue()[1], tarea.getValue()[2]));
		new Thread(tarea).start();
	}

	private void bloquearFila(TextField tfServir, TextField tfPrep, Button btnValidar, TextField tfQuitar,
			Button btnQuitar) {
		String estiloLocked = "-fx-background-color: #f1f2f6; -fx-text-fill: #7f8c8d;";

		tfServir.setEditable(false);
		tfServir.setStyle(tfServir.getStyle() + estiloLocked);

		tfPrep.setEditable(false);
		tfPrep.setStyle(tfPrep.getStyle() + estiloLocked);

		tfQuitar.setEditable(false);
		tfQuitar.setStyle(tfQuitar.getStyle() + estiloLocked);

		btnValidar.setDisable(true);
		btnValidar.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; "
				+ "-fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 4 10;");

		btnQuitar.setDisable(true);
		btnQuitar.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; "
				+ "-fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 4 10;");
	}

	private void bloquearFilaCompras(TextField tfPedir, Button btnValidar, TextField tfQuitar, Button btnQuitar) {
		tfPedir.setEditable(false);
		tfPedir.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #7f8c8d; " + "-fx-border-color: transparent;");

		btnValidar.setDisable(true);
		btnValidar.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; "
				+ "-fx-font-weight: bold; -fx-font-size: 11px; " + "-fx-background-radius: 6; -fx-padding: 4 10;");

		// Quitar sigue activo para poder corregir pedidos erróneos
		tfQuitar.setEditable(true);
		btnQuitar.setDisable(false);
	}

	private void desbloquearFilaCompras(TextField tfPedir, Button btnValidar) {
		tfPedir.setEditable(true);
		tfPedir.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");

		btnValidar.setDisable(false);
		btnValidar.setStyle(
				"-fx-background-color: #2980b9; -fx-text-fill: white; " + "-fx-font-weight: bold; -fx-font-size: 11px; "
						+ "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");
	}

	private void desbloquearFila(TextField tfServir, TextField tfPrep, Button btnValidar, TextField tfQuitar,
			Button btnQuitar) {
		tfServir.setEditable(true);
		tfServir.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");

		tfPrep.setEditable(false); // preparado siempre solo lectura
		tfPrep.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
				+ "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #27ae60; " + "-fx-alignment: center;");

		tfQuitar.setEditable(true);
		tfQuitar.setStyle("-fx-background-radius: 5; -fx-border-color: #dfe4ea; -fx-border-radius: 5;");

		btnValidar.setDisable(false);
		btnValidar.setStyle(
				"-fx-background-color: #2980b9; -fx-text-fill: white; " + "-fx-font-weight: bold; -fx-font-size: 11px; "
						+ "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");

		btnQuitar.setDisable(false);
		btnQuitar.setStyle(
				"-fx-background-color: #e74c3c; -fx-text-fill: white; " + "-fx-font-weight: bold; -fx-font-size: 11px; "
						+ "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");
	}

	private Document recargarMaterialPorIndice(String refObra, int indice) {
		Document docFresco = coleccion.find(eq("obra", refObra)).first();
		if (docFresco == null)
			return null;
		List<Document> mats = docFresco.getList("materiales", Document.class);
		if (mats == null || indice >= mats.size())
			return null;
		return mats.get(indice);
	}
}