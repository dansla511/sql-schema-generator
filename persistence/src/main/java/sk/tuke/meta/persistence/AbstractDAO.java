package sk.tuke.meta.persistence;

import query.QueryBuilder;

import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceException;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

public abstract class AbstractDAO<T> implements EntityDAO<T>{

    protected final Connection connection;

    protected final QueryBuilder builder;

    public AbstractDAO(Connection connection){
        this.connection = connection;
        this.builder = new QueryBuilder();
    }
    @Override
    public void createTable() {

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
