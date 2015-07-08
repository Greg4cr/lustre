package enums;

public enum Simulation {
	COMPLETE, PARTIAL;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
