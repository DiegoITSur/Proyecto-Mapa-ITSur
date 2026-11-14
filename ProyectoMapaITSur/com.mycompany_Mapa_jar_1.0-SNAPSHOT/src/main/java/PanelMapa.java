import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;
import java.util.function.Consumer;

    public class PanelMapa extends Canvas {
        private Grafo campus;
    private Map<Integer, Point2D> coordenadas;
    private Map<String, Point2D> esquinasCaminos;
    private Map<String, Point2D> puntosPorConexion;
    private Consumer<Integer> alSeleccionarEdificio;

    private Integer idOrigenSeleccionado = null;
    private Integer idDestinoSeleccionado = null;
    private int distanciaTotalRuta = 0;

        private List<Integer> rutaActual;
        private List<Point2D> puntosAnimacion;
        private double puntoX, puntoY;
        private int indiceDestinoRuta = 0;
        private AnimationTimer timerAnimacion;
        private double faseCaminata = 0;
        private boolean estaCaminando = false;
        private int direccionX = 1;
        private Integer idHover = null;

         public PanelMapa(double width, double height, Grafo campus,
                     Map<Integer, Point2D> coordenadas,
                     Map<String, Point2D> esquinasCaminos,
                     Map<String, Point2D> puntosPorConexion,
                     Consumer<Integer> alSeleccionarEdificio) {
        super(width, height);
        this.campus = campus;
        this.coordenadas = coordenadas;
        this.esquinasCaminos = esquinasCaminos;
        this.puntosPorConexion = puntosPorConexion;
        this.alSeleccionarEdificio = alSeleccionarEdificio;

        configurarEventosRaton();
        dibujar();
    }
public void actualizarEstadoSeleccion(Integer origen, Integer destino, int distancia) {
        this.idOrigenSeleccionado = origen;
        this.idDestinoSeleccionado = destino;
        this.distanciaTotalRuta = distancia;
        dibujar();
    }
        //para mover el pin de ubicacion de los techos
        private Point2D getCentroTecho(int id) {
            Point2D p = coordenadas.get(id);
            double px = p.getX();
            double py = p.getY();
            return switch (id) {
                case 0 ->
                    new Point2D(px + 20, py - 6);
                case 1 ->
                    new Point2D(px + 10, py - 8);
                case 2 ->
                    new Point2D(px + 65, py + 60);
                case 3 ->
                    new Point2D(px, py - 24);
                case 4, 5, 6 ->
                    new Point2D(px, py - 22);
                case 7 ->
                    new Point2D(px, py - 18);
                case 8 ->
                    new Point2D(px + 10, py - 50);
                case 9 ->
                    new Point2D(px, py - 18);
                case 10 ->
                    new Point2D(px, py - 12);
                case 11 ->
                    new Point2D(px - 2, py - 4);
                case 12 ->
                    new Point2D(px + 25, py + 5);
                case 13 ->
                    new Point2D(px, py + 5);
                case 14 ->
                    new Point2D(px, py - 60);
                case 15 ->
                    new Point2D(px, py - 20);
                case 16 ->
                    new Point2D(px - 10, py + 60);
                case 17, 20 ->
                    new Point2D(px - 15, py + 15);
                case 18 ->
                    new Point2D(px + 17, py + 8);
                case 19 ->
                    new Point2D(px + 20, py + 30);
                default ->
                    p;
            };
        }

        //para que ponga la manito
        private void configurarEventosRaton() {
            setOnMouseMoved(e -> {
                double mx = e.getX();
                double my = e.getY();
                Integer encontrado = null;
                for (Integer id : coordenadas.keySet()) {
                    Point2D puntaPin = getCentroTecho(id);
                    if (Math.hypot(mx - puntaPin.getX(), my - (puntaPin.getY() - 18)) <= 18) {
                        encontrado = id;
                        break;
                    }
                }
                if (!Objects.equals(idHover, encontrado)) {
                    idHover = encontrado;
                    setCursor(idHover != null ? Cursor.HAND : Cursor.DEFAULT);
                    dibujar();
                }
            });
            setOnMouseClicked(e -> {
                double mx = e.getX();
                double my = e.getY();
                for (Integer id : coordenadas.keySet()) {
                    Point2D puntaPin = getCentroTecho(id);
                    if (Math.hypot(mx - puntaPin.getX(), my - (puntaPin.getY() - 18)) <= 18) {
                        alSeleccionarEdificio.accept(id);
                        break;
                    }
                }
            });
        }

        public void limpiarRutaVisual() {
            if (timerAnimacion != null) {
                timerAnimacion.stop();
            }
            this.estaCaminando = false;
            this.rutaActual = null;
            dibujar();
        }

        private Point2D getPuntoConexion(int id) {
            Point2D p = coordenadas.get(id);
            return switch (id) {
                case 0 ->
                    new Point2D(p.getX() + 20, p.getY() - 5);
                case 1 ->
                    new Point2D(p.getX() + 0, p.getY() - 10);
                case 2 ->
                    new Point2D(p.getX() + 60, p.getY() + 50);
                case 3, 4, 5, 7 ->
                    new Point2D(p.getX() - 20, p.getY() - 10);
                case 8 ->
                    new Point2D(p.getX() + 25, p.getY() - 60);
                case 11 ->
                    new Point2D(p.getX() - 5, p.getY() + 30);
                case 12 ->
                    new Point2D(p.getX() + 20, p.getY() + 5);
                case 14 ->
                    new Point2D(p.getX() + 10, p.getY() - 60);
                default ->
                    p;
            };
        }
        private Point2D getPuntoConexion(int origen, int destino) {
            Point2D personalizado = puntosPorConexion.get(origen + "-" + destino);
            if (personalizado != null) {
                return personalizado;
            }
            return getPuntoConexion(origen); 
        }

        public void comenzarAnimacion(List nuevaRuta) {
            if (timerAnimacion != null) {
                timerAnimacion.stop();
            }
            this.rutaActual = nuevaRuta;
            this.indiceDestinoRuta = 1;
            this.estaCaminando = true;
            this.faseCaminata = 0;
            Point2D inicio = (rutaActual.size() > 1) 
                ? getPuntoConexion(rutaActual.get(0), rutaActual.get(1)) 
                : getPuntoConexion(rutaActual.get(0));
            puntoX = inicio.getX();
            puntoY = inicio.getY();
            this.puntosAnimacion = new ArrayList<>();
            for (int i = 0; i < nuevaRuta.size() - 1; i++) {
                int idActual = (int) nuevaRuta.get(i);
                int idSiguiente = (int) nuevaRuta.get(i + 1);
                
                puntosAnimacion.add(getPuntoConexion(idActual, idSiguiente));
                
                Point2D esquina = esquinasCaminos.get(idActual + "-" + idSiguiente);
                if (esquina != null) {
                    puntosAnimacion.add(esquina);
                }
                
                puntosAnimacion.add(getPuntoConexion(idSiguiente, idActual));
            }

            timerAnimacion = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (indiceDestinoRuta >= puntosAnimacion.size()) {
                        estaCaminando = false;
                        stop();
                        dibujar();
                        return;
                    }
                    Point2D target = puntosAnimacion.get(indiceDestinoRuta);
                    double dx = target.getX() - puntoX;
                    double dy = target.getY() - puntoY;
                    double distancia = Math.hypot(dx, dy);
                    if (Math.abs(dx) > 0.2) {
                        direccionX = (dx >= 0) ? 1 : -1;
                    }
                    double velocidad = 2.5;
                    faseCaminata += 0.24;
                    if (distancia <= velocidad) {
                        puntoX = target.getX();
                        puntoY = target.getY();
                        indiceDestinoRuta++;
                    } else {
                        puntoX += (dx / distancia) * velocidad;
                        puntoY += (dy / distancia) * velocidad;
                    }
                    dibujar();
                }
            };
            timerAnimacion.start();
        }

        public void dibujar() {
            GraphicsContext gc = getGraphicsContext2D();
            double w = getWidth();
            double h = getHeight();

            // 0. fondo
            gc.setFill(Color.web("#F4F6F0"));
            gc.fillRect(0, 0, w, h);
            gc.setStroke(Color.web("#E6EAE0"));
            gc.setLineWidth(1.0);
            for (double x = 0; x < w; x += 40) {
                gc.strokeLine(x, 0, x, h);
            }
            for (double y = 0; y < h; y += 40) {
                gc.strokeLine(0, y, w, y);
            }

            // area verdes
            gc.setFill(Color.web("#E3EDD8"));
            gc.fillRoundRect(340, 160, 260, 220, 40, 40);
            gc.fillRoundRect(200, 180, 170, 240, 30, 30);
            gc.fillRoundRect(450, 480, 250, 160, 35, 35);
            dibujarEncabezado(gc);

            // 1. Conexiones
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);
            gc.setLineWidth(13);
            gc.setStroke(Color.web("#CBD5E1"));
            trazarTodosLosCaminos(gc);
            gc.setLineWidth(9);
            gc.setStroke(Color.web("#FFFFFF"));
            trazarTodosLosCaminos(gc);

            // 2. Ruta activa
            if (rutaActual != null && !rutaActual.isEmpty()) {
                gc.setLineWidth(11);
                gc.setStroke(Color.rgb(37, 99, 235, 0.25));
                trazarRutaActiva(gc);
                gc.setLineWidth(5.5);
                gc.setStroke(Color.web("#6882C5"));
                trazarRutaActiva(gc);
                gc.setLineWidth(1.8);
                gc.setStroke(Color.rgb(255, 255, 255, 0.85));
                gc.setLineDashes(6, 8);
                trazarRutaActiva(gc);
                gc.setLineDashes(null);
            }
            gc.setLineWidth(1.5);

            // 3. Estructuras
            for (Map.Entry<Integer, Point2D> entry : coordenadas.entrySet()) {
                int id = entry.getKey();
                double px = entry.getValue().getX();
                double py = entry.getValue().getY();
                boolean esParteDeLaRuta = (rutaActual != null && rutaActual.contains(id));
                switch (id) {
                    case 0 ->
                        dibujarCaseta(gc, px + 10, py, 40, 20, esParteDeLaRuta, true);
                    case 1 ->
                        dibujarCaseta(gc, px, py, 40, 20, esParteDeLaRuta, false);
                    case 2 ->
                        dibujarEstacionamiento(gc, px, py + 50, esParteDeLaRuta);
                    case 3 ->
                        dibujarEdificio3D(gc, px, py, 60, 80, 15, 30, esParteDeLaRuta, "#D8DBED");
                    case 4 ->
                        dibujarEdificio3D(gc, px, py, 60, 80, 10, 28, esParteDeLaRuta, "#D8DBED");
                    case 5, 6 ->
                        dibujarEdificio3D(gc, px, py, 60, 80, 10, 28, esParteDeLaRuta, "#D8DBED");
                    case 7 ->
                        dibujarEdificio3D(gc, px, py, 65, 65, 8, 24, esParteDeLaRuta, "#D8DBED");
                    case 8 ->
                        dibujarEdificio3D(gc, px + 10, py - 45, 5, 5, 5, 5, esParteDeLaRuta, "#D8DBED");
                    case 9 ->
                        dibujarAuditorioPrincipal(gc, px, py, esParteDeLaRuta);
                    case 10 ->
                        dibujarEdificio3D(gc, px, py, 18, 18, 4, 16, esParteDeLaRuta, "#D8DBED");
                    case 11 ->
                        dibujarBiblioteca(gc, px, py, esParteDeLaRuta);
                    case 12 ->
                        dibujarCafeteria(gc, px, py, esParteDeLaRuta);
                    case 13 ->
                        dibujarPalapas(gc, px, py, esParteDeLaRuta);
                    case 14 ->
                        dibujarEdificio3D(gc, px, py - 20, 2, 2, 8, 0, esParteDeLaRuta, "#D8DBED");
                    case 15 ->
                        dibujarCabana(gc, px, py, esParteDeLaRuta);
                    case 16 ->
                        dibujarCanchaFutbol(gc, px, py);
                    case 17, 20 ->
                        dibujarCanchaBasket(gc, px, py);
                    case 19 ->
                        dibujarJardin(gc, px, py + 20);
                }
            }

            // 4. Botones de pin de ubicación 
            for (Integer id : coordenadas.keySet()) {
                if (id == 19) {
                    continue;
                }
                dibujarPinTecho(gc, id);
            }

            // 5. Modificar la direeccion de los nombres 
            for (Map.Entry<Integer, Point2D> entry : coordenadas.entrySet()) {
                int id = entry.getKey();
                double px = entry.getValue().getX();
                double py = entry.getValue().getY();
                String nombre = campus.getVertices().get(id).nombre;
                boolean esOrigen = (idOrigenSeleccionado != null && idOrigenSeleccionado == id);
                boolean esDestino = (idDestinoSeleccionado != null && idDestinoSeleccionado == id);
                double offsetX = 0, offsetY = 0;
                switch (id) {
                    case 0 -> {
                        offsetX = -35;
                        offsetY = -45;
                    }
                    case 1 -> {
                        offsetX = -30;
                        offsetY = -40;
                    }
                    case 2 -> {
                        offsetX = 160;
                        offsetY = 60;
                    }
                    case 3 -> {
                        offsetX = -40;
                        offsetY = 60;
                    }
                    case 4 -> {
                        offsetX = +50;
                        offsetY = -20;
                    }
                    case 5 -> {
                        offsetX = -30;
                        offsetY = 60;
                    }
                    case 6 -> {
                        offsetX = +40;
                        offsetY = -20;
                    }
                    case 7 -> {
                        offsetX = -170;
                        offsetY = -30;
                    }
                    case 8 -> {
                        offsetX = -140;
                        offsetY = -80;
                    }
                    case 9 -> {
                        offsetX = -95;
                        offsetY = +60;
                    }
                    case 10 -> {
                        offsetX = -25;
                        offsetY = 24;
                    }
                    case 11 -> {
                        offsetX = -35;
                        offsetY = 58;
                    }
                    case 12 -> {
                        offsetX = 10;
                        offsetY = 58;
                    }
                    case 13 -> {
                        offsetX = -25;
                        offsetY = 56;
                    }
                    case 14 -> {
                        offsetX = -170;
                        offsetY = -45;
                    }
                    case 15 -> {
                        offsetX = -150;
                        offsetY = -5;
                    }
                    case 16 -> {
                        offsetX = -52;
                        offsetY = +164;
                    }
                    case 17 -> {
                        offsetX = +30;
                        offsetY = 50;
                    }
                    case 19 -> {
                        offsetX = -5;
                        offsetY = 104;
                    }
                    case 20 -> {
                        offsetX = +40;
                        offsetY = +8;
                    }
                }
                dibujarEtiquetaPildora(gc, nombre, px + offsetX, py + offsetY, esOrigen, esDestino);
            }
            if (rutaActual != null && !rutaActual.isEmpty()) {
                dibujarMonitoCaminando(gc, puntoX, puntoY);
            }
            if (rutaActual != null && !rutaActual.isEmpty()) {
                dibujarTarjetaRuta(gc, h);
            }
        }

        private void dibujarPinTecho(GraphicsContext gc, int id) {
            Point2D punta = getCentroTecho(id);
            double tipX = punta.getX();
            double tipY = punta.getY();
            boolean esOrigen = (idOrigenSeleccionado != null && idOrigenSeleccionado == id);
            boolean esDestino = (idDestinoSeleccionado != null && idDestinoSeleccionado == id);
            boolean isHover = (idHover != null && idHover == id);
            double scale = isHover ? 1.15 : 1.0;
            double r = 11.5 * scale;
            double hTotal = 31.0 * scale;
            double centerY = tipY - (hTotal - r);
            gc.setFill(Color.rgb(15, 23, 42, 0.25));
            gc.fillOval(tipX - 7 * scale, tipY - 2.5 * scale, 14 * scale, 5 * scale);
            Color colorPin;
            if (esOrigen) {
                colorPin = Color.web("#5D6C96");
            } else if (esDestino) {
                colorPin = Color.web("#B44957");
            } else if (isHover) {
                colorPin = Color.web("#5D6C96");
            } else {
                colorPin = Color.web("#6892C5");
            }
            gc.beginPath();
            gc.moveTo(tipX, tipY);
            gc.bezierCurveTo(
                    tipX - r * 0.45, tipY - hTotal * 0.32,
                    tipX - r, centerY + r * 0.45,
                    tipX - r, centerY
            );
            gc.arc(tipX, centerY, r, r, 180, -180);
            gc.bezierCurveTo(
                    tipX + r, centerY + r * 0.45,
                    tipX + r * 0.45, tipY - hTotal * 0.32,
                    tipX, tipY
            );
            gc.closePath();
            gc.setFill(colorPin);
            gc.fill();
            gc.setStroke(Color.web("#1E293B", 0.45));
            gc.setLineWidth(1.2);
            gc.stroke();
            double holeR = r * 0.42;
            gc.setFill(Color.WHITE);
            gc.fillOval(tipX - holeR, centerY - holeR, holeR * 2, holeR * 2);
            gc.setStroke(Color.web("#1E293B", 0.3));
            gc.setLineWidth(0.9);
            gc.strokeOval(tipX - holeR, centerY - holeR, holeR * 2, holeR * 2);
        }

        private void dibujarMonitoCaminando(GraphicsContext gc, double x, double y) {
            double balanceo = estaCaminando ? Math.sin(faseCaminata) : 0;
            double reboteY = estaCaminando ? Math.abs(Math.cos(faseCaminata)) * 2.5 : 0;
            double baseY = y - reboteY;
            gc.setFill(Color.rgb(15, 23, 42, 0.28));
            gc.fillOval(x - 8, y - 3, 16, 6);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setStroke(Color.web("#F9D958"));
            gc.setLineWidth(3.2);
            double pie1X = x - (balanceo * 7 * direccionX);
            double pie1Y = y - Math.max(0, -balanceo * 2);
            gc.strokeLine(x, baseY - 11, pie1X, pie1Y);
            gc.setStroke(Color.web("#F9D958"));
            gc.setLineWidth(2.8);
            double mano1X = x + (balanceo * 6 * direccionX);
            double mano1Y = baseY - 12;
            gc.strokeLine(x, baseY - 20, mano1X, mano1Y);
            gc.setStroke(Color.web("#F9D958"));
            gc.setLineWidth(5.5);
            gc.strokeLine(x, baseY - 21, x, baseY - 12);
            gc.setStroke(Color.web("#F9D958"));
            gc.setLineWidth(3.2);
            double pie2X = x + (balanceo * 7 * direccionX);
            double pie2Y = y - Math.max(0, balanceo * 2);
            gc.strokeLine(x, baseY - 11, pie2X, pie2Y);
            gc.setStroke(Color.web("#F9D958"));
            gc.setLineWidth(2.8);
            double mano2X = x - (balanceo * 6 * direccionX);
            double mano2Y = baseY - 12;
            gc.strokeLine(x, baseY - 20, mano2X, mano2Y);
            gc.setFill(Color.web("#F9D958"));
            gc.fillOval(x - 5, baseY - 32, 10, 10);
            gc.setStroke(Color.web("#F9D958"));
            gc.setLineWidth(1.2);
            gc.strokeOval(x - 5, baseY - 32, 10, 10);
            gc.setFill(Color.web("#F9D958"));
            if (direccionX >= 0) {
                gc.fillArc(x - 5, baseY - 33, 11, 8, 0, 180, ArcType.ROUND);
            } else {
                gc.fillArc(x - 6, baseY - 33, 11, 8, 0, 180, ArcType.ROUND);
            }
        }

        private void trazarTodosLosCaminos(GraphicsContext gc) {
            for (int i = 0; i < campus.getListaA().size(); i++) {
               
                for (Arista a : campus.getListaA().get(i)) {
                     Point2D p1 = getPuntoConexion(i, a.destino);
                    Point2D p2 = getPuntoConexion(a.destino,i);
                    Point2D esquina = esquinasCaminos.get(i + "-" + a.destino);
                    if (esquina != null) {
                        gc.strokeLine(p1.getX(), p1.getY(), esquina.getX(), esquina.getY());
                        gc.strokeLine(esquina.getX(), esquina.getY(), p2.getX(), p2.getY());
                    } else {
                        gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    }
                }
            }
        }

        private void trazarRutaActiva(GraphicsContext gc) {
            for (int i = 0; i < rutaActual.size() - 1; i++) {
                int id1 = rutaActual.get(i);
                int id2 = rutaActual.get(i + 1);
                Point2D p1 = getPuntoConexion(id1,id2);
                Point2D p2 = getPuntoConexion(id2,id1);
                Point2D esquina = esquinasCaminos.get(id1 + "-" + id2);
                if (esquina != null) {
                    gc.strokeLine(p1.getX(), p1.getY(), esquina.getX(), esquina.getY());
                    gc.strokeLine(esquina.getX(), esquina.getY(), p2.getX(), p2.getY());
                } else {
                    gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                }
            }
        }

        private void dibujarEncabezado(GraphicsContext gc) {
            gc.setFill(Color.rgb(255, 255, 255, 0.94));
            gc.fillRoundRect(25, 20, 245, 62, 12, 12);
            gc.setStroke(Color.web("#CBD5E1"));
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(25, 20, 245, 62, 12, 12);

            gc.setFill(Color.web("#0F172A"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
            gc.fillText("GPS ITSUR", 82, 47);
            gc.setFill(Color.web("#64748B"));
            gc.setFont(Font.font("Segoe UI", 11));
            gc.fillText("Toca los pines para trazar ruta", 82, 65);
        }

        private void dibujarCaseta(GraphicsContext gc, double px, double py, double w, double h, boolean activa, boolean principal) {
            double h3D = principal ? 14 : 11;
            double x = px - 10;
            double y = py - 10;

            gc.setFill(Color.rgb(15, 23, 42, 0.22));
            gc.fillRoundRect(x + 4, y + 5, w, h, 6, 6);

            Color baseMuro = Color.web("#AAABAE");
            Color colorMuro = activa ? baseMuro.brighter() : baseMuro;
            gc.setFill(colorMuro);
            gc.fillRoundRect(x, y, w, h, 5, 5);
            gc.setStroke(Color.web("#1E293B"));
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(x, y, w, h, 5, 5);

            gc.setFill(Color.web("#7C7C7C"));
            gc.fillRect(x + 5, y + h - 7, w - 10, 4.5);

            double techoX = x - 3;
            double techoY = y - h3D;
            double techoW = w + 6;
            double techoH = h + 2;

            Color baseTecho = Color.web("#C7C9CD");
            Color colorTecho = activa ? baseTecho.brighter() : baseTecho;
            gc.setFill(colorTecho);
            gc.fillRoundRect(techoX, techoY, techoW, techoH, 6, 6);

            gc.setStroke(Color.web("#334155"));
            gc.setLineWidth(1.3);
            gc.strokeRoundRect(techoX, techoY, techoW, techoH, 6, 6);
        }

        private void dibujarEdificio3D(GraphicsContext gc, double px, double py, double ancho, double alto, double inc, double h3D, boolean activa, String colorHex) {
            Color colorTecho = activa ? Color.web("#A6ADD1") : Color.web(colorHex);
            Color paredFrontal = colorTecho.darker();
            Color paredLateral = colorTecho.darker().darker();
            double b1x = px - ancho / 2 + inc, b1y = py - alto / 2;
            double b2x = px + ancho / 2 + inc, b2y = py - alto / 2;
            double b3x = px + ancho / 2 - inc, b3y = py + alto / 2;
            double b4x = px - ancho / 2 - inc, b4y = py + alto / 2;
            gc.setFill(Color.rgb(15, 23, 42, 0.16));
            gc.fillPolygon(
                    new double[]{b1x + 7, b2x + 7, b3x + 7, b4x + 7},
                    new double[]{b1y + 7, b2y + 7, b3y + 7, b4y + 7}, 4);
            gc.setFill(paredFrontal);
            double[] pFx = {b4x, b3x, b3x, b4x};
            double[] pFy = {b4y, b3y, b3y - h3D, b4y - h3D};
            gc.fillPolygon(pFx, pFy, 4);
            gc.setFill(paredLateral);
            double[] pDx = {b3x, b2x, b2x, b3x};
            double[] pDy = {b3y, b2y, b2y - h3D, b3y - h3D};
            gc.fillPolygon(pDx, pDy, 4);
            gc.setFill(colorTecho);
            double[] tX = {b1x, b2x, b3x, b4x};
            double[] tY = {b1y - h3D, b2y - h3D, b3y - h3D, b4y - h3D};
            gc.fillPolygon(tX, tY, 4);
            gc.setStroke(colorTecho.brighter());
            gc.setLineWidth(1.0);
            gc.strokePolygon(
                    new double[]{b1x + 4, b2x - 4, b3x - 4, b4x + 4},
                    new double[]{b1y - h3D + 4, b2y - h3D + 4, b3y - h3D - 4, b4y - h3D - 4}, 4);
            gc.setStroke(Color.rgb(30, 41, 59, 0.65));
            gc.setLineWidth(1.2);
            gc.strokePolygon(tX, tY, 4);
            gc.strokePolygon(pFx, pFy, 4);
            gc.strokePolygon(pDx, pDy, 4);
        }

        private void dibujarEstacionamiento(GraphicsContext gc, double px, double py, boolean activa) {
            double escala = 0.95;

            gc.save(); 
            gc.translate(px, py); 
            gc.scale(escala, escala);
            gc.translate(-px, -py); 

            gc.setFill(Color.rgb(15, 23, 42, 0.15));
            gc.fillRoundRect(px - 26, py - 36, 190, 100, 10, 10);
            gc.setFill(Color.web("#ABABAB"));
            gc.fillRoundRect(px - 30, py - 40, 190, 100, 10, 10);

            if (activa) {
                gc.setStroke(Color.web("#585858"));
                gc.setLineWidth(3.5);
                gc.strokeRoundRect(px - 31, py - 41, 192, 102, 10, 10);
            } else {
                gc.setStroke(Color.web("#64748B"));
                gc.setLineWidth(1.5);
                gc.strokeRoundRect(px - 30, py - 40, 190, 100, 10, 10);
            }

            double[] ejesJardineras = {px + 33, px + 98};
            for (double ejeX : ejesJardineras) {
                gc.setFill(Color.web("#7DA376"));
                gc.fillRoundRect(ejeX - 5, py - 30, 10, 80, 4, 4);
                gc.setFill(Color.web("#A1C49B"));
                for (double posY = py - 24; posY <= py + 42; posY += 16) {
                    gc.fillOval(ejeX - 3, posY, 6, 6);
                }
            }

            gc.setStroke(Color.rgb(255, 255, 255, 0.7));
            gc.setLineWidth(2);
            for (double y = py - 35; y <= py + 45; y += 15) {
                gc.strokeLine(px - 28, y, px - 12, y);
                gc.strokeLine(px + 15, y, px + 28, y);
                gc.strokeLine(px + 38, y, px + 51, y);
                gc.strokeLine(px + 80, y, px + 93, y);
                gc.strokeLine(px + 103, y, px + 116, y);
                gc.strokeLine(px + 142, y, px + 158, y);
            }
          
            gc.restore();
        }

        private void dibujarBiblioteca(GraphicsContext gc, double px, double py, boolean activa) {
            double escala = 0.75;

            gc.save();
            gc.translate(px, py);
            gc.scale(escala, escala);
            gc.translate(-px, -py);

            double aB = 70;
            double alB = 90;
            double h3D = 28;
            double xB = px - 41;
            double yB = py - 32;

            gc.setFill(Color.rgb(15, 23, 42, 0.20));
            gc.fillRect(xB + 7, yB + 7, aB, alB);

            Color colorBase = activa ? Color.web("#304E66") : Color.web("#D8DBED");
            Color paredFrontal = colorBase.darker();

            gc.setFill(paredFrontal);
            double[] pFx = {xB, xB + aB, xB + aB, xB};
            double[] pFy = {yB + alB, yB + alB, yB + alB - h3D, yB + alB - h3D};
            gc.fillPolygon(pFx, pFy, 4);

            double techoY = yB - h3D;
            gc.setFill(colorBase);
            gc.fillRect(xB, techoY, aB, alB);

            gc.setStroke(colorBase.brighter().brighter());
            gc.setLineWidth(1.2);
            gc.strokeRect(xB + 4, techoY + 4, aB - 8, alB - 8);

            double domoW = 28;
            double domoH = 44;
            double domoX = xB + (aB - domoW) / 2.0;
            double domoY = techoY + (alB - domoH) / 2.0;

            gc.setFill(Color.web("#E0F2FE", 0.92));
            gc.fillRect(domoX, domoY, domoW, domoH);
            gc.setStroke(Color.web("#0284C7"));
            gc.setLineWidth(1.2);
            gc.strokeRect(domoX, domoY, domoW, domoH);

            gc.setStroke(Color.rgb(15, 23, 42, 0.75));
            gc.setLineWidth(1.2);
            gc.strokeRect(xB, techoY, aB, alB);
            gc.strokePolygon(pFx, pFy, 4);

            gc.restore();
        }

        private void dibujarCabana(GraphicsContext gc, double px, double py, boolean activa) {
            double ancho = 30;
            double alto = 22;
            double hPared = 10;
            double hTecho = 8;

            double xIzq = px - ancho / 2;
            double xDer = px + ancho / 2;
            double ySup = py - alto / 2;
            double yInf = py + alto / 2;

            Color colorBase = Color.web("#EDE1D8");
            gc.setFill(Color.rgb(15, 23, 42, 0.22));
            gc.fillRoundRect(xIzq + 5, ySup + 5, ancho, alto + 4, 6, 6);

            gc.setFill(colorBase.darker());
            gc.fillRect(xIzq + 4, yInf - 2, ancho - 8, 6);
            gc.setStroke(Color.web("#451A03"));
            gc.setLineWidth(1.0);
            gc.strokeRect(xIzq + 4, yInf - 2, ancho - 8, 6);

            Color colorMadera = activa ? colorBase.brighter() : colorBase;
            gc.setFill(colorMadera);
            gc.fillRect(xIzq, yInf - hPared, ancho, hPared);

            gc.setStroke(Color.web("#451A03", 0.45));
            gc.setLineWidth(1.0);
            for (double yTronco = yInf - hPared + 3.5; yTronco < yInf; yTronco += 3.5) {
                gc.strokeLine(xIzq, yTronco, xDer, yTronco);
            }

            gc.setFill(Color.web("#451A03"));
            gc.fillRect(px - 4, yInf - 10, 8, 10);
            gc.setFill(Color.web("#FFFFFF"));
            gc.fillRect(xIzq + 7, yInf - 9, 6, 5);
            gc.setStroke(Color.web("#451A03"));
            gc.strokeRect(xIzq + 7, yInf - 9, 6, 5);

            double yAleroInf = yInf - hPared;
            double yAleroSup = ySup - hPared;
            double yCentro = (yAleroInf + yAleroSup) / 2.0 - hTecho;

            Color techoFrente = activa ? colorBase : colorBase.darker();
            Color techoAtras = activa ? colorBase.darker() : colorBase.darker().darker();

            gc.setFill(techoAtras);
            gc.fillPolygon(
                    new double[]{xIzq - 3, xDer + 3, xDer + 3, xIzq - 3},
                    new double[]{yAleroSup, yAleroSup, yCentro, yCentro}, 4);

            gc.setFill(techoFrente);
            gc.fillPolygon(
                    new double[]{xIzq - 3, xDer + 3, xDer + 3, xIzq - 3},
                    new double[]{yCentro, yCentro, yAleroInf, yAleroInf}, 4);

            gc.setStroke(Color.web("#451A03"));
            gc.setLineWidth(1.2);
            gc.strokePolygon(
                    new double[]{xIzq - 3, xDer + 3, xDer + 3, xIzq - 3},
                    new double[]{yAleroSup, yAleroSup, yAleroInf, yAleroInf}, 4);

            double chimX = xDer - 11;
            double chimY = yCentro - 5;
            gc.setFill(Color.web("#64748B"));
            gc.fillRect(chimX, chimY, 6, 9);
            gc.setStroke(Color.web("#1E293B"));
            gc.setLineWidth(1.0);
            gc.strokeRect(chimX, chimY, 6, 9);
        }

        private void dibujarCafeteria(GraphicsContext gc, double px, double py, boolean activa) {
            double aC = 75;
            double alC = 80;
            double h3D = 12;
            double arcX = px - aC / 3.0;
            double arcY = py - alC / 2.0;

            gc.setFill(Color.rgb(15, 23, 42, 0.20));
            gc.fillArc(arcX + 6, arcY + 7, aC, alC, 225, 160, ArcType.ROUND);

            Color colorBase = Color.web("#EDE1D8");
            Color colorTecho = activa ? colorBase.brighter() : colorBase;
            Color colorMuro = colorTecho.darker().darker();

            gc.setFill(colorMuro);
            gc.fillArc(arcX, arcY, aC, alC, 225, 160, ArcType.ROUND);

            double techoY = arcY - h3D;
            gc.setFill(colorTecho);
            gc.fillArc(arcX, techoY, aC, alC, 225, 160, ArcType.ROUND);

            gc.setStroke(colorTecho.brighter());
            gc.setLineWidth(1.3);
            gc.strokeArc(arcX + 4, techoY + 4, aC - 8, alC - 8, 228, 154, ArcType.ROUND);

            double centroArcX = arcX + aC / 2.0;
            double centroArcY = techoY + alC / 2.0;
            double radioX = aC / 2.0 - 3;
            double radioY = alC / 2.0 - 3;
            gc.setStroke(Color.rgb(255, 255, 255, 0.55));
            gc.setLineWidth(1.1);
            for (int ang = 240; ang <= 370; ang += 22) {
                double rad = Math.toRadians(ang);
                double endX = centroArcX + Math.cos(rad) * radioX;
                double endY = centroArcY - Math.sin(rad) * radioY;
                gc.strokeLine(centroArcX, centroArcY, endX, endY);
            }

            gc.setStroke(Color.web("#451A03"));
            gc.setLineWidth(1.5);
            gc.strokeArc(arcX, techoY, aC, alC, 225, 160, ArcType.ROUND);
        }

        private void dibujarPalapas(GraphicsContext gc, double px, double py, boolean activa) {
            double anchoM = 30, altoM = 30, hTechoM = 7;
            gc.setFill(Color.web("#CBD5E1"));
            gc.fillRoundRect(px - 6, py - 18, 12, 60, 4, 4);
            for (int m = 0; m < 2; m++) {
                double curY = py - 15 + (m * 38);
                double xIzq = px - anchoM / 2, xDer = px + anchoM / 2;
                double ySup = curY - altoM / 2, yInf = curY + altoM / 2;
                double cXIzq = px - 10, cXDer = px + 10, cYC = curY - hTechoM;
                gc.setFill(Color.rgb(15, 23, 42, 0.14));
                gc.fillRect(xIzq + 5, ySup + 5, anchoM, altoM);
                Color vSombra = activa ? Color.web("#585858") : Color.web("#7C7C7C");
                Color vMedio = activa ? Color.web("#878787") : Color.web("#ADADAD");
                Color vLuz = activa ? Color.web("#AAABAE") : Color.web("#C7C9CD");
                gc.setFill(vSombra);
                gc.fillPolygon(new double[]{xIzq, xIzq, cXIzq}, new double[]{ySup, yInf, cYC}, 3);
                gc.fillPolygon(new double[]{xIzq, xDer, cXDer, cXIzq}, new double[]{ySup, ySup, cYC, cYC}, 4);
                gc.setFill(vMedio);
                gc.fillPolygon(new double[]{xIzq, xDer, cXDer, cXIzq}, new double[]{yInf, yInf, cYC, cYC}, 4);
                gc.setFill(vLuz);
                gc.fillPolygon(new double[]{xDer, xDer, cXDer}, new double[]{ySup, yInf, cYC}, 3);
            }
        }

        private void dibujarAuditorioPrincipal(GraphicsContext gc, double px, double py, boolean activa) {
            double ancho = 70;
            double alto = 80;
            double inc = 8;
            double hSala = 28; 

            double b1x = px - ancho / 2 + inc, b1y = py - alto / 2;
            double b2x = px + ancho / 2 + inc, b2y = py - alto / 2;
            double b3x = px + ancho / 2 - inc, b3y = py + alto / 2;
            double b4x = px - ancho / 2 - inc, b4y = py + alto / 2;

            gc.setFill(Color.rgb(15, 23, 42, 0.25));
            gc.fillPolygon(
                    new double[]{b1x + 8, b2x + 8, b3x + 8, b4x + 8},
                    new double[]{b1y + 8, b2y + 8, b3y + 8, b4y + 8}, 4);

            Color colorBase = activa ? Color.web("#9CA3AF") : Color.web("#D8DBED");
            Color paredFrontal = colorBase.darker();
            Color paredLateral = colorBase.darker().darker();

            gc.setFill(paredLateral);
            gc.fillPolygon(
                    new double[]{b2x, b3x, b3x, b2x},
                    new double[]{b2y, b3y, b3y - hSala, b2y - hSala}, 4);

            gc.setFill(paredFrontal);
            double[] pFx = {b4x, b3x, b3x, b4x};
            double[] pFy = {b4y, b3y, b3y - hSala, b4y - hSala};
            gc.fillPolygon(pFx, pFy, 4);

            gc.setFill(colorBase);
            double[] tSalaX = {b1x, b2x, b3x, b4x};
            double[] tSalaY = {b1y - hSala, b2y - hSala, b3y - hSala, b4y - hSala};
            gc.fillPolygon(tSalaX, tSalaY, 4);

            gc.setStroke(Color.rgb(255, 255, 255, 0.30));
            gc.setLineWidth(1.2);
            for (int r = 1; r <= 3; r++) {
                double frac = r / 4.0;
                double lx1 = b1x + (b2x - b1x) * frac;
                double lx2 = b4x + (b3x - b4x) * frac;
                gc.strokeLine(lx1, b1y - hSala + 2, lx2, b4y - hSala - 2);
            }

            gc.setStroke(Color.rgb(17, 24, 39, 0.8));
            gc.setLineWidth(1.2);
            gc.strokePolygon(tSalaX, tSalaY, 4);
            gc.strokePolygon(pFx, pFy, 4);
        }

        private void dibujarCanchaFutbol(GraphicsContext gc, double px, double py) {
            double escala = 0.9;

            gc.save(); 
            gc.translate(px, py); 
            gc.scale(escala, escala); 
            gc.translate(-px, -py); 

            gc.setFill(Color.web("#6D9865"));
            gc.fillRoundRect(px - 75, py - 50, 130, 220, 12, 12);

            gc.setFill(Color.web("#A1C49B"));
            gc.fillRect(px - 70, py - 45, 120, 210);

            gc.setStroke(Color.rgb(255, 255, 255, 0.9));
            gc.setLineWidth(1.5);
            gc.strokeRect(px - 70, py - 45, 120, 210);
            gc.strokeRect(px - 30, py - 45, 40, 25);
            gc.strokeRect(px - 30, py + 140, 40, 25);
            gc.strokeLine(px - 70, py + 60, px + 50, py + 60);
            gc.strokeOval(px - 20, py + 50, 20, 20);
            gc.restore(); 
        }

        private void dibujarCanchaBasket(GraphicsContext gc, double px, double py) {
            gc.setFill(Color.rgb(15, 23, 42, 0.14));
            gc.fillRoundRect(px - 42, py - 27, 60, 90, 8, 8);
            gc.setFill(Color.web("#CBD5E1"));
            gc.fillRoundRect(px - 45, py - 30, 60, 90, 8, 8);
            gc.setFill(Color.web("#7683A8"));
            gc.fillRect(px - 25, py - 30, 20, 20);
            gc.fillRect(px - 25, py + 40, 20, 20);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.6);
            gc.strokeRoundRect(px - 45, py - 30, 60, 90, 8, 8);
            gc.strokeLine(px - 45, py + 15, px + 15, py + 15);
            gc.strokeOval(px - 25, py + 5, 20, 20);
        }

        private void dibujarJardin(GraphicsContext gc, double px, double py) {
            double escala = 0.9;

            gc.save(); 
            gc.translate(px, py); 
            gc.scale(escala, escala); 
            gc.translate(-px, -py); 

            gc.setFill(Color.web("#A1C49B"));
            gc.fillRoundRect(px - 30, py - 30, 100, 110, 1, 1);

            double[][] arboles = {
                {px - 12, py - 12},
                {px + 45, py - 15},
                {px + 15, py + 25},
                {px - 10, py + 65},
                {px + 50, py + 60}
            };

            for (double[] pos : arboles) {
                gc.setFill(Color.rgb(15, 23, 42, 0.15));
                gc.fillOval(pos[0] - 5, pos[1] - 3, 20, 20);
                gc.setFill(Color.web("#507E47"));
                gc.fillOval(pos[0] - 8, pos[1] - 8, 18, 18);
                gc.setFill(Color.web("#85AD7E"));
                gc.fillOval(pos[0] - 6, pos[1] - 10, 14, 14);
            }

            gc.setFill(Color.web("#F8FAFC"));
            gc.fillRect(px + 70, py - 30, 25, 110);

            gc.setStroke(Color.web("#94A3B8"));
            gc.strokeRoundRect(px - 30, py - 30, 125, 110, 12, 12);

            gc.restore(); 
        }

        private void dibujarEtiquetaPildora(GraphicsContext gc, String texto, double x, double y, boolean esOrigen, boolean esDestino) {
            Font fuente = Font.font("Segoe UI", FontWeight.BOLD, 10.5);
            gc.setFont(fuente);
            Text textNode = new Text(texto);
            textNode.setFont(fuente);
            double textWidth = textNode.getLayoutBounds().getWidth();
            double padX = 7;
            double boxW = textWidth + padX * 2;
            double boxH = 18;
            gc.setFill(Color.rgb(15, 23, 42, 0.12));
            gc.fillRoundRect(x - padX + 1, y - 12 + 2, boxW, boxH, 9, 9);
            if (esOrigen) {
                gc.setFill(Color.web("#6882C5"));
            } else if (esDestino) {
                gc.setFill(Color.web("#B44957"));
            } else {
                gc.setFill(Color.rgb(255, 255, 255, 0.94));
            }
            gc.fillRoundRect(x - padX, y - 12, boxW, boxH, 9, 9);
            gc.setStroke((esOrigen || esDestino) ? Color.WHITE : Color.web("#CBD5E1"));
            gc.setLineWidth(1.0);
            gc.strokeRoundRect(x - padX, y - 12, boxW, boxH, 9, 9);
            gc.setFill((esOrigen || esDestino) ? Color.WHITE : Color.web("#1E293B"));
            gc.fillText(texto, x, y);
        }

        private void dibujarTarjetaRuta(GraphicsContext gc, double h) {
            double xPanel = 25, yPanel = h - 135, anchoPanel = 430, altoPanel = 110;
            gc.setFill(Color.rgb(15, 23, 42, 0.14));
            gc.fillRoundRect(xPanel + 3, yPanel + 4, anchoPanel, altoPanel, 16, 16);
            gc.setFill(Color.rgb(255, 255, 255, 0.96));
            gc.fillRoundRect(xPanel, yPanel, anchoPanel, altoPanel, 16, 16);
            gc.setFill(Color.web("#2563EB"));
            gc.fillRoundRect(xPanel, yPanel, 6, altoPanel, 6, 6);
            gc.setStroke(Color.web("#CBD5E1"));
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(xPanel, yPanel, anchoPanel, altoPanel, 16, 16);
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            gc.setFill(Color.web("#0F172A"));
            gc.fillText("Distancia total: " + distanciaTotalRuta + " metros", xPanel + 22, yPanel + 32);
            int segundos = (int) Math.ceil(distanciaTotalRuta / 1.3);
            String tiempoTexto = segundos < 60 ? segundos + " seg caminando" : (segundos / 60) + " min " + (segundos % 60) + " seg caminando";
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11.5));
            gc.setFill(Color.web("#16A34A"));
            gc.fillText("⏱ ~" + tiempoTexto, xPanel + 275, yPanel + 31);
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            gc.setFill(Color.web("#64748B"));
            gc.fillText("RECORRIDO SUGERIDO:", xPanel + 22, yPanel + 58);
            StringBuilder sbCamino = new StringBuilder();
            for (int i = 0; i < rutaActual.size(); i++) {
                String nombreCorto = campus.getVertices().get(rutaActual.get(i)).nombre;
                if (nombreCorto.contains("Caseta")) {
                    nombreCorto = "Caseta";
                }
                if (nombreCorto.contains("Edificio")) {
                    nombreCorto = nombreCorto.replace("Edificio de ", "Edif. ").replace("Edificio ", "Edif. ");
                }
                sbCamino.append(nombreCorto);
                if (i < rutaActual.size() - 1) {
                    sbCamino.append("  ➔  ");
                }
            }
            gc.setFont(Font.font("Segoe UI", 12));
            gc.setFill(Color.web("#334155"));
            String textoRuta = sbCamino.toString();
            if (textoRuta.length() > 62) {
                gc.fillText(textoRuta.substring(0, 59) + "...", xPanel + 22, yPanel + 82);
            } else {
                gc.fillText(textoRuta, xPanel + 22, yPanel + 82);
            }
        }
    }
    