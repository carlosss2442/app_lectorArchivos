package MyApp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Vista extends JFrame {
    // Variables con nombres claros para evitar conflictos
    private JTextField campoRuta;
    private JButton botonExaminar;
    private JButton botonLimpiar; 
    private JTable tablaDatos;
    private DefaultTableModel modeloTabla;

    // Paleta de colores moderna
    private final Color AZUL_OSCURO = new Color(44, 62, 80);
    private final Color AZUL_ACCION = new Color(52, 152, 219);
    private final Color GRIS_FONDO = new Color(236, 240, 241);

    public Vista() {
        setTitle("Tecnomat App - Data Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        // Contenedor principal
        JPanel panelPrincipal = new JPanel(new BorderLayout(20, 20));
        panelPrincipal.setBorder(new EmptyBorder(30, 30, 30, 30));
        panelPrincipal.setBackground(GRIS_FONDO);
        setContentPane(panelPrincipal);

        // --- BARRA SUPERIOR (Buscador) ---
        JPanel panelTop = new JPanel(new BorderLayout(15, 0));
        panelTop.setOpaque(false);

        campoRuta = new JTextField("Seleccione un archivo CSV...");
        campoRuta.setEditable(false);
        campoRuta.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        campoRuta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        botonExaminar = crearBotonPersonalizado("EXAMINAR", Color.GRAY);

        panelTop.add(new JLabel("ARCHIVO:"), BorderLayout.WEST);
        panelTop.add(campoRuta, BorderLayout.CENTER);
        panelTop.add(botonExaminar, BorderLayout.EAST);
        panelPrincipal.add(panelTop, BorderLayout.NORTH);

        // --- CUERPO CENTRAL (Tabla) ---
        modeloTabla = new DefaultTableModel();
        tablaDatos = new JTable(modeloTabla);
        
        // Estilo de tabla "Clean"
        tablaDatos.setRowHeight(35);
        tablaDatos.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tablaDatos.setSelectionBackground(new Color(212, 230, 241));
        tablaDatos.setShowVerticalLines(false); // Estilo moderno
        
        // Estilo del encabezado
        tablaDatos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        tablaDatos.getTableHeader().setBackground(AZUL_OSCURO);
        tablaDatos.getTableHeader().setForeground(Color.BLACK);

        JScrollPane scrollTable = new JScrollPane(tablaDatos);
        scrollTable.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));
        panelPrincipal.add(scrollTable, BorderLayout.CENTER);

        // --- BARRA INFERIOR (Acciones) ---
        botonLimpiar = crearBotonPersonalizado("BORRAR TABLA", Color.GRAY);
        
        JPanel panelBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBottom.setOpaque(false);
        panelBottom.add(botonLimpiar);
        panelPrincipal.add(panelBottom, BorderLayout.SOUTH);

        setVisible(true);
    }

    // Método para crear botones con efecto interactivo
    private JButton crearBotonPersonalizado(String texto, Color colorFondo) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.BLACK);
        btn.setBackground(colorFondo);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        // Efecto Hover (Cambio de brillo)
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(colorFondo.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(colorFondo); }
        });
        return btn;
    }

    // Métodos para el Controlador (Getters)
    public JButton getBoto() { return botonLimpiar; } 
    public JButton getBtnExaminar() { return botonExaminar; }
    public JTextField getTextBuscar() { return campoRuta; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }

    public void cargarDatosTabla(String[] columnas, String[][] datos) {
        modeloTabla.setDataVector(datos, columnas);
        // Centrar texto de las celdas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tablaDatos.getColumnCount(); i++) {
            tablaDatos.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }
}