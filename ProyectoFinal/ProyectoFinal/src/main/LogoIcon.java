package main;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class LogoIcon implements Icon {
    private final int width;
    private final int height;

    public LogoIcon(int size) {
        this.width = size;
        this.height = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();

        // Activa el antialiasing para que el texto se vea suave
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dibuja el fondo rojo
        g2d.setColor(new Color(204, 0, 0)); // Un rojo fuerte
        g2d.fillRect(x, y, width, height);

        // Configura la fuente para el texto
        g2d.setFont(new Font("Segoe UI", Font.BOLD, height / 2));
        g2d.setColor(Color.WHITE);

        // Dibuja el texto "UD"
        String texto1 = "UD";
        int strWidth1 = g2d.getFontMetrics().stringWidth(texto1);
        g2d.drawString(texto1, x + (width - strWidth1) / 2, y + (height / 2) - 5);
        
        // Dibuja el texto "FORO"
        g2d.setFont(new Font("Segoe UI", Font.BOLD, height / 3));
        String texto2 = "FORO";
        int strWidth2 = g2d.getFontMetrics().stringWidth(texto2);
        g2d.drawString(texto2, x + (width - strWidth2) / 2, y + (height - 10));

        g2d.dispose();
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}