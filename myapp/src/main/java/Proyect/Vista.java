package Proyect;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import org.bson.Document;

import com.mongodb.client.*;

import java.awt.*;

import static com.mongodb.client.model.Filters.*;

public class Vista extends JFrame {

    public JButton btnImportar;
    public JButton btnMostrar;
    public JButton btnBuscar;
    public JButton btnAgregar;
    public JButton btnActualizar;
    public JButton btnEliminarFila;
    public JButton btnEliminarObra;
    public JTextField txtObra;
    public JTextField txtA3;
    public JTextField txtDescripcion;
    
    public JTable tabla;
    public DefaultTableModel modeloTabla;

    public Vista() {
        // Inicializamos los botones
        btnImportar = new JButton("Importar Excel");
        btnMostrar = new JButton("Mostrar Obras");
        btnBuscar = new JButton("Buscar Cliente");
        btnAgregar = new JButton("Agregar Fila");
        btnActualizar = new JButton("Actualizar Fila");
        btnEliminarFila = new JButton("Eliminar Fila");
        btnEliminarObra = new JButton("Eliminar Obra");
        
        // Inicializamos la tabla
        modeloTabla = new DefaultTableModel();
        tabla = new JTable(modeloTabla);

        // Añadimos columnas (ejemplo)
        modeloTabla.addColumn("Obra");
        modeloTabla.addColumn("Cliente");
        modeloTabla.addColumn("Proyecto");

        // Layout de botones
        JPanel panelBotones = new JPanel();
        panelBotones.add(btnImportar);
        panelBotones.add(btnMostrar);
        panelBotones.add(btnBuscar);
        panelBotones.add(btnAgregar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEliminarFila);
        panelBotones.add(btnEliminarObra);

        // Scroll de la tabla
        JScrollPane scroll = new JScrollPane(tabla);

        // Agregar todo al JFrame
        this.setLayout(new BorderLayout());
        this.add(panelBotones, BorderLayout.NORTH);
        this.add(scroll, BorderLayout.CENTER);

        // Configuración de la ventana
        this.setTitle("Gestión de Obras");
        this.setSize(900, 500);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        txtObra = new JTextField(10);
        txtA3 = new JTextField(10);
        txtDescripcion = new JTextField(10);

        JPanel panelCampos = new JPanel();

        panelCampos.add(new JLabel("Obra"));
        panelCampos.add(txtObra);

        panelCampos.add(new JLabel("A3"));
        panelCampos.add(txtA3);

        panelCampos.add(new JLabel("Descripcion"));
        panelCampos.add(txtDescripcion);

        add(panelCampos, BorderLayout.SOUTH);
        
    }

    // Métodos para mostrar mensajes o cargar obras
    public void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje);
    }

    public void cargarObras(MongoCollection<Document> coleccion){
        modeloTabla.setRowCount(0);
        MongoCursor<Document> cursor = coleccion.find().iterator();
        while(cursor.hasNext()){
            Document doc = cursor.next();
            modeloTabla.addRow(new Object[]{
                    doc.getString("obra"),
                    doc.getString("cliente"),
                    doc.getString("proyecto")
            });
        }
    }
}