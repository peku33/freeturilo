FreeTurilo
==========
Wyszukiwarka bezpłatnej trasy przejazdu rowerem Veturilo w Warszawie.

![FreeTurilo](https://raw.githubusercontent.com/peku33/freeturilo/master/docs/FreeTurilo.JPG)

Opis aplikacji
--------------
Aplikacja pozwala w łatwy sposób wyszukać trasę przejazdu rowerem Veturilo w taki sposób, aby nie zapłacić ani złotówki. Przejazd do 20 minut jest gratis, a czas resetuje się w chwili postawienia roweru w stacji. Dlaczego by więc nie przejechać całej trasy maksymalnie 20-minutowymi odcinkami?

Interfejs graficzny pozwala łatwo znaleźć trasę. Stawiając kursor w polu `From` / `To` i klikając na mapie - ustawiamy punkt początkowy i końcowy. Po naciśnięciu przycisku `Search` aplikacja znajduje trasę przejazdu i wyświetla ją na ekranie.

Sekcja `Instructions` zawiera listę odcinków i stacji które trzeba przejść.

Zaznaczenie opcji `Show all Veturilo Stops` włącza / wyłącza wyświetlanie wszystkich stacji w Warszawie.

Opis działania i algorytmów
--------------
Aplikacja bazuje na danych `OpenStreetMap` dla obszaru Warszawy. Za ich pobranie i późniejsze sprawdzanie aktualności odpowiedzialna jest klasa `WarsawMapDownloader`.

Lista stacji wraz ze współrzędnymi pobierana jest przez klasę `VeturiloStops` bezpośrednio ze strony Veturilo.

Pobrane mapy `OpenStreetMap` są wykorzystywane przez pakiet `GraphHooper` - *router* (wyszukiwarkę trasy na mapie). 

Klasa `FreeTuriloRouter` buduje i utrzymuje graf połączeń pomiędzy wszystkimi stacjami w Warszawie (mieszczącymi się w 20 minutowym czasie). Do budowy trasy przejazdu pomiędzy parami stacji wykorzystywany jest pakiet `GraphHooper`. 

W konstruktorze singletonu tworzony jest graf połączeń. Dla każdej stacji rozważane są trasy do wszystkich innych stacji, w kolejności od najbliższej do najdalszej (prostolinijnie). Do wyznaczenia trasy i obliczenia czasu wykorzystywany jest `FreeTuriloGraphHooper`, metoda `routeBike()`. Kiedy czas przejazdu przekracza maksymalny czas przejazdu `absoluteDistanceMaxAcceptableRatio`-krotnie *(domyślnie 3.0)*, dalsze stacje nie są rozważane. Do grafu dodawane są krawędzie o wadze równej czasie przejazdu, ograniczone do `timeMaxMin` minut *(domyślnie 20)*. Wynikowy graf zawiera więc wszystkie możliwe poniżej-20 minutowe przejazdy. Z każdą krawędzią związana jest także informacja o dokładnej trasie przejazdu. Dla przyspieszenia obliczeń graf jest zapisywany na dysku i jeśli nie nastąpiła aktualizacja map - jest wczytywany bez ponownego przeliczania.

Znalezienie zadanej przez użytkownika trasy polega na:

 - Znalezieniu stacji najbliżej punktu startowego i końcowego - użytkownik musi dojść na piechotę do/od roweru. Tę funkcję realizuje metoda `findOpeningClosingPath()`. Tworzy zbiór wszystkich stacji, posortowany według przybliżonej odległości prostolinijnej (szybkie obliczenia). Następnie z posortowanej kolekcji obliczana jest (przy użyciu `FreeTuriloGraphHooper`, metody `routeFoot()`) dokładna trasa dla `openingClosingPathClosestStops` *(domyślnie 8)* stacji. Wybierana jest trasa o najkrótszym czasie przejścia. Ten krok wyznacza stacje początkową i końcową oraz trasę dojścia do/od nich.
 - Znalezieniu trasy pomiędzy stacjami początkową i końcową. W tym celu wykorzystywany jest ręcznie zaimplementowany algorytm `A*`, wyszukujący najkrótszą ścieżkę w zbudowanym na początku grafie. Wynikowa ścieżka zawiera pobrane z grafu informacje o odcinkach.

Opis klas
------------------
Aplikacja składa się z pakietu zawierającego algorytm (`core`) oraz przykładowego interfejsu użytkownika (`gui`).

Pakiet `core` składa się z klas `GeoPoint`, `Path`, `VeturlioStop`, będącymi prostymi, pozbawionymi logiki kontenerami atrybutów (modelem) oraz klas `FreeTuriloGraphHooper`, `VeturiloStops`, `WarsawMapDownloader`, `FreeTuriloRouter` stanowiących logikę aplikacji.

Klasy modelu:

 - `GeoPoint` - klasa opisująca współrzędne geograficzne - szerokość i długość. Zawiera proste funkcję zamieniające na napis / z napisu.
 - `Path` - klasa opisująca trasę. Składa się z sekwencji punktów `GeoPoint` oraz szacowanego całkowitego czasu podróży.
 - `VeturlioStop` - klasa opisująca stację Veturilo. Składa się z unikalnego id stacji (pobieranego ze strony Veturilo), położenia `GeoPoint` oraz nazwy (ze strony Veturilo).

Klasy logiki (wszystkie występują jako singletony):

 - `WarsawMapDownloader` - klasa odpowiedzialna za pobieranie i aktualizację map dla obszaru Warszawy. Dostarcza metodę `checkUpdate()` sprawdzającą i ewentualnie pobierającą aktualizację, oraz metodę `getLocal()` zwracającego uchwyt do pliku `.xml` mapy.
 - `VeturiloStops` - klasa odpowiedzialna za pobranie listy stacji Veturilo wraz ze współrzędnymi. Konstruktor singletonu pobiera i tworzy kolekcję, do której dostęp można uzyskać przy użyciu metod `getVeturiloStopIterable()` oraz `getVeturiloStopSize()`
 - `FreeTuriloGraphHooper` - klasa będąca adapterem pomiędzy klasami `FreeTurilo` a pakietem `GraphHooper`. Zarządza instancją GraphHooper'a oraz dostarcza metody wyszukiwania ścieżki rowerowej i pieszej pomiędzy dwoma punktami: `routeBike()` oraz `routeFoot()`.
 - `FreeTuriloRouter` - właściwa klasa zajmująca się wyszukiwaniem trasy przejazdu. Wykorzystuje dane ze wszystkich powyższych klas. Buduje i zarządza grafem połączeń, dostarcza metody `findVeturiloStopPath()` + `findOpeningClosingPath()` oraz połączeną metodę: `findPath()`

Integracja
----------
Wszystkie klasy z logiką zostały zaimplementowane jako singletony. Z wyjątkiem `WarsawMapDownloader` wszystkie klasy wykonują pełną inicjalizację w konstuktorze.

Do znajdowania trasy wystarczy wykorzystanie klasy `FreeTuriloRouter`, wykorzystującej wewnętrznie pozostałe klasy.

Zasoby, wydajność
-----------------
**Czas**
Najkosztowniejszym czasowo procesem jest aktualizacja mapy + aktualizacja cache `GraphHooper` + budowa grafu połączeń. Powyższe kroki wykonywane są w chwili startu aplikacji i/lub aktualizacji mapy.

 1. Aktualizacja mapy. Do sprawdzenia aktualności wysyłane jest jedno zapytanie `HTTP HEAD` (kilka kB). W przypadku dostępnej aktualizacji mapy pobierana jest wersja skompresowana (około 60MB). Wersja skompresowana jest rozpakowywana. Proces bez pobrania mapy zajmuje około 1 sekundę, z pobraniem mapy około 3 minuty.
 2. Aktualizacja cache `GraphHooper` zajmuje około 3-4 minut i jest wykonywana tylko po aktualizacji mapy.
 3. Budowa grafu połączeń. W przypadku aktualizacji cache `GraphHooper` budowany jest nowy graf (około 3-4 minuty). Graf jest serializowany / deserializowny i jeśli nie nastąpiła aktualizacja, jego wczytanie zajmuje około 1 minuty.

Sam proces znalezienia ścieżki polega na wyszukaniu około 16 krótkich tras pieszych (8 początkowych + 8 końcowych) i jednego wykonania algorytmu `A*`. Cały proces znalezienia trasy zajmuje około 1-5 sekund.

**RAM**
Aplikacja działa z domyślnym rozmiarem sterty wynoszącym około 512MB.

**Dysk**

 - Rozpakowana mapa - około 500MB
 - Cache `GraphHooper` - około 40MB
 - Graf połączeń - około 120MB

**Podsumowanie**
Proces inicjalizacji (wymagany jednorazowo) wykorzystuje o rząd wielkości więcej zasobów niż znalezienie pojedynczej trasy. Publiczne wykorzystanie aplikacji powinno zakładać istnienie serwera znajdującego i odsyłającego trasę, bez konieczności utrzymywania aplikacji po stronie klienta.