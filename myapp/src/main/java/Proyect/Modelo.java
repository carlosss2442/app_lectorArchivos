package Proyect;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonObject;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.*;



public class Modelo {
	
	
	public static String obtenerValorCelda(Cell celda) {

	    if (celda == null) return "";

	    switch (celda.getCellType()) {
	        case STRING:
	            return celda.getStringCellValue().trim();

	        case NUMERIC:
	            if (DateUtil.isCellDateFormatted(celda)) {
	                return celda.getDateCellValue().toString();
	            }
	            return String.valueOf((long) celda.getNumericCellValue());

	        case BOOLEAN:
	            return String.valueOf(celda.getBooleanCellValue());

	        case FORMULA:
	            return celda.getCellFormula();

	        default:
	            return "";
	    }
	}
	
	public static void importarExcelAMongo(MongoCollection<Document> coleccion) throws Exception {

	    // Seleccionar archivo Excel
	    JFileChooser fileChooser = new JFileChooser();
	    fileChooser.setDialogTitle("Selecciona el archivo Excel (.xlsx)");
	    FileNameExtensionFilter filtro = new FileNameExtensionFilter("Archivos Excel (*.xlsx)", "xlsx");
	    fileChooser.setFileFilter(filtro);

	    int resultado = fileChooser.showOpenDialog(null);

	    if (resultado != JFileChooser.APPROVE_OPTION) {
	        System.out.println("No se seleccionó archivo.");
	        return;
	    }

	    File archivo = fileChooser.getSelectedFile();
	    FileInputStream fis = new FileInputStream(archivo);
	    Workbook workbook = new XSSFWorkbook(fis);
	    Sheet sheet = workbook.getSheetAt(0);

	    Document documentoPrincipal = new Document();
	    List<Document> listaMateriales = new ArrayList<>();

	    boolean empezarMateriales = false;

	    for (Row row : sheet) {

	        String primeraCelda = obtenerValorCelda(row.getCell(0));

	        // CABECERA PRINCIPAL
	        if (primeraCelda.contains("LISTADO")) {
	            documentoPrincipal.append("titulo", obtenerValorCelda(row.getCell(3)));
	        }

	        if (primeraCelda.contains("Ref: Obra:")) {
	            documentoPrincipal.append("obra", obtenerValorCelda(row.getCell(3)));
	        }

	        if (primeraCelda.contains("Cliente:")) {
	            documentoPrincipal.append("cliente", obtenerValorCelda(row.getCell(3)));
	            documentoPrincipal.append("responsable", obtenerValorCelda(row.getCell(8)));
	        }

	        if (primeraCelda.contains("Proyecto:")) {
	            documentoPrincipal.append("proyecto", obtenerValorCelda(row.getCell(3)));
	            documentoPrincipal.append("impresion", obtenerValorCelda(row.getCell(8)));
	        }

	        if (primeraCelda.contains("Entrega:")) {
	            documentoPrincipal.append("entrega", obtenerValorCelda(row.getCell(8)));
	        }

	        // Detectar inicio tabla materiales
	        if (primeraCelda.equalsIgnoreCase("A3")) {
	            empezarMateriales = true;
	            continue;
	        }

	        // MATERIALES
	        if (empezarMateriales && !primeraCelda.isEmpty()) {

	            Document material = new Document()
	                    .append("A3", obtenerValorCelda(row.getCell(0)))
	                    .append("marca", obtenerValorCelda(row.getCell(1)))
	                    .append("referencia", obtenerValorCelda(row.getCell(2)))
	                    .append("descripcion", obtenerValorCelda(row.getCell(3)))
	                    .append("salidaUnidad", obtenerValorCelda(row.getCell(4)))
	                    .append("entradaUnidad", obtenerValorCelda(row.getCell(5)))
	                    .append("pedido", obtenerValorCelda(row.getCell(6)))
	                    .append("totalPedido", obtenerValorCelda(row.getCell(7)));

	            listaMateriales.add(material);
	        }
	    }

	    documentoPrincipal.append("materiales", listaMateriales);

	    coleccion.insertOne(documentoPrincipal);

	    workbook.close();
	    fis.close();

	    System.out.println("Excel importado correctamente a MongoDB 🚀");
	}

