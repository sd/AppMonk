package appmonk.tricks;

public class ArrayTricks {
	public static int findInt(int [] array, int value) {
		int l = array.length;
		for (int i = 0; i < l; i++) {
        	if (array[i] == value)
        		return i;
		}
		return -1;
	}

	public static int findString(String [] array, String value) {
		int l = array.length;
		for (int i = 0; i < l; i++) {
        	if (array[i] != null && array[i].equals(value))
        		return i;
		}
		return -1;
	}
}
