package db;

import db.SchemaInitializer;

public class TestSchema {
    public static void main(String[] args) {
        System.out.println("Testing Schema Initialization...");
        SchemaInitializer.initialize();
        System.out.println("Schema Initialization Test Completed.");
    }
}