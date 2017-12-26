package net.peku33.freeturilo.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.LinkedList;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import net.peku33.freeturilo.core.GeoPoint;

/**
 * Klasa wspomagająca rysowanie ścieżki
 * @author peku33
 *
 */
public class RoutePainter implements Painter<JXMapViewer> {
	
	private Color color;
	private BasicStroke stroke;
	
	private LinkedList<GeoPoint> route;
	public RoutePainter(Color color, BasicStroke stroke) {
		this.color = color;
		this.stroke = stroke;
		
		route = new LinkedList<>();
	}
	
	/**
	 * Ustawia punkty ścieżki
	 * @param route
	 */
	public void setRoute(Collection<GeoPoint> route) {
		this.route.clear();
		this.route.addAll(route);
	}
	
	/**
	 * Czyści punkty ścieżki
	 */
	public void clear() {
		this.route.clear();
	}

	@Override
	public void paint(Graphics2D graphics, JXMapViewer map, int width, int height) {
		
		if(route.isEmpty())
			return;
		
		// Duplikujemy obiekt grafiki
		graphics = (Graphics2D) graphics.create();
		
		// Ustawienia globalne
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// Zmiana na bitmapę, translacja
		Rectangle rectangle = map.getViewportBounds();
		graphics.translate(-rectangle.x, -rectangle.y);
		
		// Ustawienia wyglądu 2
		graphics.setColor(Color.black);
		graphics.setStroke(new BasicStroke(stroke.getLineWidth() * 1.5f));
		
		drawLines(graphics, map);
		
		// Ustawienia wyglądu 1
		graphics.setColor(color);
		graphics.setStroke(stroke);
		
		drawLines(graphics, map);
		
		// Czyszczenie
		graphics.dispose();
	}
	
	private void drawLines(Graphics2D graphics, JXMapViewer map) {
		GeoPoint geoPointPrevious = null;
		for(GeoPoint geoPoint : route) {
			if(geoPointPrevious != null) {
				
				Point2D pointPrevious = map.getTileFactory().geoToPixel(new GeoPosition(geoPointPrevious.getLatitude(), geoPointPrevious.getLongitude()), map.getZoom());
				Point2D pointThis = map.getTileFactory().geoToPixel(new GeoPosition(geoPoint.getLatitude(), geoPoint.getLongitude()), map.getZoom());
				
				graphics.drawLine((int) pointPrevious.getX(), (int) pointPrevious.getY(), (int) pointThis.getX(), (int) pointThis.getY());
			}
			geoPointPrevious = geoPoint;
		}
	}
}
