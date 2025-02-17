package sk.tuke.meta.example;

import org.junit.jupiter.api.Test;
import sk.tuke.meta.persistence.PersistenceManager;
import sk.tuke.meta.persistence.ReflectivePersistenceManager;

import javax.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;

/*Autor unit testov Damian Jankov */

public class UnitTesting {
    public static final String DB_PATH = "test.db";
    public Connection conn;

    public UnitTesting(){
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void MakeObjectFetchFromDB() {

        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();

        CheckPerson(hrasko,hraskoFromDb);

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void EmptyDepartmentNull() {
        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();
        Person hrasko = new Person("Janko", "Hrasko", 30);
        manager.save(hrasko);

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,hraskoFromDb);
    }


    @Test
    public void EmptyDepartment() {
        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development=new Department();
        hrasko.setDepartment(development);
        Optional<Person> retHrasko= manager.get(Person.class,manager.save(hrasko));
        Person rethrasko=retHrasko.get();
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,rethrasko);
    }


    @Test
    public void SpecialChar() {
        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();
        Person hrasko = new Person("Jan'\0ko", "Hra'sko", 30);
        manager.save(hrasko);

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,hraskoFromDb);
    }

    @Test
    public void MultiplePeople(){
        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();

        Department development = new Department("Development", "DVLP");
        Department marketing = new Department("Marketing", "MARK");
        Department operations = new Department("Operations", "OPRS");

        Person hrasko = new Person("Janko", "Hrasko", 30);
        hrasko.setDepartment(development);
        Person mrkvicka = new Person("Jozko", "Mrkvicka", 25);
        mrkvicka.setDepartment(operations);
        Person novak = new Person("Jan", "Novak", 45);
        novak.setDepartment(marketing);

        long hraskoId = manager.save(hrasko);
        long mrkvickaId = manager.save(mrkvicka);
        long novakId = manager.save(novak);

        List<Person> People = manager.getAll(Person.class);

        //assuming that only one item in db
        List<Person> retMrkvicka= People.stream()
                .filter(person -> person.getId() == mrkvickaId ).toList();

        List<Person> retHrasko= People.stream()
                .filter(person -> person.getId() == hraskoId).toList();

        List<Person> retNovak= People.stream()
                .filter(person -> person.getId() == novakId).toList();

        CheckPerson(hrasko,retHrasko.get(0));
        CheckPerson(mrkvicka,retMrkvicka.get(0));
        CheckPerson(novak,retNovak.get(0));
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void GetBy(){
        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();

        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);
        manager.save(hrasko);

        List<Person> retHrasko=manager.getBy(Person.class,"surname","Janko");

        CheckPerson(hrasko,retHrasko.get(0));

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void UpdateSimpleField(){

        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);
        manager.save(hrasko);

        hrasko.setAge(50);
        hrasko.setName("Peter");
        hrasko.setSurname("Novak");

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();

        CheckPerson(hrasko,hraskoFromDb);

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void UpdateSubObjectNew(){

        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);
        manager.save(hrasko);

        Department marketing = new Department("Marketing", "MARK");
        hrasko.setDepartment(marketing);

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();

        CheckPerson(hrasko,hraskoFromDb);

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void UpdateFieldOfSubObject(){

        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);
        manager.save(hrasko);
        development.setName("devops");
        manager.save(development);

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();

        CheckPerson(hrasko,hraskoFromDb);

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void DeleteItemFromDb(){
        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);
        long hraskoId = manager.save(hrasko);
        manager.delete(hrasko);
        Optional<Person> returned=manager.get(Person.class, hraskoId);

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if(returned.isPresent()){
            throw new RuntimeException("Problem with deleting from db");
        }
    }


    @Test
    public void DeleteSubItemFromDb(){
        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);

        long hraskoId=manager.save(hrasko);
        manager.delete(development);

        hrasko.setDepartment(null);
        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,hraskoId);
        Person hraskoFromDb=hraskoFromDbOptional.get();

        CheckPerson(hrasko,hraskoFromDb);

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


    @Test

    public void OneDepartmentMultiplePerson(){
        PersistenceManager manager= new ReflectivePersistenceManager(conn);
        manager.createTables();
        Department development = new Department("test_dep", "TEST");

        Person hrasko = new Person("Janko", "Hrasko", 30);
        hrasko.setDepartment(development);
        Person mrkvicka = new Person("Jozko", "Mrkvicka", 25);
        mrkvicka.setDepartment(development);

        long hraskoId = manager.save(hrasko);
        long mrkvickaId = manager.save(mrkvicka);

        List<Department> departments=manager.getBy(Department.class, "code", "TEST");
        if(departments.size()>1){
            throw new PersistenceException("Pocet departmentov je viac ako 1 pri rovnakom objekte!");
        }
        List<Person> People=manager.getAll(Person.class);

        List<Person> retMrkvicka= People.stream()
                .filter(person -> person.getId() == mrkvickaId).toList();


        List<Person> retHrasko= People.stream()
                .filter(person -> person.getId() == hraskoId).toList();

        if(retMrkvicka.size()>1 || retHrasko.size()>1 ){
            throw new PersistenceException();
        }

        CheckPerson(hrasko,retHrasko.get(0));
        CheckPerson(mrkvicka,retMrkvicka.get(0));

        try {
            manager.delete(development);
            conn.close();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }


    public void CheckPerson(Person before, Person after){
        try {
            assertEquals(before.getName(), after.getName());
            assertEquals(before.getSurname(), after.getSurname());
            assertEquals(before.getAge(), after.getAge());
            CheckDepartment(before.getDepartment(), after.getDepartment());
        }
        catch (Exception e){
            try {
                conn.close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }

    public void CheckDepartment(IDepartment before,IDepartment after){
        if (before == null && after == null) {
            return; // Both objects are null, so the test passes.
        }
        assertEquals(before.getName(),after.getName());
        assertEquals(before.getCode(),after.getCode());
    }
}