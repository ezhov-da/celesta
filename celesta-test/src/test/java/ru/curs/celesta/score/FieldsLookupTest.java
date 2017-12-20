package ru.curs.celesta.score;

import static org.junit.jupiter.api.Assertions.*;

import javafx.scene.control.Tab;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.curs.celesta.*;
import ru.curs.celesta.dbutils.filter.value.FieldsLookup;

import java.util.Map;
import java.util.function.Function;

/**
 * Created by ioann on 07.06.2017.
 */
public class FieldsLookupTest {

    private static Table tableA;
    private static Table tableB;
    private static Table tableC;
    private static View viewA;
    private static View viewB;
    private static View viewC;

    private static Runnable lookupChangeCallback = () -> {
    };
    private static Function<FieldsLookup, Void> newLookupCallback = (f) -> null;

    @BeforeAll
    public static void init() throws ParseException {
        Score score = new Score();
        Grain grain = new Grain(score, "test");

        tableA = generateTable(grain, "a");
        tableB = generateTable(grain, "b");
        tableC = generateTable(grain, "c");

        viewA = generateView(grain, "aV", tableA);
        viewB = generateView(grain, "bV", tableB);
        viewC = generateView(grain, "cV", tableC);
    }

    private static Table generateTable(Grain grain, String name) throws ParseException {
        Table table = new Table(grain, name);

        Column c1 = new IntegerColumn(table, name + "1");
        Column c2 = new IntegerColumn(table, name + "2");
        Column c3 = new StringColumn(table, name + "3");
        Column c4 = new DateTimeColumn(table, name + "4");

        String[] indexColumns = {name + "1", name + "2", name + "3"};
        Index i1 = new Index(table, "I" + name, indexColumns);

        return table;
    }

    private static View generateView(Grain grain, String name, Table table) throws ParseException {
        View view = new View(grain, name);
        view.addFromTableRef(new TableRef(table, table.getName()));

        view.getColumns().put(table.getName() + "1", new ViewColumnMeta(ViewColumnType.INT));
        view.getColumns().put(table.getName() + "2", new ViewColumnMeta(ViewColumnType.INT));
        view.getColumns().put(table.getName() + "3", new ViewColumnMeta(ViewColumnType.TEXT));
        view.getColumns().put(table.getName() + "4", new ViewColumnMeta(ViewColumnType.DATE));

        return view;
    }


    @Test
    public void testAddWhenBothColumnsExist() throws Exception {
        FieldsLookup lookup = new FieldsLookup(tableA, tableB, lookupChangeCallback, newLookupCallback);
        lookup.add("a1", "b1");

        lookup = new FieldsLookup(viewA, viewB, lookupChangeCallback, newLookupCallback);
        lookup.add("a1", "b1");
    }

    @Test
    public void testAddWhenLeftColumnDoesNotExist() throws Exception {
        final FieldsLookup lookup = new FieldsLookup(tableA, tableB, lookupChangeCallback, newLookupCallback);
        assertThrows(ParseException.class, () -> lookup.add("notExistedField", "b1"));

        final FieldsLookup lookup2 = new FieldsLookup(viewA, viewB, lookupChangeCallback, newLookupCallback);
        assertThrows(ParseException.class, () -> lookup2.add("notExistedField", "b1"));
    }

    @Test
    public void testAddWhenRightColumnDoesNotExist() throws Exception {
        final FieldsLookup lookup = new FieldsLookup(tableA, tableB, lookupChangeCallback, newLookupCallback);
        assertThrows(ParseException.class, () -> lookup.add("a1", "notExistedField"));

        final FieldsLookup lookup2 = new FieldsLookup(viewA, viewB, lookupChangeCallback, newLookupCallback);
        assertThrows(ParseException.class, () -> lookup2.add("a1", "notExistedField"));
    }

