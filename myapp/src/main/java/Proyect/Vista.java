package Proyect;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

public class Vista {

    // ── Botones principales ───────────────────────────────────────────────────
    private final Button btnImportar      = new Button("📂 Importar Excel");
    private final Button btnListarObras   = new Button("📋 Listar Obras");
    private final Button btnBuscarObra    = new Button("🔍 Buscar Obra");
    private final Button btnEstadisticas  = new Button("📊 Estadísticas");
    private final Button btnAgregarFila   = new Button("➕ Agregar Fila");
    private final Button btnEliminarFila  = new Button("🗑 Eliminar Fila");
    private final Button btnActualizar    = new Button("✏️ Actualizar Campo");
    private final Button btnEliminarObra  = new Button("❌ Eliminar Obra");
    private final Button btnBuscarCliente = new Button("👤 Buscar Cliente");
    private final Button btnBuscarRef     = new Button("🔎 Buscar Referencia");
    private final Button btnOrdenarA3     = new Button("🔢 Ordenar por A3");
    private final Button btnContarObras   = new Button("🔢 Contar Obras");

    // ── Campo de búsqueda y área de resultados ────────────────────────────────
    private final TextField txtInput      = new TextField();
    private final TextArea  areaResultado = new TextArea();
    private final Label     lblEstado     = new Label("Listo.");

    public Scene construirEscena() {

        // ── Fila 1 de botones ─────────────────────────────────────────────────
        HBox fila1 = new HBox(8,
            btnImportar, btnListarObras, btnBuscarObra,
            btnEstadisticas, btnOrdenarA3, btnContarObras);
        fila1.setPadding(new Insets(10, 10, 4, 10));
        fila1.setAlignment(Pos.CENTER_LEFT);

        // ── Fila 2 de botones ─────────────────────────────────────────────────
        HBox fila2 = new HBox(8,
            btnAgregarFila, btnEliminarFila, btnActualizar,
            btnEliminarObra, btnBuscarCliente, btnBuscarRef);
        fila2.setPadding(new Insets(4, 10, 8, 10));
        fila2.setAlignment(Pos.CENTER_LEFT);

        // ── Barra de input ────────────────────────────────────────────────────
        txtInput.setPromptText("Escribe ref. obra, cliente, referencia...");
        txtInput.setPrefWidth(400);
        HBox barraInput = new HBox(10, new Label("🔤 Input:"), txtInput);
        barraInput.setPadding(new Insets(0, 10, 8, 10));
        barraInput.setAlignment(Pos.CENTER_LEFT);

        // ── Área de resultados ────────────────────────────────────────────────
        areaResultado.setEditable(false);
        areaResultado.setFont(Font.font("Monospaced", 12));
        VBox.setVgrow(areaResultado, Priority.ALWAYS);

        // ── Barra de estado ───────────────────────────────────────────────────
        HBox barraEstado = new HBox(lblEstado);
        barraEstado.setPadding(new Insets(4, 10, 4, 10));
        barraEstado.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");

        // ── Layout principal ──────────────────────────────────────────────────
        VBox raiz = new VBox(fila1, fila2, barraInput, areaResultado, barraEstado);
        raiz.setPrefSize(1200, 650);

        
        
        return new Scene(raiz);
    }

    // ── Getters botones ───────────────────────────────────────────────────────
    public Button getBtnImportar()      { return btnImportar; }
    public Button getBtnListarObras()   { return btnListarObras; }
    public Button getBtnBuscarObra()    { return btnBuscarObra; }
    public Button getBtnEstadisticas()  { return btnEstadisticas; }
    public Button getBtnAgregarFila()   { return btnAgregarFila; }
    public Button getBtnEliminarFila()  { return btnEliminarFila; }
    public Button getBtnActualizar()    { return btnActualizar; }
    public Button getBtnEliminarObra()  { return btnEliminarObra; }
    public Button getBtnBuscarCliente() { return btnBuscarCliente; }
    public Button getBtnBuscarRef()     { return btnBuscarRef; }
    public Button getBtnOrdenarA3()     { return btnOrdenarA3; }
    public Button getBtnContarObras()   { return btnContarObras; }

    // ── Getters otros ─────────────────────────────────────────────────────────
    public TextField getTxtInput()      { return txtInput; }
    public TextArea getAreaResultado()  { return areaResultado; }
    public Label getLblEstado()         { return lblEstado; }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public String getInput()                   { return txtInput.getText().trim(); }
    public void mostrarTexto(String texto)     { areaResultado.setText(texto); }
    public void agregarTexto(String texto)     { areaResultado.appendText(texto + "\n"); }
    public void setEstado(String msg)          { lblEstado.setText(msg); }
    public void limpiar()                      { areaResultado.clear(); }
    public void limpiarInput()                 { txtInput.clear(); }

    // Diálogo de texto simple reutilizable
    public String pedirTexto(String titulo, String mensaje) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);
        dialog.setContentText(mensaje);
        return dialog.showAndWait().orElse("").trim();
    }

    // Diálogo de confirmación
    public boolean confirmar(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    // Diálogo de selección (menú desplegable)
    public String elegirOpcion(String titulo, String mensaje, String... opciones) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(opciones[0], opciones);
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);
        dialog.setContentText(mensaje);
        return dialog.showAndWait().orElse("");
    }

    // Alerta de error / info
    public void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}