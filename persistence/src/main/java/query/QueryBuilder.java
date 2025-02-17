package query;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class QueryBuilder {

    public String buildTableStatement(Class<?> type) {
        StringBuilder statement = new StringBuilder("CREATE TABLE IF NOT EXISTS " + type.getSimpleName() + "(\n");

        if(type.isAnnotationPresent(Entity.class)){
            int temp = 0;
            for (Field f: type.getDeclaredFields()) {
                temp++;
                if(f.isAnnotationPresent(Transient.class)){
                    continue;
                }

                statement.append(" ").append(f.getName()).append(" ");

                switch (f.getType().getSimpleName()) {
                    case "String" -> statement.append("TEXT");
                    case "float", "double" -> statement.append("REAL");
                    default -> statement.append("INTEGER");
                }

                if(f.isAnnotationPresent(Id.class)){
                    statement.append(" primary key autoincrement");
                }

                if(f.isAnnotationPresent(ManyToOne.class)){
                    Field idField = Arrays.stream(type.getDeclaredFields())
                            .filter(field -> field.isAnnotationPresent(Id.class)).findAny().get();
                    statement.append(",\n foreign key (").append(f.getName().toLowerCase()).append(") references ");
                    statement.append(f.getName()).append("(").append(idField.getName()).append(")");
                }

                if(temp != type.getDeclaredFields().length) {
                    statement.append(",\n");
                }
            }
            statement.append("\n)");
        }

        return statement.toString();
    }

    public String buildGetStatement(Class<?> type){

        return "SELECT * FROM " + getTableName(type);

    }

    public String buildGetStatement(Class<?> type, long id){
        StringBuilder statement = new StringBuilder("SELECT * FROM ");

        if(type.isInterface()){
            Class<?> nonInterfaceType = null;
            try {
                nonInterfaceType = Class.forName(type.getPackageName() + "." + type.getSimpleName().substring(1));
            } catch (ClassNotFoundException e) {
                throw new PersistenceException(e);
            }
            statement.append(getTableName(nonInterfaceType));

            for(Field f : nonInterfaceType.getDeclaredFields()){
                if(f.isAnnotationPresent(Id.class)){
                    statement.append(" WHERE ").append(getFieldName(f)).append("=").append(id);
                    break;
                }
                else if(f.isAnnotationPresent(Transient.class)){
                    continue;
                }
            }
        }
        else{
            statement.append(getTableName(type));

            for(Field f : type.getDeclaredFields()){
                if(f.isAnnotationPresent(Id.class)){
                    statement.append(" WHERE ").append(getFieldName(f)).append("=").append(id);
                    break;
                }
                else if(f.isAnnotationPresent(Transient.class)){
                    continue;
                }
            }
        }

        System.out.println(statement);

        return statement.toString();
    }

    public String buildGetStatement(Class<?> type, String fieldName, Object value){
        StringBuilder statement = new StringBuilder("SELECT * FROM ");

        statement.append(getTableName(type));

        boolean fieldExists = false;

        for(Field f : type.getDeclaredFields()){
            if(f.getName().equals(fieldName)){
                fieldExists = true;
                break;
            }
            else if(f.isAnnotationPresent(Transient.class)){
                continue;
            }
        }
        if(!fieldExists){
            throw new PersistenceException("fieldName doesn't exist");
        }

        if(value.getClass().equals(String.class)){
            statement.append(" WHERE ").append(fieldName).append("=").append("\"" + value +"\"");
        }
        else{
            statement.append(" WHERE ").append(fieldName).append("=").append(value);
        }

        System.out.println(statement);
        return statement.toString();
    }

    public String buildSaveStatement(Object entity){
        StringBuilder statement = new StringBuilder("INSERT INTO ");

        Class<?> entityClass = entity.getClass();

        if(Proxy.isProxyClass(entity.getClass())){
            Class<?> proxyInterface = Arrays.stream(entity.getClass().getInterfaces()).findFirst().get();
            try {
                entityClass = Class.forName(proxyInterface.getPackageName() + "." + proxyInterface.getSimpleName().substring(1));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        statement.append(getTableName(entityClass));

        statement.append("(");

        int temp = 0;
        int temp2 = 0;
        for(Field f : entityClass.getDeclaredFields()){
            if(f.isAnnotationPresent(Id.class)){
                temp2++;
                continue;
            }
            else if(f.isAnnotationPresent(Transient.class)){
                temp2++;
                continue;
            }
            temp++;
            statement.append(getFieldName(f));
            if(temp != (entityClass.getDeclaredFields().length - temp2)){
                statement.append(",");
            }
        }

        statement.append(") VALUES (").append("?,".repeat(temp - 1)).append("?)");

        System.out.println(statement);

        return statement.toString();
    }

    public String buildUpdateStatement(Object entity){
        StringBuilder statement = new StringBuilder("UPDATE ");

        Class<?> entityClass = entity.getClass();

        if(Proxy.isProxyClass(entity.getClass())){
            Class<?> proxyInterface = Arrays.stream(entity.getClass().getInterfaces()).findFirst().get();
            try {
                entityClass = Class.forName(proxyInterface.getPackageName() + "." + proxyInterface.getSimpleName().substring(1));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        statement.append(getTableName(entityClass));

        statement.append(" SET ");

        int temp = 1;
        int temp2 = 0;
        for(Field f : entityClass.getDeclaredFields()){
            if(f.isAnnotationPresent(Id.class)){
                continue;
            }
            else if(f.isAnnotationPresent(Transient.class)){
                temp2++;
                continue;
            }
            temp++;
            statement.append(getFieldName(f)).append(" = ?");
            if(temp != (entityClass.getDeclaredFields().length - temp2)){
                statement.append(",");
            }
        }

        statement.append(" WHERE ");
        statement.append(getFieldName(Arrays.stream(entityClass.getDeclaredFields())
                            .filter(field -> field.isAnnotationPresent(Id.class)).findAny().get()));

        statement.append(" = ? ;");

        System.out.println(statement);

        return statement.toString();
    }

    public String buildDeleteStatement(Object entity) throws Exception {

        StringBuilder statement = new StringBuilder("DELETE FROM ");

        Class<?> entityClass = entity.getClass();

        if(Proxy.isProxyClass(entity.getClass())){
            Class<?> proxyInterface = Arrays.stream(entity.getClass().getInterfaces()).findFirst().get();
            try {
                entityClass = Class.forName(proxyInterface.getPackageName() + "." + proxyInterface.getSimpleName().substring(1));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        statement.append(getTableName(entityClass));

        Field idField = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class)).findAny().get();

        statement.append(" WHERE ").append(getFieldName(idField)).append("=");
        statement.append(entityClass.getDeclaredMethod(
                "get" + idField.getName().substring(0,1).toUpperCase() + idField.getName().substring(1))
                .invoke(entity));

        System.out.println(statement);

        return statement.toString();

    }

    private String getTableName(Class<?> table){
        if(!table.getAnnotation(Table.class).name().isEmpty()){
            return table.getAnnotation(Table.class).name();
        }
        else{
            return table.getSimpleName();
        }
    }

    private String getFieldName(Field field){
        if(!field.getAnnotation(Column.class).name().isEmpty()){
            return field.getAnnotation(Column.class).name();
        }
        else{
            return field.getName();
        }
    }
}
