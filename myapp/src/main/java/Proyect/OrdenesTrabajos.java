package Proyect;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

public class OrdenesTrabajos {

	// Nombres de campo reales en MongoDB
	private static final String[] COLUMNAS = { "CLIENTE", "ORDEN", "DESCRIPCIÓN", "TIPO", "HORAS" };
	private static final String[] LLAVES = { "Cliente", "orden", "descripcion", "tipo", "horas" };

	// Valor que identifica la fila-cabecera importada del Excel (se omite)
	private static final String CABECERA_OBRA = "ORDEN";

	@SuppressWarnings("deprecation")
	public static VBox construirVista(MongoCollection<Document> colOrdenes, MongoCollection<Document> colRefObras,
			String filtroObra) {

		// ── 1. Leer documentos, saltando la fila-cabecera ────────────────────
		List<Document> filas = new ArrayList<>();
		int numeroFila = 1;

		try (MongoCursor<Document> cursor = colOrdenes.find().iterator()) {
			while (cursor.hasNext()) {
				Document doc = cursor.next();

				// Saltar el documento que es la cabecera del Excel
				// Saltar el documento que es la cabecera del Excel
				String obraVal = doc.getString("obra");
				String clienteVal = doc.getString("Cliente");
				String ordenVal2 = doc.getString("orden");
				String refObra = doc.getString("orden");
				if (CABECERA_OBRA.equalsIgnoreCase(obraVal) || "ORDEN".equalsIgnoreCase(ordenVal2)
						|| "CLIENTE".equalsIgnoreCase(clienteVal))
					continue;

				// Filtro por texto libre si el usuario escribió algo en el input
				if (filtroObra != null && !filtroObra.isBlank()) {
					String cliente = doc.getString("Cliente");
					String desc = doc.getString("descripcion");
					String tipo = doc.getString("tipo");
					boolean coincide = (obraVal != null && obraVal.toLowerCase().contains(filtroObra.toLowerCase()))
							|| (cliente != null && cliente.toLowerCase().contains(filtroObra.toLowerCase()))
							|| (desc != null && desc.toLowerCase().contains(filtroObra.toLowerCase()))
							|| (tipo != null && tipo.toLowerCase().contains(filtroObra.toLowerCase()));
					if (!coincide)
						continue;
				}

				Document enriquecido = new Document(doc);
				enriquecido.put("_numFila", numeroFila++);
				filas.add(enriquecido);
			}
		}

		// ── 2. Contenedor principal ──────────────────────────────────────────
		VBox contenedor = new VBox(0);
		contenedor.setStyle("-fx-background-color: blue; -fx-background-radius: 15; "
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 8);");

		// ── 3. Cabecera ──────────────────────────────────────────────────────
		HBox cabecera = new HBox();
		cabecera.setPadding(new Insets(15, 25, 15, 25));
		cabecera.setAlignment(Pos.CENTER_LEFT);
		cabecera.setStyle("-fx-background-color: linear-gradient(to right, #2c3e50, #4b6584);"
				+ "-fx-background-radius: 15 15 0 0;");

		Label lblTitulo = new Label("RESUMEN ÓRDENES FINALES"
				+ (filtroObra != null && !filtroObra.isBlank() ? "  —  FILTRO: \"" + filtroObra + "\"" : ""));
		lblTitulo.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 15px; -fx-letter-spacing: 1px;");

		Region spacerCab = new Region();
		HBox.setHgrow(spacerCab, Priority.ALWAYS);

		Label lblTotal = new Label(filas.size() + " orden(es)");
		lblTotal.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 12px; -fx-font-weight: bold;");

		cabecera.getChildren().addAll(lblTitulo, spacerCab, lblTotal);

		// ── 4. Tabla ─────────────────────────────────────────────────────────
		TableView<Document> tabla = new TableView<>();
		tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		tabla.setMinHeight(500);
		tabla.getSelectionModel().setCellSelectionEnabled(true);
		tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		ContextMenu ctxMenu = new ContextMenu();
		MenuItem itemCopiar = new MenuItem("📋 Copiar para Excel");
		itemCopiar.setStyle("-fx-font-weight: bold;");
		itemCopiar.setOnAction(e -> copiarAlPortapapeles(tabla));
		ctxMenu.getItems().add(itemCopiar);
		tabla.setContextMenu(ctxMenu);
		tabla.setOnKeyPressed(e -> {
			if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.C)
				copiarAlPortapapeles(tabla);
		});

		double[] anchuras = { 180, 160, 300, 140, 90 };

