package enums;

public enum Generation {
	NULL, DEFAULT, RANDOM;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
