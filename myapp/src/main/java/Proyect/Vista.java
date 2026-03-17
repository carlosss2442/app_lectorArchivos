package Proyect;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.awt.Desktop;
import java.net.URI;

public class Vista {

	// Botones del Sidebar
	private final Button btnImportar = new Button("📂 Importar Excel");
	private final Button btnExportar = new Button("📤 Exportar Obra");
	private final Button btnListarObras = new Button("📋 Listado General");
	private final Button btnBuscarObra = new Button("🔍 Buscar Obra");
	private final Button btnAgregarFila = new Button("➕ Añadir Material");
	private final Button btnEliminarFila = new Button("🗑 Eliminar Material");
	private final Button btnActualizar = new Button("✏️ Editar Material");
	private final Button btnEliminarObra = new Button("❌ Eliminar Obra");
	private final Button btnBuscarCliente = new Button("👤 Por Cliente");
	private final Button btnBuscarRef = new Button("🔎 Por Referencia");
	private final Button btnOrdenarA3 = new Button("🔢 Ordenar A3");
	private final Button btnLimpiarInput = new Button("🧹 Limpiar Pantalla");
	//private final Button btnSalir = new Button("🚪 Salir del Programa");

	private final TextField txtInput = new TextField();
	private final Label lblEstado = new Label("Sistemas listos - Base de Datos conectada");

	// Métricas del Dashboard
	private final Label lblNumObras = new Label("0");
	private final Label lblNumMateriales = new Label("0");
	private final Label lblNumAlertas = new Label("0");

	// Contenedor principal para tablas y bienvenida
	private final VBox contenedorDinamico = new VBox(15);

	private final Button btnEtiquetas = new Button("🏷 Imprimir Etiquetas / QR");

