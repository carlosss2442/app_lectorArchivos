package Proyect;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

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
    private final Button btnLimpiarInput = new Button("Limpiar");

    private final TextField txtInput = new TextField();
    private final Label lblEstado = new Label("Sistemas listos - Base de Datos conectada");

    // Métricas del Dashboard
    private final Label lblNumObras = new Label("0");
    private final Label lblNumMateriales = new Label("0");
    private final Label lblNumAlertas = new Label("0");

    // Contenedor principal para tablas
    private final VBox contenedorDinamico = new VBox(15);

    public Scene construirEscena() {
        // Estilos base
        String estiloHeader = "-fx-background-color: linear-gradient(to right, #0f2027, #203a43, #2c5364);";
        String estiloSidebar = "-fx-background-color: #1e272e;"; 

        // --- SIDEBAR ---
        VBox sidebar = new VBox(2);
        sidebar.setPrefWidth(230);
        sidebar.setStyle(estiloSidebar);
        sidebar.setPadding(new Insets(10, 0, 0, 0));

        configurarBotonesSidebar(btnListarObras, btnImportar, btnExportar, btnAgregarFila,
                btnActualizar, btnEliminarFila, btnEliminarObra, btnBuscarObra, btnBuscarCliente, btnBuscarRef,
                btnOrdenarA3);

        // Espaciador para empujar el contacto al final
        Region espaciadorVertical = new Region();
        VBox.setVgrow(espaciadorVertical, Priority.ALWAYS);

        // Bloque de Contacto inferior
        VBox bloqueContacto = new VBox(5);
        bloqueContacto.setPadding(new Insets(20, 20, 20, 20));
        bloqueContacto.setStyle("-fx-background-color: rgba(0,0,0,0.3);");
        
        Label lblContactoTit = new Label("CONTACTO EMPRESA");
        lblContactoTit.setStyle("-fx-text-fill: #3498db; -fx-font-size: 9px; -fx-font-weight: bold;");
        Label lblTelefono = new Label("📞 961 231 971");
        lblTelefono.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label lblEmail = new Label("✉ tecnomat@tecnomat.es");
        lblEmail.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 11px;");
        bloqueContacto.getChildren().addAll(lblContactoTit, lblTelefono, lblEmail);

        sidebar.getChildren().addAll(
                crearEtiquetaSeccion("ARCHIVO Y LISTADO"), btnListarObras, btnImportar, btnExportar,
                new Separator(),
                crearEtiquetaSeccion("GESTIÓN Y EDICIÓN"), btnAgregarFila, btnActualizar, btnEliminarFila, btnEliminarObra,
                new Separator(),
                crearEtiquetaSeccion("BÚSQUEDAS"), btnBuscarObra, btnBuscarCliente, btnBuscarRef, btnOrdenarA3,
                espaciadorVertical,
                bloqueContacto
        );

        // --- BARRA SUPERIOR (TOP) ---
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle(estiloHeader);

        txtInput.setPromptText("Introduce referencia de obra o cliente...");
        txtInput.setPrefWidth(450);
        txtInput.setPrefHeight(35);
        txtInput.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 0 15;");

        btnLimpiarInput.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");

        Label lblLogo = new Label("G-MATERIALS");
        lblLogo.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(lblLogo, spacer, new Label("🔍"), txtInput, btnLimpiarInput);

        // --- PANEL CENTRAL ---
        VBox centroPrincipal = new VBox(40); 
        centroPrincipal.setPadding(new Insets(40, 25, 25, 25)); // Espacio arriba para que no pegue al buscador
        centroPrincipal.setStyle("-fx-background-color: #f0f2f5;");

        // Dashboard
        HBox dashboard = new HBox(25);
        dashboard.setAlignment(Pos.CENTER);
        dashboard.getChildren().addAll(
                crearTarjetaMetrica("Obras Activas", lblNumObras, "#1e90ff"),
                crearTarjetaMetrica("Total Materiales", lblNumMateriales, "#2ed573"),
                crearTarjetaMetrica("Alertas (Falta)", lblNumAlertas, "#ff4757")
        );

        // Contenedor dinámico de tablas
        contenedorDinamico.setAlignment(Pos.TOP_CENTER);
        contenedorDinamico.setFillWidth(true);
        VBox.setVgrow(contenedorDinamico, Priority.ALWAYS);

        Label lblBienvenida = new Label("Seleccione una opción para visualizar datos");
        lblBienvenida.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 14px; -fx-font-style: italic;");
        contenedorDinamico.getChildren().add(lblBienvenida);

        centroPrincipal.getChildren().addAll(dashboard, contenedorDinamico);

        ScrollPane scrollCentro = new ScrollPane(centroPrincipal);
        scrollCentro.setFitToWidth(true);
        scrollCentro.setFitToHeight(true);
        scrollCentro.setStyle("-fx-background-color: transparent; -fx-background: #f8f9fa; -fx-border-color: transparent;");

        // --- BARRA INFERIOR ---
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

    private VBox crearTarjetaMetrica(String titulo, Label lblValor, String color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(240);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                      "-fx-border-color: " + color + "; -fx-border-width: 0 0 6 0; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 7);");

        Label lblT = new Label(titulo.toUpperCase());
        lblT.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: 900;");
        lblValor.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: #000000;"); // Valor en negro puro

        card.getChildren().addAll(lblT, lblValor);
        return card;
    }

    private Label crearEtiquetaSeccion(String texto) {
        Label l = new Label(texto);
        l.setPadding(new Insets(15, 20, 8, 20));
        l.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px; -fx-font-weight: bold;");
        return l;
    }

    private void configurarBotonesSidebar(Button... botones) {
        for (Button b : botones) {
            b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-alignment: CENTER_LEFT; " +
                       "-fx-font-weight: bold; -fx-padding: 12 25; -fx-font-size: 13px; -fx-cursor: hand;");

            b.setOnMouseEntered(e -> {
                if (b.getText().contains("🗑") || b.getText().contains("❌")) {
                    b.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold; -fx-padding: 12 25;");
                } else {
                    b.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold; -fx-padding: 12 25;");
                }
            });

            b.setOnMouseExited(e -> {
                b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold; -fx-padding: 12 25;");
            });
        }
    }

    public void setContenidoCentral(javafx.scene.Node nodo) {
        contenedorDinamico.getChildren().clear();
        contenedorDinamico.getChildren().add(nodo);
    }

    public void actualizarDashboard(int obras, int materiales, int alertas) {
        javafx.application.Platform.runLater(() -> {
            lblNumObras.setText(String.valueOf(obras));
            lblNumMateriales.setText(String.format("%,d", materiales));
            lblNumAlertas.setText(String.valueOf(alertas));
        });
    }

    // --- GETTERS ---
    public Button getBtnImportar() { return btnImportar; }
    public Button getBtnExportar() { return btnExportar; }
    public Button getBtnListarObras() { return btnListarObras; }
    public Button getBtnBuscarObra() { return btnBuscarObra; }
    public Button getBtnAgregarFila() { return btnAgregarFila; }
    public Button getBtnEliminarFila() { return btnEliminarFila; }
    public Button getBtnActualizar() { return btnActualizar; }
    public Button getBtnEliminarObra() { return btnEliminarObra; }
    public Button getBtnBuscarCliente() { return btnBuscarCliente; }
    public Button getBtnBuscarRef() { return btnBuscarRef; }
    public Button getBtnOrdenarA3() { return btnOrdenarA3; }
    public Button getBtnLimpiarInput() { return btnLimpiarInput; }
    public TextField getTxtInput() { return txtInput; }
    public String getInput() { return txtInput.getText().trim(); }
    public void setEstado(String msg) { lblEstado.setText(msg); }
    public void limpiarInput() { txtInput.clear(); txtInput.requestFocus(); }
    public void limpiar() { contenedorDinamico.getChildren().clear(); }

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