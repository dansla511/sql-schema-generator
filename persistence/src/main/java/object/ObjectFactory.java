package object;

import loading.LazyLoader;
import sk.tuke.meta.persistence.EntityDAO;
import sk.tuke.meta.persistence.PersistenceManager;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.Optional;

public class ObjectFactory {

    private PersistenceManager pManager = null;
    private EntityDAO<?> eManager = null;

    public ObjectFactory(PersistenceManager manager){
        this.pManager = manager;
    }
    public ObjectFactory(EntityDAO<?> manager) {this.eManager = manager; }
    private Class<?> primitiveToWrapper(Class<?> type){
        return switch (type.getName()) {
            case "boolean" -> Boolean.class;
            case "char" -> Character.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            default -> null;
        };
    }

    public <T> T createObject(Class<T> type, ResultSet result){

        try {

            Class<?> definitiveType = type;

            T obj;
            if(type.isInterface()){
                try {
                    definitiveType = Class.forName(type.getPackageName() + "." + type.getSimpleName().substring(1));
                } catch (ClassNotFoundException e) {
                    throw new PersistenceException(e);
                }
                obj = (T) definitiveType.getDeclaredConstructor().newInstance();
            }
            else{
                obj = type.getDeclaredConstructor().newInstance();
            }

            for(Field f : definitiveType.getDeclaredFields()){
                String methodName = "set" + f.getName().substring(0, 1).toUpperCase() + f.getName().substring(1);
                if(f.isAnnotationPresent(Transient.class)){
                    continue;
                }
                else if(f.getType().isPrimitive()){
                    obj.getClass().getDeclaredMethod(methodName, f.getType())
                            .invoke(obj, result.getObject(getFieldName(f), primitiveToWrapper(f.getType())));
                }
                else if(f.getType().equals(String.class)){
                    obj.getClass().getDeclaredMethod(methodName, f.getType())
                            .invoke(obj, result.getString(getFieldName(f)));
                }
                else{
                    if(result.getLong(getFieldName(f)) == 0){
                        obj.getClass().getDeclaredMethod(methodName, f.getType())
                                .invoke(obj, (Object) null);
                    }
                    //create proxy if lazy loading is enabled, otherwise get the object
                    else if(f.getAnnotation(ManyToOne.class).fetch() == FetchType.LAZY){

                        LazyLoader l;

                        if(pManager != null) {
                            l = new LazyLoader(
                                    result.getLong(getFieldName(f)),
                                    Class.forName(f.getAnnotation(ManyToOne.class).targetEntity().getCanonicalName()),
                                    pManager,Class.forName(f.getAnnotation(ManyToOne.class).targetEntity().getCanonicalName()));
                        }
                        else{
                            l = new LazyLoader(
                                    result.getLong(getFieldName(f)),
                                    Class.forName(f.getAnnotation(ManyToOne.class).targetEntity().getCanonicalName()),
                                    eManager,Class.forName(f.getAnnotation(ManyToOne.class).targetEntity().getCanonicalName()));
                        }

                        Object proxy = Proxy.newProxyInstance(
                            f.getType().getClassLoader(),
                            new Class[]{ f.getType() }, l
                        );

                        obj.getClass().getDeclaredMethod(methodName, f.getType())
                                .invoke(obj, proxy);
                    }
                    else {

                        Optional<?> queryResult;

                        if(pManager != null){
                            queryResult = pManager.get(f.getType(), result.getLong(getFieldName(f)));
                        }
                        else{
                            queryResult = eManager.get(result.getLong(getFieldName(f)));
                        }

                        if (queryResult.isPresent()) {
                            obj.getClass().getDeclaredMethod(methodName, f.getType())
                                    .invoke(obj, queryResult.get());
                        } else {
                            obj.getClass().getDeclaredMethod(methodName, f.getType())
                                    .invoke(obj, (Object) null);
                        }
                    }

                }
            }

            return obj;
        } catch (Exception e) {
            throw new PersistenceException(e);
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
