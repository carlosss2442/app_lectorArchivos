package Proyect;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.*;

public class Modelo {

	public static String obtenerValorCelda(Cell celda) {
		if (celda == null)
			return "";

		DataFormatter formatter = new DataFormatter();

		// Si la celda tiene una fórmula, intentamos obtener el valor del resultado
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

	public static void importarExcelAMongo(MongoCollection<Document> coleccion) throws Exception {

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
		boolean cabeceraLeida = false;

		for (Row row : sheet) {
			if (!empezarMateriales) {
				// --- BLOQUE DE CABECERA ---
				for (int i = 0; i < row.getLastCellNum(); i++) {
					Cell celda = row.getCell(i);
					if (celda == null)
						continue;

					String valorOriginal = obtenerValorCelda(celda).trim();
					String valorBusqueda = valorOriginal.toUpperCase();

					if (valorBusqueda.contains("LISTADO") && !documentoPrincipal.containsKey("titulo")) {
						documentoPrincipal.put("titulo", valorOriginal);
					}
					if (valorBusqueda.contains("OBRA") && !documentoPrincipal.containsKey("obra")) {
						documentoPrincipal.put("obra", obtenerValorCelda(row.getCell(i + 1)));
					}
					if (valorBusqueda.contains("CLIENTE") && !documentoPrincipal.containsKey("cliente")) {
						documentoPrincipal.put("cliente", obtenerValorCelda(row.getCell(i + 1)));
						documentoPrincipal.put("responsable", obtenerValorCelda(row.getCell(8)));
					}
					if (valorBusqueda.contains("PROYECTO") && !documentoPrincipal.containsKey("proyecto")) {
						documentoPrincipal.put("proyecto", obtenerValorCelda(row.getCell(i + 1)));
						documentoPrincipal.put("impresion", obtenerValorCelda(row.getCell(8)));
					}
					if (valorBusqueda.contains("ENTREGA") && !documentoPrincipal.containsKey("entrega")) {
						documentoPrincipal.put("entrega", obtenerValorCelda(row.getCell(8)));
					}

					// Si encontramos la cabecera de la tabla, activamos el modo materiales
					if (valorBusqueda.equals("A3")) {
						empezarMateriales = true;
						break; // Salimos del for de celdas, la siguiente FILA será material
					}
				}
			} else {
				// --- BLOQUE DE MATERIALES ---
				// --- BLOQUE DE MATERIALES ---
				String a3 = obtenerValorCelda(row.getCell(0));

				if (!a3.isEmpty() && !a3.equalsIgnoreCase("A3")) {
					// DEBUG: Imprime para ver qué detecta en las últimas celdas
					// System.out.println("Fila material detectada. Celda 6: " +
					// obtenerValorCelda(row.getCell(6)) + " | Celda 7: " +
					// obtenerValorCelda(row.getCell(7)));

					Document material = new Document().append("A3", Integer.parseInt(a3))
							.append("marca", obtenerValorCelda(row.getCell(1)))
							.append("referencia", obtenerValorCelda(row.getCell(2)))
							.append("descripcion", obtenerValorCelda(row.getCell(3)))
							.append("salidaUnidad", parseEntero(obtenerValorCelda(row.getCell(4))))
							.append("entradaUnidad", parseEntero(obtenerValorCelda(row.getCell(6))))
							.append("totalPedido", parseEntero(obtenerValorCelda(row.getCell(8))))
							.append("pedido", obtenerValorCelda(row.getCell(9)));
					listaMateriales.add(material);
				}
			}
		}

		documentoPrincipal.append("materiales", listaMateriales);

		// 🔎 VER JSON ANTES DE INSERTAR
		System.out.println("Documento que se insertará:");
		System.out.println(documentoPrincipal.toJson());

		coleccion.insertOne(documentoPrincipal);

		workbook.close();
		fis.close();

		System.out.println("Excel importado correctamente a MongoDB 🚀");
	}

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
		System.out.println("9. Salir");
		System.out.println("==================================");
		System.out.println("Ingrese el número de la opción deseada:");
		int opcion = teclado.nextInt();
		teclado.nextLine(); // Limpiar el buffer después de leer un número
		return opcion;
	}

