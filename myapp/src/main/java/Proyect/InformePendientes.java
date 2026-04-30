package Proyect;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import Proyect.InformeFueraDePlazo.FilaInforme;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.bson.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class InformePendientes {

	// ── Colores Excel ────────────────────────────────────────────────────────
	private static final byte[] AZUL_OSC = rgb("1F4E79");
	private static final byte[] AZUL_MED = rgb("2E75B6");
	private static final byte[] AZUL_PAL = rgb("DEEAF1");
	private static final byte[] BLANCO = rgb("FFFFFF");
	private static final byte[] GRIS_BRD = rgb("B8CCE4");

	// ────────────────────────────────────────────────────────────────────────
	// DTO interno
	// ────────────────────────────────────────────────────────────────────────
	public static class FilaInforme {
		public final String referencia;
		public final String marca;
		public final String descripcion;
		public final String obra;
		public final String cliente;
		public final String entrega;
		public final int salidaUnidad;
		public final int preparado;
		public final int pedidoCompleto2;
		public final int pendiente;
		public final int porPedir;

		public FilaInforme(String referencia, String marca, String descripcion, String obra, String cliente,
				String entrega, int salidaUnidad, int preparado, int pedidoCompleto2) {
			this.referencia = referencia != null ? referencia : "—";
			this.marca = marca != null ? marca : "—";
			this.descripcion = descripcion != null ? descripcion : "—";
			this.obra = obra != null ? obra : "—";
			this.cliente = cliente != null ? cliente : "—";
			this.entrega = entrega != null ? entrega : "—";
			this.salidaUnidad = salidaUnidad;
			this.preparado = preparado;
			this.pedidoCompleto2 = pedidoCompleto2;
			this.pendiente = Math.max(0, salidaUnidad - preparado);
			this.porPedir = Math.max(0, this.pendiente - pedidoCompleto2);
		}
	}

	// ────────────────────────────────────────────────────────────────────────
	// Consulta MongoDB → lista de filas
	// ────────────────────────────────────────────────────────────────────────
	public static List<FilaInforme> obtenerPendientes(MongoCollection<Document> coleccion) {
		List<FilaInforme> filas = new ArrayList<>();

		try (MongoCursor<Document> cursor = coleccion.find().iterator()) {
			while (cursor.hasNext()) {
				Document obra = cursor.next();
				String refObra = obra.getString("obra");
				String cliente = obra.getString("cliente");
				String entrega = obra.getString("entrega");

				List<Document> mats = obra.getList("materiales", Document.class);
				if (mats == null)
					continue;

				for (Document m : mats) {
					int salida = m.getInteger("salidaUnidad", 0);
					int preparado = parseEntero(m.getOrDefault("preparado", "0").toString());
					int pendiente = Math.max(0, salida - preparado);
					if (pendiente <= 0)
						continue;

					int pedComp2 = m.getInteger("pedidoCompleto2", 0);
					filas.add(new FilaInforme(m.getString("referencia"), m.getString("marca"),
							m.getString("descripcion"), refObra, cliente, entrega, salida, preparado, pedComp2));
				}
			}
		}

		filas.sort(Comparator.comparing((FilaInforme f) -> f.referencia).thenComparing(f -> f.obra));
		return filas;
	}

	// ────────────────────────────────────────────────────────────────────────
	// Vista del panel central
	// ────────────────────────────────────────────────────────────────────────
	@SuppressWarnings({ "deprecation", "unchecked" })
	public static VBox construirVistaInforme(List<FilaInforme> filas, Vista vista,
			MongoCollection<Document> coleccion) {

		VBox contenedor = new VBox(0);
		contenedor.setStyle("-fx-background-color: white; -fx-background-radius: 15; "
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 8);");

		// ── Cabecera ─────────────────────────────────────────────────────────
		HBox cabecera = new HBox(15);
		cabecera.setPadding(new Insets(15, 25, 15, 25));
		cabecera.setAlignment(Pos.CENTER_LEFT);
		cabecera.setStyle("-fx-background-color: linear-gradient(to right, #7f0000, #c0392b);"
				+ "-fx-background-radius: 15 15 0 0;");

		Label lblTit = new Label("📦 INFORME DE ARTÍCULOS PENDIENTES");
		lblTit.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 15px;");

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		String fechaHoy = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
		Label lblFecha = new Label("Generado: " + fechaHoy);
		lblFecha.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 11px;");

		Button btnXlsx = botonExport("📊 Excel", "#27ae60");
		Button btnPdf = botonExport("📄 PDF", "#2980b9");
		btnXlsx.setOnAction(e -> exportarExcel(filas, vista));
		btnPdf.setOnAction(e -> exportarPDF(filas, vista));

		cabecera.getChildren().addAll(lblTit, spacer, lblFecha, btnXlsx, btnPdf);

		// ── Tabla ────────────────────────────────────────────────────────────
		TableView<FilaInforme> tabla = new TableView<>();
		tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		tabla.setMinHeight(520);
		tabla.getSelectionModel().setCellSelectionEnabled(true);
		tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		tabla.setOnKeyPressed(ev -> {
			if (ev.isControlDown() && ev.getCode() == javafx.scene.input.KeyCode.C)
				copiarAlPortapapeles(tabla);
		});

		ContextMenu menu = new ContextMenu();
		MenuItem itemCopiar = new MenuItem("📋 Copiar para Excel");
		itemCopiar.setOnAction(e -> copiarAlPortapapeles(tabla));
		menu.getItems().add(itemCopiar);
		tabla.setContextMenu(menu);

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

		// Columnas — sin PREPARADO, YA PEDIDO, POR PEDIR
		tabla.getColumns().addAll(colTexto("REFERENCIA", 200, f -> f.referencia), colTexto("MARCA", 120, f -> f.marca),
				colTexto("DESCRIPCIÓN", 340, f -> f.descripcion), colTexto("OBRA", 110, f -> f.obra),
				colTexto("CLIENTE", 150, f -> f.cliente), colTexto("ENTREGA", 100, f -> f.entrega),
				colNum("PEDIDO", 75, f -> f.salidaUnidad),
				colNumColor("PENDIENTE", 85, f -> f.pendiente, "#e74c3c", "#27ae60"), colVer);

		tabla.getItems().addAll(filas);

		// Sin bloque resumen — directamente cabecera → tabla
		contenedor.getChildren().addAll(cabecera, tabla);
		return contenedor;
	}

	// ────────────────────────────────────────────────────────────────────────
	// EXPORTAR EXCEL
	// ────────────────────────────────────────────────────────────────────────
	public static void exportarExcel(List<FilaInforme> filas, Vista vista) {
		javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
		fc.setTitle("Guardar informe Excel");
		fc.setInitialFileName(
				"informe_pendientes_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx");
		fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));

		File archivo = fc.showSaveDialog(null);
		if (archivo == null)
			return;

		vista.setEstado("⏳ Exportando informe a Excel...");
		Task<Void> tarea = new Task<>() {
			@Override
			protected Void call() throws Exception {
				generarExcel(filas, archivo);
				return null;
			}
		};
		tarea.setOnSucceeded(e -> vista.setEstado("✅ Informe Excel exportado: " + archivo.getName()));
		tarea.setOnFailed(e -> vista.setEstado("❌ Error: " + tarea.getException().getMessage()));
		new Thread(tarea).start();
	}

	// ────────────────────────────────────────────────────────────────────────
	// EXPORTAR PDF
	// ────────────────────────────────────────────────────────────────────────
	public static void exportarPDF(List<FilaInforme> filas, Vista vista) {
		javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
		fc.setTitle("Guardar informe PDF");
		fc.setInitialFileName(
				"informe_pendientes_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf");
		fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));

		File archivo = fc.showSaveDialog(null);
		if (archivo == null)
			return;

		vista.setEstado("⏳ Exportando informe a PDF...");
		Task<Void> tarea = new Task<>() {
			@Override
			protected Void call() throws Exception {
				generarPDF(filas, archivo);
				return null;
			}
		};
		tarea.setOnSucceeded(e -> vista.setEstado("✅ Informe PDF exportado: " + archivo.getName()));
		tarea.setOnFailed(e -> vista.setEstado("❌ Error: " + tarea.getException().getMessage()));
		new Thread(tarea).start();
	}

	// ────────────────────────────────────────────────────────────────────────
	// Generación Excel — columnas: REFERENCIA, MARCA, DESCRIPCIÓN, OBRA,
	// CLIENTE, ENTREGA, PEDIDO, PENDIENTE
	// ────────────────────────────────────────────────────────────────────────
	private static void generarExcel(List<FilaInforme> filas, File destino) throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			XSSFSheet ws = wb.createSheet("Artículos Pendientes");
			ws.setDefaultRowHeightInPoints(16);
			ws.getPrintSetup().setLandscape(true);
			ws.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
			ws.setFitToPage(true);

			// 8 columnas: REF, MARCA, DESC, OBRA, CLIENTE, ENTREGA, PEDIDO, PENDIENTE
			int[] widths = { 18, 14, 40, 14, 20, 12, 10, 10 };
			for (int i = 0; i < widths.length; i++)
				ws.setColumnWidth(i, widths[i] * 256);

			XSSFCellStyle sTit = mkTitulo(wb);
			XSSFCellStyle sSub = mkSubtitulo(wb);
			XSSFCellStyle sHead = mkHead(wb);
			XSSFCellStyle sDataN = mkDato(wb, false, false);
			XSSFCellStyle sDataA = mkDato(wb, true, false);
			XSSFCellStyle sRojoN = mkDato(wb, false, true);
			XSSFCellStyle sRojoA = mkDato(wb, true, true);
			XSSFCellStyle sTotal = mkTotalStyle(wb);
			XSSFCellStyle sTotNum = mkTotalNum(wb);

			final int LAST_COL = 7; // índice de la última columna (PENDIENTE)

			int r = 0;
			ws.createRow(r++).setHeightInPoints(6);

			// Título
			XSSFRow rowTit = ws.createRow(r++);
			rowTit.setHeightInPoints(32);
			xCell(rowTit, 0, "INFORME DE ARTÍCULOS PENDIENTES", sTit);
			ws.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, LAST_COL));

			// Fecha
			XSSFRow rowFecha = ws.createRow(r++);
			rowFecha.setHeightInPoints(18);
			xCell(rowFecha, 0, "Generado: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()), sSub);
			ws.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, LAST_COL));

			ws.createRow(r++).setHeightInPoints(6);

			// Cabecera columnas
			String[] heads = { "REFERENCIA", "MARCA", "DESCRIPCIÓN", "OBRA", "CLIENTE", "ENTREGA", "PEDIDO",
					"PENDIENTE" };
			XSSFRow rowHead = ws.createRow(r++);
			rowHead.setHeightInPoints(24);
			for (int c = 0; c < heads.length; c++)
				xCell(rowHead, c, heads[c], sHead);

			int firstData = r + 1;

			// Filas de datos
			for (int i = 0; i < filas.size(); i++) {
				FilaInforme f = filas.get(i);
				boolean alt = (i % 2 != 0);
				XSSFRow row = ws.createRow(r++);
				row.setHeightInPoints(15);

				xCell(row, 0, f.referencia, alt ? sDataA : sDataN);
				xCell(row, 1, f.marca, alt ? sDataA : sDataN);
				xCell(row, 2, f.descripcion, alt ? sDataA : sDataN);
				xCell(row, 3, f.obra, alt ? sDataA : sDataN);
				xCell(row, 4, f.cliente, alt ? sDataA : sDataN);
				xCell(row, 5, f.entrega, alt ? sDataA : sDataN);
				xNum(row, 6, f.salidaUnidad, alt ? sDataA : sDataN);
				xNum(row, 7, f.pendiente, alt ? sRojoA : sRojoN);
			}

			int lastData = r;

			// Totales
			XSSFRow rowTot = ws.createRow(r++);
			rowTot.setHeightInPoints(20);
			xCell(rowTot, 0, "TOTALES", sTotal);
			ws.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 5));
			for (int c = 1; c <= 5; c++)
				if (rowTot.getCell(c) == null)
					xCell(rowTot, c, "", sTotal);

			// Totales numéricos: PEDIDO (col 6) y PENDIENTE (col 7)
			for (int c : new int[] { 6, 7 }) {
				String colLetra = String.valueOf((char) ('A' + c));
				XSSFCell fc = rowTot.createCell(c);
				fc.setCellFormula("SUM(" + colLetra + firstData + ":" + colLetra + lastData + ")");
				fc.setCellStyle(sTotNum);
			}

			try (FileOutputStream fos = new FileOutputStream(destino)) {
				wb.write(fos);
			}
		}
	}

	private static void generarPDF(List<FilaInforme> filas, File destino) throws Exception {
		FichaPDF.generarInformePendientes(filas, destino);
	}

	// ────────────────────────────────────────────────────────────────────────
	// Helpers columnas TableView
	// ────────────────────────────────────────────────────────────────────────
	private static <T> TableColumn<FilaInforme, T> col(String titulo, double ancho,
			javafx.util.Callback<FilaInforme, T> getter) {
		TableColumn<FilaInforme, T> col = new TableColumn<>();
		Label lbl = new Label(titulo);
		lbl.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: #000;");
		col.setGraphic(lbl);
		col.setPrefWidth(ancho);
		col.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(getter.call(d.getValue())));
		return col;
	}

	private static TableColumn<FilaInforme, String> colTexto(String titulo, double ancho,
			java.util.function.Function<FilaInforme, String> getter) {
		TableColumn<FilaInforme, String> c = col(titulo, ancho, f -> getter.apply(f));
		c.setCellFactory(col -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? null : item);
				setStyle(empty ? "" : "-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
			}
		});
		return c;
	}

	private static TableColumn<FilaInforme, Integer> colNum(String titulo, double ancho,
			java.util.function.Function<FilaInforme, Integer> getter) {
		TableColumn<FilaInforme, Integer> c = col(titulo, ancho, f -> getter.apply(f));
		c.setCellFactory(col -> new TableCell<>() {
			@Override
			protected void updateItem(Integer item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? null : String.valueOf(item));
				setStyle(empty ? "" : "-fx-alignment: center; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
			}
		});
		return c;
	}

	private static TableColumn<FilaInforme, Integer> colNumColor(String titulo, double ancho,
			java.util.function.Function<FilaInforme, Integer> getter, String colorPos, String colorZero) {
		TableColumn<FilaInforme, Integer> c = col(titulo, ancho, f -> getter.apply(f));
		c.setCellFactory(col -> new TableCell<>() {
			@Override
			protected void updateItem(Integer item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle("");
					return;
				}
				setText(String.valueOf(item));
				setStyle("-fx-alignment: center; -fx-font-weight: 900; -fx-text-fill: "
						+ (item > 0 ? colorPos : colorZero) + ";");
			}
		});
		return c;
	}

	// ────────────────────────────────────────────────────────────────────────
	// Helpers Excel
	// ────────────────────────────────────────────────────────────────────────
	private static void xCell(XSSFRow row, int col, String val, XSSFCellStyle st) {
		XSSFCell c = row.createCell(col);
		c.setCellValue(val != null ? val : "");
		c.setCellStyle(st);
	}

	private static void xNum(XSSFRow row, int col, int val, XSSFCellStyle st) {
		XSSFCell c = row.createCell(col);
		c.setCellValue(val);
		c.setCellStyle(st);
	}

	private static XSSFCellStyle mkTitulo(XSSFWorkbook wb) {
		XSSFCellStyle s = wb.createCellStyle();
		fill(s, AZUL_OSC);
		XSSFFont f = wb.createFont();
		f.setBold(true);
		f.setFontHeightInPoints((short) 18);
		fColor(f, BLANCO);
		s.setFont(f);
		s.setAlignment(HorizontalAlignment.CENTER);
		s.setVerticalAlignment(VerticalAlignment.CENTER);
		return s;
	}

	private static XSSFCellStyle mkSubtitulo(XSSFWorkbook wb) {
		XSSFCellStyle s = wb.createCellStyle();
		fill(s, rgb("EBF3FB"));
		XSSFFont f = wb.createFont();
		f.setBold(true);
		f.setFontHeightInPoints((short) 11);
		fColor(f, AZUL_OSC);
		s.setFont(f);
		s.setAlignment(HorizontalAlignment.CENTER);
		s.setVerticalAlignment(VerticalAlignment.CENTER);
		borde(s, BorderStyle.THIN, GRIS_BRD);
		return s;
	}

	private static XSSFCellStyle mkHead(XSSFWorkbook wb) {
		XSSFCellStyle s = wb.createCellStyle();
		fill(s, AZUL_MED);
		XSSFFont f = wb.createFont();
		f.setBold(true);
		f.setFontHeightInPoints((short) 10);
		fColor(f, BLANCO);
		s.setFont(f);
		s.setAlignment(HorizontalAlignment.CENTER);
		s.setVerticalAlignment(VerticalAlignment.CENTER);
		borde(s, BorderStyle.MEDIUM, AZUL_OSC);
		s.setWrapText(true);
		return s;
	}

	private static XSSFCellStyle mkDato(XSSFWorkbook wb, boolean alt, boolean resaltado) {
		XSSFCellStyle s = wb.createCellStyle();
		if (resaltado) {
			fill(s, alt ? rgb("FADBD8") : rgb("FDFEFE"));
			XSSFFont f = wb.createFont();
			f.setBold(true);
			f.setFontHeightInPoints((short) 10);
			fColor(f, rgb("C0392B"));
			s.setFont(f);
		} else {
			fill(s, alt ? AZUL_PAL : BLANCO);
			XSSFFont f = wb.createFont();
			f.setFontHeightInPoints((short) 10);
			s.setFont(f);
		}
		s.setAlignment(HorizontalAlignment.CENTER);
		s.setVerticalAlignment(VerticalAlignment.CENTER);
		borde(s, BorderStyle.THIN, GRIS_BRD);
		return s;
	}

	private static XSSFCellStyle mkTotalStyle(XSSFWorkbook wb) {
		XSSFCellStyle s = wb.createCellStyle();
		fill(s, AZUL_OSC);
		XSSFFont f = wb.createFont();
		f.setBold(true);
		f.setFontHeightInPoints((short) 11);
		fColor(f, BLANCO);
		s.setFont(f);
		s.setAlignment(HorizontalAlignment.CENTER);
		s.setVerticalAlignment(VerticalAlignment.CENTER);
		borde(s, BorderStyle.MEDIUM, AZUL_OSC);
		return s;
	}

	private static XSSFCellStyle mkTotalNum(XSSFWorkbook wb) {
		XSSFCellStyle s = mkTotalStyle(wb);
		s.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
		return s;
	}

	private static void fill(XSSFCellStyle s, byte[] color) {
		s.setFillForegroundColor(new XSSFColor(color, null));
		s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	}

	private static void fColor(XSSFFont f, byte[] color) {
		f.setColor(new XSSFColor(color, null));
	}

	private static void borde(XSSFCellStyle s, BorderStyle bs, byte[] color) {
		XSSFColor c = new XSSFColor(color, null);
		s.setBorderTop(bs);
		s.setTopBorderColor(c);
		s.setBorderBottom(bs);
		s.setBottomBorderColor(c);
		s.setBorderLeft(bs);
		s.setLeftBorderColor(c);
		s.setBorderRight(bs);
		s.setRightBorderColor(c);
	}

	private static byte[] rgb(String hex) {
		return new byte[] { (byte) Integer.parseInt(hex.substring(0, 2), 16),
				(byte) Integer.parseInt(hex.substring(2, 4), 16), (byte) Integer.parseInt(hex.substring(4, 6), 16) };
	}

	// ────────────────────────────────────────────────────────────────────────
	// Helpers UI
	// ────────────────────────────────────────────────────────────────────────
	private static Button botonExport(String texto, String color) {
		Button b = new Button(texto);
		b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
				+ "-fx-font-weight: bold; -fx-font-size: 12px; "
				+ "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14;");
		return b;
	}

	@SuppressWarnings("rawtypes")
	private static void copiarAlPortapapeles(TableView<FilaInforme> tabla) {
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
		javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
		cc.putString(sb.toString());
		javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
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
}