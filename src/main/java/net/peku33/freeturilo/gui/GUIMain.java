package net.peku33.freeturilo.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.MouseInputListener;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.MapClickListener;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointPainter;

import net.peku33.freeturilo.core.FreeTuriloRouter;
import net.peku33.freeturilo.core.FreeTuriloRouter.OpeningClosing;
import net.peku33.freeturilo.core.GeoPoint;
import net.peku33.freeturilo.core.Path;
import net.peku33.freeturilo.core.VeturiloStop;
import net.peku33.freeturilo.core.VeturiloStops;

public class GUIMain {
	
	private static final GeoPosition initialGeoPosition = new GeoPosition(52.21318446486077, 20.996229566778524);
	private static final int initialZoom = 7;
	
	private static final int openingClosingTextBoxColumns = 25;
	
	public GUIMain() throws Exception {
		
		initializeLoadingDialog();
		initializeLayout();
		
		// Załaduj listę stacji w tle
		SwingWorker<Void, Void> freeTuriloRouterloader = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				FreeTuriloRouter.getInstanceTS();
				return null;
			}
			
			@Override
			protected void done() {
				hideLoadingDialog();
			}
		};
		
		freeTuriloRouterloader.execute();
		showLoadingDialog();
	}
	
	private JDialog progreessBarDialog;
	private void initializeLoadingDialog() {
		progreessBarDialog = new JDialog(mainFrame, "Loading...", true);
		progreessBarDialog.setLocationRelativeTo(mainFrame);
		progreessBarDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		progreessBarDialog.setResizable(false);
		
		JProgressBar progressBar = new JProgressBar();
		progressBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		progreessBarDialog.add(BorderLayout.CENTER, progressBar);
		progressBar.setIndeterminate(true);
		
		progreessBarDialog.pack();
	}
	private void showLoadingDialog() {
		progreessBarDialog.setVisible(true);
	}
	private void hideLoadingDialog() {
		progreessBarDialog.setVisible(false);
	}
	
	private JPanel mapTipsPanel;
	private JPanel controlsPanel;
	private JFrame mainFrame;
	private void initializeLayout() throws Exception {
		
		mainFrame = new JFrame("FreeTurilo");
		mainFrame.setMinimumSize(new Dimension(640, 480));
		mainFrame.setExtendedState(mainFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		mainFrame.setLayout(new BoxLayout(mainFrame.getContentPane(), BoxLayout.Y_AXIS));
		
		mapTipsPanel = new JPanel();
		mapTipsPanel.setLayout(new BoxLayout(mapTipsPanel, BoxLayout.X_AXIS));
		
		controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
		
		initializeJxMapViewer();
		initializeInstructionsPanel();
		initializeControls();
		
		mainFrame.add(mapTipsPanel);
		mainFrame.add(controlsPanel);
		
		mainFrame.setVisible(true);
	}
	
	private JXMapViewer jxMapViewer;
	
	// Stacja początkowa
	private WaypointPainter<Waypoint> openingWaypointPainter;
	
	// Stacja końcowa
	private WaypointPainter<Waypoint> closingWaypointPainter;
	
	// Stacje na trasie
	private WaypointPainter<Waypoint> veturiloStopsWaypointPainter;
	
	// Wszystkie stacje na mapie
	private WaypointPainter<Waypoint> allVeturiloStopsWaypointPainter;
	

	// Trasa z buta do stacji
	private RoutePainter openingPathPainter;
	
	// Trasa od stacji z buta
	private RoutePainter closingPathPainter;
	
	// Trasa pomiędzy stacjami
	private RoutePainter veturiloStopPathPainter;
	
	private void initializeJxMapViewer() throws Exception {
		// Obiekt
		jxMapViewer = new JXMapViewer();
		
		// Źródło obrazków
		TileFactoryInfo tileFactoryInfo = new OSMTileFactoryInfo();
		DefaultTileFactory tileFactory = new DefaultTileFactory(tileFactoryInfo);
		jxMapViewer.setTileFactory(tileFactory);
		
		// Liczba wątków do pobierania obrazków
		tileFactory.setThreadPoolSize(8);
		
		// Obsługa myszki i gestów
		MouseInputListener mouseInputListener = new PanMouseInputListener(jxMapViewer);
		jxMapViewer.addMouseListener(mouseInputListener);
		jxMapViewer.addMouseMotionListener(mouseInputListener);
		
		jxMapViewer.addMouseListener(new CenterMapListener(jxMapViewer));
		jxMapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(jxMapViewer));
		jxMapViewer.addKeyListener(new PanKeyListener(jxMapViewer));
		
		jxMapViewer.addMouseListener(new MapClickListener(jxMapViewer) {
			
			@Override
			public void mapClicked(GeoPosition location) {
				
				GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
				
				// Po kliknięciu ustawia wartość pola Opening/Closing w zależności od tego co jest zaznaczone
				if(openingTextField != null && openingTextField.hasFocus()) {
					openingTextField.setText(geoPoint.toString());
					updateOpeningClosing();
					closingTextField.requestFocus();
				} else if(closingTextField != null && closingTextField.hasFocus()) {
					closingTextField.setText(geoPoint.toString());
					updateOpeningClosing();
					openingTextField.requestFocus();
				}
				
			}
		});
		
		ClassLoader classLoader = getClass().getClassLoader();

		// Poszczególne paintery
		openingWaypointPainter = new WaypointPainter<>();
		openingWaypointPainter.setRenderer(new ImageWaypointRenderer(ImageIO.read(classLoader.getResource("pinpoint-home.png"))));
		closingWaypointPainter = new WaypointPainter<>();
		closingWaypointPainter.setRenderer(new ImageWaypointRenderer(ImageIO.read(classLoader.getResource("pinpoint-finish.png"))));
		veturiloStopsWaypointPainter = new WaypointPainter<>();
		veturiloStopsWaypointPainter.setRenderer(new ImageWaypointRenderer(ImageIO.read(classLoader.getResource("pinpoint-veturilo.png"))));
		allVeturiloStopsWaypointPainter = new WaypointPainter<>();
		
		openingPathPainter = new RoutePainter(Color.GRAY, new BasicStroke(4));
		closingPathPainter = new RoutePainter(Color.GRAY, new BasicStroke(4));
		veturiloStopPathPainter = new RoutePainter(Color.BLUE, new BasicStroke(4));
		
		// Złożony painter
		jxMapViewer.setOverlayPainter(new CompoundPainter<JXMapViewer>(Arrays.asList(
			openingWaypointPainter,
			closingWaypointPainter,
			veturiloStopsWaypointPainter,
			allVeturiloStopsWaypointPainter,
			
			openingPathPainter,
			closingPathPainter,
			veturiloStopPathPainter
		)));
		
		// Rozciągnięcie
		jxMapViewer.setPreferredSize(jxMapViewer.getMaximumSize());
		
		// Startowa pozycja mapy
		jxMapViewer.setCenterPosition(initialGeoPosition);
		jxMapViewer.setZoom(initialZoom);
		
		// Wyświetlenie mapy
		mapTipsPanel.add(jxMapViewer);
	}
	
	private JPanel instructionsPanel;
	private void initializeInstructionsPanel() {
		instructionsPanel = new JPanel();
		instructionsPanel.setMinimumSize(new Dimension(320, 0));
		instructionsPanel.setBorder(BorderFactory.createTitledBorder("Instructions"));
		instructionsPanel.setLayout(new BoxLayout(instructionsPanel, BoxLayout.Y_AXIS));
		
		mapTipsPanel.add(instructionsPanel);
	}
	
	private JTextField openingTextField;
	private JTextField closingTextField;
	private JCheckBox showAllVeturiloStopsCheckBox;
	private void initializeControls() {
		JPanel panel = new JPanel();
		
		JLabel openingLabel = new JLabel("From: ");
		panel.add(openingLabel);
		
		openingTextField = new JTextField(openingClosingTextBoxColumns);
		panel.add(openingTextField);
		
		JLabel closingLabel = new JLabel("To: ");
		panel.add(closingLabel);
		
		closingTextField = new JTextField(openingClosingTextBoxColumns);
		panel.add(closingTextField);
		
		JButton searchButton = new JButton("Search");
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateOpeningClosing();
				updatePath();
			}
		});
		panel.add(searchButton);
		
		showAllVeturiloStopsCheckBox = new JCheckBox("Show all VeturiloStops");
		showAllVeturiloStopsCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateShowAllVeturiloStops();
			}
		});
		
		panel.add(showAllVeturiloStopsCheckBox);
		
		controlsPanel.add(panel);
	}
	
	private GeoPoint opening, closing;
	private void updateOpeningClosing() {
		
		// Wyczyść listę
		openingWaypointPainter.setWaypoints(new HashSet<>());
		closingWaypointPainter.setWaypoints(new HashSet<>());
		veturiloStopsWaypointPainter.setWaypoints(new HashSet<>());
		
		openingPathPainter.clear();
		closingPathPainter.clear();
		veturiloStopPathPainter.clear();
		
		jxMapViewer.repaint();
		
		// Konwertujemy punkty na pozycję
		try {
			
			String openingText = openingTextField.getText();
			
			if(openingText.isEmpty())
				opening = null;
			else
				opening = GeoPoint.fromString(openingText);
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(mapTipsPanel, "Invalid opening: " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		try {
			
			String closingText = closingTextField.getText();
			
			if(closingText.isEmpty())
				closing = null;
			else
				closing = GeoPoint.fromString(closingText);
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(mainFrame, "Invalid closing: " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		// Rysujemy istniejące punkty
		
		if(opening != null) {
			Set<Waypoint> waypoints = new HashSet<>();
			waypoints.add(new DefaultWaypoint(opening.getLatitude(), opening.getLongitude()));
			openingWaypointPainter.setWaypoints(waypoints);
		}
		
		if(closing != null) {
			Set<Waypoint> waypoints = new HashSet<>();
			waypoints.add(new DefaultWaypoint(closing.getLatitude(), closing.getLongitude()));
			closingWaypointPainter.setWaypoints(waypoints);	
		}
		
		jxMapViewer.repaint();
	}

	private void updatePath() {
		
		// Czyścimy listę punktów na mapie
		veturiloStopsWaypointPainter.setWaypoints(new HashSet<>());
		
		openingPathPainter.clear();
		closingPathPainter.clear();
		veturiloStopPathPainter.clear();
		
		jxMapViewer.repaint();
		
		class SwingWorkerResult {
			public ImmutablePair<VeturiloStop, Path> openingPath;
			public ImmutablePair<VeturiloStop, Path> closingPath;
			public Iterable<ImmutablePair<VeturiloStop, Path>> veturiloStopPath;
		}
		
		SwingWorker<SwingWorkerResult, Void> swingWorker = new SwingWorker<SwingWorkerResult, Void>() {

			@Override
			protected SwingWorkerResult doInBackground() throws Exception {
				
				// Pobieranie stanu
				FreeTuriloRouter freeTuriloRouter;
				try {
					freeTuriloRouter = FreeTuriloRouter.getInstanceTS();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				
				SwingWorkerResult swingWorkerResult = new SwingWorkerResult();
				
				// Od początku do stacji
				swingWorkerResult.openingPath = freeTuriloRouter.findOpeningClosingPath(opening, OpeningClosing.OPENING);
				
				// Od stacji do końca
				swingWorkerResult.closingPath = freeTuriloRouter.findOpeningClosingPath(closing, OpeningClosing.CLOSING);
				
				// Jeśli nie ma ścieżki od / do
				if(swingWorkerResult.openingPath == null || swingWorkerResult.closingPath  == null)
					return null;
				
				// Pomiędzy stacjami
				swingWorkerResult.veturiloStopPath = freeTuriloRouter.findVeturiloStopPath(swingWorkerResult.openingPath.left, swingWorkerResult.closingPath.left);
				
				// Jeśli nie ma trasy rowerowej
				if(swingWorkerResult.veturiloStopPath == null)
					return null;
				
				return swingWorkerResult;
			}
			
			@Override
			protected void done() {
				SwingWorkerResult swingWorkerResult = null;
				
				try {
					swingWorkerResult = get();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(mapTipsPanel, "swingWorkerResult Exception: " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
				}
				
				if(swingWorkerResult != null) {
					// Kropki stacji
					HashSet<Waypoint> veturiloStopPathWaypoints = new HashSet<Waypoint>();
					for(ImmutablePair<VeturiloStop, Path> veturiloStopPoint : swingWorkerResult.veturiloStopPath) {
						veturiloStopPathWaypoints.add(new DefaultWaypoint(veturiloStopPoint.left.getGeoPoint().getLatitude(), veturiloStopPoint.left.getGeoPoint().getLongitude()));
					}
					veturiloStopsWaypointPainter.setWaypoints(veturiloStopPathWaypoints);
					
					// Linia dojścia do
					LinkedList<GeoPoint> openingPathPoints = new LinkedList<>();
					for(GeoPoint geoPoint : swingWorkerResult.openingPath.right.getGeoPoints()) {
						openingPathPoints.add(geoPoint);
					}
					openingPathPainter.setRoute(openingPathPoints);
					
					// Linia z
					LinkedList<GeoPoint> closingPathPoints = new LinkedList<>();
					for(GeoPoint geoPoint : swingWorkerResult.closingPath.right.getGeoPoints()) {
						closingPathPoints.add(geoPoint);
					}
					closingPathPainter.setRoute(closingPathPoints);
					
					// Połączona ścieżka
					LinkedList<GeoPoint> veturiloStopPathPoints = new LinkedList<>();
					for(ImmutablePair<VeturiloStop, Path> veturiloStopPoint : swingWorkerResult.veturiloStopPath) {
						
						// Tras jest o 1 mniej niż punktów
						if(veturiloStopPoint.right == null)
							continue;
						
						// Dodaj punkty
						for(GeoPoint geoPoint : veturiloStopPoint.right.getGeoPoints()) {
							veturiloStopPathPoints.add(geoPoint);
						}
					}
					veturiloStopPathPainter.setRoute(veturiloStopPathPoints);
					
					// Rysowanie
					jxMapViewer.repaint();
					
					
					// Instrukcje dojazdu
					instructionsPanel.removeAll();
					
					JLabel openingLabel = new JLabel("Start: " + swingWorkerResult.openingPath.right.getTimeMillis() / 1000 / 60 + "min");
					instructionsPanel.add(openingLabel);
					
					for(ImmutablePair<VeturiloStop, Path> veturiloStopPoint : swingWorkerResult.veturiloStopPath) {
						
						if(veturiloStopPoint.right != null) {
							JLabel pathLabel = new JLabel("Bike: " + veturiloStopPoint.right.getTimeMillis() / 1000 / 60 + "min");
							instructionsPanel.add(pathLabel);
						}
						
						JLabel veturiloStopLabel = new JLabel("VeturiloStop: " + veturiloStopPoint.left);
						instructionsPanel.add(veturiloStopLabel);
					}
					
					JLabel closingLabel = new JLabel("End: " + swingWorkerResult.closingPath.right.getTimeMillis() / 1000 / 60 + "min");
					instructionsPanel.add(closingLabel);
					
					instructionsPanel.revalidate();
					instructionsPanel.repaint();
				}
				
				hideLoadingDialog();
			}
		};
		
		swingWorker.execute();
		showLoadingDialog();
	}

	private void updateShowAllVeturiloStops() {
		
		Set<Waypoint> allVeturiloStops = new HashSet<>();
		
		if(showAllVeturiloStopsCheckBox.isSelected()) {
			VeturiloStops veturiloStops;
			try {
				veturiloStops = VeturiloStops.getInstanceTS();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			for(VeturiloStop veturiloStop : veturiloStops.getVeturiloStopIterable()) {
				allVeturiloStops.add(new DefaultWaypoint(veturiloStop.getGeoPoint().getLatitude(), veturiloStop.getGeoPoint().getLongitude()));
			}
		}
		
		allVeturiloStopsWaypointPainter.setWaypoints(allVeturiloStops);
		jxMapViewer.repaint();
	}
}
