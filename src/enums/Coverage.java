package enums;

public enum Coverage {
	MCDC, CONDITION, BRANCH, DECISION,
	OMCDC, OCONDITION, OBRANCH, ODECISION;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
