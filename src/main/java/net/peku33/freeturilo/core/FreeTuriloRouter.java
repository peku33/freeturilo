package net.peku33.freeturilo.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.graphhopper.PathWrapper;

public class FreeTuriloRouter {
	
	private static Logger logger = Logger.getLogger(WarsawMapDownloader.class.toString());
	
	// Maksymalny czas podróży
	private static final double timeMaxMin = 20;
	
	// O ile dłuższy bezwzględnie odcinek jeszcze rozważać?
	private static final double absoluteDistanceMaxAcceptableRatio = 3.0;
	
	private static FreeTuriloRouter instance;
	public static FreeTuriloRouter getInstance() throws Exception {
		if(instance == null)
			instance = new FreeTuriloRouter();
		return instance;
	}
	
	private FreeTuriloRouter() throws Exception {
		
		// Pobierz listę wszystkich przystanków
		VeturiloStops veturiloStops = VeturiloStops.getInstance();
		
		// Uruchom nawigację
		FreeTuriloGraphHooper freeTuriloGraphHooper = FreeTuriloGraphHooper.getInstance();
		
		// Zbuduj listę połączeń pomiędzy stacjami
		// Wpadkuj to do wątków dla przyspieszenia
		// Kolekcja do napełniania z wielu wątków
		ConcurrentLinkedQueue<FreeTuriloVeturiloStopPath> freeTuriloVeturiloStopPaths = new ConcurrentLinkedQueue<>();
		
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
						double absoluteDistance = VeturiloStopPair.calculateAbsoluteDistance(from, to);
						
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
					
					// Maksymalny dystans przy którym trasa jeszcze mieściła się w czasie
					Double absoluteDistanceMaxAcceptable = null;
					for(ImmutablePair<VeturiloStop, Double> toPair : veturiloStopEntries) {
						
						// Jeśli nasza odległość jest absoluteDistanceMaxAcceptableRatio razy większa niż najlepsza znaleziona odległość - przerywamy szukanie
						if(absoluteDistanceMaxAcceptable != null && toPair.right.compareTo(absoluteDistanceMaxAcceptable * absoluteDistanceMaxAcceptableRatio) > 0)
							break;
						
						PathWrapper pathWrapper = freeTuriloGraphHooper.route(from, toPair.left);
						
						// Jeśli trasa nie została znaleziona...
						if(pathWrapper == null)
							continue;
						
						// Jeśli trasa nie mieści się już w przedziale czasowym
						if(pathWrapper.getTime() > timeMaxMin * 60 * 1000) {
							continue;	
						}
						
						// Trasa zaakceptowana - bierzemy
						freeTuriloVeturiloStopPaths.add(
							new FreeTuriloVeturiloStopPath(from, toPair.left, pathWrapper)
						);
						
						// Podbijamy licznik
						absoluteDistanceMaxAcceptable = toPair.right;
					}
					
					
				}
			};
			executorService.execute(runnable);
		}
		
		// Z timeoutem na godzinę
		executorService.shutdown();
		executorService.awaitTermination(1, TimeUnit.HOURS);
		
		// Informacja do pomiaru czasu
		logger.info("Graph build finished");
		
		// Wszystkie ścieżki zostały zbudowane
		// Wygeneruj graf połączeń
	}

}
