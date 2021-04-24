
public class RandomNumber {
	
	// This function will return a value from 0.0-1.0 (in close interval, for 1 decimal)
	public static double random() {
		double a = Math.random();
		a = a * 11;
		a = Math.floor(a);
		return a/10;
	}
	
	/* According to the requirement, first convert a, b to Long type, then 
	   get a number in the open interval, return type is depend on the
	   third argument: isLongType
	   If the return type is Double, it is for 2 decimal */
	public static Object randomNumber(double a, double b, boolean isLongType) {
		long low = (long)a;
		long high = (long)b;
		long diff = high - low;
		if(isLongType) {
			return low + 1 + (long)(Math.random()*(diff-1));
		} else {
			double ans = low + (Math.random()*diff); 
			return 1.0 * Math.round(100 * ans) / 100;
		}
	}

}
