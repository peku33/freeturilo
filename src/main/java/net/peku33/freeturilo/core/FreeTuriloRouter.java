package net.peku33.freeturilo.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

/**
 * Klasa odpowiedzialna za budowę i wyznaczanie tras rowerowych z ograniczeniami
 * @author peku33
 *
 */
public class FreeTuriloRouter {
	
	private static Logger logger = Logger.getLogger(WarsawMapDownloader.class.toString());
	
	// Maksymalny czas podróży
	private static final double timeMaxMin = 20;
	
	// O ile dłuższy bezwzględnie odcinek jeszcze rozważać?
	private static final double absoluteDistanceMaxAcceptableRatio = 3.0;
	
	// Ile najbliższych (prostoliniowo) stacji sprawdzić przy wyznaczaniu stacji początkowej / końcowej?
	private static final int openingClosingPathClosestStops = 8;
	
	/**
	 * Wątkowo bezpieczny singleton
	 */
	private static FreeTuriloRouter instance;
	private static Object getInstanceTSLock = new Object();
	public static FreeTuriloRouter getInstanceTS() throws Exception {
		
		VeturiloStops veturiloStops = VeturiloStops.getInstanceTS();
		FreeTuriloGraphHooper freeTuriloGraphHooper = FreeTuriloGraphHooper.getInstanceTS();
		
		synchronized (getInstanceTSLock) {
			if(instance == null)
				instance = new FreeTuriloRouter(
					veturiloStops, 
					freeTuriloGraphHooper
				);
		}
		
		return instance;
	}
	
	private static final String getVeturiloStopsGraphCacheFileName = "work/" + FreeTuriloRouter.class.getName() + "/VeturiloStopsGraphCache.ser";
	
	private static File getVeturiloStopsGraphCacheFile() {
		return new File(getVeturiloStopsGraphCacheFileName);
	}
	
	public static void clearVeturiloStopsGraphCache() {
		getVeturiloStopsGraphCacheFile().delete();
	}
	
	// Uchwyt na listę stacji
	private VeturiloStops veturiloStops;
	
	// Uchwyt na FreeTuriloGraphHooper
	private FreeTuriloGraphHooper freeTuriloGraphHooper;
	
	/**
	 * Konstruktor. Woła @see rebuildVeturiloStopsGraph
	 * 
	 * @param veturiloStops
	 * @param freeTuriloGraphHooper
	 * @throws Exception
	 */
	private FreeTuriloRouter(VeturiloStops veturiloStops, FreeTuriloGraphHooper freeTuriloGraphHooper) throws Exception {
		
		this.veturiloStops = veturiloStops;
		this.freeTuriloGraphHooper = freeTuriloGraphHooper;
		
		loadVeturiloStopsGraph();
	}
	
	/**
	 * Graf połączeń między stacjami w postaci
	 * 	Stacja zródłowa -> {Stacja docelowa, wyznaczona ścieżka}
	 * Zawiera wyłącznie odcinki objęte ograniczeniami
	 */
	private HashMap<VeturiloStop, HashMap<VeturiloStop, Path>> veturiloStopsGraph;
	
	@SuppressWarnings("unchecked")
	private void loadVeturiloStopsGraph() throws Exception {
		
		File veturiloStopsGraphCacheFile = getVeturiloStopsGraphCacheFile();
		FileUtils.forceMkdir(veturiloStopsGraphCacheFile.getParentFile());
		
		if(veturiloStopsGraphCacheFile.exists()) {
			FileInputStream fileInputStream = new FileInputStream(veturiloStopsGraphCacheFile);
			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
			
			logger.info("readObject start");
			veturiloStopsGraph = (HashMap<VeturiloStop, HashMap<VeturiloStop, Path>>) objectInputStream.readObject();
			logger.info("readObject end");
			
			objectInputStream.close();
			fileInputStream.close();
		}
		
		if(veturiloStopsGraph == null) {
			rebuildVeturiloStopsGraph();
			
			FileOutputStream fileOutputStream = new FileOutputStream(veturiloStopsGraphCacheFile);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
			
			logger.info("writeObject start");
			objectOutputStream.writeObject(veturiloStopsGraph);
			logger.info("writeObject end");
			
			objectOutputStream.close();
			fileOutputStream.close();
		}
	}
	