	public Scene construirEscena() {
		// Colores y Estilos
		String estiloHeader = "-fx-background-color: linear-gradient(to right, #0f2027, #203a43, #2c5364);";
		String estiloSidebar = "-fx-background-color: #1e272e;";

		// --- SIDEBAR ---
		VBox sidebar = new VBox(2);
		sidebar.setPrefWidth(250);
		sidebar.setStyle(estiloSidebar);
		sidebar.setPadding(new Insets(10, 0, 0, 0));

		configurarBotonesSidebar(btnListarObras, btnImportar, btnExportar, btnAgregarFila, btnActualizar,
				btnEliminarFila, btnEliminarObra, btnEtiquetas, btnBuscarObra, btnBuscarCliente, btnBuscarRef,
				btnOrdenarA3);

		Region espaciadorVertical = new Region();
		VBox.setVgrow(espaciadorVertical, Priority.ALWAYS);

		// Bloque de Contacto
		VBox bloqueContacto = new VBox(8);
		bloqueContacto.setPadding(new Insets(20));
		bloqueContacto.setStyle("-fx-background-color: rgba(0,0,0,0.3);");

		Label lblContactoTit = new Label("CONTACTO Y AYUDA");
		lblContactoTit.setStyle("-fx-text-fill: #3498db; -fx-font-size: 9px; -fx-font-weight: bold;");

		Hyperlink linkGoogle = new Hyperlink("🌐 Buscar en Google");
		linkGoogle.setStyle(
				"-fx-text-fill: #ecf0f1; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");
		linkGoogle.setOnAction(e -> abrirEnlace("https://www.google.com"));

		Hyperlink linkEmail = new Hyperlink("✉ tecnomat@tecnomat.es");
		linkEmail.setStyle(
				"-fx-text-fill: #ecf0f1; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");
		linkEmail.setOnAction(e -> abrirEnlace("mailto:tecnomat@tecnomat.es"));

		Label lblTelefono = new Label("📞 961 231 971");
		lblTelefono.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 11px; -fx-font-weight: bold;");

		bloqueContacto.getChildren().addAll(lblContactoTit, linkGoogle, linkEmail, lblTelefono);

		sidebar.getChildren().addAll(crearEtiquetaSeccion("ARCHIVO Y LISTADO"), btnListarObras, btnImportar,
				btnExportar, new Separator(), crearEtiquetaSeccion("GESTIÓN Y EDICIÓN"), btnAgregarFila, btnActualizar,
				btnEliminarFila, btnEliminarObra, btnEtiquetas, new Separator(), crearEtiquetaSeccion("BÚSQUEDAS"),
				btnBuscarObra, btnBuscarCliente, btnBuscarRef, btnOrdenarA3, espaciadorVertical, bloqueContacto
				);

		// --- BARRA SUPERIOR ---
		HBox topBar = new HBox(15);
		topBar.setPadding(new Insets(15, 25, 15, 25));
		topBar.setAlignment(Pos.CENTER_LEFT);
		topBar.setStyle(estiloHeader);

		txtInput.setPromptText("Introduce referencia de obra o cliente...");
		txtInput.setPrefWidth(450);
		txtInput.setPrefHeight(35);
		txtInput.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 0 15;");

		btnLimpiarInput.setStyle(
				"-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");

		Label lblLogo = new Label("GESTIÓN DE MATERIALES");
		lblLogo.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		topBar.getChildren().addAll(lblLogo, spacer, new Label("🔍"), txtInput, btnLimpiarInput);

		// --- PANEL CENTRAL ---
		VBox centroPrincipal = new VBox(40);
		centroPrincipal.setPadding(new Insets(40, 25, 25, 25));
		centroPrincipal.setStyle("-fx-background-color: #f0f2f5;");

		// Dashboard
		HBox dashboard = new HBox(25);
		dashboard.setAlignment(Pos.CENTER);
		dashboard.getChildren().addAll(crearTarjetaMetrica("Obras Activas", lblNumObras, "#1e90ff"),
				crearTarjetaMetrica("Total Materiales", lblNumMateriales, "#2ed573"),
				crearTarjetaMetrica("Alertas (Falta)", lblNumAlertas, "#ff4757"));

		// Contenedor dinámico (Inicia con Pantalla de Bienvenida)
		mostrarPantallaBienvenida();
		VBox.setVgrow(contenedorDinamico, Priority.ALWAYS);

		centroPrincipal.getChildren().addAll(dashboard, contenedorDinamico);

		ScrollPane scrollCentro = new ScrollPane(centroPrincipal);
		scrollCentro.setFitToWidth(true);
		scrollCentro.setFitToHeight(true);
		scrollCentro
				.setStyle("-fx-background-color: transparent; -fx-background: #f8f9fa; -fx-border-color: transparent;");

		// --- BARRA DE ESTADO ---
		HBox barraEstado = new HBox(10);
		barraEstado.setPadding(new Insets(8, 20, 8, 20));
		barraEstado.setStyle("-fx-background-color: white; -fx-border-color: #dfe4ea; -fx-border-width: 1 0 0 0;");
		lblEstado.setStyle("-fx-text-fill: #2f3542; -fx-font-weight: bold;");
		barraEstado.getChildren().addAll(new Label("📌 Estado:"), lblEstado);

		BorderPane layout = new BorderPane();
		layout.setTop(topBar);
		layout.setLeft(sidebar);
		layout.setCenter(scrollCentro);
		layout.setBottom(barraEstado);

		return new Scene(layout, 1350, 850);
	}

	private void mostrarPantallaBienvenida() {
		contenedorDinamico.getChildren().clear();
		contenedorDinamico.setAlignment(Pos.CENTER);

		try {
			ImageView logoGrande = new ImageView(new Image(getClass().getResourceAsStream("/logo.jpg")));
			logoGrande.setFitWidth(450);
			logoGrande.setPreserveRatio(true);
			logoGrande.setSmooth(true);

			Label lblB1 = new Label("BIENVENIDO A GESTIÓN DE MATERIALES TECNOMAT");
			lblB1.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 26px; -fx-font-weight: 900; -fx-padding: 20 0 5 0;");

			Label lblB2 = new Label("Seleccione una operación en el menú lateral para gestionar la base de datos");
			lblB2.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 15px;");

			contenedorDinamico.getChildren().addAll(logoGrande, lblB1, lblB2);
		} catch (Exception e) {
			Label lblError = new Label("GESTIÓN DE MATERIALES");
			lblError.setStyle("-fx-font-size: 60px; -fx-font-weight: 900; -fx-text-fill: #dfe4ea;");
			contenedorDinamico.getChildren().add(lblError);
		}
	}

	public void setContenidoCentral(javafx.scene.Node nodo) {
		contenedorDinamico.getChildren().clear();
		contenedorDinamico.setAlignment(Pos.TOP_CENTER); // Al cargar tablas, alinear arriba
		contenedorDinamico.getChildren().add(nodo);
	}