	public static void mostrarTituloss(MongoCollection<Document> coleccion) {
		MongoCursor<Document> cursor = coleccion.find().iterator();
		int contador = 0;

		if (!cursor.hasNext()) {
			System.out.println("==============================================");
			System.out.println("No hay obras disponibles.");
			System.out.println("==============================================");
			return;
		}
		System.out.println("================ TITULOS OBRAS =================");
		while (cursor.hasNext()) {
			contador++;
			JSONObject jsonO = new JSONObject(cursor.next().toJson());
			String obra = jsonO.getString("obra");

			System.out.println(contador + " - Ref obra: " + obra);
		}
		System.out.println("==============================================");
	}

	public static void mostrarDatos(MongoCollection<Document> coleccion) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingrese el número de obra que quieres mostrar:");
		String numeroObra = teclado.nextLine();

		Bson filtro = eq("obra", numeroObra);
		MongoCursor<Document> cursor = coleccion.find(filtro).iterator();

		if (!cursor.hasNext()) {
			System.out.println("❌ No se encontró ninguna obra con esa referencia.");
			return;
		}

		while (cursor.hasNext()) {
			JSONObject jsonO = new JSONObject(cursor.next().toJson());

			// --- CABECERA DE LA OBRA ---
			System.out.println(
					"\n==========================================================================================================================================");
			System.out.println("   							" + jsonO.optString("titulo", "SIN TÍTULO").toUpperCase());
			System.out.println(
					"==========================================================================================================================================");

			// Usamos un formato de dos columnas para la información general
			String fmtCabecera = "%-20s %-40s | %-20s %-40s %n";

			System.out.format(fmtCabecera, "REF. OBRA:", jsonO.optString("obra", "N/A"), "RESPONSABLE:",
					jsonO.optString("responsable", "N/A"));
			System.out.format(fmtCabecera, "CLIENTE:", jsonO.optString("cliente", "N/A"), "IMPRESIÓN:",
					jsonO.optString("impresion", "N/A"));
			System.out.format(fmtCabecera, "PROYECTO:", jsonO.optString("proyecto", "N/A"), "ENTREGA:",
					jsonO.optString("entrega", "N/A"));

			System.out.println(
					"------------------------------------------------------------------------------------------------------------------------------------------");

			// --- TABLA DE MATERIALES ---
			// Definición de anchos: A3(6), Marca(12), Ref(18), Desc(35), Salida(10),
			// Entrada(10), Total(10), Pedido(10)
			String formatoTabla = "| %-6s | %-12s | %-18s | %-35s | %-10s | %-10s | %-10s | %-10s |%n";

			// Imprimir Encabezado de la Tabla
			System.out.format(formatoTabla, "A3", "MARCA", "REFERENCIA", "DESCRIPCIÓN", "SALIDA", "ENTRADA", "TOTAL",
					"PEDIDO");
			System.out.println(
					"------------------------------------------------------------------------------------------------------------------------------------------");

			JSONArray materiales = jsonO.getJSONArray("materiales");

			for (int i = 0; i < materiales.length(); i++) {
				JSONObject m = materiales.getJSONObject(i);

				System.out.format(formatoTabla, m.optString("A3", ""), m.optString("marca", ""),
						m.optString("referencia", ""), cortarTexto(m.optString("descripcion", ""), 35),
						m.optString("salidaUnidad", ""), m.optString("entradaUnidad", ""),
						m.optString("totalPedido", ""), m.optString("pedido", ""));
			}
			System.out.println(
					"------------------------------------------------------------------------------------------------------------------------------------------");
		}
	}

	// Método auxiliar para evitar que descripciones largas rompan las columnas
	private static String cortarTexto(String texto, int largo) {
		if (texto == null)
			return "";
		if (texto.length() <= largo)
			return texto;
		return texto.substring(0, largo - 3) + "...";
	}

	public static void actualizarColumna(MongoCollection<Document> coleccion) {

		Scanner teclado = new Scanner(System.in);

		System.out.println("Ingresa la referencia de obra que deseas actualizar:");
		String numeroObra = teclado.nextLine();

		// 1️⃣ Verificar si existe la obra
		Document obra = coleccion.find(eq("obra", numeroObra)).first();

		if (obra == null) {
			System.out.println("La obra no existe.");
			return;
		}

		System.out.println("Ingresa el número de A3 que deseas editar:");
		int numeroA3 = teclado.nextInt();

		// 2️⃣ Verificar si existe ese A3 dentro de la obra
		List<Document> materiales = (List<Document>) obra.get("materiales");

		boolean a3Encontrado = false;

		for (Document material : materiales) {
			if (material.getInteger("A3").equals(numeroA3)) {
				a3Encontrado = true;
				break;
			}
		}

		if (!a3Encontrado) {
			System.out.println("El número A3 no existe en esa obra.");
			return;
		}

		System.out.println(" MINI MENU DE ACTUALIZACIÓN ");
		System.out.println("==================================");
		System.out.println("¿Qué campo deseas actualizar?");
		System.out.println("1. Marca");
		System.out.println("2. Referencia");
		System.out.println("3. Descripción");
		System.out.println("4. Salida");
		System.out.println("5. Entrada");
		System.out.println("6. Total Pedido");
		System.out.println("7. Pedido");
		System.out.println("8. Volver al menú principal");
		System.out.println("==================================");

		int opcion = teclado.nextInt();
		teclado.nextLine();

		String campoActualizar = "";

		switch (opcion) {
		case 1:
			campoActualizar = "materiales.$.marca";
			break;
		case 2:
			campoActualizar = "materiales.$.referencia";
			break;
		case 3:
			campoActualizar = "materiales.$.descripcion";
			break;
		case 4:
			campoActualizar = "materiales.$.salidaUnidad";
			break;
		case 5:
			campoActualizar = "materiales.$.entradaUnidad";
			break;
		case 6:
			campoActualizar = "materiales.$.totalPedido";
			break;
		case 7:
			campoActualizar = "materiales.$.pedido";
			break;
		case 8:
			System.out.println("Volviendo...");
			return;
		default:
			System.out.println("Opción no válida");
			return;
		}

		System.out.println("Ingresa el nuevo valor:");
		String nuevoValor = teclado.nextLine();

		System.out.println("¿Estas seguro que quieres atualizar la fila (S/N)? ");
		String confirmacion = teclado.nextLine();

		if (confirmacion.equalsIgnoreCase("S")) {
			coleccion.updateOne(and(eq("obra", numeroObra), eq("materiales.A3", numeroA3)),
					new Document("$set", new Document(campoActualizar, nuevoValor)));

			System.out.println("Fila actualizada correctamente.");

		} else {
			System.out.println("Operacion cancelada. No se actualizo la fila.");
		}

	}

	public static void eliminarFila(MongoCollection<Document> coleccion) {

		Scanner teclado = new Scanner(System.in);

		System.out.println("Ingresa la referencia de obra:");
		String numeroObra = teclado.nextLine();

		// 1️⃣ Verificar si existe la obra
		Document obra = coleccion.find(eq("obra", numeroObra)).first();

		if (obra == null) {
			System.out.println("La obra no existe.");
			return;
		}

		System.out.println("Ingresa el número de A3 de la fila que deseas eliminar:");
		int numeroA3 = teclado.nextInt();
		teclado.nextLine();

		// 2️⃣ Verificar si existe ese A3 dentro de la obra
		List<Document> materiales = (List<Document>) obra.get("materiales");

		boolean a3Encontrado = false;

		for (Document material : materiales) {
			if (material.getInteger("A3").equals(numeroA3)) {
				a3Encontrado = true;
				break;
			}
		}

		if (!a3Encontrado) {
			System.out.println("El número A3 no existe en esa obra.");
			return;
		}

		System.out.println("¿Estas seguro que quiere eliminar la fila (S/N)?");
		String cofirmacion = teclado.nextLine();

		if (cofirmacion.equalsIgnoreCase("S")) {
			// 3️⃣ Eliminar
			coleccion.updateOne(eq("obra", numeroObra),
					new Document("$pull", new Document("materiales", new Document("A3", numeroA3))));

			System.out.println("Fila eliminada correctamente.");
		} else {
			System.out.println("Operacion Cancelada. No se ha eliminado la fila.");
		}

	}

	public static void agregarFila(MongoCollection<Document> coleccion) {

		Scanner teclado = new Scanner(System.in);

		System.out.println("Ingresa la referencia de obra:");
		String numeroObra = teclado.nextLine();

		// Buscamos directamente la obra
		Document obra = coleccion.find(eq("obra", numeroObra)).first();

		if (obra == null) {
			System.out.println("No se encontró la obra. Operación cancelada.");
			return;
		}

		System.out.println("Obra encontrada. Procediendo a agregar fila...");

		Document nuevaFila = new Document();

		System.out.println("Ingresa el número de A3:");
		nuevaFila.append("A3", Integer.parseInt(teclado.nextLine()));

		System.out.println("Ingresa la marca:");
		nuevaFila.append("marca", teclado.nextLine());

		System.out.println("Ingresa la referencia:");
		nuevaFila.append("referencia", teclado.nextLine());

		System.out.println("Ingresa la descripción:");
		nuevaFila.append("descripcion", teclado.nextLine());

		System.out.println("Ingresa la salida unidad:");
		nuevaFila.append("salidaUnidad", Integer.parseInt(teclado.nextLine()));

		System.out.println("Ingresa la entrada unidad:");
		nuevaFila.append("entradaUnidad", Integer.parseInt(teclado.nextLine()));

		System.out.println("Ingresa el total pedido:");
		nuevaFila.append("totalPedido", Integer.parseInt(teclado.nextLine()));

		System.out.println("Ingresa el pedido:");
		nuevaFila.append("pedido", teclado.nextLine());

		System.out.println("¿Estás seguro? (S/N)");
		String confirmacion = teclado.nextLine();

		if (confirmacion.equalsIgnoreCase("S")) {

			UpdateResult resultado = coleccion.updateOne(eq("obra", numeroObra),
					new Document("$push", new Document("materiales", nuevaFila)));

			System.out.println("Fila agregada correctamente.");

		} else {
			System.out.println("Operación cancelada.");
		}
	}

	public static void eliminarObra(MongoCollection<Document> coleccion) {

		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingresa la referencia de la obra a eliminar:");
		String numeroObra = teclado.nextLine();

		boolean obraExiste = false;
		MongoCursor<Document> cursor = coleccion.find(eq("obra", numeroObra)).iterator();

		while (cursor.hasNext()) {
			JSONObject jsonoOB = new JSONObject(cursor.next().toJson());
			if (jsonoOB.getString("obra").equals(numeroObra)) {
				obraExiste = true;
				break;
			}
		}

		if (obraExiste) {

			System.out.println("¿Quieres eliminar la obra completa? Esta acción no se puede deshacer. (S/N)");
			String confirmacion = teclado.nextLine();

			if (confirmacion.equalsIgnoreCase("S")) {

				System.out.println("Obra encontrada. Procediendo a eliminar...");
				coleccion.deleteOne(eq("obra", numeroObra));

				System.out.println("Obra eliminada correctamente.");

			} else {
				System.out.println("Operación cancelada. No se eliminó la obra.");

			}

		} else {
			System.out.println("No se encontró la obra. Operación cancelada.");
			return;
		}

	}

	public static void buscarPorCliente(MongoCollection<Document> coleccion) {

		Scanner teclado = new Scanner(System.in);
		System.out.println("Ingresa el nombre del cliente:");
		String cliente = teclado.nextLine();

		MongoCursor<Document> cursor = coleccion.find().iterator();
		System.out.println("===================== LISTADO NUMEROS REFERENCIAS ====================");
		System.out.println("========================================================================");
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			
			if (doc.getString("cliente").startsWith(cliente)) {
				System.out.println("Obra: " + doc.getString("obra"));
				System.out.println("========================================================================");	
			} else {
				System.out.println("No existe ese cliente.");
			}
			
		}
	}

	private static int parseEntero(String valor) {
		try {
			if (valor == null || valor.trim().isEmpty()) {
				return 0;
			}
			return (int) Double.parseDouble(valor.replace(",", "."));
		} catch (NumberFormatException e) {
			return 0;
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
				actualizarColumna(coleccion);
				break;
			case 5:
				eliminarFila(coleccion);
				break;
			case 6:
				agregarFila(coleccion);
				break;
			case 7:
				eliminarObra(coleccion);
				break;
			case 8:
				buscarPorCliente(coleccion);
				break;
			case 9:
				System.out.println("Saliendo...");
				break;
			default:
				System.out.println("Opción no válida, intente nuevamente.");
			}
		} while (opcion != 9);

	}

}
