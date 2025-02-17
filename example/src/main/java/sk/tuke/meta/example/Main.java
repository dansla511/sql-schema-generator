package sk.tuke.meta.example;

import sk.tuke.meta.persistence.PersistenceManager;
import sk.tuke.meta.persistence.AtomicPresistenceOperatio;
import sk.tuke.meta.persistence.ReflectivePersistenceManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class Main {
    public static final String DB_PATH = "test.db";

    @AtomicPresistenceOperatio
    public static void setAgeAllPersons(PersistenceManager manager) {
        var persons = manager.getAll(Person.class);
        for(var person: persons) {
            person.setAge(1);
            //if(person.getId() == 3) person.setName(null);
            manager.save(person);
        }
    }
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);

        sk.tuke.meta.example.GeneratedPersistenceManager manager = new sk.tuke.meta.example.GeneratedPersistenceManager(conn);

        manager.createTables();

        Department development = new Department("Development", "DVLP");
        Department marketing = new Department("Marketing", "MARK");
        Department operations = new Department("Operations", "OPRS");

        Person hrasko = new Person("Janko", "Hrasko", 30);
        hrasko.setDepartment(development);
        Person mrkvicka = new Person("Jozko", "Mrkvicka", 25);
        mrkvicka.setDepartment(development);
        Person novak = new Person("Jan", "Novak", 45);
        novak.setDepartment(marketing);

        manager.save(hrasko);
        manager.save(mrkvicka);
        manager.save(novak);

        manager.get(Person.class, hrasko.getId());

        List<Person> persons = manager.getAll(Person.class);
        for (Person person : persons) {
            System.out.println(person);
            System.out.println("  " + person.getDepartment());
        }

        persons.get(0).setName("Samko");

        manager.save(persons.get(0));

        persons = manager.getAll(Person.class);

        for (Person person : persons) {
            System.out.println(person);
            System.out.println("  " + person.getDepartment());
        }

        manager.delete(persons.get(1));

        persons = manager.getAll(Person.class);

        for (Person person : persons) {
            System.out.println(person);
            System.out.println("  " + person.getDepartment());
        }

        System.out.println(manager.get(Person.class,novak.getId()));

        setAgeAllPersons(manager);

        persons = manager.getAll(Person.class);

        for (Person person : persons) {
            System.out.println(person);
            System.out.println("  " + person.getDepartment());
        }

        conn.close();
    }

}