	/**
	 * Buduje veturiloStopsGraph
	 * 
	 * @throws Exception
	 */
	private void rebuildVeturiloStopsGraph() {
		// Zbuduj listę połączeń pomiędzy stacjami
		// Wpakuj to do wątków dla przyspieszenia
		
		// Kolekcja do napełniania z wielu wątków
		// From -> { To, Path }
		ConcurrentHashMap<VeturiloStop, HashMap<VeturiloStop, Path>> veturiloStopsGraphConcurrent = new ConcurrentHashMap<>();
		
		// Informacja do pomiaru czasu
		logger.info("Graph build started");
		
		// Executor o liczbę wątków równej liczbie procesorów
		ExecutorService executorService = Executors.newFixedThreadPool(
			Runtime.getRuntime().availableProcessors()
		);
		
		// Tworzymy listę zadań
		for(VeturiloStop from : veturiloStops.getVeturiloStopIterable()) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					
					// Tworzymy kolekcję stacji
					// Posortujemy ją według odległości bezwzględnej, dzięki czemu będzie można pominąć rozważanie na pewno zbyt długich ścieżek
					// W liście nie zawieramy samych siebie - stąd -1
					ArrayList<ImmutablePair<VeturiloStop, Double>> veturiloStopEntries = new ArrayList<>(veturiloStops.getVeturiloStopSize() - 1);
					
					// Dodjemy do listy wszystkie elementy
					for(VeturiloStop to : veturiloStops.getVeturiloStopIterable()) {
						
						// Pomijamy trasę od siebie do siebie
						if(from.equals(to))
							continue;
						
						// Obliczamy dystans heurystyczny
						double absoluteDistance = GeoPoint.calculateAbsoluteDistance(from.getGeoPoint(), to.getGeoPoint());
						
						// Dodajemy element do kolekcji
						veturiloStopEntries.add(new ImmutablePair<VeturiloStop, Double>(to, absoluteDistance));
					}
					
					// Sortujemy kolekcję według dystansu, rosnąco
					Collections.sort(veturiloStopEntries, new Comparator<ImmutablePair<VeturiloStop, Double>>() {
						@Override
						public int compare(ImmutablePair<VeturiloStop, Double> o1, ImmutablePair<VeturiloStop, Double> o2) {
							return o1.right.compareTo(o2.right);
						}
					});
					
					// Rozpoczynamy przeglądanie posortowanej kolekcji
					// Kiedy czas przekroczy ustawowe maksimum dwukrotnie, przestajemy przeglądać
					
					// Lokalna kolekcja ścieżek
					HashMap<VeturiloStop, Path> veturiloStopGraph = new HashMap<>();
					
					// Maksymalny dystans przy którym trasa jeszcze mieściła się w czasie
					Double absoluteDistanceMaxAcceptable = null;
					for(ImmutablePair<VeturiloStop, Double> toPair : veturiloStopEntries) {
						
						// Jeśli nasza odległość jest absoluteDistanceMaxAcceptableRatio razy większa niż najlepsza znaleziona odległość - przerywamy szukanie
						if(absoluteDistanceMaxAcceptable != null && toPair.right.compareTo(absoluteDistanceMaxAcceptable * absoluteDistanceMaxAcceptableRatio) > 0)
							break;
						
						Path path = freeTuriloGraphHooper.routeBike(
							from.getGeoPoint(),
							toPair.left.getGeoPoint()
						);
						
						// Jeśli trasa nie została znaleziona...
						if(path == null)
							continue;
						
						// Jeśli trasa nie mieści się już w przedziale czasowym
						if(path.getTimeMillis() > timeMaxMin * 60 * 1000) {
							continue;	
						}
						
						// Trasa zaakceptowana - bierzemy
						veturiloStopGraph.put(
							toPair.left,
							path
						);
						
						// Podbijamy licznik
						absoluteDistanceMaxAcceptable = toPair.right;
					}
					
					// Dodajemy pełną kolekcję
					veturiloStopsGraphConcurrent.put(from, veturiloStopGraph);
				}
			};
			executorService.execute(runnable);
		}
		
		// Z timeoutem na godzinę
		executorService.shutdown();
		try {
			executorService.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		// Informacja do pomiaru czasu
		logger.info("Graph build finished");
		
		// Wszystkie ścieżki zostały zbudowane
		// Wygeneruj graf połączeń
		veturiloStopsGraph = new HashMap<>();
		veturiloStopsGraph.putAll(veturiloStopsGraphConcurrent);
	}
	
	/**
	 * Wyszukuje ścieżkę pomiędzy dwoma stacjami from i to z uwzględnieniem ograniczeń.
	 * Zwraca listę odcinków w postaci:
	 * 	VeturiloStop - stacja
	 * 	Path - ścieżka dojazdu DO tej stacji z poprzedniej. Dla pierwszego elementu NULL.
	 * 
	 * W przypadku braku ścieżki zwraca NULL.
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public Iterable<ImmutablePair<VeturiloStop, Path>> findVeturiloStopPath(VeturiloStop from, VeturiloStop to) {
		
		if(from.equals(to))
			return new LinkedList<>();
		
		// Algorytm A*
		
		// Wierzchołki już zbudowanej ścieżki
		HashSet<VeturiloStop> closedSet = new HashSet<>();
		
		// Wierzchołki oczekujące i odpowiadający im gScore
		// VeturiloStop -> GScore
		HashMap<VeturiloStop, Long> openSetGScores = new HashMap<>();
		
		// Poprzedniki punktów
		// VeturiloStop -> {PreviousVeturiloStop, Path}
		HashMap<VeturiloStop, ImmutablePair<VeturiloStop, Path>> previousVeturiloStops = new HashMap<>();
		
		// Cache na hScore
		HashMap<VeturiloStop, Long> hScores = new HashMap<>();
		

		
		// Do listy otwartych dodajemy pierwszy wierzchołek, bez poprzednika
		openSetGScores.put(from, 0L);
		
		// Przed pierwszym punktem nie ma już nic
		previousVeturiloStops.put(from, new ImmutablePair<VeturiloStop, Path>(null, null));
		
		// Cache na hScole inicjujemy zerem dla końcowego punktu
		hScores.put(to, 0L);
		
		while(!openSetGScores.isEmpty()) {
			
			// {VeturiloStop, gScore, fScore}
			ImmutableTriple<VeturiloStop, Long, Long> bestVeturiloStopTuple = null;
			
			// Znajdz wierzchołek o najmniejszym fScore (= gScore + hScore)
			for(Entry<VeturiloStop, Long> tentativeVeturiloStopEntry : openSetGScores.entrySet()) {
				
				// Wyciągamy element z kolekcji
				VeturiloStop tentativeVeturiloStop = tentativeVeturiloStopEntry.getKey();
				Long tentativeVeturiloStopGScore = tentativeVeturiloStopEntry.getValue();
				
				// Próbujemy pobrać lub obliczyć hScore
				// Coś ala programowanie dynamiczne
				Long hScore = hScores.get(tentativeVeturiloStop);
				if(hScore == null) {
					
					// Znajdz ścieżkę z tego punktu do końca
					Path path = freeTuriloGraphHooper.routeBike(
						tentativeVeturiloStop.getGeoPoint(),
						to.getGeoPoint()
					);
					
					// Jeśli ścieżka nie istnieje - hScore w nieskończoność (przy okazji załatwia problem null)
					// Jeśli ścieżka istnieje - bierzemy jej wartość
					hScore = (path != null) ? path.getTimeMillis() : Long.MAX_VALUE;
					
					// Zapisujemy na liście
					hScores.put(tentativeVeturiloStop, hScore);
				}
				
				// fScore = gScore + hScore
				long fScore = tentativeVeturiloStopGScore + hScore;
				
				// Sprawdzamy czy mamy lepszy wynik
				if(bestVeturiloStopTuple == null || fScore < bestVeturiloStopTuple.right)
					bestVeturiloStopTuple = new ImmutableTriple<VeturiloStop, Long, Long>(tentativeVeturiloStop, tentativeVeturiloStopGScore, fScore);
			}
			
			// Warunkiem pętli jest istnienie conajmniej jednego elementu, więc na pewno istnieje
			VeturiloStop bestVeturiloStop = bestVeturiloStopTuple.left;
			Long bestGScore = bestVeturiloStopTuple.middle;
			
			// Ścieżka została znaleziona, kończymy
			if(bestVeturiloStop.equals(to)) {
				
				// Konstruujemy ścieżkę...
				LinkedList<ImmutablePair<VeturiloStop, Path>> path = new LinkedList<>();
				
				VeturiloStop previous = to;
				while(previous != null) {
					ImmutablePair<VeturiloStop, Path> previousData = previousVeturiloStops.get(previous);
					path.addFirst(new ImmutablePair<VeturiloStop, Path>(previous, previousData.right));
					previous = previousData.left;
				}
				
				// Zwracamy wynik;
				return path; 
			}
			
			// Usuń najlepszy ze zbioru otwartego
			openSetGScores.remove(bestVeturiloStop);
			
			// Dodaj najlepszy do zbioru zamkniętego
			closedSet.add(bestVeturiloStop);
			
			// Wez sąsiadów najlepszego
			for(Entry<VeturiloStop, Path> tentativeNeighborEdge : veturiloStopsGraph.get(bestVeturiloStop).entrySet()) {
				
				// Wyciągnięcie elementy pętli
				VeturiloStop tentativeNeighborVeturiloStop = tentativeNeighborEdge.getKey();
				Path tentativeNeighborPath = tentativeNeighborEdge.getValue(); // Jak cojechać od najlepszego do tego
				
				// Jeśli sąsiad należy do zbioru zamkniętego - pomijamy
				if(closedSet.contains(tentativeNeighborVeturiloStop))
					continue;
				
				// gScore od najlepszego do sąsiada
				Long bestTentativeNeighborGScore = tentativeNeighborPath.getTimeMillis();
				
				// gScore od początku do najlepszego sąsiada
				long tentativeGScore = bestGScore + bestTentativeNeighborGScore;
				
				// Poprzedni gScore kandydata / null jeśli nie został rozpatrzony
				Long previousTentativeGScore = openSetGScores.get(tentativeNeighborVeturiloStop);
				
				// Jeśli poprzednia wartość nie istnieje lub jest gorsza - podmieniamy
				if(previousTentativeGScore == null || tentativeGScore < previousTentativeGScore) {
					openSetGScores.put(tentativeNeighborVeturiloStop, tentativeGScore);
					previousVeturiloStops.put(tentativeNeighborVeturiloStop, new ImmutablePair<VeturiloStop, Path>(bestVeturiloStop, tentativeNeighborPath));
				}
			}
			
		}
		
		// Nie znaleziono ścieżki
		return null;
	}
	
	/**
	 * Znajduje ścieżkę pieszą od dowolnego punktu do najbliższej stacji
	 * 
	 * @param fromTo Dowolna pozycja
	 * @param openingClosing Kierunek wycieczki. Opening - z punktu do stacji, Closing - ze stacji do puntktu
	 * @return Para: VeturiloStop - najbliższa stacja, Path - ścieżka dojścia
	 */
	public enum OpeningClosing { OPENING, CLOSING }; 
	public ImmutablePair<VeturiloStop, Path> findOpeningClosingPath(GeoPoint fromTo, OpeningClosing openingClosing) {
		
		// Dla przyspieszenia obliczeń rozważamy tylko kilka najbliższych stacji
		// Tworzymy listę stacji wraz z odległościami w prostej linii
		ArrayList<ImmutablePair<VeturiloStop, Double>> veturiloStopEntries = new ArrayList<>(veturiloStops.getVeturiloStopSize());
		
		// Generujemy odległości w prostej linii
		for(VeturiloStop veturiloStop : veturiloStops.getVeturiloStopIterable()) {
			
			// Oblicz dystans
			// Kolejność nie ma znaczenia
			double absoluteDistance = GeoPoint.calculateAbsoluteDistance(fromTo, veturiloStop.getGeoPoint());
			
			// Dodaj do listy
			veturiloStopEntries.add(new ImmutablePair<VeturiloStop, Double>(veturiloStop, absoluteDistance));
		}
		
		// Sortujemy kolekcję według dystansu, rosnąco
		Collections.sort(veturiloStopEntries, new Comparator<ImmutablePair<VeturiloStop, Double>>() {
			@Override
			public int compare(ImmutablePair<VeturiloStop, Double> o1, ImmutablePair<VeturiloStop, Double> o2) {
				return o1.right.compareTo(o2.right);
			}
		});
		
		// Poszukujemy najlepszego (najbliższego punktu)
		ImmutablePair<VeturiloStop, Path> best = null;
		
		// Sprawdzamy openingClosingPathClosestStops najbliższych stacji
		for(int veturiloStopEntryId = 0; veturiloStopEntryId < openingClosingPathClosestStops; ++veturiloStopEntryId) {
			
			VeturiloStop veturiloStop = veturiloStopEntries.get(veturiloStopEntryId).left;
			
			// W nawigacji kolejność ma znaczenie
			// W zależności od kolejności podróży - ustawiamy from/to
			GeoPoint from, to;
			switch(openingClosing) {
				case OPENING: {
					from = fromTo;
					to = veturiloStop.getGeoPoint();
					break;
				}
				case CLOSING: {
					from = veturiloStop.getGeoPoint();
					to = fromTo;
					break;
				}
				default:
					throw new RuntimeException("Unknown openingClosing");
			}
			
			// Wyznaczenie trasy nawigacji
			Path path = freeTuriloGraphHooper.routeFoot(
				from,
				to
			);
			
			// Trasa nie istnieje - pomijamy
			if(path == null)
				continue;
			
			// Jeśli nastąpiła poprawa trasy - zapisujemy
			if(best == null || path.getTimeMillis() < best.right.getTimeMillis())
				best = new ImmutablePair<VeturiloStop, Path>(veturiloStop, path);
		}
		
		// Zwracamy najlepszy wynik
		return best;
	}
	
	/**
	 * All-in-one metoda znalezienia ścieżki pomiędzy dwoma punktami.
	 * 
	 * Ścieżka składa się z:
	 * 		- (left) odcinka od from do najbliższej stacji / null jeśli nie istnieje
	 * 		- (middle) kolekcji połączeń między stacjami / null jeśli nie istnieje
	 * 		- (right) odcinka od najbliższej stacji do to / null jeśli nie istnieje
	 * 
	 * @param from Początkowa lokalizacja użytkownika (miejsce z którego użytkownik chce znalezc trasę)
	 * @param to Końcowa lokalizacja użytkownika (miejsce do którego użytkownik chce znalezć trasę) 
	 * @return Triple, jak w opisie ścieżki
	 */
	public ImmutableTriple<ImmutablePair<VeturiloStop, Path>, Iterable<ImmutablePair<VeturiloStop, Path>>, ImmutablePair<VeturiloStop, Path>> findPath(GeoPoint from, GeoPoint to) {
		ImmutablePair<VeturiloStop, Path> openingPath = findOpeningClosingPath(from, OpeningClosing.OPENING);
		ImmutablePair<VeturiloStop, Path> closingPath = findOpeningClosingPath(to, OpeningClosing.CLOSING);
		
		Iterable<ImmutablePair<VeturiloStop, Path>> veturiloStopPath = null;
		if(openingPath != null && closingPath != null)
			veturiloStopPath = findVeturiloStopPath(openingPath.left, closingPath.left);
		
		return new ImmutableTriple<ImmutablePair<VeturiloStop,Path>, Iterable<ImmutablePair<VeturiloStop,Path>>, ImmutablePair<VeturiloStop,Path>>(
			openingPath,
			veturiloStopPath,
			closingPath
		);
	}
	
}
