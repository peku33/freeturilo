import net.peku33.freeturilo.gui.GUIMain;

public class Main {
	public static void main(String[] args) {
		try {
			new GUIMain();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
