package javax.management;

public class InstanceNotFoundException extends JMException {
    private static final long serialVersionUID = 1L;

    public InstanceNotFoundException() {
        super();
    }

    public InstanceNotFoundException(String message) {
        super(message);
    }
}
