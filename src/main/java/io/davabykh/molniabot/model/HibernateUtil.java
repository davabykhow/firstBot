package io.davabykh.molniabot.model;
import java.util.Properties;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;


public class HibernateUtil {
    static SessionFactory factory = null;
    static {
        Configuration cfg = new Configuration();
        Properties prop = new Properties();
        prop.put(Environment.URL, "jdbc:mysql://localhost:3306/tg-bot");
        prop.put(Environment.USER, "tgbot");
        prop.put(Environment.PASS, "abc123#@!");
        prop.put(Environment.SHOW_SQL, true);
        prop.put(Environment.FORMAT_SQL, true);
        prop.put(Environment.HBM2DDL_AUTO, "update");
        cfg.setProperties(prop);
        cfg.addAnnotatedClass(User.class);
        factory = cfg.buildSessionFactory();
    }
    public static SessionFactory getSessionFactory() {
        return factory;
    }
    public static Session getSession() {
        return factory.openSession();
    }
}