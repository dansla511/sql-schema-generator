package sk.tuke.meta.example;

import javax.persistence.*;

@Entity
@Table(name = "employee")
public class Person {
    @Id
    @Column
    private long id;
    @Column
    private String surname;
    @Column
    private String name;
    @Column
    private int age;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Department.class)
    @Column
    private IDepartment department;

    public Person(String surname, String name, int age) {
        this.surname = surname;
        this.name = name;
        this.age = age;
    }

    public Person() {
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


    public IDepartment getDepartment() {
        return department;
    }

    public void setDepartment(IDepartment department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return String.format("Person %d: %s %s (%d)", id, surname, name, age);
    }
}
