package Proyect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Date;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import javafx.stage.FileChooser;

import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

public class Modelo {

	// ── FIX 1: TOTAL_COLS pasa de 12 a 13 para incluir N° PEDIDO ─────────────
	private static final int TOTAL_COLS = 13; // A..M

    private static final int[] COL_WIDTHS = {
        8,   // A  A3
        14,  // B  MARCA
        16,  // C  REFERENCIA
        36,  // D  DESCRIPCIÓN
        13,  // E  SALIDA UNIDAD
        13,  // F  SERVIR UNIDAD
        11,  // G  VALIDACIÓN
        11,  // H  PREPARADO
        8,   // I  FALTA
        15,  // J  PEDIDO COMPLETO
        16,  // K  N° PEDIDO          
        17,  // L  FECHA PEDIDO
        26,  // M  OBSERVACIONES
    };

    private static final String[] COL_TITLES = {
        "A3", "MARCA", "REFERENCIA", "DESCRIPCIÓN",
        "SALIDA\nUNIDAD", "SERVIR\nUNIDAD", "VALIDACIÓN",
        "PREPARADO", "FALTA", "PEDIDO\nCOMPLETO",
        "N° PEDIDO",        // ← nueva columna
        "FECHA PEDIDO", "OBSERVACIONES"
    };

    // Colores
    private static final byte[] AZUL_OSCURO  = rgb1("1F4E79");
    private static final byte[] AZUL_MEDIO   = rgb1("2E75B6");
    private static final byte[] AZUL_PALIDO  = rgb1("DEEAF1");
    private static final byte[] AZUL_CABMETA = rgb1("BDD7EE");
    private static final byte[] NARANJA      = rgb1("F4B942");
    private static final byte[] AMARILLO_PAL = rgb1("FFFDE7");
    private static final byte[] BLANCO       = rgb1("FFFFFF");
    private static final byte[] ROJO         = rgb1("C0392B");
    private static final byte[] GRIS_BORDE   = rgb1("B8CCE4");
    private static final byte[] NEGRO        = rgb1("1F1F1F");

