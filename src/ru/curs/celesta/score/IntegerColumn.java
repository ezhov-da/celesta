package ru.curs.celesta.score;

/**
 * Целочисленная колонка.
 * 
 */
public final class IntegerColumn extends Column {
	private Integer defaultvalue;
	private boolean identity;

	public IntegerColumn(Table table, String name) throws ParseException {
		super(table, name);
	}

	@Override
	protected void setDefault(String lexvalue) throws ParseException {
		if (lexvalue == null) {
			defaultvalue = null;
			identity = false;
		} else if ("IDENTITY".equalsIgnoreCase(lexvalue)) {
			for (Column c : getParentTable().getColumns().values())
				if (c instanceof IntegerColumn && c != this
						&& ((IntegerColumn) c).isIdentity())
					throw new ParseException(
							"More than one identity columns are defind in table "
									+ getParentTable().getName());
			defaultvalue = null;
			identity = true;
		} else {
			defaultvalue = Integer.parseInt(lexvalue);
			identity = false;
		}
	}

	/**
	 * Возвращает значение по умолчанию.
	 */
	public Integer getDefaultValue() {
		return defaultvalue;
	}

	/**
	 * Является ли поле IDENTITY.
	 */
	public boolean isIdentity() {
		return identity;
	}

	@Override
	protected String getDefaultDefault() {
		return "0";
	}

	@Override
	public String pythonDefaultValue() {
		return "0";
	}

	@Override
	public String jdbcGetterName() {
		return "getInt";
	}
}
