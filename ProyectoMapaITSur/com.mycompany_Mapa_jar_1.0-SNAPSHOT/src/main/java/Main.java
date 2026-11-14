
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.*;

public class Main extends Application {

    private int distanciaTotalRuta = 0;
    private Integer idOrigenSeleccionado = null;
    private Integer idDestinoSeleccionado = null;

    private Grafo campus;
    private Label lblValorOrigen;
    private Label lblValorDestino;
    private PanelMapa panelMapa;

    private final Map<Integer, Point2D> coordenadas = new HashMap<>();
    private final Map<String, Point2D> esquinasCaminos = new HashMap<>();
    private final Map<String, Point2D> puntosPorConexion = new HashMap<>();
    //parte de arriba
    @Override
    public void start(Stage primaryStage) {
        inicializarCampus();
        definirCoordenadas();
        definirEsquinas();
        definirPuntosPorConexion();
 
        panelMapa = new PanelMapa(
                960, 680,
                campus,
                coordenadas,
                esquinasCaminos,
                puntosPorConexion,
                this::seleccionarEdificioDesdeMapa 
        );
        primaryStage.setTitle("GPS ITSUR");

        // 1. Barra dearriba que muestra origen y destino
        HBox panelControl = new HBox(22);
        panelControl.setAlignment(Pos.CENTER);
        panelControl.setPadding(new Insets(12, 20, 12, 20));
        panelControl.setStyle(
                "-fx-background-color: #FFFFFF;"
                + "-fx-border-color: transparent transparent #E2E8F0 transparent;"
                + "-fx-border-width: 0 0 1.5 0;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(15, 23, 42, 0.06), 8, 0, 0, 3);"
        );

        // mostrador del origen
        HBox boxOrigen = new HBox(8);
        boxOrigen.setAlignment(Pos.CENTER_LEFT);
        boxOrigen.setPadding(new Insets(6, 14, 6, 14));
        boxOrigen.setStyle("-fx-background-color: #F0F9FF; -fx-border-color: #BAE6FD; -fx-border-radius: 8; -fx-background-radius: 8;");
        Label tituloOrigen = new Label("● ORIGEN:");
        tituloOrigen.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #3752A1;");
        lblValorOrigen = new Label("Toca un pin en el mapa");
        lblValorOrigen.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0F172A;");
        boxOrigen.getChildren().addAll(tituloOrigen, lblValorOrigen);

        // muestra el destino
        HBox boxDestino = new HBox(8);
        boxDestino.setAlignment(Pos.CENTER_LEFT);
        boxDestino.setPadding(new Insets(6, 14, 6, 14));
        boxDestino.setStyle("-fx-background-color: #FFF1F2; -fx-border-color: #FECDD3; -fx-border-radius: 8; -fx-background-radius: 8;");
        Label tituloDestino = new Label("● DESTINO:");
        tituloDestino.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #A1383F;");
        lblValorDestino = new Label("Esperando selección...");
        lblValorDestino.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #64748B;");
        boxDestino.getChildren().addAll(tituloDestino, lblValorDestino);

        // Botón para limpiar selección
        Button btnLimpiar = new Button("Limpiar");
        btnLimpiar.setPrefSize(95, 34);
        String btnNormal = "-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 12.5px; -fx-cursor: hand;";
        String btnHover = "-fx-background-color: #E2E8F0; -fx-text-fill: #0F172A; -fx-border-color: #94A3B8; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 12.5px; -fx-cursor: hand;";
        btnLimpiar.setStyle(btnNormal);
        btnLimpiar.setOnMouseEntered(e -> btnLimpiar.setStyle(btnHover));
        btnLimpiar.setOnMouseExited(e -> btnLimpiar.setStyle(btnNormal));
        btnLimpiar.setOnAction(e -> reiniciarSeleccion());
        panelControl.getChildren().addAll(boxOrigen, boxDestino, btnLimpiar);

        // 2. Mapa interactivo
        BorderPane root = new BorderPane();
        root.setTop(panelControl);
        root.setCenter(panelMapa);

        Scene scene = new Scene(root, 960, 745);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void seleccionarEdificioDesdeMapa(int idEdificio) {
        String nombreLugar = campus.getVertices().get(idEdificio).nombre;

        // Cuando se selecciona el ORIGEN
        if (idOrigenSeleccionado == null || (idOrigenSeleccionado != null && idDestinoSeleccionado != null)) {
            idOrigenSeleccionado = idEdificio;
            idDestinoSeleccionado = null;
            distanciaTotalRuta = 0;

            lblValorOrigen.setText(nombreLugar);
            lblValorOrigen.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #3752A1;");

            lblValorDestino.setText("Elige destino en el mapa...");
            lblValorDestino.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-style: italic; -fx-font-size: 13px; -fx-text-fill: #A1383F;");

            panelMapa.actualizarEstadoSeleccion(idOrigenSeleccionado, null, 0);
            panelMapa.limpiarRutaVisual();
            return;
        }

        if (idEdificio == idOrigenSeleccionado) {
            return;
        }

        // Cuando se selecciona el DESTINO
        idDestinoSeleccionado = idEdificio;
        lblValorDestino.setText(nombreLugar);
        lblValorDestino.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #BE123C;");

        List<Integer> rutaCalculada = ejecutarDijkstra(idOrigenSeleccionado, idDestinoSeleccionado);
        if (rutaCalculada != null) {
            distanciaTotalRuta = calcularMetros(idOrigenSeleccionado, idDestinoSeleccionado);
            
            panelMapa.actualizarEstadoSeleccion(idOrigenSeleccionado, idDestinoSeleccionado, distanciaTotalRuta);
           
            panelMapa.comenzarAnimacion(rutaCalculada);
        } else {
            mostrarAlerta("Sin conexión", "No existe un camino conectado entre " + lblValorOrigen.getText() + " y " + nombreLugar + ".");
            idDestinoSeleccionado = null;
            lblValorDestino.setText("Sin ruta, elige otro...");
            panelMapa.actualizarEstadoSeleccion(idOrigenSeleccionado, null, 0);
        }
    }

    private void reiniciarSeleccion() {
        idOrigenSeleccionado = null;
        idDestinoSeleccionado = null;
        distanciaTotalRuta = 0;

        lblValorOrigen.setText("Toca un pin en el mapa");
        lblValorOrigen.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0F172A;");

        lblValorDestino.setText("Esperando selección...");
        lblValorDestino.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #64748B;");

        panelMapa.actualizarEstadoSeleccion(null, null, 0);
        panelMapa.limpiarRutaVisual();
    }

    //alerta
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    //calcular metros
    private int calcularMetros(int origen, int destino) {
        int n = campus.getListaA().size();
        int[] distancias = new int[n];
        Arrays.fill(distancias, Integer.MAX_VALUE);
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));

