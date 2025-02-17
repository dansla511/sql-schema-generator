package loading;

import sk.tuke.meta.persistence.EntityDAO;
import sk.tuke.meta.persistence.PersistenceManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Optional;

public class LazyLoader implements InvocationHandler {

    private Object target;
    private PersistenceManager pManager = null;
    private EntityDAO<?> eManager = null;
    private final long id;
    private boolean loaded = false;
    private final Class<?> type;

    public LazyLoader(long id, Object target, PersistenceManager manager, Class<?> type){
        this.target = target;
        this.pManager = manager;
        this.id = id;
        this.type = type;
    }

    public LazyLoader(long id, Object target, EntityDAO<?> manager, Class<?> type){
        this.target = target;
        this.eManager = manager;
        this.id = id;
        this.type = type;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(!loaded){
            target = pManager == null ? eManager.get(id) : pManager.get(type, id);
            loaded = true;
        }

        return method.invoke(((Optional<?>) target).get(), args);
    }

    public long getId(){
        return this.id;
    }
}
