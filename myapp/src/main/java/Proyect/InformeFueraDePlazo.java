package Proyect;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.bson.Document;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class InformeFueraDePlazo {

	// ═════════════════════════════════════════════════════════════════════════
	// MODELO DE FILA — sin preparado, ya pedido, por pedir
	// ═════════════════════════════════════════════════════════════════════════
	public static class FilaInforme {
		public final String obra;
		public final String cliente;
		public final String proyecto;
		public final String entrega;
		public final long diasRetraso;
		public final int A3;
		public final String marca;
		public final String referencia;
		public final String descripcion;
		public final int salidaUnidad;
		public final int pendiente;

		public FilaInforme(String obra, String cliente, String proyecto, String entrega, long diasRetraso, int A3,
				String marca, String referencia, String descripcion, int salidaUnidad, int preparado) {
			this.obra = nvl(obra);
			this.cliente = nvl(cliente);
			this.proyecto = nvl(proyecto);
			this.entrega = nvl(entrega);
			this.diasRetraso = diasRetraso;
			this.A3 = A3;
			this.marca = nvl(marca);
			this.referencia = nvl(referencia);
			this.descripcion = nvl(descripcion);
			this.salidaUnidad = salidaUnidad;
			this.pendiente = Math.max(0, salidaUnidad - preparado);
		}
	}

	// ═════════════════════════════════════════════════════════════════════════
	// EXTRACCIÓN DE DATOS
	// ═════════════════════════════════════════════════════════════════════════
	public static List<FilaInforme> obtenerFueraDePlazo(MongoCollection<Document> coleccion) {
		List<FilaInforme> resultado = new ArrayList<>();
		Date hoy = new Date();

		List<SimpleDateFormat> formatos = Arrays.asList(new SimpleDateFormat("dd/MM/yyyy HH:mm"),
				new SimpleDateFormat("dd/MM/yyyy"), new SimpleDateFormat("d/M/yyyy"),
				new SimpleDateFormat("yyyy-MM-dd"), new SimpleDateFormat("dd/MM/yy"), new SimpleDateFormat("d/M/yy"),
				new SimpleDateFormat("M/dd/yy"), new SimpleDateFormat("MM/dd/yy"), new SimpleDateFormat("M/dd/yyyy"),
				new SimpleDateFormat("MM/dd/yyyy"));

		try (MongoCursor<Document> cursor = coleccion.find().iterator()) {
			while (cursor.hasNext()) {
				Document obra = cursor.next();

				String entregaStr = obra.getString("entrega");
				if (entregaStr == null || entregaStr.isBlank() || entregaStr.equals("—"))
					continue;

				Date fechaEntrega = parsearFecha(entregaStr, formatos);
				if (fechaEntrega == null || !fechaEntrega.before(hoy))
					continue;

				long diasRetraso = java.util.concurrent.TimeUnit.MILLISECONDS
						.toDays(hoy.getTime() - fechaEntrega.getTime());

				List<Document> mats = obra.getList("materiales", Document.class);
				if (mats == null)
					continue;

				for (Document m : mats) {
					int salida = m.getInteger("salidaUnidad", 0);
					int prep = parseEntero(m.getOrDefault("preparado", "0").toString());
					int pendiente = Math.max(0, salida - prep);
					if (pendiente == 0)
						continue;

					resultado.add(new FilaInforme(obra.getString("obra"), obra.getString("cliente"),
							obra.getString("proyecto"), entregaStr, diasRetraso, m.getInteger("A3", 0),
							m.getString("marca"), m.getString("referencia"), m.getString("descripcion"), salida, prep));
				}
			}
		}

		resultado.sort(Comparator.comparingLong((FilaInforme f) -> -f.diasRetraso).thenComparing(f -> f.obra)
				.thenComparingInt(f -> f.A3));

		return resultado;
	}

	// ═════════════════════════════════════════════════════════════════════════
	// VISTA
	// ═════════════════════════════════════════════════════════════════════════
	@SuppressWarnings({ "deprecation", "unchecked", "unchecked" })
	public static VBox construirVistaInforme(List<FilaInforme> filas, Vista vista,
			MongoCollection<Document> coleccion) {

		VBox contenedor = new VBox(0);
		contenedor.setStyle("-fx-background-color: white; -fx-background-radius: 15; "
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 8);");

		// ── Cabecera ──────────────────────────────────────────────────────────
		HBox cabecera = new HBox(20);
		cabecera.setPadding(new Insets(15, 25, 15, 25));
		cabecera.setAlignment(Pos.CENTER_LEFT);
		cabecera.setStyle("-fx-background-color: linear-gradient(to right, #922b21, #e74c3c);"
				+ "-fx-background-radius: 15 15 0 0;");

		long obras = filas.stream().map(f -> f.obra).distinct().count();
		int totalPend = filas.stream().mapToInt(f -> f.pendiente).sum();

		Label lblTitulo = new Label("⚠  ARTÍCULOS FUERA DE PLAZO  —  " + filas.size() + " artículo(s)");
		lblTitulo.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 15px;");
		Label lblSub = new Label("Obras afectadas: " + obras + "  |  Uds. pendientes: " + totalPend);
		lblSub.setStyle("-fx-text-fill: #fadbd8; -fx-font-size: 12px;");

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		Button btnExcel = new Button("📊 Exportar Excel");
		btnExcel.setStyle("-fx-background-color:  #27ae60; -fx-text-fill: white; "
				+ "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14;");
		btnExcel.setOnAction(e -> exportarExcel(filas, vista));

		Button btnPDF = new Button("📄 Exportar PDF");
		btnPDF.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; "
				+ "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14;");
		btnPDF.setOnAction(e -> exportarPDF(filas, vista));

		cabecera.getChildren().addAll(new VBox(4, lblTitulo, lblSub), spacer, btnExcel, btnPDF);

		// ── Chips de resumen ──────────────────────────────────────────────────
		long criticos = filas.stream().filter(f -> f.diasRetraso > 30).count();
		long refs = filas.stream().map(f -> f.referencia).distinct().count();

		HBox resumen = new HBox(20);
		resumen.setPadding(new Insets(12, 25, 12, 25));
		resumen.setAlignment(Pos.CENTER_LEFT);
		resumen.setStyle("-fx-background-color: #fdf2f2; -fx-border-color: #e8d0d0; -fx-border-width: 0 0 1 0;");
		resumen.getChildren().addAll(chip("Obras afectadas", String.valueOf(obras), "#c0392b"),
				chip("Referencias únicas", String.valueOf(refs), "#8e44ad"),
				chip("Uds. pendientes", String.valueOf(totalPend), "#e74c3c"),
				chip("Críticos (>30 días)", String.valueOf(criticos), "#922b21"));

		// ── Tabla ─────────────────────────────────────────────────────────────
		TableView<FilaInforme> tabla = new TableView<>();
		tabla.setMinHeight(520);
		tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		tabla.getSelectionModel().setCellSelectionEnabled(true);
		tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tabla.setOnKeyPressed(ev -> {
			if (ev.isControlDown() && ev.getCode() == javafx.scene.input.KeyCode.C)
				copiarAlPortapapeles(tabla);
		});

		ContextMenu menu = new ContextMenu();
		MenuItem copiar = new MenuItem("📋 Copiar para Excel");
		copiar.setOnAction(e -> copiarAlPortapapeles(tabla));
		menu.getItems().add(copiar);
		tabla.setContextMenu(menu);

		// Colores de fila según días de retraso
		tabla.setRowFactory(tv -> new TableRow<>() {
			@Override
			protected void updateItem(FilaInforme item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setStyle("");
					return;
				}
				if (item.diasRetraso > 30)
					setStyle("-fx-background-color: #fadbd8;");
				else if (item.diasRetraso > 7)
					setStyle("-fx-background-color: #fdebd0;");
				else
					setStyle("-fx-background-color: #fef9e7;");
			}
		});

		// ── Columnas ──────────────────────────────────────────────────────────

		TableColumn<FilaInforme, Void> colVer = new TableColumn<>();
		Label lblVer = new Label("VER");
		lblVer.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606f; -fx-font-size: 11px;");
		colVer.setGraphic(lblVer);
		colVer.setPrefWidth(70);
		colVer.setCellFactory(col -> new TableCell<>() {
			private final Button btn = new Button("Ver →");
			{
				btn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; "
						+ "-fx-font-size: 10px; -fx-font-weight: bold; "
						+ "-fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 3 8;");
				btn.setOnAction(e -> {
					FilaInforme fila = getTableView().getItems().get(getIndex());
					Document obraDoc = coleccion.find(com.mongodb.client.model.Filters.eq("obra", fila.obra)).first();
					if (obraDoc != null)
						VentanaResumenObra.mostrar(obraDoc);
				});
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(empty ? null : btn);
			}
		});
		
		tabla.getColumns().addAll(colTexto("OBRA", 110, f -> f.obra), colTexto("CLIENTE", 130, f -> f.cliente),
				colTexto("PROYECTO", 160, f -> f.proyecto), colTexto("F. ENTREGA", 95, f -> f.entrega), colRetraso(),
				colNumero("A3", 60, f -> f.A3), colTexto("MARCA", 95, f -> f.marca),
				colTexto("REFERENCIA", 120, f -> f.referencia), colTexto("DESCRIPCIÓN", 280, f -> f.descripcion),
				colNumero("PEDIDO", 70, f -> f.salidaUnidad), colPendiente(), colVer);

		tabla.getItems().addAll(filas);
		contenedor.getChildren().addAll(cabecera, tabla);
		return contenedor;
	}

	// ═════════════════════════════════════════════════════════════════════════
	// EXPORTAR EXCEL
	// ═════════════════════════════════════════════════════════════════════════
	private static void exportarExcel(List<FilaInforme> filas, Vista vista) {
		javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
		fc.setTitle("Guardar informe Excel");
		fc.setInitialFileName("informe_fuera_de_plazo.xlsx");
		fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
		File archivo = fc.showSaveDialog(null);
		if (archivo == null)
			return;

		vista.setEstado("⏳ Exportando Excel...");
		Task<Void> tarea = new Task<>() {
			@Override
			protected Void call() throws Exception {
				generarExcel(filas, archivo);
				return null;
			}
		};
		tarea.setOnSucceeded(e -> {
			vista.setEstado("✅ Excel exportado: " + archivo.getName());
			try {
				if (java.awt.Desktop.isDesktopSupported())
					java.awt.Desktop.getDesktop().open(archivo);
			} catch (Exception ignored) {
			}
		});
		tarea.setOnFailed(e -> vista.setEstado("❌ Error Excel: " + tarea.getException().getMessage()));
		new Thread(tarea).start();
	}

	// ═════════════════════════════════════════════════════════════════════════
	// EXPORTAR PDF
	// ═════════════════════════════════════════════════════════════════════════
	private static void exportarPDF(List<FilaInforme> filas, Vista vista) {
		javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
		fc.setTitle("Guardar informe PDF");
		fc.setInitialFileName("informe_fuera_de_plazo.pdf");
		fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
		File archivo = fc.showSaveDialog(null);
		if (archivo == null)
			return;

		vista.setEstado("⏳ Exportando PDF...");
		Task<Void> tarea = new Task<>() {
			@Override
			protected Void call() throws Exception {
				FichaPDF.generarInformeFueraDePlazo(filas, archivo);
				return null;
			}
		};
		tarea.setOnSucceeded(e -> {
			vista.setEstado("✅ PDF exportado: " + archivo.getName());
			try {
				if (java.awt.Desktop.isDesktopSupported())
					java.awt.Desktop.getDesktop().open(archivo);
			} catch (Exception ignored) {
			}
		});
		tarea.setOnFailed(e -> vista.setEstado("❌ Error PDF: " + tarea.getException().getMessage()));
		new Thread(tarea).start();
	}

	// ═════════════════════════════════════════════════════════════════════════
	// GENERACIÓN EXCEL — sin columnas preparado / ya pedido / por pedir
	// ═════════════════════════════════════════════════════════════════════════
	private static void generarExcel(List<FilaInforme> filas, File destino) throws Exception {
		try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {

			org.apache.poi.xssf.usermodel.XSSFSheet ws = wb.createSheet("Fuera de Plazo");
			ws.getPrintSetup().setLandscape(true);
			ws.getPrintSetup().setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE);

			// Anchos: OBRA, CLIENTE, PROYECTO, F.ENTREGA, RETRASO, A3, REFERENCIA,
			// DESCRIPCIÓN, PEDIDO, PENDIENTE
			int[] anchos = { 14, 18, 22, 12, 14, 7, 16, 40, 10, 10 };
			for (int i = 0; i < anchos.length; i++)
				ws.setColumnWidth(i, anchos[i] * 256);

			byte[] ROJO_OSC = rgb("922B21");
			byte[] ROJO_PAL = rgb("FADBD8");
			byte[] NARAN_PAL = rgb("FDEBD0");
			byte[] AMAR_PAL = rgb("FEF9E7");
			byte[] AZUL_OSC = rgb("1F4E79");
			byte[] ROJO_CAB = rgb("C0392B");
			byte[] BLANCO = rgb("FFFFFF");
			byte[] NEGRO = rgb("1F1F1F");

			org.apache.poi.xssf.usermodel.XSSFCellStyle sTit = mkSt(wb, ROJO_OSC, BLANCO, true, 14);
			org.apache.poi.xssf.usermodel.XSSFCellStyle sRes = mkSt(wb, ROJO_PAL, ROJO_OSC, true, 9);
			org.apache.poi.xssf.usermodel.XSSFCellStyle sCab = mkSt(wb, AZUL_OSC, BLANCO, true, 9);
			org.apache.poi.xssf.usermodel.XSSFCellStyle sCabR = mkSt(wb, ROJO_CAB, BLANCO, true, 9);
			org.apache.poi.xssf.usermodel.XSSFCellStyle sCrit = mkSt(wb, ROJO_PAL, NEGRO, true, 10);
			org.apache.poi.xssf.usermodel.XSSFCellStyle sHigh = mkSt(wb, NARAN_PAL, NEGRO, true, 10);
			org.apache.poi.xssf.usermodel.XSSFCellStyle sMed = mkSt(wb, AMAR_PAL, NEGRO, true, 10);
			org.apache.poi.xssf.usermodel.XSSFCellStyle sTot = mkSt(wb, AZUL_OSC, BLANCO, true, 11);
			org.apache.poi.xssf.usermodel.XSSFCellStyle sTotR = mkSt(wb, ROJO_CAB, BLANCO, true, 11);

			String[] cabeceras = { "OBRA", "CLIENTE", "PROYECTO", "F. ENTREGA", "RETRASO (días)", "A3", "REFERENCIA",
					"DESCRIPCIÓN", "PEDIDO", "PENDIENTE" };

			String fecha = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
			long totalObras = filas.stream().map(f -> f.obra).distinct().count();
			int totalPend = filas.stream().mapToInt(f -> f.pendiente).sum();

			int r = 0;

			// Título
			org.apache.poi.xssf.usermodel.XSSFRow rowTit = ws.createRow(r++);
			rowTit.setHeightInPoints(26);
			xc(rowTit, 0, "INFORME ARTÍCULOS FUERA DE PLAZO — " + fecha, sTit);
			ws.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(r - 1, r - 1, 0, cabeceras.length - 1));

			// Resumen
			org.apache.poi.xssf.usermodel.XSSFRow rowRes = ws.createRow(r++);
			rowRes.setHeightInPoints(16);
			xc(rowRes, 0, "Obras afectadas: " + totalObras + "   |   Artículos: " + filas.size()
					+ "   |   Uds. pendientes: " + totalPend, sRes);
			ws.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(r - 1, r - 1, 0, cabeceras.length - 1));

			// Cabecera columnas
			org.apache.poi.xssf.usermodel.XSSFRow rowCab = ws.createRow(r++);
			rowCab.setHeightInPoints(20);
			for (int c = 0; c < cabeceras.length; c++)
				xc(rowCab, c, cabeceras[c], c == 9 ? sCabR : sCab);

			// Filas de datos
			for (FilaInforme f : filas) {
				org.apache.poi.xssf.usermodel.XSSFRow row = ws.createRow(r++);
				row.setHeightInPoints(15);
				org.apache.poi.xssf.usermodel.XSSFCellStyle sf = f.diasRetraso > 30 ? sCrit
						: f.diasRetraso > 7 ? sHigh : sMed;
				xc(row, 0, f.obra, sf);
				xc(row, 1, f.cliente, sf);
				xc(row, 2, f.proyecto, sf);
				xc(row, 3, f.entrega, sf);
				xc(row, 4, f.diasRetraso + " días", sf);
				xcN(row, 5, f.A3, sf);
				xc(row, 6, f.referencia, sf);
				xc(row, 7, f.descripcion, sf);
				xcN(row, 8, f.salidaUnidad, sf);
				xcN(row, 9, f.pendiente, sf);
			}

			// Totales
			org.apache.poi.xssf.usermodel.XSSFRow rowTot = ws.createRow(r++);
			rowTot.setHeightInPoints(18);
			xc(rowTot, 0, "TOTAL", sTot);
			ws.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(r - 1, r - 1, 0, 7));
			for (int c = 1; c <= 7; c++)
				xc(rowTot, c, "", sTot);
			xcN(rowTot, 8, filas.stream().mapToInt(f -> f.salidaUnidad).sum(), sTot);
			xcN(rowTot, 9, filas.stream().mapToInt(f -> f.pendiente).sum(), sTotR);

			try (java.io.FileOutputStream fos = new java.io.FileOutputStream(destino)) {
				wb.write(fos);
			}
		}
	}

	// ═════════════════════════════════════════════════════════════════════════
	// HELPERS EXCEL
	// ═════════════════════════════════════════════════════════════════════════
	private static void xc(org.apache.poi.xssf.usermodel.XSSFRow row, int col, String val,
			org.apache.poi.xssf.usermodel.XSSFCellStyle st) {
		org.apache.poi.xssf.usermodel.XSSFCell c = row.createCell(col);
		c.setCellValue(val != null ? val : "");
		c.setCellStyle(st);
	}

	private static void xcN(org.apache.poi.xssf.usermodel.XSSFRow row, int col, long val,
			org.apache.poi.xssf.usermodel.XSSFCellStyle st) {
		org.apache.poi.xssf.usermodel.XSSFCell c = row.createCell(col);
		c.setCellValue(val);
		c.setCellStyle(st);
	}

	private static org.apache.poi.xssf.usermodel.XSSFCellStyle mkSt(org.apache.poi.xssf.usermodel.XSSFWorkbook wb,
			byte[] bg, byte[] fc, boolean bold, int sz) {
		org.apache.poi.xssf.usermodel.XSSFCellStyle s = wb.createCellStyle();
		s.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(bg, null));
		s.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
		org.apache.poi.xssf.usermodel.XSSFFont f = wb.createFont();
		f.setBold(bold);
		f.setFontHeightInPoints((short) sz);
		f.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(fc, null));
		s.setFont(f);
		s.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
		s.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
		org.apache.poi.xssf.usermodel.XSSFColor borde = new org.apache.poi.xssf.usermodel.XSSFColor(rgb("B8CCE4"),
				null);
		s.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
		s.setTopBorderColor(borde);
		s.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
		s.setBottomBorderColor(borde);
		s.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
		s.setLeftBorderColor(borde);
		s.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
		s.setRightBorderColor(borde);
		return s;
	}

	private static byte[] rgb(String hex) {
		return new byte[] { (byte) Integer.parseInt(hex.substring(0, 2), 16),
				(byte) Integer.parseInt(hex.substring(2, 4), 16), (byte) Integer.parseInt(hex.substring(4, 6), 16) };
	}

	// ═════════════════════════════════════════════════════════════════════════
	// HELPERS COLUMNAS JAVAFX
	// ═════════════════════════════════════════════════════════════════════════
	@FunctionalInterface
	private interface Ext<T> {
		T get(FilaInforme f);
	}

	private static TableColumn<FilaInforme, String> colTexto(String titulo, double ancho, Ext<String> ex) {
		TableColumn<FilaInforme, String> col = new TableColumn<>();
		col.setGraphic(hdr(titulo));
		col.setPrefWidth(ancho);
		col.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(ex.get(d.getValue())));
		col.setCellFactory(c -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? null : item);
				setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
			}
		});
		return col;
	}

	private static TableColumn<FilaInforme, Number> colNumero(String titulo, double ancho, Ext<Integer> ex) {
		TableColumn<FilaInforme, Number> col = new TableColumn<>();
		col.setGraphic(hdr(titulo));
		col.setPrefWidth(ancho);
		col.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(ex.get(d.getValue())));
		col.setCellFactory(c -> new TableCell<>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? null : item.toString());
				setStyle("-fx-font-weight: bold; -fx-alignment: center; -fx-text-fill: #2c3e50;");
			}
		});
		return col;
	}

	private static TableColumn<FilaInforme, Number> colRetraso() {
		TableColumn<FilaInforme, Number> col = new TableColumn<>();
		col.setGraphic(hdr("RETRASO"));
		col.setPrefWidth(90);
		col.setCellValueFactory(d -> new javafx.beans.property.SimpleLongProperty(d.getValue().diasRetraso));
		col.setCellFactory(c -> new TableCell<>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle("");
					return;
				}
				long d = item.longValue();
				setText(d + " día" + (d == 1 ? "" : "s"));
				String color = d > 30 ? "#e74c3c" : d > 7 ? "#e67e22" : "#f39c12";
				setStyle("-fx-font-weight: 900; -fx-alignment: center; -fx-text-fill: " + color + ";");
			}
		});
		return col;
	}

	private static TableColumn<FilaInforme, Number> colPendiente() {
		TableColumn<FilaInforme, Number> col = new TableColumn<>();
		col.setGraphic(hdr("PENDIENTE"));
		col.setPrefWidth(85);
		col.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().pendiente));
		col.setCellFactory(c -> new TableCell<>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle("");
					return;
				}
				int v = item.intValue();
				setText(String.valueOf(v));
				setStyle("-fx-font-weight: 900; -fx-alignment: center; -fx-text-fill: "
						+ (v > 0 ? "#e74c3c" : "#27ae60") + ";");
			}
		});
		return col;
	}

	private static Label hdr(String texto) {
		Label l = new Label(texto);
		l.setStyle("-fx-text-fill: #000000; -fx-font-weight: 900; -fx-font-size: 11px;");
		return l;
	}

	// ═════════════════════════════════════════════════════════════════════════
	// HELPERS UI
	// ═════════════════════════════════════════════════════════════════════════
	private static HBox chip(String label, String valor, String color) {
		VBox box = new VBox(2);
		box.setAlignment(Pos.CENTER);
		box.setPadding(new Insets(6, 14, 6, 14));
		box.setStyle("-fx-background-color: white; -fx-background-radius: 10; " + "-fx-border-color: " + color
				+ "; -fx-border-width: 0 0 3 0; " + "-fx-border-radius: 10;");
		Label lv = new Label(valor);
		lv.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + color + ";");
		Label ll = new Label(label);
		ll.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
		box.getChildren().addAll(lv, ll);
		return new HBox(box);
	}

	// ═════════════════════════════════════════════════════════════════════════
	// HELPERS GENERALES
	// ═════════════════════════════════════════════════════════════════════════
	@SuppressWarnings("deprecation")
	private static void copiarAlPortapapeles(TableView<FilaInforme> tabla) {
		StringBuilder sb = new StringBuilder();
		var celdas = tabla.getSelectionModel().getSelectedCells();
		int filaActual = -1;
		for (TablePosition pos : celdas) {
			if (filaActual != -1 && filaActual != pos.getRow())
				sb.append("\n");
			else if (filaActual == pos.getRow())
				sb.append("\t");
			Object val = tabla.getColumns().get(pos.getColumn()).getCellData(pos.getRow());
			sb.append(val == null ? "" : val.toString());
			filaActual = pos.getRow();
		}
		javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
		cc.putString(sb.toString());
		javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
	}

	private static Date parsearFecha(String texto, List<SimpleDateFormat> formatos) {
		if (texto == null || texto.isBlank())
			return null;

		// Fix: corregir año 0026 → 2026
		texto = texto.trim();
		if (texto.matches("\\d{1,2}/\\d{1,2}/00\\d{2}")) {
			texto = texto.replaceAll("/00(\\d{2})$", "/20$1");
		}

		Date yearStart;
		try {
			yearStart = new SimpleDateFormat("yyyy").parse("2000");
		} catch (ParseException e) {
			yearStart = new Date(100, 0, 1);
		}
		for (SimpleDateFormat fmt : formatos) {
			try {
				SimpleDateFormat copia = new SimpleDateFormat(fmt.toPattern());
				copia.setLenient(false);
				copia.set2DigitYearStart(yearStart);
				return copia.parse(texto);
			} catch (ParseException ignored) {
			}
		}
		return null;
	}

	private static int parseEntero(String valor) {
		try {
			if (valor == null || valor.isBlank())
				return 0;
			return (int) Double.parseDouble(valor.replace(",", "."));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String nvl(String s) {
		return s != null ? s : "—";
	}
}