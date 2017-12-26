package net.peku33.freeturilo.gui;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointRenderer;

/**
 * Klasa wspomagająca renderowanie punktów na trasie przy użyciu podanego obrazka
 * @author peku33
 *
 */
public class ImageWaypointRenderer implements WaypointRenderer<Waypoint> {
	
	// Uchwyt na obrazek
	private BufferedImage bufferedImage;
	
	/**
	 * Konstruktor
	 * @param bufferedImage obrazek służący jako ikona, wyrównany do dołu do środka.
	 */
	public ImageWaypointRenderer(BufferedImage bufferedImage) {
		this.bufferedImage = bufferedImage;
	}

	@Override
	public void paintWaypoint(Graphics2D g, JXMapViewer map, Waypoint waypoint) {
		Point2D point2d = map.getTileFactory().geoToPixel(waypoint.getPosition(), map.getZoom());
		g.drawImage(
			bufferedImage,
			(int) (point2d.getX() - bufferedImage.getWidth() / 2),
			(int) (point2d.getY() - bufferedImage.getHeight()),
			null
		);
	}

}
