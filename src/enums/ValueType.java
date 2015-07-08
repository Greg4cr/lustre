package enums;

public enum ValueType {
	NULL, DEFAULT, RANDOM;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
