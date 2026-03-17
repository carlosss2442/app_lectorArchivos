package Proyect;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EtiquetaQR {

    // ── Dimensiones de la etiqueta (píxeles a 150 dpi) ───────────────────────
    private static final int ETIQUETA_W    = 600;
    private static final int ETIQUETA_H    = 380;
    private static final int QR_SIZE       = 180;
    private static final int PADDING       = 20;
    private static final int RADIO         = 18;   // esquinas redondeadas

    // ── Colores corporativos ──────────────────────────────────────────────────
    private static final Color AZUL_OSCURO  = new Color(0x1F, 0x4E, 0x79);
    private static final Color AZUL_CLARO   = new Color(0xDE, 0xEA, 0xF1);
    private static final Color ROJO_ALERTA  = new Color(0xC0, 0x39, 0x2B);
    private static final Color VERDE_OK     = new Color(0x27, 0xAE, 0x60);
    private static final Color BLANCO       = Color.WHITE;
    private static final Color GRIS_TEXTO   = new Color(0x2C, 0x3E, 0x50);

    /**
     * Genera una etiqueta PNG para un material concreto.
     *
     * @param obraRef   Referencia de la obra (ej. "2024-001")
     * @param cliente   Nombre del cliente
     * @param material  Documento MongoDB del material
     * @param destino   Archivo PNG de salida
     */
    public static void generarEtiquetaPNG(String obraRef, String cliente,
                                          Document material, File destino) throws Exception {

        BufferedImage img = new BufferedImage(ETIQUETA_W, ETIQUETA_H,
                                              BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // ── Fondo blanco con borde redondeado ─────────────────────────────────
        g.setColor(BLANCO);
        g.fillRoundRect(0, 0, ETIQUETA_W, ETIQUETA_H, RADIO, RADIO);

        // ── Banda superior azul ───────────────────────────────────────────────
        g.setColor(AZUL_OSCURO);
        g.fillRoundRect(0, 0, ETIQUETA_W, 70, RADIO, RADIO);
        g.fillRect(0, 50, ETIQUETA_W, 20); // cuadrar esquinas inferiores

        // Título en banda
        g.setColor(BLANCO);
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString("GESTIÓN DE MATERIALES · TECNOMAT", PADDING + 5, 28);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("OBRA: " + obraRef + "  |  " + cliente.toUpperCase(), PADDING + 5, 56);

        // ── QR a la derecha ───────────────────────────────────────────────────
        String qrContenido = buildQRContent(obraRef, material);
        BufferedImage qrImg = generarQR(qrContenido, QR_SIZE);
        int qrX = ETIQUETA_W - QR_SIZE - PADDING;
        int qrY = 80;
        g.drawImage(qrImg, qrX, qrY, null);

        // Mini-texto bajo el QR
        g.setColor(GRIS_TEXTO);
        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g.drawString("Escanear para detalles", qrX + 10, qrY + QR_SIZE + 14);

        // ── Datos del material (columna izquierda) ────────────────────────────
        int textX  = PADDING;
        int startY = 95;
        int lineH  = 28;

        // A3 grande y destacado
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.setColor(AZUL_OSCURO);
        g.drawString("A3: " + material.getInteger("A3", 0), textX, startY + 5);

        // Línea separadora
        g.setColor(AZUL_CLARO);
        g.fillRect(textX, startY + 12, qrX - PADDING - 10, 3);

        int y = startY + lineH + 8;

        // Referencia y Marca
        dibujarCampo(g, "REF:", str(material, "referencia"), textX, y,
                     qrX - PADDING - 10, true);   y += lineH;
        dibujarCampo(g, "MARCA:", str(material, "marca"), textX, y,
                     qrX - PADDING - 10, false);  y += lineH;

        // Descripción (puede ser larga → se recorta)
        String desc = str(material, "descripcion");
        if (desc.length() > 42) desc = desc.substring(0, 40) + "…";
        dibujarCampo(g, "DESC:", desc, textX, y, qrX - PADDING - 10, false); y += lineH;

        // Salida / Falta con colores de estado
        int falta = material.getInteger("falta", 0);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(GRIS_TEXTO);
        g.drawString("SALIDA: " + material.getInteger("salidaUnidad", 0), textX, y);
        g.setColor(falta > 0 ? ROJO_ALERTA : VERDE_OK);
        g.drawString("  FALTA: " + falta,
                     textX + g.getFontMetrics().stringWidth("SALIDA: XXX  "), y);
        y += lineH;

        // Pedido completo
        boolean pedCompleto = "✔".equals(material.getString("pedidoCompleto"));
        g.setColor(pedCompleto ? VERDE_OK : ROJO_ALERTA);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString(pedCompleto ? "✔ PEDIDO COMPLETO" : "✘ PEDIDO PENDIENTE", textX, y);

        // ── Pie de etiqueta ───────────────────────────────────────────────────
        g.setColor(AZUL_CLARO);
        g.fillRect(0, ETIQUETA_H - 30, ETIQUETA_W, 30);
        g.setColor(AZUL_OSCURO);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String pie = "N° Pedido: " + strODash(material, "numeroPedido")
                   + "   Fecha: " + strODash(material, "fechaPedido")
                   + "   Obs: " + recortar(str(material, "observaciones"), 35);
        g.drawString(pie, PADDING, ETIQUETA_H - 10);

        // ── Borde general ─────────────────────────────────────────────────────
        g.setColor(AZUL_OSCURO);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(1, 1, ETIQUETA_W - 2, ETIQUETA_H - 2, RADIO, RADIO);

        g.dispose();
        ImageIO.write(img, "PNG", destino);
    }

    /**
     * Genera un PDF de múltiples etiquetas (3 por fila) usando Apache POI
     * de respaldo — o, más sencillo, un PNG multi-etiqueta en una sola imagen.
     *
     * Para simplicidad (sin iText) generamos un PNG grande con todas las
     * etiquetas de la obra en una cuadrícula y el usuario lo imprime.
     */
    public static File generarHojaEtiquetas(String obraRef, String cliente,
                                            List<Document> materiales,
                                            File destino) throws Exception {
        final int COLS       = 2;
        final int GAP        = 15;
        final int MARGIN     = 30;
        final int TITULO_H   = 55;

        int rows = (int) Math.ceil((double) materiales.size() / COLS);
        int totalW = MARGIN * 2 + COLS * ETIQUETA_W + (COLS - 1) * GAP;
        int totalH = MARGIN * 2 + TITULO_H + rows * ETIQUETA_H + (rows - 1) * GAP;

        BufferedImage hoja = new BufferedImage(totalW, totalH,
                                               BufferedImage.TYPE_INT_RGB);
        Graphics2D gh = hoja.createGraphics();
        gh.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        // Fondo de página
        gh.setColor(new Color(0xF0, 0xF2, 0xF5));
        gh.fillRect(0, 0, totalW, totalH);

        // Encabezado de hoja
        gh.setColor(AZUL_OSCURO);
        gh.fillRect(0, 0, totalW, TITULO_H);
        gh.setColor(BLANCO);
        gh.setFont(new Font("SansSerif", Font.BOLD, 22));
        gh.drawString("HOJA DE ETIQUETAS · OBRA: " + obraRef + "  |  " + cliente,
                      MARGIN, 35);

        // Generar y componer cada etiqueta
        Path tmpDir = Files.createTempDirectory("etiquetas_");
        for (int i = 0; i < materiales.size(); i++) {
            File tmp = tmpDir.resolve("etiqueta_" + i + ".png").toFile();
            generarEtiquetaPNG(obraRef, cliente, materiales.get(i), tmp);
            BufferedImage etiq = ImageIO.read(tmp);

            int col = i % COLS;
            int row = i / COLS;
            int x = MARGIN + col * (ETIQUETA_W + GAP);
            int y = MARGIN + TITULO_H + row * (ETIQUETA_H + GAP);
            gh.drawImage(etiq, x, y, null);
            tmp.delete();
        }
        tmpDir.toFile().delete();

        gh.dispose();
        ImageIO.write(hoja, "PNG", destino);
        return destino;
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    /** Construye el texto que irá codificado en el QR */
    private static String buildQRContent(String obraRef, Document m) {
        return "OBRA=" + obraRef
             + "|A3=" + m.getInteger("A3", 0)
             + "|REF=" + str(m, "referencia")
             + "|MARCA=" + str(m, "marca")
             + "|DESC=" + recortar(str(m, "descripcion"), 60)
             + "|SALIDA=" + m.getInteger("salidaUnidad", 0)
             + "|FALTA=" + m.getInteger("falta", 0)
             + "|PEDIDO=" + str(m, "pedidoCompleto");
    }

    private static BufferedImage generarQR(String contenido, int size)
            throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(contenido, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private static void dibujarCampo(Graphics2D g, String label, String valor,
                                     int x, int y, int maxW, boolean negrita) {
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(AZUL_OSCURO);
        g.drawString(label, x, y);
        int labelW = g.getFontMetrics().stringWidth(label + " ");
        g.setFont(new Font("SansSerif", negrita ? Font.BOLD : Font.PLAIN, 12));
        g.setColor(GRIS_TEXTO);
        // Recortar si no cabe
        FontMetrics fm = g.getFontMetrics();
        while (valor.length() > 3 &&
               fm.stringWidth(valor) > maxW - labelW - 5) {
            valor = valor.substring(0, valor.length() - 4) + "…";
        }
        g.drawString(valor, x + labelW, y);
    }

    private static String str(Document d, String key) {
        Object v = d.get(key);
        return v != null ? v.toString().trim() : "";
    }

    private static String strODash(Document d, String key) {
        String v = str(d, key);
        return v.isEmpty() ? "—" : v;
    }

    private static String recortar(String s, int max) {
        if (s == null || s.isEmpty()) return "—";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}