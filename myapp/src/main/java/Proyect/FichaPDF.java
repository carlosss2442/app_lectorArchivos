package Proyect;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.bson.Document;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FichaPDF {

	private static final float PW = PDRectangle.A4.getHeight();
	private static final float PH = PDRectangle.A4.getWidth();
	private static final float ML = 20f;
	private static final float MR = 20f;
	private static final float MT = 20f;
	private static final float MB = 25f;

	private static final float[] AZUL_OSC = hex("1F4E79");
	private static final float[] AZUL_MED = hex("2E75B6");
	private static final float[] AZUL_PAL = hex("DEEAF1");
	private static final float[] AZUL_CABM = hex("BDD7EE");
	private static final float[] NARANJA = hex("F4B942");
	private static final float[] BLANCO = hex("FFFFFF");
	private static final float[] ROJO = hex("C0392B");
	private static final float[] VERDE = hex("27AE60");
	private static final float[] AZUL_VAL = hex("2980B9");
	private static final float[] GRIS_BORDE = hex("B8CCE4");
	private static final float[] NEGRO = hex("1F1F1F");
	private static final float[] FONDO_PG = hex("F0F2F5");
	private static final float[] GRIS_TEXT = hex("7F8C8D");
	private static final float[] ROJO_PAL = hex("FADBD8");

	// ── 9 columnas obra ───────────────────────────────────────────────────────
	private static final int[] COL_W_CHARS = { 8, 14, 16, 36, 13, 11, 16, 16, 20 };
	private static final String[] COL_TITLES = { "A3", "MARCA", "REFERENCIA", "DESCRIPCION", "SALIDA", "A PEDIR",
			"PEDIDO\nCOMPLETO", "CANTIDAD\nPEDIDO", "FECHA PEDIDO" };
	private static final int NC = COL_TITLES.length;

	// ── 11 columnas informe pendientes ────────────────────────────────────────
	private static final int[] INF_W = { 14, 10, 28, 10, 14, 10, 7, 8, 8, 8, 8 };
	private static final String[] INF_TITLES = { "REFERENCIA", "MARCA", "DESCRIPCION", "OBRA", "CLIENTE", "ENTREGA",
			"PEDIDO", "PREPARADO", "PENDIENTE", "YA PEDIDO", "POR PEDIR" };
	private static final int INC = INF_TITLES.length;

	private PDFont BOLD, REGULAR, ITALIC;
	private float[] colX, colW;
	private float tableW;
	private float y;
	private PDDocument doc;
	private PDPageContentStream cs;

	// ═════════════════════════════════════════════════════════════════════════
	// API PÚBLICA — obras
	// ═════════════════════════════════════════════════════════════════════════

	public static void generarObraCompleta(Document obra, File destino) throws Exception {
		new FichaPDF().generar(obra, destino);
	}

	public static byte[] generarFichaBytes(Document obra, Document mat) throws Exception {
		Document obraTemp = new Document();
		obraTemp.put("obra", obra.get("obra"));
		obraTemp.put("cliente", obra.get("cliente"));
		obraTemp.put("proyecto", obra.get("proyecto"));
		obraTemp.put("entrega", obra.get("entrega"));
		obraTemp.put("responsable", obra.get("responsable"));
		obraTemp.put("impresion", obra.get("impresion"));
		obraTemp.put("titulo", obra.get("titulo"));
		obraTemp.put("materiales", java.util.Collections.singletonList(mat));
		File tmp = File.createTempFile("qr_ficha_", ".pdf");
		try {
			new FichaPDF().generar(obraTemp, tmp);
			return java.nio.file.Files.readAllBytes(tmp.toPath());
		} finally {
			tmp.delete();
		}
	}

	// ═════════════════════════════════════════════════════════════════════════
	// API PÚBLICA — informe pendientes (PDFBox puro, sin iText)
	// ═════════════════════════════════════════════════════════════════════════

	public static void generarInformePendientes(List<InformePendientes.FilaInforme> filas, File destino)
			throws Exception {
		new FichaPDF().generarInforme(filas, destino);
	}

	// ═════════════════════════════════════════════════════════════════════════
	// GENERACIÓN — obra
	// ═════════════════════════════════════════════════════════════════════════

	private void generar(Document obra, File destino) throws Exception {
		List<Document> mats = obra.getList("materiales", Document.class);
		if (mats == null || mats.isEmpty())
			throw new IllegalArgumentException("La obra no tiene materiales.");

		doc = new PDDocument();
		initFuentes();
		initColumnas();

		abrirPagina();
		dibujarFondo();
		dibujarTitulo();
		dibujarMetadatos(obra);
		y -= 6;
		dibujarCabeceraTabla();

		final float H_FILA = 13f;
		final float H_TOTAL = 18f;
		final float RESERVA = MB + H_TOTAL + H_FILA + 18f;

		for (int i = 0; i < mats.size(); i++) {
			if (y - H_FILA < RESERVA) {
				dibujarPie(false);
				abrirPagina();
				dibujarFondo();
				dibujarCabeceraTabla();
			}
			dibujarFilaMaterial(mats.get(i), i, H_FILA);
		}

		if (y - H_TOTAL < MB + 18f) {
			dibujarPie(false);
			abrirPagina();
			dibujarFondo();
		}
		dibujarFilaTotal(mats);
		dibujarPie(true);

		cs.close();
		doc.save(destino);
		doc.close();
	}

	// ═════════════════════════════════════════════════════════════════════════
	// GENERACIÓN — informe pendientes
	// ═════════════════════════════════════════════════════════════════════════

	private void generarInforme(List<InformePendientes.FilaInforme> filas, File destino) throws Exception {

		doc = new PDDocument();
		initFuentes();

		// Calcular anchos de columna para las 11 columnas del informe
		tableW = PW - ML - MR;
		int totalW = 0;
		for (int w : INF_W)
			totalW += w;
		float[] iColW = new float[INC];
		float[] iColX = new float[INC];
		float cx = ML;
		for (int i = 0; i < INC; i++) {
			iColW[i] = tableW * INF_W[i] / (float) totalW;
			iColX[i] = cx;
			cx += iColW[i];
		}

		final float H_CAB = 22f;
		final float H_FILA = 12f;
		final float H_TOT = 18f;
		final float RESERVA = MB + H_TOT + H_FILA + 18f;

		// Calcular resumen
		long totalRefs = filas.stream().map(f -> f.referencia).distinct().count();
		long totalObras = filas.stream().map(f -> f.obra).distinct().count();
		int totalPend = filas.stream().mapToInt(f -> f.pendiente).sum();
		int totalPorP = filas.stream().mapToInt(f -> f.porPedir).sum();
		String fechaHoy = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());

		// ── Primera página ────────────────────────────────────────────────────
		abrirPagina();
		dibujarFondo();

		// Título
		float hTit = 28f;
		rectFill(ML, y - hTit, tableW, hTit, AZUL_OSC);
		setFill(BLANCO);
		String tit = "INFORME DE ARTICULOS PENDIENTES";
		float tw = anchoTexto(tit, BOLD, 15);
		texto(tit, BOLD, 15, ML + (tableW - tw) / 2f, y - hTit + 8f);
		y -= hTit;

		// Subtítulo / resumen
		float hSub = 16f;
		rectFill(ML, y - hSub, tableW, hSub, hex("EBF3FB"));
		borde(ML, y - hSub, tableW, hSub, GRIS_BORDE, 0.5f);
		setFill(AZUL_OSC);
		String sub = "Generado: " + fechaHoy + "   |   Refs. unicas: " + totalRefs + "   |   Obras afectadas: "
				+ totalObras + "   |   Uds. pendientes: " + totalPend + "   |   Uds. sin pedir: " + totalPorP;
		float sw = anchoTexto(sub, BOLD, 7);
		texto(sub, BOLD, 7, ML + (tableW - sw) / 2f, y - hSub + 5f);
		y -= hSub + 4f;

		// Cabecera de columnas
		dibujarCabeceraInforme(iColX, iColW, H_CAB);

		// Filas de datos
		for (int i = 0; i < filas.size(); i++) {
			if (y - H_FILA < RESERVA) {
				dibujarPieInforme();
				abrirPagina();
				dibujarFondo();
				dibujarCabeceraInforme(iColX, iColW, H_CAB);
			}
			dibujarFilaInforme(filas.get(i), i, H_FILA, iColX, iColW);
		}

		// Fila de totales
		if (y - H_TOT < MB + 18f) {
			dibujarPieInforme();
			abrirPagina();
			dibujarFondo();
		}
		dibujarTotalInforme(filas, H_TOT, iColX, iColW);
		dibujarPieInforme();

		cs.close();
		doc.save(destino);
		doc.close();
	}

	// ── Cabecera tabla informe ────────────────────────────────────────────────
	private void dibujarCabeceraInforme(float[] iColX, float[] iColW, float h) throws IOException {
		for (int i = 0; i < INC; i++) {
			// Columnas PENDIENTE(8) y POR PEDIR(10) en naranja para destacarlas
			boolean dest = (i == 8 || i == 10);
			rectFill(iColX[i], y - h, iColW[i], h, dest ? NARANJA : AZUL_MED);
			borde(iColX[i], y - h, iColW[i], h, hex("1F4E79"), 1f);
			setFill(dest ? NEGRO : BLANCO);
			textoCentrado(INF_TITLES[i], BOLD, 6.5f, iColX[i], y - h, iColW[i], h);
		}
		y -= h;
	}

	// ── Fila de datos informe ─────────────────────────────────────────────────
	private void dibujarFilaInforme(InformePendientes.FilaInforme f, int idx, float h, float[] iColX, float[] iColW)
			throws IOException {
		boolean alt = (idx % 2 != 0);

		String[] vals = { sanitizar(f.referencia), sanitizar(f.marca), recortar(sanitizar(f.descripcion), 38),
				sanitizar(f.obra), recortar(sanitizar(f.cliente), 16), sanitizar(f.entrega),
				String.valueOf(f.salidaUnidad), String.valueOf(f.preparado), String.valueOf(f.pendiente),
				String.valueOf(f.pedidoCompleto2), String.valueOf(f.porPedir) };

		for (int c = 0; c < INC; c++) {
			// Fondo
			float[] bg;
			if (c == 8 && f.pendiente > 0)
				bg = alt ? hex("F5B7B1") : ROJO_PAL;
			else if (c == 10 && f.porPedir > 0)
				bg = alt ? hex("F5B7B1") : ROJO_PAL;
			else if (c == 8 || c == 10)
				bg = alt ? hex("D5F5E3") : hex("EAFAF1");
			else
				bg = alt ? AZUL_PAL : BLANCO;

			rectFill(iColX[c], y - h, iColW[c], h, bg);
			borde(iColX[c], y - h, iColW[c], h, GRIS_BORDE, 0.4f);

			// Color texto
			if (c == 8 && f.pendiente > 0)
				setFill(ROJO);
			else if (c == 8)
				setFill(VERDE);
			else if (c == 10 && f.porPedir > 0)
				setFill(ROJO);
			else if (c == 10)
				setFill(VERDE);
			else
				setFill(NEGRO);

			PDFont font = (c == 0 || c == 2 || c == 3) ? BOLD : REGULAR;
			textoCentrado(vals[c], font, 6.5f, iColX[c], y - h, iColW[c], h);
		}
		y -= h;
	}

	// ── Fila total informe ────────────────────────────────────────────────────
	private void dibujarTotalInforme(List<InformePendientes.FilaInforme> filas, float h, float[] iColX, float[] iColW)
			throws IOException {
		int tPedido = filas.stream().mapToInt(f -> f.salidaUnidad).sum();
		int tPrep = filas.stream().mapToInt(f -> f.preparado).sum();
		int tPend = filas.stream().mapToInt(f -> f.pendiente).sum();
		int tYaPed = filas.stream().mapToInt(f -> f.pedidoCompleto2).sum();
		int tPorPedir = filas.stream().mapToInt(f -> f.porPedir).sum();

		// Span cols 0-5 con "TOTAL"
		float spanW = iColX[6] - ML;
		rectFill(ML, y - h, spanW, h, AZUL_OSC);
		borde(ML, y - h, spanW, h, hex("1F4E79"), 1f);
		setFill(BLANCO);
		textoCentrado("TOTAL", BOLD, 9f, ML, y - h, spanW, h);

		// Cols 6-10 con valores
		int[] totVals = { tPedido, tPrep, tPend, tYaPed, tPorPedir };
		for (int c = 6; c < INC; c++) {
			float[] bg = (c == 8 || c == 10) ? ROJO : AZUL_OSC;
			rectFill(iColX[c], y - h, iColW[c], h, bg);
			borde(iColX[c], y - h, iColW[c], h, hex("1F4E79"), 1f);
			setFill(BLANCO);
			textoCentrado(String.valueOf(totVals[c - 6]), BOLD, 9f, iColX[c], y - h, iColW[c], h);
		}
		y -= h;
	}

	// ── Pie de página informe ─────────────────────────────────────────────────
	private void dibujarPieInforme() throws IOException {
		setFill(GRIS_BORDE);
		cs.setLineWidth(0.5f);
		cs.moveTo(ML, MB + 10);
		cs.lineTo(PW - MR, MB + 10);
		cs.stroke();
		setFill(AZUL_OSC);
		String pie = "Generado: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())
				+ "   -   Tecnomat - Informe Articulos Pendientes";
		texto(pie, ITALIC, 7, ML, MB - 2);
		int numPag = doc.getNumberOfPages();
		String pag = "Pagina " + numPag;
		float tw = anchoTexto(pag, ITALIC, 7);
		texto(pag, ITALIC, 7, PW - MR - tw, MB - 2);
	}

	// ═════════════════════════════════════════════════════════════════════════
	// MÉTODOS DE OBRA (sin cambios)
	// ═════════════════════════════════════════════════════════════════════════

	private void abrirPagina() throws IOException {
		if (cs != null)
			cs.close();
		PDPage page = new PDPage(new PDRectangle(PW, PH));
		doc.addPage(page);
		cs = new PDPageContentStream(doc, page);
		y = PH - MT;
	}

	private void initFuentes() throws IOException {
		BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
		REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
		ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
	}

	private void initColumnas() {
		tableW = PW - ML - MR;
		int total = 0;
		for (int w : COL_W_CHARS)
			total += w;
		colW = new float[NC];
		colX = new float[NC];
		float x = ML;
		for (int i = 0; i < NC; i++) {
			colW[i] = tableW * COL_W_CHARS[i] / (float) total;
			colX[i] = x;
			x += colW[i];
		}
	}

	private void dibujarFondo() throws IOException {
		setFill(FONDO_PG);
		cs.addRect(0, 0, PW, PH);
		cs.fill();
	}

	private void dibujarTitulo() throws IOException {
		float h = 30f;
		rectFill(ML, y - h, tableW, h, AZUL_OSC);
		setFill(BLANCO);
		String t = "LISTADO MATERIALES CE";
		float tw = anchoTexto(t, BOLD, 16);
		texto(t, BOLD, 16, ML + (tableW - tw) / 2f, y - h + 9);
		y -= h;
	}

	private void dibujarMetadatos(Document obra) throws IOException {
		float h = 16f;
		float lw = 58f;
		float c1W = tableW * 0.55f;
		float c2X = ML + c1W;
		float c2W = tableW - c1W;

		metaFila(h, c1W, c2X, c2W, lw, "Ref. Obra:", str(obra, "obra"), "Responsable:", str(obra, "responsable"));
		y -= h;
		metaFila(h, c1W, c2X, c2W, lw, "Cliente:", str(obra, "cliente"), "Impresion:", str(obra, "impresion"));
		y -= h;
		metaFila(h, c1W, c2X, c2W, lw, "Proyecto:", str(obra, "proyecto"), "Entrega:", str(obra, "entrega"));
		y -= h;

		rectFill(ML, y - h, tableW, h, hex("EBF3FB"));
		borde(ML, y - h, tableW, h, GRIS_BORDE, 0.5f);
		setFill(AZUL_OSC);
		texto("Titulo:", BOLD, 8, ML + 4, y - h + 4);
		setFill(NEGRO);
		texto(sanitizar(str(obra, "titulo")), BOLD, 9, ML + 58f, y - h + 4);
		y -= h;
	}

	private void metaFila(float h, float c1W, float c2X, float c2W, float lw, String l1, String v1, String l2,
			String v2) throws IOException {
		rectFill(ML, y - h, c1W, h, AZUL_CABM);
		borde(ML, y - h, c1W, h, GRIS_BORDE, 0.5f);
		setFill(AZUL_OSC);
		texto(l1, BOLD, 8, ML + 4, y - h + 4);
		setFill(NEGRO);
		texto(sanitizar(v1), BOLD, 9, ML + lw, y - h + 4);

		rectFill(c2X, y - h, c2W, h, hex("EBF3FB"));
		borde(c2X, y - h, c2W, h, GRIS_BORDE, 0.5f);
		setFill(AZUL_OSC);
		texto(l2, BOLD, 8, c2X + 4, y - h + 4);
		setFill(NEGRO);
		texto(sanitizar(v2), BOLD, 9, c2X + lw, y - h + 4);
	}

	private void dibujarCabeceraTabla() throws IOException {
		float h = 24f;
		for (int i = 0; i < NC; i++) {
			boolean locked = (i == 7 || i == 8);
			rectFill(colX[i], y - h, colW[i], h, locked ? NARANJA : AZUL_MED);
			borde(colX[i], y - h, colW[i], h, hex("1F4E79"), 1f);
			setFill(locked ? NEGRO : BLANCO);
			String[] p = COL_TITLES[i].split("\n");
			if (p.length == 1) {
				textoCentrado(p[0], BOLD, 7, colX[i], y - h, colW[i], h);
			} else {
				textoCentrado(p[0], BOLD, 7, colX[i], y - h + h / 2f, colW[i], h / 2f);
				textoCentrado(p[1], BOLD, 7, colX[i], y - h, colW[i], h / 2f);
			}
		}
		y -= h;
	}

	private void dibujarFilaMaterial(Document m, int idx, float h) throws IOException {
		boolean alt = (idx % 2 != 0);
		int salida = m.getInteger("salidaUnidad", 0);
		int prep = prepInt(m.getOrDefault("preparado", "0"));
		int aPedir = Math.max(0, salida - prep);
		int cantPed = m.getInteger("pedidoCompleto2", 0);
		String pedCom = aPedir == 0 ? "SI" : "NO";
		String fecha = str(m, "fechaPedido").isEmpty() ? "—" : str(m, "fechaPedido");

		String[] vals = { String.valueOf(m.getInteger("A3", 0)), str(m, "marca"), str(m, "referencia"),
				recortar(str(m, "descripcion"), 42), String.valueOf(salida), String.valueOf(aPedir), pedCom,
				String.valueOf(cantPed), fecha };

		for (int c = 0; c < NC; c++) {
			float[] bg = alt ? AZUL_PAL : BLANCO;
			rectFill(colX[c], y - h, colW[c], h, bg);
			borde(colX[c], y - h, colW[c], h, GRIS_BORDE, 0.4f);

			if (c == 5)
				setFill(aPedir > 0 ? ROJO : VERDE);
			else if (c == 6)
				setFill(aPedir == 0 ? VERDE : ROJO);
			else if (c == 7)
				setFill(cantPed > 0 ? AZUL_VAL : GRIS_TEXT);
			else if (c == 8)
				setFill("—".equals(fecha) ? GRIS_TEXT : NEGRO);
			else
				setFill(NEGRO);

			PDFont f = (c == 0 || c == 2 || c == 3) ? BOLD : REGULAR;
			textoCentrado(sanitizar(vals[c]), f, 7, colX[c], y - h, colW[c], h);
		}
		y -= h;
	}

	private void dibujarFilaTotal(List<Document> mats) throws IOException {
		float h = 18f;
		int tSalida = 0, tAPedir = 0, tCantPed = 0;
		for (Document m : mats) {
			int salida = m.getInteger("salidaUnidad", 0);
			int prep = prepInt(m.getOrDefault("preparado", "0"));
			tSalida += salida;
			tAPedir += Math.max(0, salida - prep);
			tCantPed += m.getInteger("pedidoCompleto2", 0);
		}
		float spanW = colX[4] - ML;
		rectFill(ML, y - h, spanW, h, AZUL_OSC);
		borde(ML, y - h, spanW, h, hex("1F4E79"), 1f);
		setFill(BLANCO);
		textoCentrado("TOTAL", BOLD, 9, ML, y - h, spanW, h);
		for (int c = 4; c < NC; c++) {
			rectFill(colX[c], y - h, colW[c], h, AZUL_OSC);
			borde(colX[c], y - h, colW[c], h, hex("1F4E79"), 1f);
		}
		int[][] totsCols = { { 4, tSalida }, { 5, tAPedir }, { 7, tCantPed } };
		for (int[] tc : totsCols) {
			setFill(BLANCO);
			textoCentrado(String.valueOf(tc[1]), BOLD, 9, colX[tc[0]], y - h, colW[tc[0]], h);
		}
		y -= h;
	}

	private void dibujarPie(boolean esUltima) throws IOException {
		setFill(hex("B8CCE4"));
		cs.setLineWidth(0.5f);
		cs.moveTo(ML, MB + 10);
		cs.lineTo(PW - MR, MB + 10);
		cs.stroke();
		setFill(AZUL_OSC);
		String pie = "Generado: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())
				+ "   -   Tecnomat - Gestion de Materiales";
		texto(pie, ITALIC, 7, ML, MB - 2);
		int numPag = doc.getNumberOfPages();
		String pag = "Pagina " + numPag;
		float tw = anchoTexto(pag, ITALIC, 7);
		texto(pag, ITALIC, 7, PW - MR - tw, MB - 2);
	}

	// ═════════════════════════════════════════════════════════════════════════
	// PRIMITIVAS
	// ═════════════════════════════════════════════════════════════════════════

	private void rectFill(float x, float fy, float w, float h, float[] c) throws IOException {
		setFill(c);
		cs.addRect(x, fy, w, h);
		cs.fill();
	}

	private void borde(float x, float fy, float w, float h, float[] c, float lw) throws IOException {
		setStroke(c);
		cs.setLineWidth(lw);
		cs.addRect(x, fy, w, h);
		cs.stroke();
	}

	private void texto(String t, PDFont f, float sz, float x, float yp) throws IOException {
		String s = sanitizar(t);
		if (s.isEmpty())
			return;
		cs.beginText();
		cs.setFont(f, sz);
		cs.newLineAtOffset(x, yp);
		cs.showText(s);
		cs.endText();
	}

	private void textoCentrado(String t, PDFont f, float sz, float cx, float cy, float cw, float ch)
			throws IOException {
		String s = sanitizar(t);
		if (s.isEmpty())
			return;
		float tw = anchoTexto(s, f, sz);
		while (s.length() > 1 && tw > cw - 2) {
			s = s.substring(0, s.length() - 2) + ".";
			tw = anchoTexto(s, f, sz);
		}
		float x = cx + Math.max(0, (cw - tw) / 2f);
		float yp = cy + (ch - sz) / 2f + 1f;
		cs.beginText();
		cs.setFont(f, sz);
		cs.newLineAtOffset(x, yp);
		cs.showText(s);
		cs.endText();
	}

	private float anchoTexto(String t, PDFont f, float sz) throws IOException {
		String s = sanitizar(t);
		if (s.isEmpty())
			return 0f;
		try {
			return f.getStringWidth(s) / 1000f * sz;
		} catch (Exception e) {
			return s.length() * sz * 0.5f;
		}
	}

	private void setFill(float[] c) throws IOException {
		cs.setNonStrokingColor(c[0], c[1], c[2]);
	}

	private void setStroke(float[] c) throws IOException {
		cs.setStrokingColor(c[0], c[1], c[2]);
	}

	// ═════════════════════════════════════════════════════════════════════════
	// HELPERS ESTÁTICOS
	// ═════════════════════════════════════════════════════════════════════════

	private static String sanitizar(String s) {
		if (s == null)
			return "";
		return s.replace("✔", "SI").replace("✘", "NO").replace("✎", "*").replaceAll("[^\\x20-\\xFF]", "");
	}

	private static float[] hex(String h) {
		return new float[] { Integer.parseInt(h.substring(0, 2), 16) / 255f,
				Integer.parseInt(h.substring(2, 4), 16) / 255f, Integer.parseInt(h.substring(4, 6), 16) / 255f };
	}

	private static String str(Document d, String k) {
		Object v = d.get(k);
		return v != null ? v.toString().trim() : "";
	}

	private static int prepInt(Object v) {
		try {
			return (int) Double.parseDouble(v.toString().replace(",", "."));
		} catch (Exception e) {
			return 0;
		}
	}

	private static String recortar(String s, int max) {
		if (s == null || s.isEmpty())
			return "";
		return s.length() <= max ? s : s.substring(0, max - 1) + ".";
	}

	// ═════════════════════════════════════════════════════════════════════════
	// AÑADIR A FichaPDF.java — justo debajo de generarInformePendientes(...)
	// ═════════════════════════════════════════════════════════════════════════

	// ── Definición de columnas para el informe fuera de plazo ────────────────
	private static final int[] FDP_W = { 10, 13, 28, 10, 9, 7, 7, 7, 7, 7 };
	private static final String[] FDP_TITLES = { "OBRA", "CLIENTE", "DESCRIPCION", "F.ENTREGA", "RETRASO", "PEDIDO",
			"PREP.", "PENDIENTE", "YA PED.", "POR PEDIR" };
	private static final int FDPC = FDP_TITLES.length;

	// ── API pública ───────────────────────────────────────────────────────────
	public static void generarInformeFueraDePlazo(List<InformeFueraDePlazo.FilaInforme> filas, File destino)
			throws Exception {
		new FichaPDF().generarFDP(filas, destino);
	}

	// ── Generador interno ─────────────────────────────────────────────────────
	private void generarFDP(List<InformeFueraDePlazo.FilaInforme> filas, File destino) throws Exception {

		doc = new PDDocument();
		initFuentes();

		tableW = PW - ML - MR;
		int totalW = 0;
		for (int w : FDP_W)
			totalW += w;
		float[] fColW = new float[FDPC];
		float[] fColX = new float[FDPC];
		float cx = ML;
		for (int i = 0; i < FDPC; i++) {
			fColW[i] = tableW * FDP_W[i] / (float) totalW;
			fColX[i] = cx;
			cx += fColW[i];
		}

		final float H_CAB = 22f;
		final float H_FILA = 12f;
		final float H_TOT = 18f;
		final float RESERVA = MB + H_TOT + H_FILA + 18f;

		long totalObras = filas.stream().map(f -> f.obra).distinct().count();
		int totalPend = filas.stream().mapToInt(f -> f.pendiente).sum();

		String fechaHoy = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date());

		abrirPagina();
		dibujarFondo();

		// ── Título ────────────────────────────────────────────────────────────
		float hTit = 28f;
		rectFill(ML, y - hTit, tableW, hTit, ROJO);
		setFill(BLANCO);
		String tit = "INFORME DE ARTICULOS FUERA DE PLAZO";
		float tw = anchoTexto(tit, BOLD, 14);
		texto(tit, BOLD, 14, ML + (tableW - tw) / 2f, y - hTit + 8f);
		y -= hTit;

		// ── Subtítulo / resumen ───────────────────────────────────────────────
		float hSub = 16f;
		rectFill(ML, y - hSub, tableW, hSub, hex("FADBD8"));
		borde(ML, y - hSub, tableW, hSub, GRIS_BORDE, 0.5f);
		setFill(ROJO);
		String sub = "Generado: " + fechaHoy + "   |   Obras afectadas: " + totalObras + "   |   Uds. pendientes: "
				+ totalPend;
		float sw = anchoTexto(sub, BOLD, 7);
		texto(sub, BOLD, 7, ML + (tableW - sw) / 2f, y - hSub + 5f);
		y -= hSub + 4f;

		// ── Cabecera de columnas ──────────────────────────────────────────────
		fdpCabecera(fColX, fColW, H_CAB);

		// ── Filas de datos ────────────────────────────────────────────────────
		for (int i = 0; i < filas.size(); i++) {
			if (y - H_FILA < RESERVA) {
				fdpPie();
				abrirPagina();
				dibujarFondo();
				fdpCabecera(fColX, fColW, H_CAB);
			}
			fdpFila(filas.get(i), i, H_FILA, fColX, fColW);
		}

		// ── Totales ───────────────────────────────────────────────────────────
		if (y - H_TOT < MB + 18f) {
			fdpPie();
			abrirPagina();
			dibujarFondo();
		}
		fdpTotal(filas, H_TOT, fColX, fColW);
		fdpPie();

		cs.close();
		doc.save(destino);
		doc.close();
	}

	private void fdpCabecera(float[] cx, float[] cw, float h) throws java.io.IOException {
		// Columnas PENDIENTE(7) y POR PEDIR(9) en naranja; RETRASO(4) en rojo
		for (int i = 0; i < FDPC; i++) {
			float[] color = (i == 7 || i == 9) ? NARANJA : (i == 4) ? ROJO : AZUL_MED;
			rectFill(cx[i], y - h, cw[i], h, color);
			borde(cx[i], y - h, cw[i], h, hex("1F4E79"), 1f);
			setFill((i == 7 || i == 9 || i == 4) ? BLANCO : BLANCO);
			textoCentrado(FDP_TITLES[i], BOLD, 6f, cx[i], y - h, cw[i], h);
		}
		y -= h;
	}

	private void fdpFila(InformeFueraDePlazo.FilaInforme f, int idx, float h, float[] cx, float[] cw)
			throws java.io.IOException {
		boolean alt = (idx % 2 != 0);
		// Fondo base: más rojo cuanto mayor retraso
		float[] bgBase = f.diasRetraso > 30 ? hex("FADBD8") : f.diasRetraso > 7 ? hex("FDEBD0") : hex("FEF9E7");
		float[] bgAlt = f.diasRetraso > 30 ? hex("F5B7B1") : f.diasRetraso > 7 ? hex("FAD7A0") : hex("FCF3CF");

		String retrasoStr = f.diasRetraso + " d.";

		String[] vals = { sanitizar(f.obra), recortar(sanitizar(f.cliente), 14), recortar(sanitizar(f.descripcion), 36),
				sanitizar(f.entrega), retrasoStr, String.valueOf(f.salidaUnidad), String.valueOf(f.pendiente), };

		for (int c = 0; c < FDPC; c++) {
			float[] bg = alt ? bgAlt : bgBase;
			rectFill(cx[c], y - h, cw[c], h, bg);
			borde(cx[c], y - h, cw[c], h, GRIS_BORDE, 0.4f);

			if (c == 4)
				setFill(f.diasRetraso > 30 ? ROJO : NARANJA);
			else if (c == 7 && f.pendiente > 0)
				setFill(ROJO);
			else if (c == 7)
				setFill(VERDE);

			else if (c == 9)
				setFill(VERDE);

			else
				setFill(NEGRO);

			PDFont font = (c == 0 || c == 2) ? BOLD : REGULAR;
			textoCentrado(vals[c], font, 6f, cx[c], y - h, cw[c], h);
		}
		y -= h;
	}

	private void fdpTotal(List<InformeFueraDePlazo.FilaInforme> filas, float h, float[] cx, float[] cw)
			throws java.io.IOException {
		int tPed = filas.stream().mapToInt(f -> f.salidaUnidad).sum();

		int tPend = filas.stream().mapToInt(f -> f.pendiente).sum();

		// Span cols 0-4 con "TOTAL"
		float spanW = cx[5] - ML;
		rectFill(ML, y - h, spanW, h, AZUL_OSC);
		borde(ML, y - h, spanW, h, hex("1F4E79"), 1f);
		setFill(BLANCO);
		textoCentrado("TOTAL", BOLD, 9f, ML, y - h, spanW, h);

		int[] totals = { tPed, tPend };
		for (int c = 5; c < FDPC; c++) {
			float[] bg = (c == 7 || c == 9) ? ROJO : AZUL_OSC;
			rectFill(cx[c], y - h, cw[c], h, bg);
			borde(cx[c], y - h, cw[c], h, hex("1F4E79"), 1f);
			setFill(BLANCO);
			textoCentrado(String.valueOf(totals[c - 5]), BOLD, 9f, cx[c], y - h, cw[c], h);
		}
		y -= h;
	}

	private void fdpPie() throws java.io.IOException {
		setFill(GRIS_BORDE);
		cs.setLineWidth(0.5f);
		cs.moveTo(ML, MB + 10);
		cs.lineTo(PW - MR, MB + 10);
		cs.stroke();
		setFill(ROJO);
		String pie = "Generado: " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date())
				+ "   -   Tecnomat - Articulos Fuera de Plazo";
		texto(pie, ITALIC, 7, ML, MB - 2);
		int numPag = doc.getNumberOfPages();
		String pag = "Pagina " + numPag;
		float tw = anchoTexto(pag, ITALIC, 7);
		texto(pag, ITALIC, 7, PW - MR - tw, MB - 2);
	}
}