    public static void exportar(Document obra, File destino) throws Exception {

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet ws = wb.createSheet("Materiales");
            ws.setDefaultRowHeightInPoints(16);
            ws.getPrintSetup().setLandscape(true);
            ws.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
            ws.setFitToPage(true);

            for (int i = 0; i < COL_WIDTHS.length; i++) {
                ws.setColumnWidth(i, COL_WIDTHS[i] * 256);
            }

            XSSFCellStyle sTitulo    = mkTitulo(wb);
            XSSFCellStyle sMetaLabel = mkMetaLabel(wb);
            XSSFCellStyle sMetaVal   = mkMetaVal(wb);
            XSSFCellStyle sMetaLabelR= mkMetaLabelRight(wb);
            XSSFCellStyle sColHead   = mkColHead(wb, false);
            XSSFCellStyle sColHeadLk = mkColHead(wb, true);
            XSSFCellStyle sDataN     = mkData(wb, false, false);
            XSSFCellStyle sDataA     = mkData(wb, true,  false);
            XSSFCellStyle sDataLkN   = mkData(wb, false, true);
            XSSFCellStyle sDataLkA   = mkData(wb, true,  true);
            XSSFCellStyle sCkN       = mkCheckmark(wb, false);
            XSSFCellStyle sCkA       = mkCheckmark(wb, true);
            XSSFCellStyle sTotal     = mkTotal(wb);
            XSSFCellStyle sTotalNum  = mkTotalNum(wb);

            int r = 0;

            ws.createRow(r++).setHeightInPoints(6);

            XSSFRow rowTit = ws.createRow(r++);
            rowTit.setHeightInPoints(34);
            cell(rowTit, 0, "LISTADO MATERIALES CE", sTitulo);
            merge(ws, r - 1, 0, TOTAL_COLS - 1);

            ws.createRow(r++).setHeightInPoints(8);

            // Ref. Obra | Responsable
            {
                XSSFRow row = ws.createRow(r++);
                row.setHeightInPoints(20);
                cell(row, 0, "Ref. Obra:",    sMetaLabel);
                cell(row, 1, s(obra, "obra"), sMetaVal);
                merge(ws, r - 1, 1, 6);
                cell(row, 7, "",                     sMetaVal);
                cell(row, 8, "Responsable:",         sMetaLabelR);
                cell(row, 9, s(obra, "responsable"), sMetaVal);
                merge(ws, r - 1, 9, TOTAL_COLS - 1);
            }
            // Cliente | Impresión
            {
                XSSFRow row = ws.createRow(r++);
                row.setHeightInPoints(20);
                cell(row, 0, "Cliente:",         sMetaLabel);
                cell(row, 1, s(obra, "cliente"), sMetaVal);
                merge(ws, r - 1, 1, 6);
                cell(row, 7, "",                   sMetaVal);
                cell(row, 8, "Impresión:",         sMetaLabelR);
                cell(row, 9, s(obra, "impresion"), sMetaVal);
                merge(ws, r - 1, 9, TOTAL_COLS - 1);
            }
            // Proyecto | Entrega
            {
                XSSFRow row = ws.createRow(r++);
                row.setHeightInPoints(20);
                cell(row, 0, "Proyecto:",         sMetaLabel);
                cell(row, 1, s(obra, "proyecto"), sMetaVal);
                merge(ws, r - 1, 1, 6);
                cell(row, 7, "",                 sMetaVal);
                cell(row, 8, "Entrega:",         sMetaLabelR);
                cell(row, 9, s(obra, "entrega"), sMetaVal);
                merge(ws, r - 1, 9, TOTAL_COLS - 1);
            }
            // Título (span completo)
            {
                XSSFRow row = ws.createRow(r++);
                row.setHeightInPoints(20);
                cell(row, 0, "Título:",         sMetaLabel);
                cell(row, 1, s(obra, "titulo"), sMetaVal);
                merge(ws, r - 1, 1, TOTAL_COLS - 1);
            }

            ws.createRow(r++).setHeightInPoints(6);

            XSSFRow rowHead = ws.createRow(r++);
            rowHead.setHeightInPoints(28);
            for (int c = 0; c < COL_TITLES.length; c++) {
                // Columnas bloqueadas (solo lectura visual): VALIDACIÓN(6), FALTA(8)
                boolean locked = (c == 6 || c == 8);
                cell(rowHead, c, COL_TITLES[c], locked ? sColHeadLk : sColHead);
            }

            List<Document> mats = obra.getList("materiales", Document.class);
            int firstDataRow = r + 1;

            if (mats != null) {
                for (int i = 0; i < mats.size(); i++) {
                    Document m   = mats.get(i);
                    boolean  alt = (i % 2 != 0);

                    XSSFRow row = ws.createRow(r++);
                    row.setHeightInPoints(16);

                    cellN(row, 0,  m.getInteger("A3", 0),                       alt ? sDataA   : sDataN);
                    cell (row, 1,  s(m, "marca"),                               alt ? sDataA   : sDataN);
                    cell (row, 2,  s(m, "referencia"),                          alt ? sDataA   : sDataN);
                    cell (row, 3,  s(m, "descripcion"),                         alt ? sDataA   : sDataN);
                    cellN(row, 4,  m.getInteger("salidaUnidad", 0),             alt ? sDataA   : sDataN);
                    cellN(row, 5,  m.getInteger("servirUnidad", 0),             alt ? sDataA   : sDataN);
                    cell (row, 6,  ck(s(m, "validacion")),                      alt ? sCkA     : sCkN);
                    cellN(row, 7,  prepInt(m.getOrDefault("preparado", "0")),   alt ? sDataA   : sDataN);
                    cellN(row, 8,  m.getInteger("falta", 0),                    alt ? sDataLkA : sDataLkN);
                    cell (row, 9,  ck(s(m, "pedidoCompleto")),                  alt ? sCkA     : sCkN);
                    // ── FIX 1: columna N° PEDIDO añadida al Excel ─────────────
                    cell (row, 10, s(m, "numeroPedido"),                        alt ? sDataA   : sDataN);
                    cell (row, 11, s(m, "fechaPedido"),                         alt ? sDataA   : sDataN);
                    cell (row, 12, s(m, "observaciones"),                       alt ? sDataA   : sDataN);
                }
            }

            int lastDataRow = r;

            XSSFRow rowTot = ws.createRow(r++);
            rowTot.setHeightInPoints(20);
            cell(rowTot, 0, "TOTAL", sTotal);
            merge(ws, r - 1, 0, 3);

            // Totales de columnas numéricas (índices actualizados tras insertar col 10)
            for (int c : new int[]{ 4, 5, 7, 8 }) {
                String col = String.valueOf((char)('A' + c));
                XSSFCell fc = rowTot.createCell(c);
                fc.setCellFormula("SUM(" + col + firstDataRow + ":" + col + lastDataRow + ")");
                fc.setCellStyle(sTotalNum);
            }
            for (int c = 0; c < TOTAL_COLS; c++) {
                if (rowTot.getCell(c) == null) cell(rowTot, c, "", sTotal);
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                wb.write(fos);
            }
        }
    }

    private static void cell(XSSFRow row, int col, String val, XSSFCellStyle st) {
        XSSFCell c = row.createCell(col);
        c.setCellValue(val != null ? val : "");
        c.setCellStyle(st);
    }

    private static void cellN(XSSFRow row, int col, int val, XSSFCellStyle st) {
        XSSFCell c = row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(st);
    }

    private static void merge(XSSFSheet ws, int row, int c1, int c2) {
        if (c2 > c1) ws.addMergedRegion(new CellRangeAddress(row, row, c1, c2));
    }

    private static String s(Document d, String key) {
        Object v = d.get(key);
        return v != null ? v.toString() : "";
    }

    private static String ck(String val) {
        return "✔".equals(val) ? "✔" : "✘";
    }

    private static int prepInt(Object val) {
        try { return (int) Double.parseDouble(val.toString().replace(",", ".")); }
        catch (Exception e) { return 0; }
    }

    private static XSSFCellStyle mkTitulo(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        fill(s, AZUL_OSCURO);
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 20);
        fontColor(f, BLANCO);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private static XSSFCellStyle mkMetaLabel(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        fill(s, AZUL_CABMETA);
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.THIN, GRIS_BORDE);
        return s;
    }

    private static XSSFCellStyle mkMetaLabelRight(XSSFWorkbook wb) {
        XSSFCellStyle s = mkMetaLabel(wb);
        s.setAlignment(HorizontalAlignment.LEFT);
        return s;
    }

    private static XSSFCellStyle mkMetaVal(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        fill(s, rgb1("EBF3FB"));
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 12);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.THIN, GRIS_BORDE);
        return s;
    }

    private static XSSFCellStyle mkColHead(XSSFWorkbook wb, boolean locked) {
        XSSFCellStyle s = wb.createCellStyle();
        fill(s, locked ? NARANJA : AZUL_MEDIO);
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 9);
        fontColor(f, locked ? NEGRO : BLANCO);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.MEDIUM, rgb1("1F4E79"));
        s.setWrapText(true);
        return s;
    }

    private static XSSFCellStyle mkData(XSSFWorkbook wb, boolean alt, boolean locked) {
        XSSFCellStyle s = wb.createCellStyle();
        if (locked) {
            fill(s, alt ? rgb1("FFF4CE") : AMARILLO_PAL);
            XSSFFont f = wb.createFont();
            f.setBold(true); f.setFontHeightInPoints((short) 10);
            fontColor(f, ROJO);
            s.setFont(f);
        } else {
            fill(s, alt ? AZUL_PALIDO : BLANCO);
            XSSFFont f = wb.createFont();
            f.setFontHeightInPoints((short) 10);
            s.setFont(f);
        }
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.THIN, GRIS_BORDE);
        return s;
    }

    private static XSSFCellStyle mkCheckmark(XSSFWorkbook wb, boolean alt) {
        XSSFCellStyle s = mkData(wb, alt, false);
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 12);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private static XSSFCellStyle mkTotal(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        fill(s, AZUL_OSCURO);
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11);
        fontColor(f, BLANCO);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.MEDIUM, rgb1("1F4E79"));
        return s;
    }

    private static XSSFCellStyle mkTotalNum(XSSFWorkbook wb) {
        XSSFCellStyle s = mkTotal(wb);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
        return s;
    }

    private static void fill(XSSFCellStyle s, byte[] color) {
        s.setFillForegroundColor(new XSSFColor(color, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    private static void fontColor(XSSFFont f, byte[] color) {
        f.setColor(new XSSFColor(color, null));
    }

    private static void border(XSSFCellStyle s, BorderStyle style, byte[] color) {
        XSSFColor c = new XSSFColor(color, null);
        s.setBorderTop(style);    s.setTopBorderColor(c);
        s.setBorderBottom(style); s.setBottomBorderColor(c);
        s.setBorderLeft(style);   s.setLeftBorderColor(c);
        s.setBorderRight(style);  s.setRightBorderColor(c);
    }

    private static byte[] rgb1(String hex) {
        return new byte[]{
            (byte) Integer.parseInt(hex.substring(0, 2), 16),
            (byte) Integer.parseInt(hex.substring(2, 4), 16),
            (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
    // ── FIX 2: método rgb(String) eliminado — era código muerto (solo retornaba null) ──

	// ─────────────────────────────────────────────────────────────────────────
	public static String obtenerValorCelda(Cell celda) {
		if (celda == null)
			return "";
		DataFormatter formatter = new DataFormatter();
		if (celda.getCellType() == CellType.FORMULA) {
			switch (celda.getCachedFormulaResultType()) {
			case NUMERIC:
				return String.valueOf(celda.getNumericCellValue());
			case STRING:
				return celda.getRichStringCellValue().getString();
			default:
				return formatter.formatCellValue(celda).trim();
			}
		}
		return formatter.formatCellValue(celda).trim();
	}

	private static String buscarValorSiguiente(Row row, int desdeCol) {
		for (int c = desdeCol + 1; c <= row.getLastCellNum(); c++) {
			String val = obtenerValorCelda(row.getCell(c));
			if (!val.isEmpty())
				return val;
		}
		return "";
	}

	private static String getCelda(Row row, Map<String, Integer> colMap, String campo) {
		Integer idx = colMap.get(campo);
		if (idx == null)
			return "";
		return obtenerValorCelda(row.getCell(idx));
	}

	public static File seleccionarArchivo() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Selecciona el archivo Excel (.xlsx)");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel (*.xlsx)", "*.xlsx"));
		return fileChooser.showOpenDialog(null);
	}

	public static void importarExcelAMongo(File archivo, MongoCollection<Document> coleccion) throws Exception {
		if (archivo == null)
			return;

		FileInputStream fis = new FileInputStream(archivo);
		Workbook workbook = new XSSFWorkbook(fis);
		Sheet sheet = workbook.getSheetAt(0);

		Document documentoPrincipal = new Document();
		List<Document> listaMateriales = new ArrayList<>();
		boolean empezarMateriales = false;
		Map<String, Integer> colMap = new HashMap<>();

		for (Row row : sheet) {
			if (!empezarMateriales) {

				for (int i = 0; i < row.getLastCellNum(); i++) {
					Cell celda = row.getCell(i);
					if (celda == null)
						continue;

					String valorOriginal = obtenerValorCelda(celda).trim();
					String val = valorOriginal.toUpperCase();
					if (val.isEmpty())
						continue;

					if (val.contains("LISTADO") && !documentoPrincipal.containsKey("titulo"))
						documentoPrincipal.put("titulo", valorOriginal);

					if ((val.contains("REF") || val.contains("OBRA")) && !documentoPrincipal.containsKey("obra")) {
						String v = buscarValorSiguiente(row, i);
						if (!v.isEmpty())
							documentoPrincipal.put("obra", v);
					}

					if (val.contains("CLIENTE") && !documentoPrincipal.containsKey("cliente")) {
						documentoPrincipal.put("cliente", buscarValorSiguiente(row, i));
						for (int j = i + 1; j < row.getLastCellNum(); j++) {
							if (obtenerValorCelda(row.getCell(j)).toUpperCase().contains("RESPONS")) {
								documentoPrincipal.put("responsable", buscarValorSiguiente(row, j));
								break;
							}
						}
					}

					if (val.contains("PROYECTO") && !documentoPrincipal.containsKey("proyecto")) {
						documentoPrincipal.put("proyecto", buscarValorSiguiente(row, i));
						for (int j = i + 1; j < row.getLastCellNum(); j++) {
							if (obtenerValorCelda(row.getCell(j)).toUpperCase().contains("IMPRES")) {
								documentoPrincipal.put("impresion", buscarValorSiguiente(row, j));
								break;
							}
						}
					}

					if (val.contains("ENTREGA") && !documentoPrincipal.containsKey("entrega"))
						documentoPrincipal.put("entrega", buscarValorSiguiente(row, i));

					if (val.equals("A3")) {
						for (int j = 0; j < row.getLastCellNum(); j++) {
							String enc = obtenerValorCelda(row.getCell(j)).toUpperCase().trim();
							if (enc.isEmpty()) continue;
							if (enc.equals("A3"))                            colMap.put("id",            j);
							if (enc.contains("MARCA"))                       colMap.put("marca",         j);
							if (enc.contains("REFERENCIA"))                  colMap.put("referencia",    j);
							if (enc.contains("DESCRIP"))                     colMap.put("descripcion",   j);
							if (enc.contains("SALIDA"))                      colMap.put("salidaUnidad",  j);
							if (enc.contains("SERVIR"))                      colMap.put("servirUnidad",  j);
							if (enc.contains("VALID"))                       colMap.put("validacion",    j);
							if (enc.contains("PREPAR"))                      colMap.put("preparado",     j);
							if (enc.contains("FALTA"))                       colMap.put("falta",         j);
							if (enc.contains("PEDIDO COMPLETO") || enc.equals("PEDIDO")) colMap.put("pedidoCompleto", j);
							// ── FIX 1: detección de la columna N° PEDIDO en importación ──
							if (enc.contains("N° PEDIDO") || enc.contains("NUM PEDIDO") || enc.contains("NUMERO PEDIDO")) colMap.put("numeroPedido", j);
							if (enc.contains("FECHA PEDIDO"))                colMap.put("fechaPedido",   j);
							if (enc.contains("OBSERV"))                      colMap.put("observaciones", j);
						}
						empezarMateriales = true;
						break;
					}
				}

			} else {
				int idxId = colMap.getOrDefault("id", 0);
				String valorId = obtenerValorCelda(row.getCell(idxId));
				if (valorId.isEmpty() || valorId.equalsIgnoreCase("A3"))
					continue;

				int idA3;
				try {
					idA3 = (int) Double.parseDouble(valorId);
				} catch (NumberFormatException e) {
					continue;
				}

				int salidaVal = parseEntero(getCelda(row, colMap, "salidaUnidad"));

				String validacionRaw = getCelda(row, colMap, "validacion");
				String validacionVal = validacionRaw.isEmpty() ? "✘"
						: (validacionRaw.equals("✔")
								|| validacionRaw.equalsIgnoreCase("SI")
								|| validacionRaw.equalsIgnoreCase("S")
								|| validacionRaw.equals("1") ? "✔" : "✘");

				String pedidoRaw = getCelda(row, colMap, "pedidoCompleto");
				String pedidoVal = pedidoRaw.isEmpty() ? "✘"
						: (pedidoRaw.equals("✔")
								|| pedidoRaw.equalsIgnoreCase("SI")
								|| pedidoRaw.equalsIgnoreCase("S")
								|| pedidoRaw.equals("1") ? "✔" : "✘");

				int preparadoVal = parseEntero(getCelda(row, colMap, "preparado"));

				String faltaRaw = getCelda(row, colMap, "falta");
				int faltaVal = faltaRaw.isEmpty()
						? Math.max(0, salidaVal - preparadoVal)
						: parseEntero(faltaRaw);

				Document material = new Document()
						.append("A3",             idA3)
						.append("marca",          getCelda(row, colMap, "marca"))
						.append("referencia",     getCelda(row, colMap, "referencia"))
						.append("descripcion",    getCelda(row, colMap, "descripcion"))
						.append("salidaUnidad",   salidaVal)
						.append("servirUnidad",   parseEntero(getCelda(row, colMap, "servirUnidad")))
						.append("validacion",     validacionVal)
						.append("preparado",      preparadoVal)
						.append("falta",          faltaVal)
						.append("pedidoCompleto", pedidoVal)
						// ── FIX 1: numeroPedido incluido en la importación (vacío si no existe la col) ──
						.append("numeroPedido",   getCelda(row, colMap, "numeroPedido"))
						.append("fechaPedido",    getCelda(row, colMap, "fechaPedido"))
						.append("observaciones",  getCelda(row, colMap, "observaciones"));

				listaMateriales.add(material);
			}
		}

		documentoPrincipal.append("materiales", listaMateriales);
		System.out.println("Documento que se insertará:");
		System.out.println(documentoPrincipal.toJson());
		coleccion.insertOne(documentoPrincipal);
		workbook.close();
		fis.close();
		System.out.println("Excel importado correctamente a MongoDB 🚀");
	}

	// ── MENÚ ─────────────────────────────────────────────────────────────────
	public static int menu() {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Seleccione una opción:");
		System.out.println("==================================");
		System.out.println("1. Ingresar datos desde el Excel");
		System.out.println("2. Mostrar títulos de obras");
		System.out.println("3. Mostrar datos completos");
		System.out.println("4. Actualizar datos de una obra");
		System.out.println("5. Eliminar fila de obra");
		System.out.println("6. Agregar fila a obra");
		System.out.println("7. Eliminar obra completa");
		System.out.println("8. Buscar obras por cliente");
		System.out.println("9. Buscar por referencia de material.");
		System.out.println("10. Ordenados por A3");
		System.out.println("11. Estadísticas de obra.");
		System.out.println("12. Mostrar Todas las obras resumidas.");
		System.out.println("13. Cantidad de obras.");
		System.out.println("14. Salir.");
		System.out.println("==================================");
		System.out.println("Ingrese el número de la opción deseada:");
		int opcion = teclado.nextInt();
		teclado.nextLine();
		return opcion;
	}

	public static void mostrarTituloss(MongoCollection<Document> coleccion) {
		MongoCursor<Document> cursor = coleccion.find().iterator();
		int contador = 0;
		if (!cursor.hasNext()) {
			System.out.println("No hay obras disponibles.");
			return;
		}
		System.out.println("================ TITULOS OBRAS =================");
		while (cursor.hasNext()) {
			contador++;
			JSONObject jsonO = new JSONObject(cursor.next().toJson());
			System.out.println(contador + " - Ref obra: " + jsonO.getString("obra"));
		}
		System.out.println("==============================================");
	}

	public static void mostrarDatos(MongoCollection<Document> coleccion) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingrese el número de obra que quieres mostrar:");
		String numeroObra = teclado.nextLine();

		MongoCursor<Document> cursor = coleccion.find(eq("obra", numeroObra)).iterator();
		if (!cursor.hasNext()) {
			System.out.println("❌ No se encontró ninguna obra con esa referencia.");
			return;
		}

		while (cursor.hasNext()) {
			JSONObject jsonO = new JSONObject(cursor.next().toJson());

			System.out.println("\n" + "=".repeat(180));
			System.out.println("  " + jsonO.optString("titulo", "SIN TÍTULO").toUpperCase());
			System.out.println("=".repeat(180));
			System.out.printf("%-20s %-30s | %-20s %-30s%n", "REF. OBRA:", jsonO.optString("obra", "N/A"),
					"RESPONSABLE:", jsonO.optString("responsable", "N/A"));
			System.out.printf("%-20s %-30s | %-20s %-30s%n", "CLIENTE:", jsonO.optString("cliente", "N/A"),
					"IMPRESIÓN:", jsonO.optString("impresion", "N/A"));
			System.out.printf("%-20s %-30s | %-20s %-30s%n", "PROYECTO:", jsonO.optString("proyecto", "N/A"),
					"ENTREGA:", jsonO.optString("entrega", "N/A"));
			System.out.println("-".repeat(180));

			String fmt = "| %-6s | %-12s | %-15s | %-30s | %-6s | %-6s | %-10s | %-10s | %-5s | %-15s | %-12s | %-15s | %-15s |%n";
			System.out.format(fmt, "A3", "MARCA", "REFERENCIA", "DESCRIPCIÓN", "SALIDA", "SERVIR", "VALIDACIÓN",
					"PREPARADO", "FALTA", "PEDIDO COMPLETO", "N° PEDIDO", "FECHA PEDIDO", "OBSERVACIONES");
			System.out.println("-".repeat(180));

			JSONArray materiales = jsonO.getJSONArray("materiales");
			for (int i = 0; i < materiales.length(); i++) {
				JSONObject m = materiales.getJSONObject(i);
				System.out.format(fmt, m.optInt("A3"), m.optString("marca", ""), m.optString("referencia", ""),
						cortarTexto(m.optString("descripcion", ""), 30), m.optInt("salidaUnidad"),
						m.optInt("servirUnidad"), m.optString("validacion", ""), m.optString("preparado", ""),
						m.optInt("falta"), m.optString("pedidoCompleto", ""),
						m.optString("numeroPedido", ""),   // ← FIX 1
						m.optString("fechaPedido", ""),
						m.optString("observaciones", ""));
			}
			System.out.println("-".repeat(130));
		}
	}

	private static String cortarTexto(String texto, int largo) {
		if (texto == null) return "";
		return texto.length() <= largo ? texto : texto.substring(0, largo - 3) + "...";
	}

	public static void actualizarColumna(MongoCollection<Document> coleccion) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingresa la referencia de obra que deseas actualizar:");
		String numeroObra = teclado.nextLine();

		Document obra = coleccion.find(eq("obra", numeroObra)).first();
		if (obra == null) { System.out.println("La obra no existe."); return; }

		System.out.println("Ingresa el número de A3 que deseas editar:");
		int numeroA3 = teclado.nextInt();
		teclado.nextLine();

		List<Document> materiales = (List<Document>) obra.get("materiales");
		boolean encontrado = materiales.stream().anyMatch(m -> m.getInteger("A3").equals(numeroA3));
		if (!encontrado) { System.out.println("El número A3 no existe en esa obra."); return; }

		System.out.println("¿Qué campo deseas actualizar?");
		System.out.println("1. Marca         2. Referencia    3. Descripción");
		System.out.println("4. Salida        5. Servir        6. Validación");
		System.out.println("7. Preparado     8. Falta         9. Pedido Completo");
		System.out.println("10. N° Pedido    11. Fecha Pedido 12. Observaciones  13. Volver");

		int opcion = teclado.nextInt();
		teclado.nextLine();

		// ── FIX 1: numeroPedido añadido al menú de consola (opción 10) ────────
		String[] campos = { "", "marca", "referencia", "descripcion", "salidaUnidad", "servirUnidad", "validacion",
				"preparado", "falta", "pedidoCompleto", "numeroPedido", "fechaPedido", "observaciones" };

		if (opcion < 1 || opcion > 12) { System.out.println("Volviendo..."); return; }

		System.out.println("Ingresa el nuevo valor:");
		String nuevoValor = teclado.nextLine();

		System.out.println("¿Estás seguro? (S/N)");
		if (!teclado.nextLine().equalsIgnoreCase("S")) { System.out.println("Operación cancelada."); return; }

		String fechaHoy = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
		Document setFields = new Document("materiales.$." + campos[opcion], nuevoValor)
				.append("materiales.$.fechaPedido", fechaHoy);
		coleccion.updateOne(and(eq("obra", numeroObra), eq("materiales.A3", numeroA3)),
				new Document("$set", setFields));

		if (opcion == 7) {
			Document obraActualizada = coleccion.find(eq("obra", numeroObra)).first();
			List<Document> matsActualizados = (List<Document>) obraActualizada.get("materiales");
			for (Document m : matsActualizados) {
				if (m.getInteger("A3").equals(numeroA3)) {
					int salida = m.getInteger("salidaUnidad", 0);
					int preparado = 0;
					Object prepObj = m.get("preparado");
					if (prepObj != null) {
						try { preparado = (int) Double.parseDouble(prepObj.toString()); }
						catch (NumberFormatException ignored) {}
					}
					int falta = Math.max(0, salida - preparado);
					coleccion.updateOne(and(eq("obra", numeroObra), eq("materiales.A3", numeroA3)),
							new Document("$set", new Document("materiales.$.falta", falta)));
					System.out.println("✅ Preparado actualizado → Falta calculado: " + falta);
					break;
				}
			}
		} else {
			System.out.println("✅ Campo actualizado correctamente.");
		}
		System.out.println("📅 Fecha de pedido registrada: " + fechaHoy);
	}

	public static void eliminarFila(MongoCollection<Document> coleccion) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingresa la referencia de obra:");
		String numeroObra = teclado.nextLine();
		Document obra = coleccion.find(eq("obra", numeroObra)).first();
		if (obra == null) { System.out.println("La obra no existe."); return; }
		System.out.println("Ingresa el número de A3 a eliminar:");
		int numeroA3 = teclado.nextInt();
		teclado.nextLine();
		List<Document> materiales = (List<Document>) obra.get("materiales");
		boolean encontrado = materiales.stream().anyMatch(m -> m.getInteger("A3").equals(numeroA3));
		if (!encontrado) { System.out.println("El número A3 no existe."); return; }
		System.out.println("¿Estás seguro? (S/N)");
		if (teclado.nextLine().equalsIgnoreCase("S")) {
			coleccion.updateOne(eq("obra", numeroObra),
					new Document("$pull", new Document("materiales", new Document("A3", numeroA3))));
			System.out.println("Fila eliminada correctamente.");
		} else {
			System.out.println("Operación cancelada.");
		}
	}

	public static void agregarFila(MongoCollection<Document> coleccion) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingresa la referencia de obra:");
		String numeroObra = teclado.nextLine();
		Document obra = coleccion.find(eq("obra", numeroObra)).first();
		if (obra == null) { System.out.println("No se encontró la obra."); return; }
		System.out.println("Ingresa el número de A3:");
		int numeroA3 = Integer.parseInt(teclado.nextLine());
		List<Document> materiales = obra.getList("materiales", Document.class);
		if (materiales != null && existeA3(materiales, numeroA3)) {
			System.out.println("ERROR: El número A3 ya existe en esta obra.");
			return;
		}
		Document nuevaFila = new Document().append("A3", numeroA3);
		System.out.print("Marca: ");           nuevaFila.append("marca",         teclado.nextLine());
		System.out.print("Referencia: ");      nuevaFila.append("referencia",    teclado.nextLine());
		System.out.print("Descripción: ");     nuevaFila.append("descripcion",   teclado.nextLine());
		System.out.print("Salida unidad: ");   nuevaFila.append("salidaUnidad",  parseEntero(teclado.nextLine()));
		System.out.print("Servir unidad: ");   nuevaFila.append("servirUnidad",  parseEntero(teclado.nextLine()));
		System.out.print("Validación: ");      nuevaFila.append("validacion",    teclado.nextLine());
		System.out.print("Preparado: ");       nuevaFila.append("preparado",     teclado.nextLine());
		System.out.print("Falta: ");           nuevaFila.append("falta",         parseEntero(teclado.nextLine()));
		System.out.print("Pedido completo: "); nuevaFila.append("pedidoCompleto",teclado.nextLine());
		// ── FIX 1: campo numeroPedido añadido a agregarFila en consola ────────
		System.out.print("N° Pedido: ");       nuevaFila.append("numeroPedido",  teclado.nextLine());
		System.out.print("Fecha pedido: ");    nuevaFila.append("fechaPedido",   teclado.nextLine());
		System.out.print("Observaciones: ");   nuevaFila.append("observaciones", teclado.nextLine());
		System.out.println("¿Estás seguro? (S/N)");
		if (teclado.nextLine().equalsIgnoreCase("S")) {
			coleccion.updateOne(eq("obra", numeroObra), new Document("$push", new Document("materiales", nuevaFila)));
			System.out.println("Fila agregada correctamente.");
		} else {
			System.out.println("Operación cancelada.");
		}
	}

	public static boolean existeA3(List<Document> materiales, int a3) {
		for (Document m : materiales)
			if (m.getInteger("A3") == a3) return true;
		return false;
	}

	public static boolean existeA3(List<Document> materiales, String a3) {
		return false;
	}

	public static void eliminarObra(MongoCollection<Document> coleccion) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingresa la referencia de la obra a eliminar:");
		String numeroObra = teclado.nextLine();
		Document obra = coleccion.find(eq("obra", numeroObra)).first();
		if (obra == null) { System.out.println("No se encontró la obra."); return; }
		System.out.println("¿Quieres eliminar la obra completa? Esta acción no se puede deshacer. (S/N)");
		if (teclado.nextLine().equalsIgnoreCase("S")) {
			coleccion.deleteOne(eq("obra", numeroObra));
			System.out.println("Obra eliminada correctamente.");
		} else {
			System.out.println("Operación cancelada.");
		}
	}

	public static void buscarPorCliente(MongoCollection<Document> coleccion) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingresa el nombre del cliente:");
		String cliente = teclado.nextLine();
		boolean encontrado = false;
		System.out.println("=".repeat(70));
		try (MongoCursor<Document> cursor = coleccion.find().iterator()) {
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				String c = doc.getString("cliente");
				if (c != null && c.toLowerCase().contains(cliente.toLowerCase())) {
					System.out.println("Obra: " + doc.getString("obra") + " | Cliente: " + c);
					encontrado = true;
				}
			}
		}
		if (!encontrado) System.out.println("No existe ese cliente.");
		System.out.println("=".repeat(70));
	}

	public static void buscarPorReferencia(MongoCollection<Document> coleccion) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingresa la referencia del material: ");
		String ref = teclado.nextLine();
		boolean encontrado = false;
		System.out.println("=".repeat(52));
		centrarTexto("MATERIALES POR REFERENCIA");
		System.out.println("=".repeat(52));
		try (MongoCursor<Document> cursor = coleccion.find().iterator()) {
			while (cursor.hasNext()) {
				JSONObject jsonO = new JSONObject(cursor.next().toJson());
				String obra = jsonO.getString("obra");
				JSONArray materiales = jsonO.getJSONArray("materiales");
				for (int i = 0; i < materiales.length(); i++) {
					JSONObject m = materiales.getJSONObject(i);
					if (m.optString("referencia").equalsIgnoreCase(ref)) {
						System.out.println("Obra: " + obra);
						System.out.println("Marca: " + m.optString("marca"));
						System.out.println("Descripción: " + m.optString("descripcion"));
						System.out.println("=".repeat(52));
						encontrado = true;
					}
				}
			}
		}
		if (!encontrado) centrarTexto("NO HAY MATERIALES CON ESA REFERENCIA.");
	}

	public static void ordenarMaterialesPorA3(MongoCollection<Document> coleccion) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingresa la referencia de obra:");
		String obraRef = teclado.nextLine();
		Document obraDoc = coleccion.find(eq("obra", obraRef)).first();
		if (obraDoc == null) { centrarTexto("La obra no existe."); return; }
		JSONArray materiales = new JSONObject(obraDoc.toJson()).getJSONArray("materiales");
		List<JSONObject> lista = new ArrayList<>();
		for (int i = 0; i < materiales.length(); i++) lista.add(materiales.getJSONObject(i));
		lista.sort((m1, m2) -> Integer.compare(m1.getInt("A3"), m2.getInt("A3")));
		String fmt = "| %-6s | %-12s | %-15s | %-30s | %-6s | %-6s |%n";
		System.out.println("-".repeat(90));
		System.out.format(fmt, "A3", "MARCA", "REFERENCIA", "DESCRIPCIÓN", "SALIDA", "SERVIR");
		System.out.println("-".repeat(90));
		for (JSONObject m : lista) {
			System.out.format(fmt, m.optInt("A3"), m.optString("marca"), m.optString("referencia"),
					cortarTexto(m.optString("descripcion"), 30), m.optInt("salidaUnidad"), m.optInt("servirUnidad"));
		}
		System.out.println("-".repeat(90));
	}

	public static void estadisticasObra(MongoCollection<Document> coleccion) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingresa la referencia de obra: ");
		String obraRef = teclado.nextLine();
		Document obra = coleccion.find(eq("obra", obraRef)).first();
		if (obra == null) { centrarTexto("La obra no existe."); return; }
		List<Document> materiales = (List<Document>) obra.get("materiales");
		int totalMateriales = materiales.size();
		int totalSalida = 0, totalServir = 0, totalFalta = 0;
		for (Document m : materiales) {
			totalSalida += m.getInteger("salidaUnidad", 0);
			totalServir += m.getInteger("servirUnidad", 0);
			totalFalta  += m.getInteger("falta", 0);
		}
		System.out.println("===========================================");
		System.out.println("Total materiales : " + totalMateriales);
		System.out.println("Total salida     : " + totalSalida);
		System.out.println("Total servir     : " + totalServir);
		System.out.println("Total falta      : " + totalFalta);
		System.out.println("===========================================");
	}

	public static void mostrarObraResumida(MongoCollection<Document> coleccion) {
		System.out.println("=".repeat(55));
		centrarTexto("LISTA DE OBRAS");
		System.out.println("=".repeat(55));
		String fmt = "| %-12s | %-15s | %-18s |%n";
		System.out.format(fmt, "OBRA", "CLIENTE", "PROYECTO");
		System.out.println("=".repeat(55));
		if (coleccion.countDocuments() == 0) {
			centrarTexto("NO HAY OBRAS REGISTRADAS");
			System.out.println("=".repeat(55));
			return;
		}
		try (MongoCursor<Document> cursor = coleccion.find().iterator()) {
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				System.out.format(fmt, doc.getOrDefault("obra", "N/A"),
						doc.getOrDefault("cliente", "N/A"), doc.getOrDefault("proyecto", "N/A"));
			}
		}
		System.out.println("=".repeat(55));
	}

	public static void centrarTexto(String mensaje) {
		int ancho = 52;
		int espacios = (ancho - mensaje.length()) / 2;
		System.out.printf("%" + espacios + "s%s%n", "", mensaje);
	}

	private static int parseEntero(String valor) {
		try {
			if (valor == null || valor.trim().isEmpty()) return 0;
			return (int) Double.parseDouble(valor.replace(",", "."));
		} catch (NumberFormatException e) { return 0; }
	}

	public static void exportarObraAExcel(Document obra, File archivo) throws Exception {
		exportar(obra, archivo);
	}
	
	// --- En Modelo.java ---

	public static int contarTodosLosMateriales(com.mongodb.client.MongoCollection<org.bson.Document> coleccion) {
	    int total = 0;
	    for (org.bson.Document doc : coleccion.find()) {
	        java.util.List<org.bson.Document> mats = doc.getList("materiales", org.bson.Document.class);
	        if (mats != null) total += mats.size();
	    }
	    return total;
	}

	public static int contarAlertasFalta(com.mongodb.client.MongoCollection<org.bson.Document> coleccion) {
	    int alertas = 0;
	    for (org.bson.Document doc : coleccion.find()) {
	        java.util.List<org.bson.Document> mats = doc.getList("materiales", org.bson.Document.class);
	        if (mats != null) {
	            for (org.bson.Document m : mats) {
	                if (m.getInteger("falta", 0) > 0) alertas++;
	            }
	        }
	    }
	    return alertas;
	}
	public static void main(String[] args) throws Exception {
		System.out.println("Conectando a MongoDB...");
		MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
		MongoDatabase database = mongoClient.getDatabase("Listados");
		MongoCollection<Document> coleccion = database.getCollection("refObras");
		System.out.println("Conexión exitosa a MongoDB");

		int opcion = 0;
		do {
			opcion = menu();
			switch (opcion) {
			case 1:  File archivoSelected = seleccionarArchivo(); importarExcelAMongo(archivoSelected, coleccion); break;
			case 2:  mostrarTituloss(coleccion);       break;
			case 3:  mostrarDatos(coleccion);           break;
			case 4:  actualizarColumna(coleccion);      break;
			case 5:  eliminarFila(coleccion);           break;
			case 6:  agregarFila(coleccion);            break;
			case 7:  eliminarObra(coleccion);           break;
			case 8:  buscarPorCliente(coleccion);       break;
			case 9:  buscarPorReferencia(coleccion);    break;
			case 10: ordenarMaterialesPorA3(coleccion); break;
			case 11: estadisticasObra(coleccion);       break;
			case 12: mostrarObraResumida(coleccion);    break;
			case 14: System.out.println("Saliendo..."); break;
			default: System.out.println("Opción no válida."); break;
			}
		} while (opcion != 14);
	}
}