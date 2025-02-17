package sk.tuke.meta.persistence;

import query.QueryBuilder;
import object.ObjectFactory;

import javax.persistence.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ReflectivePersistenceManager implements PersistenceManager {

    private final Connection connection;
    private final Class<?>[] types;
    private final QueryBuilder builder;
    private final ObjectFactory factory;

    public ReflectivePersistenceManager(Connection connection, Class<?>... types) {
        this.connection = connection;
        this.types = types;
        this.builder = new QueryBuilder();
        this.factory = new ObjectFactory(this);
    }

    @Override
    public void createTables() {
        System.out.println("Creating tables");
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("db.sql");
        try {
            String[] statements = new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";");
            for (String s: Arrays.copyOf(statements, statements.length-1)) {
                connection.prepareStatement(s).execute();
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public <T> Optional<T> get(Class<T> type, long id) {
        try {
            ResultSet result = connection.prepareStatement(builder.buildGetStatement(type, id)).executeQuery();

            T o;

            if(result.next()){
                o = factory.createObject(type,result);
            }
            else{
                o = null;
            }

            if(o == null){
                return Optional.empty();
            }
            else{
                return Optional.of(o);
            }

        }
        catch (Exception e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public <T> List<T> getAll(Class<T> type) {
        try {
            ResultSet results = connection.prepareStatement(builder.buildGetStatement(type)).executeQuery();

            List<T> resultList = new ArrayList<>();

            while(results.next()){
                resultList.add(factory.createObject(type, results));
            }

            System.out.println(resultList);

            return resultList;
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public <T> List<T> getBy(Class<T> type, String fieldName, Object value) {
        try {
            ResultSet results = connection.prepareStatement(
                    builder.buildGetStatement(type, fieldName, value)).executeQuery();

            List<T> resultList = new ArrayList<>();

            while(results.next()){
                resultList.add(factory.createObject(type, results));
            }

            return resultList;
        }
        catch (Exception e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public long save(Object entity) {

        if(entity == null){
            return -1;
        }

        if(Proxy.isProxyClass(entity.getClass())){
            Class<?> proxyInterface = Arrays.stream(entity.getClass().getInterfaces()).findFirst().get();
            Class<?> entityClass;
            try {
                entityClass = Class.forName(proxyInterface.getPackageName() + "." + proxyInterface.getSimpleName().substring(1));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            String idName = Arrays.stream(entityClass.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Id.class)).findAny().get().getName();
            String getIdMethod = "get" + idName.substring(0,1).toUpperCase() + idName.substring(1);

            try {
                return (long) Proxy.getInvocationHandler(entity).invoke(entity, proxyInterface.getDeclaredMethod(getIdMethod), null);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

        }

        Class<?> entityClass = entity.getClass();

        Field[] fields = entityClass.getDeclaredFields();
        try {
            PreparedStatement statm;
            String idName = Arrays.stream(fields)
                    .filter(field -> field.isAnnotationPresent(Id.class)).findAny().get().getName();
            String getIdMethod = "get" + idName.substring(0,1).toUpperCase() + idName.substring(1);

            int i = 1;

            Long idValue;
            idValue = (Long) entityClass.getDeclaredMethod(getIdMethod).invoke(entity);

            if(idValue == 0){
                statm = connection.prepareStatement(builder.buildSaveStatement(entity));
            }
            else {
                statm = connection.prepareStatement(builder.buildUpdateStatement(entity));
            }

            for(Field f : fields){
                String fName = f.getName();
                String methodName = "get" + fName.substring(0,1).toUpperCase() + fName.substring(1);
                if(f.isAnnotationPresent(Id.class)){
                    if(!(idValue == 0)){
                        int idIndex = fields.length - Arrays.stream(fields)
                                .filter(field -> field.isAnnotationPresent(Transient.class)).toArray().length;
                        statm.setLong(idIndex, idValue);
                    }
                }
                else if(f.isAnnotationPresent(ManyToOne.class)){
                    long id = save(entityClass.getDeclaredMethod(methodName).invoke(entity));
                    if(id == -1){
                        statm.setObject(i, null);
                    }
                    else {
                        statm.setLong(i, id);
                    }
                    i++;
                }
                else if(f.isAnnotationPresent(Transient.class)){
                    continue;
                }
                else {
                    Object obj;
                    obj = entityClass.getDeclaredMethod(methodName).invoke(entity);
                    statm.setObject(i, obj);
                    i++;
                }
            }

            statm.executeUpdate();

            ResultSet key = statm.getGeneratedKeys();

            if(idValue == 0) {
                key.next();

                for (Field f : entity.getClass().getDeclaredFields()) {
                    if (f.isAnnotationPresent(Id.class)) {
                        String fieldName = f.getName();
                        String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                        entity.getClass().getDeclaredMethod(
                                        methodName, f.getType())
                                .invoke(entity, key.getLong(1));
                    }
                }
            }
            else{
                return idValue;
            }

            return key.getLong(1);
        }
        catch (Exception e){
            throw new PersistenceException(e);
        }
    }

    @Override
    public void delete(Object entity) {
        try {
            PreparedStatement statm = connection.prepareStatement(builder.buildDeleteStatement(entity));
            statm.executeUpdate();
        }
        catch (Exception e){
            throw new PersistenceException(e);
        }
    }

}
