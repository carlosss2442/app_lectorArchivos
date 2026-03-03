package MyApp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
		// TODO Auto-generated method stub
		
	}

    
}

  
