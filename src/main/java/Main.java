import java.util.logging.Logger;

import net.peku33.freeturilo.core.FreeTuriloRouter;
import net.peku33.freeturilo.core.WarsawMapDownloader;

public class Main {
	
	private static Logger logger = Logger.getLogger(WarsawMapDownloader.class.toString());
	
	public static void main(String[] args) {
		try {
			FreeTuriloRouter.getInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
