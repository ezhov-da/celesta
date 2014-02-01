package ru.curs.celesta.score;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Объект-представление в метаданных.
 */
public class View extends NamedElement {

	private Grain grain;

	private boolean distinct;
	private final Map<String, Expr> columns = new LinkedHashMap<>();
	private final Map<String, TableRef> tables = new LinkedHashMap<>();
	private Expr whereCondition;

	private final Map<String, Expr> unmodifiableColumns = Collections
			.unmodifiableMap(columns);
	private final Map<String, TableRef> unmodifiableTables = Collections
			.unmodifiableMap(tables);

	View(Grain grain, String name) throws ParseException {
		super(name);
		if (grain == null)
			throw new IllegalArgumentException();
		this.grain = grain;
		grain.addView(this);

	}

	public View(Grain grain, String name, String sql) throws ParseException {
		this(grain, name);
		StringReader sr = new StringReader(sql);
		CelestaParser parser = new CelestaParser(sr);
		try {
			try {
				parser.select(this);
			} finally {
				sr.close();
			}
			finalizeParsing();
		} catch (ParseException e) {
			delete();
			throw e;
		}
	}

	/**
	 * Возвращает гранулу, к которой относится представление.
	 */
	public Grain getGrain() {
		return grain;
	}

	/**
	 * Использовано ли слово DISTINCT в запросе представления.
	 */
	public boolean isDistinct() {
		return distinct;
	}

	/**
	 * Устанавливает использование слова DISTINCT в запросе представления.
	 * 
	 * @param distinct
	 *            Если запрос имеет вид SELECT DISTINCT.
	 */
	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	/**
	 * Добавляет колонку к представлению.
	 * 
	 * @param alias
	 *            Алиас колонки.
	 * @param expr
	 *            Выражение колонки.
	 * @throws ParseException
	 *             Неуникальное имя алиаса или иная семантическая ошибка
	 */
	void addColumn(String alias, Expr expr) throws ParseException {
		if (expr == null)
			throw new IllegalArgumentException();

		if (alias == null || alias.isEmpty())
			throw new ParseException(String.format(
					"View '%s' contains a column with undefined alias.",
					getName()));
		if (columns.containsKey(alias))
			throw new ParseException(
					String.format(
							"View '%s' already contains column with name or alias '%s'. Use unique aliases for view columns.",
							getName(), alias));

		columns.put(alias, expr);
	}

	/**
	 * Добавляет ссылку на таблицу к представлению.
	 * 
	 * @param ref
	 *            Ссылка на таблицу.
	 * @throws ParseException
	 *             Неуникальный алиас или иная ошибка.
	 */
	void addFromTableRef(TableRef ref) throws ParseException {
		if (ref == null)
			throw new IllegalArgumentException();

		String alias = ref.getAlias();
		if (alias == null || alias.isEmpty())
			throw new ParseException(String.format(
					"View '%s' contains a table with undefined alias.",
					getName()));
		if (tables.containsKey(alias))
			throw new ParseException(
					String.format(
							"View '%s' already contains table with name or alias '%s'. Use unique aliases for view tables.",
							getName(), alias));

		tables.put(alias, ref);

		Expr onCondition = ref.getOnExpr();
		if (onCondition != null) {
			onCondition.resolveFieldRefs(new ArrayList<>(tables.values()));
			onCondition.validateTypes();
		}
	}

	/**
	 * Возвращает перечень столбцов представления.
	 */
	public Map<String, Expr> getColumns() {
		return unmodifiableColumns;
	}

	/**
	 * Возвращает перечень from-таблиц представления.
	 */
	public Map<String, TableRef> getTables() {
		return unmodifiableTables;
	}

	/**
	 * Финализирует разбор представления, разрешая ссылки на поля и проверяя
	 * типы выражений.
	 * 
	 * @throws ParseException
	 *             ошибка проверки типов или разрешения ссылок.
	 */
	void finalizeParsing() throws ParseException {
		List<TableRef> t = new ArrayList<>(tables.values());
		for (Expr e : columns.values()) {
			e.resolveFieldRefs(t);
			e.validateTypes();
		}
		if (whereCondition != null) {
			whereCondition.resolveFieldRefs(t);
			whereCondition.validateTypes();
		}
	}

	/**
	 * Возвращает условие where для SQL-запроса.
	 */
	public Expr getWhereCondition() {
		return whereCondition;
	}

	/**
	 * Устанавливает условие where для SQL-запроса.
	 * 
	 * @param whereCondition
	 *            условие where.
	 * @throws ParseException
	 *             если тип выражения неверный.
	 */
	void setWhereCondition(Expr whereCondition) throws ParseException {
		if (whereCondition != null)
			whereCondition.assertType(ExprType.LOGIC);
		this.whereCondition = whereCondition;
	}

	void selectScript(BufferedWriter bw, SQLGenerator gen) throws IOException {
		bw.write("  select ");
		if (distinct)
			bw.write("distinct ");
		boolean cont = false;
		for (Map.Entry<String, Expr> e : columns.entrySet()) {
			if (cont)
				bw.write(", ");
			bw.write(gen.generateSQL(e.getValue()));
			bw.write(" as ");
			bw.write(e.getKey());
			cont = true;
		}
		bw.newLine();
		bw.write("  from ");
		cont = false;
		for (TableRef tRef : tables.values()) {
			if (cont) {
				bw.newLine();
				bw.write(String
						.format("    %s ", tRef.getJoinType().toString()));
				bw.write("join ");
			}
			Table t = tRef.getTable();
			if (t.getGrain() == getGrain()) {
				bw.write(String.format("%s as %s", t.getName(), tRef.getAlias()));
			} else {
				bw.write(String.format("%s.%s as %s", t.getGrain().getName(),
						t.getName(), tRef.getAlias()));
			}
			if (cont) {
				bw.write(" on ");
				bw.write(gen.generateSQL(tRef.getOnExpr()));
			}
			cont = true;
		}
		if (whereCondition != null) {
			bw.newLine();
			bw.write("  where ");
			bw.write(gen.generateSQL(whereCondition));
		}
	}

	/**
	 * Создаёт скрипт CREATE VIEW в различных диалектах SQL, используя паттерн
	 * visitor.
	 * 
	 * @param bw
	 *            поток, в который происходит сохранение.
	 * @param gen
	 *            генератор-visitor
	 * @throws IOException
	 *             ошибка записи в поток
	 */
	public void createViewScript(BufferedWriter bw, SQLGenerator gen)
			throws IOException {
		bw.write(gen.preamble(this));
		bw.newLine();
		selectScript(bw, gen);
		bw.write(";");
		bw.newLine();
		bw.newLine();
	}

	void save(BufferedWriter bw) throws IOException {
		SQLGenerator gen = new SQLGenerator() {

			@Override
			protected String preamble(View view) {
				StringBuilder sb = new StringBuilder();
				sb.append("/**");
				sb.append(view.getCelestaDoc());
				sb.append("*/");
				sb.append("\n");
				sb.append(String.format("create view %s as", viewName(view)));
				return sb.toString();
			}

			@Override
			protected String viewName(View v) {
				return getName();
			}

			@Override
			protected String tableName(Table t) {
				return String.format("%s.%s", t.getGrain().getName(),
						t.getName());
			}

		};
		createViewScript(bw, gen);
	}

	/**
	 * Удаляет таблицу.
	 * 
	 * @throws ParseException
	 *             при попытке изменить системную гранулу
	 */
	public void delete() throws ParseException {
		grain.removeView(this);
	}
}