	public static int menu() {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Seleccione una opción:");
		System.out.println("==================================");
		System.out.println("1. Ingresar datos desde el CSV");
		System.out.println("2. Mostrar títulos de obras");
		System.out.println("3. Mostrar datos completos");
		System.out.println("4. Salir");
		System.out.println("==================================");
		System.out.println("Ingrese el número de la opción deseada:");
		int opcion = teclado.nextInt();
		teclado.nextLine(); // Limpiar el buffer después de leer un número
		return opcion;
	}

	public static void mostrarTituloss(MongoCollection<Document> coleccion) {
		MongoCursor<Document> cursor = coleccion.find().iterator();
		int contador = 0;

		System.out.println("================ TITULOS OBRAS =================");

		while (cursor.hasNext()) {
			contador++;
			JSONObject jsonO = new JSONObject(cursor.next().toJson());
			String obra = jsonO.getString("obra");
			System.out.println(contador + " - Ref: obra: " + obra);
		}
		System.out.println("==============================================");
	}

	public static void mostrarDatos(MongoCollection<Document> coleccion) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingre el numero de obra que quieres mostrar:");
		String numeroObra = teclado.nextLine();

		Bson filtro = eq("obra", numeroObra);

		MongoCursor<Document> cursor = coleccion.find(filtro).iterator();

		while (cursor.hasNext()) {
			JSONObject jsonO = new JSONObject(cursor.next().toJson());
			String titulo = jsonO.getString("titulo");
			String obra = jsonO.getString("obra");
			String cliente = jsonO.getString("cliente");
			String responsable = jsonO.getString("responsable");
			String proyecto = jsonO.getString("proyecto");
			String impresion = jsonO.getString("impresion");
			String entrega = jsonO.getString("entrega");

			System.out.println("================ " + titulo + " ================");
			System.out.println("========= Ref: Obra: " + obra);
			System.out.println("========= Cliente: " + cliente + "  ========= Responsable: " + responsable);
			System.out.println("========= Proyecto: " + proyecto + " ========= Impresión: " + impresion);
			System.out.println("                                  ========= Entrega: " + entrega);
			System.out.println("==============================================");

			// datos materiales
			JSONArray materiales = jsonO.getJSONArray("materiales");
			System.out.println(
					"[ A3: || Marca: || Referencia: ||             Descripción:            || Total Pedido: ]");
			System.out.println(
					"---------------------------------------------------------------------------------------------");

			for (int i = 0; i < materiales.length(); i++) {
			    JSONObject material = materiales.getJSONObject(i);
			    String a3 = material.getString("A3");
			    String marca = material.getString("marca");
			    String referencia = material.getString("referencia");
			    String descripcion = material.getString("descripcion");
			    String salidaUnidad = material.getString("salidaUnidad");
			    String entradaUnidad = material.getString("entradaUnidad");
			    String totalPedido = material.getString("totalPedido");
			    String pedido = material.getString("pedido");
				// Imprimir datos del material

				System.out.println("[ " + a3 + " || " + marca + " || " + referencia + " || " + descripcion + " || " + salidaUnidad +  " || "  + entradaUnidad + " || " + totalPedido + " || " + pedido + " ]");
				System.out.println("---------------------------------------------------------------------------------------------");
			}

		}

	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("Conectando a MongoDB...");
		MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017/DAM_MongoDB");

		MongoDatabase database = mongoClient.getDatabase("Listados");
		MongoCollection<Document> coleccion = database.getCollection("refObras");

		System.out.println("Conexión exitosa a MongoDB");

		int opcion = 0;

		do {

			opcion = menu();

			switch (opcion) {
			case 1:
				importarExcelAMongo(coleccion);
				break;
			case 2:
				mostrarTituloss(coleccion);
				break;
			case 3:
				mostrarDatos(coleccion);
				break;
			case 4:
				System.out.println("Saliendo...");
				break;
			default:
				System.out.println("Opción no válida, intente nuevamente.");
			}
		} while (opcion != 4);

		// ingresarDatos(coleccion);
		// mostrarTituloss(coleccion);
		// mostrarDatos(coleccion);

	}

}