		for (int i = 0; i < COLUMNAS.length; i++) {
			final String llave = LLAVES[i];
			TableColumn<Document, Object> col = new TableColumn<>();

			Label lblHeader = new Label(COLUMNAS[i]);
			lblHeader.setStyle("-fx-text-fill: #000000; -fx-font-weight: 900; -fx-font-size: 11px;");
			col.setGraphic(lblHeader);
			col.setPrefWidth(anchuras[i]);

			col.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().get(llave)));

			col.setCellFactory(column -> new TableCell<>() {
				@Override
				protected void updateItem(Object item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
						setStyle("");
						return;
					}
					setText(item.toString());
					switch (llave) {
					case "obra" -> setStyle("-fx-font-weight: 900; -fx-text-fill: #2980b9;");
					case "tipo" ->
						setStyle("-fx-font-weight: bold; -fx-text-fill: " + colorPorTipo(item.toString()) + ";");
					case "horas" -> {
						int h = parseEntero(item.toString());
						setText(String.valueOf(h)); // ← añade esta línea
						setStyle("-fx-alignment: center-right; -fx-font-weight: bold; -fx-text-fill: "
								+ (h == 0 ? "#a4b0be" : "#2c3e50") + ";");
					}
					default -> setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
					}
				}
			});

			tabla.getColumns().add(col);
		}

		// ── Columna FINALIZADA ───────────────────────────────────────────────
		TableColumn<Document, Void> colFinalizada = new TableColumn<>();
		Label lblFinalizada = new Label("FINALIZADA");
		lblFinalizada.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606f; -fx-font-size: 11px;");
		colFinalizada.setGraphic(lblFinalizada);
		colFinalizada.setPrefWidth(100);

		colFinalizada.setCellFactory(c -> new TableCell<>() {
			private final Button btn = new Button();
			{
				btn.setOnAction(e -> {
					Document doc = getTableView().getItems().get(getIndex());
					boolean estaFinalizada = Boolean.TRUE.equals(doc.getBoolean("finalizada"));
					boolean nuevaFinalizada = !estaFinalizada;

					// Si se finaliza → guardar fecha actual, si se desfinaliza → borrar fecha
					String fechaHoy = nuevaFinalizada
							? new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date())
							: null;

					// Actualizar en MongoDB
					colOrdenes.updateOne(eq("_id", doc.getObjectId("_id")), new Document("$set",
							new Document("finalizada", nuevaFinalizada).append("fechaFinalizada", fechaHoy)));

					// Actualizar en memoria
					doc.put("finalizada", nuevaFinalizada);
					doc.put("fechaFinalizada", fechaHoy);
					getTableView().refresh();
				});
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
					return;
				}
				Document doc = getTableView().getItems().get(getIndex());
				boolean finalizada = Boolean.TRUE.equals(doc.getBoolean("finalizada"));

				if (finalizada) {
					btn.setText("✔ Finalizada");
					btn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; "
							+ "-fx-font-size: 10px; -fx-font-weight: bold; "
							+ "-fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 3 8;");
				} else {
					btn.setText("○ Pendiente");
					btn.setStyle("-fx-background-color: #dfe4ea; -fx-text-fill: #57606f; "
							+ "-fx-font-size: 10px; -fx-font-weight: bold; "
							+ "-fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 3 8;");
				}
				setGraphic(btn);
			}
		});

		tabla.getColumns().add(colFinalizada);

		// ── Columna FECHA FINALIZACIÓN ───────────────────────────────────────
		TableColumn<Document, Object> colFechaFin = new TableColumn<>();
		Label lblFechaFin = new Label("FECHA FINALIZADA");
		lblFechaFin.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606f; -fx-font-size: 11px;");
		colFechaFin.setGraphic(lblFechaFin);
		colFechaFin.setPrefWidth(130);

		colFechaFin.setCellValueFactory(
				d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().get("fechaFinalizada")));

		colFechaFin.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(Object item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setText(null);
					setStyle("");
					return;
				}
				Document doc = getTableView().getItems().get(getIndex());
				boolean finalizada = Boolean.TRUE.equals(doc.getBoolean("finalizada"));
				String fecha = doc.getString("fechaFinalizada");

				if (finalizada && fecha != null) {
					setText(fecha);
					setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 11px;");
				} else {
					setText("—");
					setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 11px;");
				}
			}
		});

		tabla.getColumns().add(colFechaFin);
		tabla.getItems().addAll(filas);

		Label lblVacio = new Label("😕  No se encontraron órdenes de trabajo");
		lblVacio.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 14px;");
		tabla.setPlaceholder(lblVacio);

		contenedor.getChildren().addAll(cabecera, tabla);
		return contenedor;
	}

	// ── Helpers ──────────────────────────────────────────────────────────────

	private static String colorPorTipo(String tipo) {
		if (tipo == null)
			return "#2c3e50";
		return switch (tipo.toUpperCase().trim()) {
		case "TALLER" -> "#8e44ad";
		case "OFICIAL" -> "#2980b9";
		case "OFICIAL 1ª" -> "#2980b9";
		case "PROGRAMADOR" -> "#16a085";
		case "AYUDANTE" -> "#e67e22";
		default -> "#2c3e50";
		};
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

	@SuppressWarnings("deprecation")
	private static void copiarAlPortapapeles(TableView<Document> tabla) {
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
			sb.append(valor == null ? "" : valor);
			filaActual = fila;
		}
		javafx.scene.input.ClipboardContent contenido = new javafx.scene.input.ClipboardContent();
		contenido.putString(sb.toString());
		javafx.scene.input.Clipboard.getSystemClipboard().setContent(contenido);
	}
}