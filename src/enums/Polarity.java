package enums;

public enum Polarity {
	ALL, TRUE, FALSE;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
