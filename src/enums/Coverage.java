package enums;

public enum Coverage {
	MCDC, CONDITION, BRANCH, DECISION;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
