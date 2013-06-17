package ru.curs.celesta;

/**
 * Бинарная колонка (тип IMAGE или BLOB).
 * 
 */
public final class BinaryColumn extends Column {

	private String defaultvalue;

	public BinaryColumn(Table table, String name) throws ParseException {
		super(table, name);
	}

	@Override
	protected void setDefault(String lexvalue) {
		defaultvalue = lexvalue;
	}

	/**
	 * Возвращает значение по умолчанию.
	 */
	public String getDefaultValue() {
		return defaultvalue;
	}

}
