package Proyect;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

public class Controlador {

    private Vista vista;
    private MongoCollection<Document> coleccion;

    public Controlador(Vista vista, MongoCollection<Document> coleccion) {

        this.vista = vista;
        this.coleccion = coleccion;

        iniciarEventos();
    }

    private void iniciarEventos() {

        // BOTON IMPORTAR EXCEL
        vista.btnImportar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    Modelo.importarExcelAMongo(coleccion);
                    vista.mostrarMensaje("Excel importado correctamente");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        });

        // BOTON MOSTRAR OBRAS
        vista.btnMostrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                vista.cargarObras(coleccion);

            }
        });

        // BOTON BUSCAR CLIENTE
        vista.btnBuscar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Modelo.buscarPorCliente(coleccion);

            }
        });

        // BOTON AGREGAR FILA
        vista.btnAgregar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Modelo.agregarFila(coleccion);

            }
        });

        // BOTON ACTUALIZAR FILA
        vista.btnActualizar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Modelo.actualizarColumna(coleccion);

            }
        });

        // BOTON ELIMINAR FILA
        vista.btnEliminarFila.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Modelo.eliminarFila(coleccion);

            }
        });

        // BOTON ELIMINAR OBRA
        vista.btnEliminarObra.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Modelo.eliminarObra(coleccion);

            }
        });

    }

}