    @Test
    public void testAddWhenBothColumnsDoNotExist() throws Exception {
        FieldsLookup lookup = new FieldsLookup(tableA, tableB, lookupChangeCallback, newLookupCallback);
        assertThrows(ParseException.class, () -> lookup.add("notExistedField", "notExistedField"));

        FieldsLookup lookup2 = new FieldsLookup(viewA, viewB, lookupChangeCallback, newLookupCallback);
        assertThrows(ParseException.class, () -> lookup2.add("notExistedField", "notExistedField"));
    }

    @Test
    public void testWithCorrectInputData() throws Exception {
        FieldsLookup lookup = new FieldsLookup(tableA, tableB, lookupChangeCallback, newLookupCallback);
        lookup.add("a1", "b1");
        lookup.add("a2", "b2");
        lookup.add("a3", "b3");

        FieldsLookup lookup2 = new FieldsLookup(viewA, viewB, lookupChangeCallback, newLookupCallback);
        lookup2.add("a1", "b1");
        lookup2.add("a2", "b2");
        lookup2.add("a3", "b3");
    }


    @Test
    public void testWithCorrectInputDataAndAdditionalLookup() throws Exception {
        FieldsLookup lookup = new FieldsLookup(tableA, tableB, lookupChangeCallback, newLookupCallback);
        lookup.add("a1", "b1");
        lookup.add("a2", "b2");
        lookup.add("a3", "b3");

        lookup = lookup.and(tableC);

        lookup.add("a1", "c1");
        lookup.add("a2", "c2");
        lookup.add("a3", "c3");

        FieldsLookup lookup2 = new FieldsLookup(viewA, viewB, lookupChangeCallback, newLookupCallback);
        lookup2.add("a1", "b1");
        lookup2.add("a2", "b2");
        lookup2.add("a3", "b3");

        lookup2 = lookup2.and(viewC);

        lookup2.add("a1", "c1");
        lookup2.add("a2", "c2");
        lookup2.add("a3", "c3");
    }

    @Test
    public void testWhenIndicesDoNotMatchForTable() throws Exception {
        FieldsLookup lookup = new FieldsLookup(tableA, tableB, lookupChangeCallback, newLookupCallback);
        lookup.add("a1", "b1");
        assertThrows(CelestaException.class, () -> lookup.add("a3", "b3"));
    }


    @Test
    public void testIndependenceFromIndicesForView() throws Exception {
        FieldsLookup lookup = new FieldsLookup(viewA, viewB, lookupChangeCallback, newLookupCallback);
        lookup.add("a1", "b1");
        lookup.add("a3", "b3");
    }

    @Test
    public void testWhenIndicesDoNotMatchInAdditionalLookupForTable() throws Exception {
        FieldsLookup lookup = new FieldsLookup(tableA, tableB, lookupChangeCallback, newLookupCallback);
        lookup.add("a1", "b1");
        lookup.add("a2", "b2");
        lookup.add("a3", "b3");

        FieldsLookup anotherLookup = lookup.and(tableC);

        anotherLookup.add("a1", "c1");
        assertThrows(CelestaException.class, () -> anotherLookup.add("a3", "c3"));
    }

    @Test
    public void testIndependenceFromIndicesInAdditionalLookupForView() throws Exception {
        FieldsLookup lookup = new FieldsLookup(viewA, viewB, lookupChangeCallback, newLookupCallback);
        lookup.add("a1", "b1");
        lookup.add("a2", "b2");
        lookup.add("a3", "b3");

        FieldsLookup anotherLookup = lookup.and(viewC);

        anotherLookup.add("a1", "c1");
        anotherLookup.add("a3", "c3");
    }

    @Test
    void testImpossibleToAddNonTargetClassLookupToExistedLookup() throws Exception {
        final FieldsLookup lookup = new FieldsLookup(tableA, tableB, lookupChangeCallback, newLookupCallback);
        assertThrows(CelestaException.class, () -> lookup.and(viewC));

        final FieldsLookup lookup2 = new FieldsLookup(viewA, viewB, lookupChangeCallback, newLookupCallback);
        assertThrows(CelestaException.class, () -> lookup2.and(tableC));
    }
}