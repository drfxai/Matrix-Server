package javax.management;

public class ObjectName {
    private String name;

    public ObjectName(String name) {
        this.name = name;
    }

    public String getCanonicalName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
