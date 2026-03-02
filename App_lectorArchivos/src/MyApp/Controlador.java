package MyApp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import java.io.File;

public class Controlador {
    private Modelo m;
    private Vista v;

    public Controlador(Modelo m, Vista v) {
        this.m = m;
        this.v = v;
        inicializarEventos();
    }

    private void inicializarEventos() {
        
        // Evento botón buscar archivo
    	// Acción para el botón de buscar archivo
    	v.getBtnExaminar().addActionListener(new ActionListener() {
    	    @Override
    	    public void actionPerformed(ActionEvent e) {
    	        // 1. Crear el buscador de archivos
    	        JFileChooser fileChooser = new JFileChooser();
    	        
    	        // 2. Abrir la ventana y capturar la respuesta
    	        int seleccion = fileChooser.showOpenDialog(v);
    	        
    	        // 3. Si el usuario selecciona un archivo y pulsa "Abrir"
    	        if (seleccion == JFileChooser.APPROVE_OPTION) {
    	            File archivo = fileChooser.getSelectedFile();
    	            
    	            // Ponemos la ruta en el campo de texto de la vista
    	            v.getTextBuscar().setText(archivo.getAbsolutePath());
    	            
    	            // AQUÍ LLAMARÍAS A TU MODELO PARA LEER EL CSV (opcional)
    	            cargarDatosEnTabla(archivo.getAbsolutePath());
    	            
    	            
    	        }
    	    }
    	});
    	    
    	v.getBoto().addActionListener(new  ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				v.getModeloTabla().setRowCount(0);
				
				v.getTextBuscar().setText("");
				JOptionPane.showMessageDialog(v, "Tabla limpiza correctamente");
			}
		});
    }
    private void cargarDatosEnTabla(String ruta) {
        try {
            // Pedimos al modelo que lea el archivo
            java.util.List<String[]> lista = m.leerCSV(ruta);
            
            if (!lista.isEmpty()) {
                String[] cabeceras = lista.get(0);
                String[][] datos = new String[lista.size() - 1][cabeceras.length];
                
                for (int i = 1; i < lista.size(); i++) {
                    datos[i - 1] = lista.get(i);
                }
                
                // Enviamos los datos procesados a la Vista
                v.cargarDatosTabla(cabeceras, datos);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(v, "Error al cargar el CSV: " + ex.getMessage());
        }
    }
}