	private VBox crearTarjetaMetrica(String titulo, Label lblValor, String color) {
		VBox card = new VBox(5);
		card.setPadding(new Insets(20));
		card.setAlignment(Pos.CENTER);
		card.setPrefWidth(240);
		card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " + "-fx-border-color: " + color
				+ "; -fx-border-width: 0 0 6 0; "
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 7);");

		Label lblT = new Label(titulo.toUpperCase());
		lblT.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: 900;");
		lblValor.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: #000000;");

		card.getChildren().addAll(lblT, lblValor);
		return card;
	}

	private void configurarBotonesSidebar(Button... botones) {
		for (Button b : botones) {
			b.setMaxWidth(Double.MAX_VALUE);
			b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-alignment: CENTER_LEFT; "
					+ "-fx-font-weight: bold; -fx-padding: 12 25; -fx-font-size: 13px; -fx-cursor: hand;");

			b.setOnMouseEntered(e -> {
				if (b.getText().contains("🗑") || b.getText().contains("❌") || b.getText().contains("🚪")) {
					b.setStyle(
							"-fx-background-color: #c0392b; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold; -fx-padding: 12 25;");
				} else {
					b.setStyle(
							"-fx-background-color: #3498db; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold; -fx-padding: 12 25;");
				}
			});
			b.setOnMouseExited(e -> {
				b.setStyle(
						"-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold; -fx-padding: 12 25;");
			});
		}
	}

	private void abrirEnlace(String url) {
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().browse(new URI(url));
			} else {
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
			}
		} catch (Exception e) {
			setEstado("❌ Error al abrir el enlace.");
		}
	}

	private Label crearEtiquetaSeccion(String texto) {
		Label l = new Label(texto);
		l.setPadding(new Insets(15, 20, 8, 20));
		l.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px; -fx-font-weight: bold;");
		return l;
	}

	// --- MÉTODOS DE SOPORTE ---
	public void actualizarDashboard(int obras, int materiales, int alertas) {
		javafx.application.Platform.runLater(() -> {
			lblNumObras.setText(String.valueOf(obras));
			lblNumMateriales.setText(String.format("%,d", materiales));
			lblNumAlertas.setText(String.valueOf(alertas));
		});
	}

	public void limpiar() {
		mostrarPantallaBienvenida();
	}

	public void limpiarInput() {
		txtInput.clear();
		txtInput.requestFocus();
	}

	public void setEstado(String msg) {
		lblEstado.setText(msg);
	}

	public String getInput() {
		return txtInput.getText().trim();
	}

	public boolean confirmar(String msj) {
		Alert a = new Alert(Alert.AlertType.CONFIRMATION, msj, ButtonType.YES, ButtonType.NO);
		return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
	}

	public void mostrarAlerta(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK);
		a.setTitle(t);
		a.setHeaderText(null);
		a.showAndWait();
	}

	// GETTERS PARA EL CONTROLADOR
	public Button getBtnImportar() {
		return btnImportar;
	}

	public Button getBtnExportar() {
		return btnExportar;
	}

	public Button getBtnListarObras() {
		return btnListarObras;
	}

	public Button getBtnBuscarObra() {
		return btnBuscarObra;
	}

	public Button getBtnAgregarFila() {
		return btnAgregarFila;
	}

	public Button getBtnEliminarFila() {
		return btnEliminarFila;
	}

	public Button getBtnActualizar() {
		return btnActualizar;
	}

	public Button getBtnEliminarObra() {
		return btnEliminarObra;
	}

	public Button getBtnBuscarCliente() {
		return btnBuscarCliente;
	}

	public Button getBtnBuscarRef() {
		return btnBuscarRef;
	}

	public Button getBtnOrdenarA3() {
		return btnOrdenarA3;
	}

	public Button getBtnLimpiarInput() {
		return btnLimpiarInput;
	}

	/*
	 * public Button getBtnSalir() { return btnSalir; }
	 */

	public Button getBtnEtiquetas() {
		return btnEtiquetas;
	}

	public void setOnInputChange(javafx.beans.value.ChangeListener<String> listener) {
		txtInput.textProperty().addListener(listener);
	}

	public void setInputFiltrandoEstilo(boolean filtrando) {
		if (filtrando) {
			txtInput.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 0 15; "
					+ "-fx-border-color: #3498db; -fx-border-width: 2;");
		} else {
			txtInput.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 0 15;");
		}
	}
	
	

}