        distancias[origen] = 0;
        pq.add(new int[]{origen, 0});

        while (!pq.isEmpty()) {
            int[] actual = pq.poll();
            int u = actual[0];
            int distU = actual[1];

            if (distU > distancias[u]) {
                continue;
            }

            for (Arista arista : campus.getListaA().get(u)) {
                int v = arista.destino;
                int peso = arista.distanciaMetros;
                if (distancias[u] + peso < distancias[v]) {
                    distancias[v] = distancias[u] + peso;
                    pq.add(new int[]{v, distancias[v]});
                }
            }
        }
        return distancias[destino];
    }

    //coordenadas
    private void definirCoordenadas() {
        coordenadas.put(0, new Point2D(480, 50));
        coordenadas.put(1, new Point2D(670, 50));
        coordenadas.put(2, new Point2D(691, 50));
        coordenadas.put(3, new Point2D(530, 290));
        coordenadas.put(4, new Point2D(690, 240));
        coordenadas.put(5, new Point2D(550, 450));
        coordenadas.put(6, new Point2D(700, 370));
        coordenadas.put(7, new Point2D(280, 235));
        coordenadas.put(8, new Point2D(255, 240));
        coordenadas.put(9, new Point2D(415, 560));
        coordenadas.put(10, new Point2D(715, 450));
        coordenadas.put(11, new Point2D(420, 115));
        coordenadas.put(12, new Point2D(620, 460));
        coordenadas.put(13, new Point2D(420, 240));
        coordenadas.put(14, new Point2D(270, 295));
        coordenadas.put(15, new Point2D(280, 370));
        coordenadas.put(16, new Point2D(895, 240));
        coordenadas.put(17, new Point2D(520, 565));
        coordenadas.put(19, new Point2D(565, 70));
        coordenadas.put(20, new Point2D(490, 660));
    }

    //aristas de esquina
    private void definirEsquinas() {
        esquinasCaminos.put("0-4", new Point2D(495, 180));
        esquinasCaminos.put("4-0", new Point2D(495, 180));
        esquinasCaminos.put("0-11", new Point2D(480, 115));
        esquinasCaminos.put("11-0", new Point2D(480, 115));
    }
    
   private void definirPuntosPorConexion() {
    Point2D p5 = coordenadas.get(5);   // Edificio E
    Point2D p6 = coordenadas.get(6);   // Auditorio Tics
    Point2D p7 = coordenadas.get(7);   // Edificio de Vinculación
    Point2D p8 = coordenadas.get(8);   // Auditorio de Vinculación
    Point2D p11 = coordenadas.get(5);   // Biblioteca
    Point2D p13 = coordenadas.get(6);   // Palapas
    Point2D p14 = coordenadas.get(14); // Cocinas de Gastronomía
    Point2D p15 = coordenadas.get(15); // Cabaña de Gastronomía
    
    puntosPorConexion.put("6-7", new Point2D(p6.getX()-20, p7.getY() + 150));  
    Point2D entradaCabana = new Point2D(p15.getX(), p15.getY() - 10);
    puntosPorConexion.put("15-7", entradaCabana);
    puntosPorConexion.put("15-8", entradaCabana);
    puntosPorConexion.put("15-14", entradaCabana);
    puntosPorConexion.put("7-15", new Point2D(p7.getX(), p7.getY() -15));   
    puntosPorConexion.put("8-15", new Point2D(p8.getX() + 25, p8.getY() - 30)); 
    puntosPorConexion.put("14-15", new Point2D(p14.getX() + 10, p14.getY() - 30)); 
}


    // el coso de dijkstra
    private List ejecutarDijkstra(int origen, int destino) {
        int n = campus.getListaA().size();
        int[] distancias = new int[n];
        int[] predecesores = new int[n];
        Arrays.fill(distancias, Integer.MAX_VALUE);
        Arrays.fill(predecesores, -1);
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        distancias[origen] = 0;
        pq.add(new int[]{origen, 0});
        while (!pq.isEmpty()) {
            int[] actual = pq.poll();
            int u = actual[0];
            if (actual[1] > distancias[u]) {
                continue;
            }
            for (Arista arista : campus.getListaA().get(u)) {
                int v = arista.destino;
                int peso = arista.distanciaMetros;
                if (distancias[u] + peso < distancias[v]) {
                    distancias[v] = distancias[u] + peso;
                    predecesores[v] = u;
                    pq.add(new int[]{v, distancias[v]});
                }
            }
        }
        if (distancias[destino] == Integer.MAX_VALUE) {
            return null;
        }
        List ruta = new ArrayList<>();
        int paso = destino;
        while (paso != -1) {
            ruta.add(paso);
            paso = predecesores[paso];
        }
        Collections.reverse(ruta);
        return ruta;
    }

    // Nnombres, distancias y conexiones de las estructuras
    private void inicializarCampus() {
        campus = new Grafo(21);
        campus.agregarLugar(0, "Caseta de Puerta Principal");
        campus.agregarLugar(1, "Caseta de Estacionamiento");
        campus.agregarLugar(2, "Estacionamiento");
        campus.agregarLugar(3, "Edificio A");
        campus.agregarLugar(4, "Edificio B");
        campus.agregarLugar(5, "Edificio E");
        campus.agregarLugar(6, "Edificio de TICS");
        campus.agregarLugar(7, "Edificio de Vinculación");
        campus.agregarLugar(8, "Auditorio de Vinculación");
        campus.agregarLugar(9, "Auditorio Principal");
        campus.agregarLugar(10, "Dirección");
        campus.agregarLugar(11, "Biblioteca");
        campus.agregarLugar(12, "Cafetería");
        campus.agregarLugar(13, "Palapas");
        campus.agregarLugar(14, "Cocinas de Gastronomía");
        campus.agregarLugar(15, "Cabaña de Gastronomía");
        campus.agregarLugar(16, "Cancha de Futbol");
        campus.agregarLugar(17, "Canchas de basketball");
        campus.agregarLugar(19, "Jardin");
        campus.agregarLugar(20, "Cancha2");
        campus.agregarCamino(0, 3, 91);
        campus.agregarCamino(0, 4, 124);
        campus.agregarCamino(0, 11, 36);
        campus.agregarCamino(1, 2, 40);
        campus.agregarCamino(2, 4, 71);
        campus.agregarCamino(2, 16, 109);
        campus.agregarCamino(3, 4, 62);
        campus.agregarCamino(3, 5, 52);
        campus.agregarCamino(3, 6, 74);
        campus.agregarCamino(3, 13, 40);
        campus.agregarCamino(3, 15, 94);
        campus.agregarCamino(4, 6, 60);
        campus.agregarCamino(4, 11, 108);
        campus.agregarCamino(4, 16, 82);
        campus.agregarCamino(5, 7, 107);
        campus.agregarCamino(5, 12, 37);
        campus.agregarCamino(5, 15, 84);
        campus.agregarCamino(5, 17, 46);
        campus.agregarCamino(6, 5, 43);
        campus.agregarCamino(6, 7, 150);
        campus.agregarCamino(6, 12, 41);
        campus.agregarCamino(6, 10, 30);
        campus.agregarCamino(6, 16, 95);
        campus.agregarCamino(7, 8, 21);
        campus.agregarCamino(7, 14, 18);
        campus.agregarCamino(7, 13, 48);
        campus.agregarCamino(7, 15, 40);
        campus.agregarCamino(9, 15, 58);
        campus.agregarCamino(9, 17, 27);
        campus.agregarCamino(11, 13, 47);
        campus.agregarCamino(12, 10, 23);
        campus.agregarCamino(12, 17, 60);
        campus.agregarCamino(14, 15, 60);
        campus.agregarCamino(20, 17, 10);

    }

    public static void main(String[] args) {
        launch(args);
    }
}
