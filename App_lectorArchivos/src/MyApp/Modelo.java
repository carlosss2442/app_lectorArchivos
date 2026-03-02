package MyApp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Modelo {
	
String msg = "Adeu";
	
	Modelo() {
	}
	
	public List<String[]> leerCSV(String ruta) throws IOException {
		List<String[]> datos = new ArrayList<>();
		BufferedReader br = new BufferedReader(new FileReader(ruta ));
		String linea; 
		
		while ((linea = br.readLine()) != null) {
			datos.add(linea.split(","));
		}
		return datos;
	}
}
