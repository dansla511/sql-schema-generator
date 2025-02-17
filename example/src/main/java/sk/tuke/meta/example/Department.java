package sk.tuke.meta.example;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "dep")
public class Department implements IDepartment {
    @Id
    @Column(name = "id")
    private long pk;
    @Column(nullable = false)
    private String name;
    @Column
    private String code;

    public Department() {
    }

    public Department(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public long getPk() { return pk; }

    public void setPk(long pk) {
        this.pk = pk;
    }


    public String toString() {
        return String.format("Department %d: %s (%s)", pk, name, code);
    }
}
