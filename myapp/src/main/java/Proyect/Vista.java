package Proyect;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Vista {

    // Botones (Variables originales para mantener lógica del Controlador)
    private final Button btnImportar = new Button("📂 Importar Excel");
    private final Button btnExportar = new Button("📤 Exportar Obra");
    private final Button btnListarObras = new Button("📋 Listado General");
    private final Button btnBuscarObra = new Button("🔍 Buscar Obra");
    private final Button btnEstadisticas = new Button("📊 Estadísticas");
    private final Button btnAgregarFila = new Button("➕ Añadir Material");
    private final Button btnEliminarFila = new Button("🗑 Eliminar Material");
    private final Button btnActualizar = new Button("✏️ Editar Material");
    private final Button btnEliminarObra = new Button("❌ Eliminar Obra");
    private final Button btnBuscarCliente = new Button("👤 Por Cliente");
    private final Button btnBuscarRef = new Button("🔎 Por Referencia");
    private final Button btnOrdenarA3 = new Button("🔢 Ordenar A3");
    private final Button btnContarObras = new Button("🔢 Total Obras");
    private final Button btnLimpiarInput = new Button("Limpiar");
    
    private final TextField txtInput = new TextField();
    private final Label lblEstado = new Label("Sistemas listos - Base de Datos conectada");

    public Scene construirEscena() {
        // --- ESTILOS MODERNOS ---
        String estiloHeader = "-fx-background-color: #2f3542;";
        String estiloSidebar = "-fx-background-color: #f1f2f6; -fx-border-color: #dfe4ea; -fx-border-width: 0 1 0 0;";
        String btnSidebar = "-fx-background-color: transparent; -fx-text-fill: #2f3542; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold; -fx-padding: 10 20;";
        String estiloDashboard = "-fx-background-color: white;";

        // --- SIDEBAR IZQUIERDO (MENU DE NAVEGACIÓN) ---
        VBox sidebar = new VBox(5);
        sidebar.setPrefWidth(220);
        sidebar.setStyle(estiloSidebar);
        sidebar.setPadding(new Insets(20, 0, 20, 0));

        Label lblCat1 = crearEtiquetaSeccion("ARCHIVO Y LISTADO");
        Label lblCat2 = crearEtiquetaSeccion("GESTIÓN Y EDICIÓN");
        Label lblCat3 = crearEtiquetaSeccion("BÚSQUEDAS");

        // Aplicar estilos a botones de navegación
        configurarBotonesSidebar(btnListarObras, btnImportar, btnExportar, btnEstadisticas, 
                                 btnAgregarFila, btnActualizar, btnEliminarFila, btnEliminarObra,
                                 btnBuscarObra, btnBuscarCliente, btnBuscarRef, btnOrdenarA3, btnContarObras);

        sidebar.getChildren().addAll(
            lblCat1, btnListarObras, btnImportar, btnExportar, btnEstadisticas,
            new Separator(),
            lblCat2, btnAgregarFila, btnActualizar, btnEliminarFila, btnEliminarObra,
            new Separator(),
            lblCat3, btnBuscarObra, btnBuscarCliente, btnBuscarRef, btnOrdenarA3, btnContarObras
        );

        // --- BARRA SUPERIOR (BÚSQUEDA RÁPIDA) ---
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle(estiloHeader);

        txtInput.setPromptText("Introduce referencia de obra o cliente...");
        txtInput.setPrefWidth(400);
        txtInput.setPrefHeight(35);
        txtInput.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 0 15;");

        btnLimpiarInput.setStyle("-fx-background-color: #57606f; -fx-text-fill: white; -fx-background-radius: 20;");

        Label lblLogo = new Label("G-MATERIALS");
        lblLogo.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(lblLogo, spacer, new Label("🔍"), txtInput, btnLimpiarInput);

        // --- PANEL CENTRAL (DASHBOARD) ---
        StackPane centro = new StackPane();
        centro.setStyle(estiloDashboard);

        VBox bienvenida = new VBox(20);
        bienvenida.setAlignment(Pos.CENTER);
        
        ImageView logoView = new ImageView();
        try {
            Image img = new Image(getClass().getResourceAsStream("/logo.jpg"));
            logoView.setImage(img);
            logoView.setFitWidth(300);
            logoView.setPreserveRatio(true);
        } catch (Exception e) {}

        Label lblSlogan = new Label("Selecciona una opción del menú para comenzar");
        lblSlogan.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 16px; -fx-font-style: italic;");
        
        bienvenida.getChildren().addAll(logoView, lblSlogan);
        centro.getChildren().add(bienvenida);

        // --- BARRA DE ESTADO ---
        HBox barraEstado = new HBox(10);
        barraEstado.setPadding(new Insets(8, 20, 8, 20));
        barraEstado.setStyle("-fx-background-color: #f1f2f6; -fx-border-color: #dfe4ea; -fx-border-width: 1 0 0 0;");
        lblEstado.setStyle("-fx-text-fill: #2f3542; -fx-font-weight: bold;");
        barraEstado.getChildren().addAll(new Label("📌 Estado:"), lblEstado);

        // --- DISEÑO FINAL ---
        BorderPane layout = new BorderPane();
        layout.setTop(topBar);
        layout.setLeft(sidebar);
        layout.setCenter(centro);
        layout.setBottom(barraEstado);

        return new Scene(layout, 1300, 850);
    }

    // --- MÉTODOS AUXILIARES DE ESTILO ---
    private Label crearEtiquetaSeccion(String texto) {
        Label l = new Label(texto);
        l.setPadding(new Insets(15, 20, 5, 20));
        l.setStyle("-fx-text-fill: #747d8c; -fx-font-size: 10px; -fx-font-weight: bold;");
        return l;
    }

    private void configurarBotonesSidebar(Button... botones) {
        for (Button b : botones) {
            b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #2f3542; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold; -fx-padding: 10 20;");
            b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #dfe4ea; -fx-text-fill: #2f3542; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold; -fx-padding: 10 20;"));
            b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #2f3542; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold; -fx-padding: 10 20;"));
        }
    }

    // --- GETTERS (Se mantienen para que el controlador funcione) ---
    public Button getBtnImportar() { return btnImportar; }
    public Button getBtnListarObras() { return btnListarObras; }
    public Button getBtnBuscarObra() { return btnBuscarObra; }
    public Button getBtnEstadisticas() { return btnEstadisticas; }
    public Button getBtnAgregarFila() { return btnAgregarFila; }
    public Button getBtnEliminarFila() { return btnEliminarFila; }
    public Button getBtnActualizar() { return btnActualizar; }
    public Button getBtnEliminarObra() { return btnEliminarObra; }
    public Button getBtnBuscarCliente() { return btnBuscarCliente; }
    public Button getBtnBuscarRef() { return btnBuscarRef; }
    public Button getBtnOrdenarA3() { return btnOrdenarA3; }
    public Button getBtnContarObras() { return btnContarObras; }
    public Button getBtnLimpiarInput() { return btnLimpiarInput; }
    public TextField getTxtInput() { return txtInput; }
    public Label getLblEstado() { return lblEstado; }
    public Button getBtnExportar() { return btnExportar; }
    
    // El TextArea ya no se usa, pero dejamos el método por si el controlador lo llama para no romper nada
    public void mostrarTexto(String texto) { 
        // Ya no hace nada o podrías redirigirlo a una alerta
    }

    public String getInput() { return txtInput.getText().trim(); }
    public void setEstado(String msg) { lblEstado.setText(msg); }
    public void limpiar() { /* No hace falta con tablas */ }

    public void limpiarInput() {
        this.txtInput.clear();
        this.txtInput.requestFocus();
    }

    public String pedirTexto(String titulo, String mensaje) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);
        dialog.setContentText(mensaje);
        return dialog.showAndWait().orElse("").trim();
    }

    public boolean confirmar(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}