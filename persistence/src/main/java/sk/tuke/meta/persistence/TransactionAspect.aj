package sk.tuke.meta.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

public aspect TransactionAspect {
    private Connection conn;

    after(PersistenceManager manager): execution(PersistenceManager+.new(..)) && this(manager){

        this.conn = (Connection) Arrays.stream(thisJoinPoint.getArgs()).findFirst().get();

    }

    before(): @annotation(AtomicPresistenceOperatio) && execution(* *(..)){
        System.out.println("Caught transaction");
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    after(): @annotation(AtomicPresistenceOperatio) && execution(* *(..)){
        System.out.println("Commiting transaction");
        try {
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    before(): handler(*) && cflow(@annotation(AtomicPresistenceOperatio) && execution(* *(..))){
        System.out.println("Rolling back transaction");
        try {
            conn.rollback();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
