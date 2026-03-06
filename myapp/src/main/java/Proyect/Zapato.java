package Proyect;

public class Zapato {
    private String modelo;
    private double precio;
    private String color;

    public Zapato(String modelo, double precio, String color) {
        this.modelo = modelo;
        this.precio = precio;
        this.color = color;
    }
    // Getters
    public String getModelo() { return modelo; }
    public double getPrecio() { return precio; }
    public String getColor() { return color; }
}